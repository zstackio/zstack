package org.zstack.kvm.memory;

import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import static org.junit.Assert.*;

public class MemoryClusterOperationTest {
    static class Repository extends MemoryRepository {
        final List<MemoryActionRequest> submitted = new ArrayList<>();
        final Map<String, Deque<String>> statuses = new HashMap<>();
        final Map<String, String> reasons = new HashMap<>();
        final Set<String> rejected = new HashSet<>();
        final Set<String> crashed = new HashSet<>();
        final Set<String> unreadable = new HashSet<>();
        @Override public List<String> targets(String scope, String resource) {
            if ("Host".equals(scope)) { return Collections.singletonList(resource); }
            assertEquals("Cluster", scope); assertEquals("cluster", resource);
            return Arrays.asList("host1", "host2");
        }
        final Set<String> drained = new HashSet<>();
        @Override public boolean isDrainedHost(String host) { return drained.contains(host); }
        @Override public String recoverDrain(String host, String actor, MemoryAgentResponse observation) {
            throw new IllegalStateException("database unavailable");
        }
        MemoryAgentResponse preparationObservation;
        @Override String submit(MemoryActionRequest msg, MemoryAgentResponse observation) {
            preparationObservation = observation;
            return submit(msg);
        }
        @Override public String submit(MemoryActionRequest msg) {
            submitted.add(msg);
            if (crashed.contains(msg.getResourceUuid())) { throw new IllegalStateException("database unavailable"); }
            if (rejected.contains(msg.getResourceUuid())) {
                throw new MemoryOperationException("MEMORY_HOST_NOT_CONNECTED", "Host is not connected");
            }
            return "task-" + msg.getResourceUuid();
        }
        /** The queued work the Host's queue claims, in order; the test sets each final status. */
        final Deque<MemoryTaskVO> claims = new ArrayDeque<>();
        final List<String> claimedHosts = new ArrayList<>();
        final List<String> results = new ArrayList<>();
        final List<String> pending = new ArrayList<>();
        final List<String> unconfirmed = new ArrayList<>();
        final List<String> expired = new ArrayList<>();
        @Override public MemoryTaskVO claim(String host) { claimedHosts.add(host); return claims.poll(); }
        @Override public void result(String uuid, String status, String reason, MemoryAgentResponse response) {
            results.add(uuid);
        }
        @Override public void result(String uuid, String status, String reason, MemoryAgentResponse response,
                                     MemoryAgentResponse observation) { results.add(uuid); }
        @Override public List<String> hostsWithPendingWork() { return pending; }
        @Override public List<String> hostsWithUnconfirmedWork() { return unconfirmed; }
        @Override public void expireUnconfirmed(Collection<String> hosts) { expired.addAll(hosts); }
        @Override public List<String> connectedHosts() { return Collections.emptyList(); }
        @Override public List<String> hostsWithState() { return Collections.emptyList(); }
        @Override public int purgeHistory(long now) { return 0; }
        @Override public void setActiveJob(String host, String job, String previous) { }
        @Override MemoryTaskVO findTask(String uuid) {
            if (unreadable.contains(uuid)) { throw new IllegalStateException("database unavailable"); }
            MemoryTaskVO task = new MemoryTaskVO(); task.setUuid(uuid);
            Deque<String> sequence = statuses.get(uuid);
            task.setStatus(sequence.peek());
            task.setReason(reasons.get(uuid));
            return task;
        }
    }

    static class Manager extends MemoryOptimizationManager {
        final List<Integer> progress = new ArrayList<>();
        final List<String> probes = new ArrayList<>();
        final List<String> preparationOutcomes = new ArrayList<>();
        CloudBus hostBus;
        final List<Long> hostTimeouts = new ArrayList<>();
        /** The Host outcome alone; a platform error code needs the running platform. */
        @Override void replyHostOperation(MemoryHostOperationMsg msg, String outcome) {
            preparationOutcomes.add(outcome);
            MemoryClusterOperationReply reply = new MemoryClusterOperationReply();
            reply.setResults(Collections.singletonMap(msg.getHostUuid(), outcome));
            hostBus.reply(msg, reply);
        }
        @Override void reportClusterProgress(int done, int total) { progress.add(100 * done / total); }
        @Override void call(String host, String action, Map<String, Object> command, java.util.function.Consumer<MemoryAgentResponse> completion) {
            probes.add(host + ":" + action);
            MemoryAgentResponse response = new MemoryAgentResponse(); response.setSuccess(true);
            completion.accept(response);
        }
    }

    @Test public void hostPreparationFailsTheJobUnlessPrepared() throws Exception {
        for (String finalStatus : Arrays.asList("Succeeded", "Failed")) {
            Manager manager = new Manager(); Repository repository = new Repository();
            repository.statuses.put("task-host1", new ArrayDeque<>(Collections.singletonList(finalStatus)));
            repository.reasons.put("task-host1", "Failed".equals(finalStatus) ? "WRITEBACK_INVENTORY_IDENTITY_OR_CONTROL_MISMATCH" : null);
            List<MessageReply> replies = new ArrayList<>();
            inject(manager, "repository", repository);
            inject(manager, "bus", bus(replies, manager));
            inject(manager, "thdf", synchronousThreads());
            inject(manager, "pluginRgty", licensedRegistry());

            MemoryBackendPreparation preparation = new MemoryBackendPreparation();
            preparation.setCandidateId("candidate:" + String.join("", Collections.nCopies(64, "a")));
            preparation.setExpectedHostBootId("d339587c-b015-4745-8091-d00521319861");
            preparation.setExpectedPoolGeneration("none");
            preparation.setBackendCapacityBytes(10737418240L); preparation.setResetConfirmed(true);
            MemoryHostOperationMsg msg = new MemoryHostOperationMsg();
            msg.setHostUuid("host1"); msg.setAction("prepareWritebackBackend"); msg.setPreparation(preparation);
            msg.setAccountUuid("account"); msg.setUserUuid("user");
            invokeHostOperation(manager, msg);

            MemoryActionRequest request = repository.submitted.get(0);
            assertEquals("prepareWritebackBackend", request.getAction());
            assertEquals("host1", request.getResourceUuid()); assertEquals("{}", request.getPolicy());
            assertNull("the repository fences on the Host's current control", request.getExpectedControlOperationUuid());
            assertSame(preparation, request.getBackendPreparation());
            assertEquals(Collections.singletonList("host1:writeback-backends"), manager.probes);
            assertNotNull("the fresh probe is the submission evidence", repository.preparationObservation);
            assertEquals(Collections.singletonList("Succeeded".equals(finalStatus) ? "success"
                    : "Failed: WRITEBACK_INVENTORY_IDENTITY_OR_CONTROL_MISMATCH"), manager.preparationOutcomes);
        }
    }

    @Test public void clusterPauseRunsHostByHostAndReportsEachOutcome() throws Exception {
        Manager manager = new Manager(); Repository repository = new Repository();
        repository.statuses.put("task-host1", new ArrayDeque<>(Collections.singletonList("Succeeded")));
        repository.statuses.put("task-host2", new ArrayDeque<>(Collections.singletonList("Failed")));
        repository.reasons.put("task-host2", "KSM_CONTROL_OPERATION_UNKNOWN");
        List<MessageReply> replies = new ArrayList<>();
        inject(manager, "repository", repository);
        inject(manager, "bus", bus(replies, manager));
        inject(manager, "thdf", synchronousThreads());
        inject(manager, "pluginRgty", licensedRegistry());

        MemoryClusterOperationMsg msg = new MemoryClusterOperationMsg();
        msg.setClusterUuid("cluster"); msg.setAction("pause");
        msg.setAccountUuid("account"); msg.setUserUuid("user"); msg.setTimeout(7200000L);
        invoke(manager, msg);

        assertEquals("each Host step may last as long as the job", Arrays.asList(7200000L, 7200000L), manager.hostTimeouts);
        assertEquals(2, repository.submitted.size());
        MemoryActionRequest first = repository.submitted.get(0), second = repository.submitted.get(1);
        assertEquals("host1", first.getResourceUuid()); assertEquals("host2", second.getResourceUuid());
        assertEquals("Host", first.getScope()); assertEquals("pause", first.getAction()); assertEquals("{}", first.getPolicy());
        assertEquals("account", first.getSession().getAccountUuid());
        assertEquals("user", first.getSession().getUserUuid());

        MemoryClusterOperationReply reply = (MemoryClusterOperationReply) replies.get(0);
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("host1", "success"); expected.put("host2", "Failed: KSM_CONTROL_OPERATION_UNKNOWN");
        assertEquals(expected, reply.getResults());
        assertEquals(Arrays.asList(50, 100), manager.progress);
    }

    @Test public void oneHostRejectedAtSubmissionDoesNotStopTheRest() throws Exception {
        Manager manager = new Manager(); Repository repository = new Repository();
        repository.rejected.add("host1");
        repository.statuses.put("task-host2", new ArrayDeque<>(Collections.singletonList("Succeeded")));
        List<MessageReply> replies = new ArrayList<>();
        inject(manager, "repository", repository);
        inject(manager, "bus", bus(replies, manager));
        inject(manager, "thdf", synchronousThreads());
        inject(manager, "pluginRgty", licensedRegistry());

        MemoryClusterOperationMsg msg = new MemoryClusterOperationMsg();
        msg.setClusterUuid("cluster"); msg.setAction("drain");
        msg.setAccountUuid("account"); msg.setUserUuid("user");
        invoke(manager, msg);

        Map<String, String> results = ((MemoryClusterOperationReply) replies.get(0)).getResults();
        assertEquals("MEMORY_HOST_NOT_CONNECTED: Host is not connected", results.get("host1"));
        assertEquals("success", results.get("host2"));
    }

    @Test public void hostRecoveryUsesTheConfirmedUncertainControlNotTheCurrentOne() throws Exception {
        Manager manager = new Manager(); Repository repository = new Repository();
        repository.statuses.put("task-host1", new ArrayDeque<>(Collections.singletonList("Succeeded")));
        inject(manager, "repository", repository);
        inject(manager, "bus", bus(new ArrayList<>(), manager));
        inject(manager, "thdf", synchronousThreads());
        inject(manager, "pluginRgty", licensedRegistry());

        APIRecoverHostMemoryOptimizationMsg api = new APIRecoverHostMemoryOptimizationMsg();
        api.setUuid("host1"); api.setExpectedControlOperationUuid("uncertain-activation");
        api.setExpectedHostBootId("d339587c-b015-4745-8091-d00521319861");
        api.setExpectedPoolGeneration("none"); api.setDrainControlOperationUuid("drain");
        MemoryHostOperationMsg msg = new MemoryHostOperationMsg();
        msg.setHostUuid("host1"); msg.setAction("recoverUncertain");
        msg.setExpectedControlOperationUuid(api.getExpectedControlOperationUuid()); msg.setRecovery(api.toRecovery());
        msg.setAccountUuid("account"); msg.setUserUuid("user");
        invokeHostOperation(manager, msg);

        MemoryActionRequest request = repository.submitted.get(0);
        assertEquals("recoverUncertain", request.getAction()); assertEquals("{}", request.getPolicy());
        assertEquals("uncertain-activation", request.getExpectedControlOperationUuid());
        assertTrue(request.getRecovery().isConfirmed());
        assertEquals("drain", request.getRecovery().getDrainControlOperationUuid());
        assertEquals("none", request.getRecovery().getExpectedPoolGeneration());
        assertTrue("recovery takes no Agent probe", manager.probes.isEmpty());
        assertEquals(Collections.singletonList("success"), manager.preparationOutcomes);
    }

    @Test public void anAgentCallbackFailureStillFinishesTheHostStep() throws Exception {
        Manager manager = new Manager(); Repository repository = new Repository();
        repository.crashed.add("host1"); repository.drained.add("host2");
        List<MessageReply> replies = new ArrayList<>();
        inject(manager, "repository", repository);
        inject(manager, "bus", bus(replies, manager));
        inject(manager, "thdf", synchronousThreads());
        inject(manager, "pluginRgty", licensedRegistry());

        MemoryHostOperationMsg prepare = new MemoryHostOperationMsg();
        prepare.setHostUuid("host1"); prepare.setAction("prepareWritebackBackend");
        prepare.setPreparation(new MemoryBackendPreparation());
        invokeHostOperation(manager, prepare);
        assertEquals(Collections.singletonList("MEMORY_RESULT_UNKNOWN: database unavailable"), manager.preparationOutcomes);

        MemoryClusterOperationMsg resume = new MemoryClusterOperationMsg();
        resume.setClusterUuid("cluster"); resume.setHostUuid("host2"); resume.setAction("resume");
        invoke(manager, resume);
        assertEquals("MEMORY_RESULT_UNKNOWN: database unavailable",
                ((MemoryClusterOperationReply) replies.get(replies.size() - 1)).getResults().get("host2"));
    }

    private static MemoryTaskVO queued(String uuid, String status) {
        MemoryTaskVO task = new MemoryTaskVO(); task.setUuid(uuid); task.setHostUuid("host1");
        task.setScope("Host"); task.setResourceUuid("host1"); task.setAction("pause");
        task.setStatus(status); task.setPolicy("{}"); task.setDesiredRevision(1);
        return task;
    }

    @Test public void hostOperationSendsEveryQueuedStepAndPollsDrainingBeforeTheOutcome() throws Exception {
        Manager manager = new Manager(); Repository repository = new Repository();
        repository.statuses.put("task-host1", new ArrayDeque<>(Collections.singletonList("Succeeded")));
        repository.claims.add(queued("task-host1", "Applying"));
        repository.claims.add(queued("task-host1", "Draining"));
        repository.claims.add(queued("next-step", "Applying"));
        inject(manager, "repository", repository);
        inject(manager, "bus", bus(new ArrayList<>(), manager));
        inject(manager, "thdf", synchronousThreads());
        inject(manager, "pluginRgty", licensedRegistry());

        MemoryHostOperationMsg msg = new MemoryHostOperationMsg();
        msg.setHostUuid("host1"); msg.setAction("pause"); msg.setAccountUuid("account"); msg.setUserUuid("user");
        invokeHostOperation(manager, msg);

        assertEquals(Arrays.asList("host1:apply", "host1:reconcile", "host1:apply"), manager.probes);
        assertEquals(Arrays.asList("task-host1", "task-host1", "next-step"), repository.results);
        assertEquals("the queue stops once nothing is claimable", 4, repository.claimedHosts.size());
        assertEquals(Collections.singletonList("success"), manager.preparationOutcomes);
    }

    @Test public void scanDrivesOnlyOwnedHostsAndExpiresOnlyRequestsWithoutALocalDrive() throws Exception {
        Manager manager = new Manager(); Repository repository = new Repository();
        repository.pending.addAll(Arrays.asList("host1", "host2"));
        repository.unconfirmed.addAll(Arrays.asList("host1", "host2", "host3"));
        inject(manager, "repository", repository);
        inject(manager, "bus", bus(new ArrayList<>(), manager));
        inject(manager, "thdf", synchronousThreads());
        inject(manager, "pluginRgty", licensedRegistry());
        inject(manager, "destMaker", ownedHosts("host1", "host3"));
        Method tick = MemoryOptimizationManager.class.getDeclaredMethod("tick");
        tick.setAccessible(true); tick.invoke(manager);
        assertEquals(Collections.singletonList("host1"), repository.claimedHosts);

        Field active = MemoryOptimizationManager.class.getDeclaredField("activeDrives"); active.setAccessible(true);
        ((Set<String>) active.get(manager)).add("host1");
        manager.nodeLeft(null);
        assertEquals("a Host this node is sending to is not another node's lost request",
                Collections.singletonList("host3"), repository.expired);
    }

    private static org.zstack.core.cloudbus.ResourceDestinationMaker ownedHosts(String... hosts) {
        Set<String> owned = new HashSet<>(Arrays.asList(hosts));
        return (org.zstack.core.cloudbus.ResourceDestinationMaker) Proxy.newProxyInstance(
                org.zstack.core.cloudbus.ResourceDestinationMaker.class.getClassLoader(),
                new Class<?>[]{org.zstack.core.cloudbus.ResourceDestinationMaker.class},
                (proxy, method, args) -> "isManagedByUs".equals(method.getName()) ? owned.contains(args[0])
                        : method.getReturnType() == boolean.class ? false : null);
    }

    private static void invokeHostOperation(MemoryOptimizationManager manager, MemoryHostOperationMsg msg) throws Exception {
        Method method = MemoryOptimizationManager.class.getDeclaredMethod("hostOperation", MemoryHostOperationMsg.class);
        method.setAccessible(true); method.invoke(manager, msg);
    }

    @Test public void anUnexpectedFailureStillFinishesTheHostAndTheJob() throws Exception {
        Manager manager = new Manager(); Repository repository = new Repository();
        repository.crashed.add("host1"); repository.unreadable.add("task-host2");
        List<MessageReply> replies = new ArrayList<>();
        inject(manager, "repository", repository);
        inject(manager, "bus", bus(replies, manager));
        inject(manager, "thdf", synchronousThreads());
        inject(manager, "pluginRgty", licensedRegistry());

        MemoryClusterOperationMsg msg = new MemoryClusterOperationMsg();
        msg.setClusterUuid("cluster"); msg.setAction("pause");
        msg.setAccountUuid("account"); msg.setUserUuid("user");
        invoke(manager, msg);

        assertEquals("the job completes even when no Host step ended normally", 1, replies.size());
        Map<String, String> results = ((MemoryClusterOperationReply) replies.get(0)).getResults();
        assertEquals("Unknown: database unavailable", results.get("host1"));
        assertEquals("Unknown: task status unavailable: database unavailable", results.get("host2"));
        assertEquals(Arrays.asList(50, 100), manager.progress);
    }

    @Test public void aHostOutsideTheClusterIsRejectedBeforeAnySubmission() throws Exception {
        Manager manager = new Manager(); Repository repository = new Repository();
        inject(manager, "repository", repository);
        inject(manager, "bus", bus(new ArrayList<>(), manager));
        inject(manager, "thdf", synchronousThreads());
        inject(manager, "pluginRgty", licensedRegistry());
        MemoryClusterOperationMsg msg = new MemoryClusterOperationMsg();
        msg.setClusterUuid("cluster"); msg.setHostUuid("elsewhere"); msg.setAction("pause");
        try {
            invoke(manager, msg);
            fail("a Host outside the Cluster must be rejected");
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertEquals("MEMORY_INVALID_REQUEST", ((MemoryOperationException) e.getCause()).getCode());
        }
        assertTrue(repository.submitted.isEmpty());
    }

    private static PluginRegistry licensedRegistry() {
        MemoryLicenseExtensionPoint license = () -> System.currentTimeMillis() + 3600000L;
        return (PluginRegistry) Proxy.newProxyInstance(PluginRegistry.class.getClassLoader(), new Class<?>[]{PluginRegistry.class},
                (proxy, method, args) -> "getExtensionList".equals(method.getName()) && args != null && args.length == 1
                        && args[0] == MemoryLicenseExtensionPoint.class
                        ? Collections.singletonList(license) : Collections.emptyList());
    }

    /** Delivers each Host step to the manager, as the Host owner's service, and its reply to the sender. */
    private static CloudBus bus(List<MessageReply> replies, MemoryOptimizationManager manager) {
        Map<Message, CloudBusCallBack> pending = new IdentityHashMap<>();
        CloudBus bus = (CloudBus) Proxy.newProxyInstance(CloudBus.class.getClassLoader(), new Class<?>[]{CloudBus.class},
                (proxy, method, args) -> {
                    if ("send".equals(method.getName()) && args.length == 2 && args[0] instanceof MemoryHostOperationMsg) {
                        pending.put((Message) args[0], (CloudBusCallBack) args[1]);
                        ((Manager) manager).hostTimeouts.add(((MemoryHostOperationMsg) args[0]).getTimeout());
                        manager.handleMessage((Message) args[0]);
                    } else if ("reply".equals(method.getName()) || "replyErrorByMessageType".equals(method.getName())) {
                        MessageReply reply = args[1] instanceof MessageReply ? (MessageReply) args[1] : new MessageReply();
                        if (args[1] instanceof ErrorCode) { reply.setError((ErrorCode) args[1]); }
                        CloudBusCallBack callback = pending.remove(args[0]);
                        if (callback != null) { callback.run(reply); } else { replies.add(reply); }
                    }
                    return method.getReturnType() == boolean.class ? false : null;
                });
        ((Manager) manager).hostBus = bus;
        return bus;
    }

    private static ThreadFacade synchronousThreads() {
        return (ThreadFacade) Proxy.newProxyInstance(ThreadFacade.class.getClassLoader(), new Class<?>[]{ThreadFacade.class},
                (proxy, method, args) -> {
                    if ("chainSubmit".equals(method.getName())) {
                        SyncTaskChain chain = (SyncTaskChain) Proxy.newProxyInstance(SyncTaskChain.class.getClassLoader(),
                                new Class<?>[]{SyncTaskChain.class}, (p, m, a) -> null);
                        ((ChainTask) args[0]).run(chain);
                    } else if ("submitTimeoutTask".equals(method.getName())) {
                        ((Runnable) args[0]).run();
                    }
                    return null;
                });
    }

    private static void invoke(MemoryOptimizationManager manager, MemoryClusterOperationMsg msg) throws Exception {
        Method method = MemoryOptimizationManager.class.getDeclaredMethod("clusterOperation", MemoryClusterOperationMsg.class);
        method.setAccessible(true); method.invoke(manager, msg);
    }

    private static void inject(Object target, String name, Object value) throws Exception {
        Field field = MemoryOptimizationManager.class.getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }
}
