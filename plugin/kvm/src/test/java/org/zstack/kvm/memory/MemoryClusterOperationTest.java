package org.zstack.kvm.memory;

import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
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
        @Override public MemoryPolicyInventory getPolicy(String scope, String resource) {
            MemoryPolicyInventory policy = new MemoryPolicyInventory();
            policy.setRevision("host1".equals(resource) ? 7 : 9);
            policy.setSourceRevisions(Collections.singletonMap("Host:" + resource, 7L));
            return policy;
        }
        @Override public List<MemoryStateVO> allStates(List<String> hosts) {
            MemoryStateVO state = new MemoryStateVO(); state.setHostUuid(hosts.get(0));
            state.setControlOperationUuid("control-" + hosts.get(0));
            return Collections.singletonList(state);
        }
        final Set<String> drained = new HashSet<>();
        @Override public boolean isDrainedHost(String host) { return drained.contains(host); }
        @Override public MemoryTaskInventory recoverDrain(String host, String actor, String requestKey, MemoryAgentResponse observation) {
            throw new IllegalStateException("database unavailable");
        }
        MemoryAgentResponse preparationObservation;
        @Override MemoryTaskInventory submit(MemoryActionRequest msg, MemoryAgentResponse observation) {
            preparationObservation = observation;
            return submit(msg);
        }
        @Override public MemoryTaskInventory submit(MemoryActionRequest msg) {
            submitted.add(msg);
            if (crashed.contains(msg.getResourceUuid())) { throw new IllegalStateException("database unavailable"); }
            if (rejected.contains(msg.getResourceUuid())) {
                throw new MemoryOperationException("MEMORY_HOST_NOT_CONNECTED", "Host is not connected");
            }
            MemoryTaskInventory task = new MemoryTaskInventory(); task.setUuid("task-" + msg.getResourceUuid());
            return task;
        }
        @Override MemoryTaskVO findTask(String uuid) {
            if (unreadable.contains(uuid)) { throw new IllegalStateException("database unavailable"); }
            MemoryTaskVO task = new MemoryTaskVO(); task.setUuid(uuid);
            Deque<String> sequence = statuses.get(uuid);
            task.setStatus(sequence.size() > 1 ? sequence.poll() : sequence.peek());
            task.setReason(reasons.get(uuid));
            return task;
        }
    }

    static class Manager extends MemoryOptimizationManager {
        final List<Integer> progress = new ArrayList<>();
        final List<String> probes = new ArrayList<>();
        final List<String> preparationOutcomes = new ArrayList<>();
        @Override void replyHostOperation(MemoryHostOperationMsg msg, String outcome) { preparationOutcomes.add(outcome); }
        @Override void reportClusterProgress(int done, int total) { progress.add(100 * done / total); }
        @Override void call(String host, String action, Map<String, Object> command, java.util.function.Consumer<MemoryAgentResponse> completion) {
            probes.add(host + ":" + action);
            MemoryAgentResponse response = new MemoryAgentResponse(); response.setSuccess(true);
            completion.accept(response);
        }
    }

    @Test public void hostPreparationFillsCurrentFencesAndFailsTheJobUnlessPrepared() throws Exception {
        for (String finalStatus : Arrays.asList("Succeeded", "Failed")) {
            Manager manager = new Manager(); Repository repository = new Repository();
            repository.statuses.put("task-host1", new ArrayDeque<>(Arrays.asList("Applying", finalStatus)));
            repository.reasons.put("task-host1", "Failed".equals(finalStatus) ? "WRITEBACK_INVENTORY_IDENTITY_OR_CONTROL_MISMATCH" : null);
            List<MessageReply> replies = new ArrayList<>();
            inject(manager, "repository", repository);
            inject(manager, "bus", bus(replies));
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
            assertEquals(7, request.getExpectedRevision());
            assertEquals(Collections.singletonMap("Host:host1", 7L), request.getExpectedSourceRevisions());
            assertEquals("control-host1", request.getExpectedControlOperationUuid());
            assertSame(preparation, request.getBackendPreparation());
            assertEquals(Collections.singletonList("host1:writeback-backends"), manager.probes);
            assertNotNull("the fresh probe is the submission evidence", repository.preparationObservation);
            assertEquals(Collections.singletonList("Succeeded".equals(finalStatus) ? "success"
                    : "Failed: WRITEBACK_INVENTORY_IDENTITY_OR_CONTROL_MISMATCH"), manager.preparationOutcomes);
        }
    }

    @Test public void clusterPauseRunsHostByHostWithCurrentFencesAndReportsEachOutcome() throws Exception {
        Manager manager = new Manager(); Repository repository = new Repository();
        repository.statuses.put("task-host1", new ArrayDeque<>(Arrays.asList("Applying", "Succeeded")));
        repository.statuses.put("task-host2", new ArrayDeque<>(Collections.singletonList("Failed")));
        repository.reasons.put("task-host2", "KSM_CONTROL_OPERATION_UNKNOWN");
        List<MessageReply> replies = new ArrayList<>();
        inject(manager, "repository", repository);
        inject(manager, "bus", bus(replies));
        inject(manager, "thdf", synchronousThreads());
        inject(manager, "pluginRgty", licensedRegistry());

        MemoryClusterOperationMsg msg = new MemoryClusterOperationMsg();
        msg.setClusterUuid("cluster"); msg.setAction("pause");
        msg.setAccountUuid("account"); msg.setUserUuid("user");
        invoke(manager, msg);

        assertEquals(2, repository.submitted.size());
        MemoryActionRequest first = repository.submitted.get(0), second = repository.submitted.get(1);
        assertEquals("host1", first.getResourceUuid()); assertEquals("host2", second.getResourceUuid());
        assertEquals("Host", first.getScope()); assertEquals("pause", first.getAction()); assertEquals("{}", first.getPolicy());
        assertEquals(7, first.getExpectedRevision()); assertEquals(9, second.getExpectedRevision());
        assertEquals("control-host1", first.getExpectedControlOperationUuid());
        assertTrue(MemoryPolicyRules.validUuid(first.getClientRequestUuid()));
        assertNotEquals(first.getClientRequestUuid(), second.getClientRequestUuid());
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
        inject(manager, "bus", bus(replies));
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
        inject(manager, "bus", bus(new ArrayList<>()));
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
        assertEquals(7, request.getExpectedRevision());
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
        inject(manager, "bus", bus(replies));
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
                ((MemoryClusterOperationReply) replies.get(0)).getResults().get("host2"));
    }

    private static void invokeHostOperation(MemoryOptimizationManager manager, MemoryHostOperationMsg msg) throws Exception {
        Method method = MemoryOptimizationManager.class.getDeclaredMethod("singleHostOperation", MemoryHostOperationMsg.class);
        method.setAccessible(true); method.invoke(manager, msg);
    }

    @Test public void anUnexpectedFailureStillFinishesTheHostAndTheJob() throws Exception {
        Manager manager = new Manager(); Repository repository = new Repository();
        repository.crashed.add("host1"); repository.unreadable.add("task-host2");
        List<MessageReply> replies = new ArrayList<>();
        inject(manager, "repository", repository);
        inject(manager, "bus", bus(replies));
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
        inject(manager, "bus", bus(new ArrayList<>()));
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

    private static CloudBus bus(List<MessageReply> replies) {
        return (CloudBus) Proxy.newProxyInstance(CloudBus.class.getClassLoader(), new Class<?>[]{CloudBus.class},
                (proxy, method, args) -> {
                    if ("reply".equals(method.getName())) { replies.add((MessageReply) args[1]); }
                    return method.getReturnType() == boolean.class ? false : null;
                });
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
