package org.zstack.kvm.memory;

import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.Consumer;
import static org.junit.Assert.*;

public class MemoryClusterOperationTest {
    static final String BACKEND = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    /** One generation per control change; a task finishes with the Agent result for it. */
    static class Repository extends MemoryRepository {
        final Map<String, Long> generations = new HashMap<>();
        final Map<String, String> controls = new HashMap<>();
        final Map<String, MemoryTaskVO> tasks = new LinkedHashMap<>();
        final List<String> plans = new ArrayList<>();
        final Set<String> rejected = new HashSet<>();
        final Set<String> drained = new HashSet<>();
        final Set<String> disconnected = new HashSet<>();
        @Override public List<String> connectedHosts() {
            List<String> hosts = new ArrayList<>(Arrays.asList("host1", "host2"));
            hosts.removeAll(disconnected);
            return hosts;
        }
        String preparedBackend;
        long preparedCapacity;

        @Override public List<String> targets(String scope, String resource) {
            if ("Host".equals(scope)) {
                if (rejected.contains(resource)) {
                    throw new MemoryOperationException("MEMORY_HOST_NOT_CONNECTED", "Host is not connected");
                }
                return Collections.singletonList(resource);
            }
            assertEquals("Cluster", scope); assertEquals("cluster", resource);
            return Arrays.asList("host1", "host2");
        }
        int failingTargets;
        final Set<String> zramOff = new HashSet<>();
        @Override public List<String> standardConfigurationTargets(String resource) {
            if (failingTargets > 0) { failingTargets--; throw new RuntimeException("database unavailable"); }
            return Arrays.asList("host1", "host2");
        }
        @Override public boolean wantsZram(String host) { return !zramOff.contains(host); }
        @Override public List<String> hostsWithState() { return Arrays.asList("host1", "host2"); }
        @Override public boolean isDrained(String host) { return drained.contains(host); }
        final Set<String> withoutPool = new HashSet<>();
        @Override public boolean hasPool(String host) { return !withoutPool.contains(host); }
        final Set<String> failingPlans = new HashSet<>();
        @Override public Planned plan(String host, String control, String action, String actor, boolean licensed, boolean operation) {
            if (failingPlans.contains(host)) { throw new RuntimeException("database unavailable"); }
            plans.add(host + ":" + action + ":" + control);
            long generation = generations.getOrDefault(host, 0L);
            if (generation == 0 || control != null && !control.equals(controls.get(host))) {
                generations.put(host, ++generation);
                if (control != null) { controls.put(host, control); }
            }
            MemoryTaskVO task = new MemoryTaskVO();
            task.setUuid("task-" + host + "-" + tasks.size()); task.setHostUuid(host); task.setAction(action);
            task.setStatus(CONVERGING); task.setDesiredRevision(generation);
            tasks.put(task.getUuid(), task);
            return new Planned(generation, task.getUuid());
        }
        @Override public Delivery pending(String host) {
            boolean waiting = tasks.values().stream().anyMatch(t -> host.equals(t.getHostUuid()) && CONVERGING.equals(t.getStatus()));
            if (!waiting) { return null; }
            Map<String, Object> desired = new LinkedHashMap<>();
            desired.put("control", controls.getOrDefault(host, "active"));
            return new Delivery(host, generations.get(host), desired);
        }
        @Override public Outcome record(String host, long generation, MemoryAgentResponse response) {
            String status = response == null || response.status == null ? CONVERGING : response.status;
            if (!CONVERGING.equals(status)) {
                for (MemoryTaskVO task : tasks.values()) {
                    if (host.equals(task.getHostUuid()) && CONVERGING.equals(task.getStatus())) {
                        task.setStatus(status); task.setReason(response.reasonCode);
                    }
                }
            }
            return new Outcome(status, response == null ? null : response.reasonCode, generation);
        }
        @Override public Map<String, MemoryTaskVO> tasks(Collection<String> uuids) {
            Map<String, MemoryTaskVO> result = new HashMap<>();
            for (String uuid : uuids) { if (tasks.containsKey(uuid)) { result.put(uuid, tasks.get(uuid)); } }
            return result;
        }
        @Override public String startTask(String host, String action, String actor, long generation) { return "prepare"; }
        @Override public void finishTask(String uuid, String status, String reason) { }
        @Override public void recordPreparedBackend(String host, String resource, long capacity) {
            preparedBackend = resource; preparedCapacity = capacity;
        }
        @Override public void setActiveJob(String host, String job, String previous) { }
    }

    /** Agent replies are scripted per action; an unscripted call is held until the test answers it. */
    static class Manager extends MemoryOptimizationManager {
        final List<Integer> progress = new ArrayList<>();
        final List<Map<String, Object>> commands = new ArrayList<>();
        final Map<String, Deque<MemoryAgentResponse>> replies = new HashMap<>();
        final Deque<Consumer<MemoryAgentResponse>> held = new ArrayDeque<>();
        final List<String> outcomes = new ArrayList<>();
        CloudBus hostBus;
        final List<Long> hostTimeouts = new ArrayList<>();

        @Override void replyHostOperation(MemoryHostOperationMsg msg, String outcome) {
            outcomes.add(outcome);
            MemoryClusterOperationReply reply = new MemoryClusterOperationReply();
            reply.setResults(Collections.singletonMap(msg.getHostUuid(), outcome));
            hostBus.reply(msg, reply);
        }
        @Override void reportClusterProgress(int done, int total) {
            progress.add(100 * done / total);
            if (cancelAfter == done) { canceled = true; }
        }
        final Deque<Runnable> deferred = new ArrayDeque<>();
        @Override void later(Runnable task, long seconds) { deferred.add(task); }
        int cancelAfter = -1;
        boolean canceled;
        @Override boolean jobCanceled(String job) { return canceled; }
        @Override void call(String host, String action, Map<String, Object> command, Consumer<MemoryAgentResponse> completion) {
            Map<String, Object> recorded = new LinkedHashMap<>(command); recorded.put("path", action);
            commands.add(recorded);
            Deque<MemoryAgentResponse> scripted = replies.get(host + ":" + action);
            if (scripted == null || scripted.isEmpty()) { held.add(completion); return; }
            completion.accept(scripted.poll());
        }
        void script(String host, String action, MemoryAgentResponse... responses) {
            replies.computeIfAbsent(host + ":" + action, k -> new ArrayDeque<>()).addAll(Arrays.asList(responses));
        }
    }

    private static MemoryAgentResponse reply(String status, String reason, Long retryAfter) {
        MemoryAgentResponse response = new MemoryAgentResponse();
        response.setSuccess(!"Failed".equals(status));
        response.status = status; response.reasonCode = reason; response.retryAfterSeconds = retryAfter;
        response.state = new LinkedHashMap<>();
        return response;
    }

    private Manager manager;
    private Repository repository;
    private final List<MessageReply> replies = new ArrayList<>();
    private long licenseDeadline;

    private void setup() throws Exception {
        manager = new Manager(); repository = new Repository();
        inject(manager, "repository", repository);
        inject(manager, "bus", bus(replies, manager));
        inject(manager, "thdf", synchronousThreads());
        licenseDeadline = Long.MAX_VALUE;
        inject(manager, "pluginRgty", licensedRegistry(() -> licenseDeadline));
        inject(manager, "destMaker", Proxy.newProxyInstance(ResourceDestinationMaker.class.getClassLoader(),
                new Class<?>[]{ResourceDestinationMaker.class}, (proxy, method, args) -> true));
    }

    private static MemoryClusterOperationMsg cluster(String action) {
        MemoryClusterOperationMsg msg = new MemoryClusterOperationMsg();
        msg.setClusterUuid("cluster"); msg.setAction(action);
        msg.setAccountUuid("account"); msg.setUserUuid("user"); msg.setTimeout(7200000L);
        return msg;
    }

    @Test public void clusterPauseSendsEachHostItsGenerationAndReportsEachOutcome() throws Exception {
        setup();
        manager.script("host1", "apply", reply("Succeeded", null, null));
        manager.script("host2", "apply", reply("Failed", "KSM_NATIVE_PROFILE_UNSUPPORTED", null));
        invoke(manager, cluster("pause"));

        assertEquals("each Host step may last as long as the job", Arrays.asList(7200000L, 7200000L), manager.hostTimeouts);
        assertEquals(Arrays.asList("host1:pause:paused", "host2:pause:paused"), repository.plans);
        Map<String, Object> apply = manager.commands.get(0);
        assertEquals("apply", apply.get("path"));
        assertEquals("host1", apply.get("hostUuid"));
        assertEquals(1L, apply.get("generation"));
        assertEquals("paused", ((Map<?, ?>) apply.get("desired")).get("control"));
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("host1", "success"); expected.put("host2", "Failed: KSM_NATIVE_PROFILE_UNSUPPORTED");
        assertEquals(expected, ((MemoryClusterOperationReply) replies.get(0)).getResults());
        assertEquals(Arrays.asList(50, 100), manager.progress);
    }

    @Test public void convergingHostIsSentTheSameGenerationAgainUntilApplied() throws Exception {
        setup();
        manager.script("host1", "apply", reply("Converging", "DRAINING", 5L), reply("Succeeded", null, null));
        MemoryHostOperationMsg msg = new MemoryHostOperationMsg();
        msg.setHostUuid("host1"); msg.setAction("drain"); msg.setAccountUuid("account"); msg.setUserUuid("user");
        msg.setTimeout(7200000L);
        manager.hostBus = bus(replies, manager);
        invokeHostOperation(manager, msg);
        assertTrue("a converging Host has not finished the operation", manager.outcomes.isEmpty());

        Method deliver = MemoryOptimizationManager.class.getDeclaredMethod("deliver", String.class);
        deliver.setAccessible(true); deliver.invoke(manager, "host1");
        assertEquals(Collections.singletonList("success"), manager.outcomes);
        assertEquals(2, manager.commands.size());
        assertEquals(manager.commands.get(0).get("generation"), manager.commands.get(1).get("generation"));
        assertEquals("drained", ((Map<?, ?>) manager.commands.get(1).get("desired")).get("control"));
    }

    @Test public void aCanceledClusterOperationLeavesTheRemainingHostsAlone() throws Exception {
        setup();
        manager.cancelAfter = 1;
        manager.script("host1", "apply", reply("Succeeded", null, null));
        MemoryClusterOperationMsg msg = cluster("drain"); msg.setJobUuid("job");
        invoke(manager, msg);
        assertEquals(Collections.singletonList("host1:drain:drained"), repository.plans);
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("host1", "success"); expected.put("host2", "Canceled");
        assertEquals(expected, ((MemoryClusterOperationReply) replies.get(0)).getResults());
    }

    @Test public void aHostWithoutAPoolIsHeldPausedInsteadOfDrained() throws Exception {
        setup();
        repository.withoutPool.add("host2");
        manager.script("host1", "apply", reply("Succeeded", null, null));
        manager.script("host2", "apply", reply("Succeeded", null, null));
        invoke(manager, cluster("drain"));
        assertEquals("drained", repository.controls.get("host1"));
        // Nothing to withdraw, and no pool forms while it is paused.
        assertEquals("paused", repository.controls.get("host2"));
    }

    @Test public void aHostWithAnApplyInFlightIsNotSentASecondOne() throws Exception {
        setup();
        MemoryHostOperationMsg msg = new MemoryHostOperationMsg();
        msg.setHostUuid("host1"); msg.setAction("pause"); msg.setAccountUuid("account"); msg.setUserUuid("user");
        manager.hostBus = bus(replies, manager);
        invokeHostOperation(manager, msg);
        Method deliver = MemoryOptimizationManager.class.getDeclaredMethod("deliver", String.class);
        deliver.setAccessible(true); deliver.invoke(manager, "host1");
        assertEquals(1, manager.commands.size());
        manager.held.poll().accept(reply("Succeeded", null, null));
        assertEquals(Collections.singletonList("success"), manager.outcomes);
    }

    @Test public void aDisconnectedHostDoesNotHoldTheClusterOperation() throws Exception {
        setup();
        repository.disconnected.add("host2");
        manager.script("host1", "apply", reply("Succeeded", null, null));
        invoke(manager, cluster("pause"));
        Map<String, String> results = ((MemoryClusterOperationReply) replies.get(0)).getResults();
        assertEquals("success", results.get("host1"));
        assertEquals("MEMORY_RESULT_CONVERGING: HOST_DISCONNECTED", results.get("host2"));
        assertEquals("the Host still has its generation to receive", "paused", repository.controls.get("host2"));
    }

    @Test public void oneHostRejectedAtSubmissionDoesNotStopTheRest() throws Exception {
        setup();
        repository.rejected.add("host1");
        manager.script("host2", "apply", reply("Succeeded", null, null));
        invoke(manager, cluster("drain"));
        Map<String, String> results = ((MemoryClusterOperationReply) replies.get(0)).getResults();
        assertEquals("MEMORY_HOST_NOT_CONNECTED: Host is not connected", results.get("host1"));
        assertEquals("success", results.get("host2"));
    }

    @Test public void unreachableAgentKeepsTheOperationWaiting() throws Exception {
        setup();
        MemoryHostOperationMsg msg = new MemoryHostOperationMsg();
        msg.setHostUuid("host1"); msg.setAction("resume"); msg.setAccountUuid("account"); msg.setUserUuid("user");
        manager.hostBus = bus(replies, manager);
        invokeHostOperation(manager, msg);
        manager.held.poll().accept(null);
        assertTrue(manager.outcomes.isEmpty());
        assertEquals("host1:resume:active", repository.plans.get(0));
    }

    private MemoryHostOperationMsg preparation() {
        MemoryBackendPreparation preparation = new MemoryBackendPreparation();
        preparation.setCandidateId("candidate:" + String.join("", Collections.nCopies(64, "a")));
        preparation.setExpectedHostBootId("d339587c-b015-4745-8091-d00521319861");
        preparation.setExpectedPoolGeneration("none");
        preparation.setBackendCapacityBytes(10737418240L); preparation.setResetConfirmed(true);
        MemoryHostOperationMsg msg = new MemoryHostOperationMsg();
        msg.setHostUuid("host1"); msg.setAction("prepareWritebackBackend"); msg.setPreparation(preparation);
        msg.setAccountUuid("account"); msg.setUserUuid("user"); msg.setTimeout(7200000L);
        return msg;
    }

    @Test public void preparationIsResentUntilTheBackendIsBoundToTheHost() throws Exception {
        setup();
        MemoryAgentResponse done = reply("Succeeded", null, null);
        done.state.put("maintenance", Collections.singletonMap("backend_resource_uuid", BACKEND));
        manager.script("host1", "writeback-backends", reply("Converging", "BACKEND_PREPARING", 1L), done);
        manager.hostBus = bus(replies, manager);
        invokeHostOperation(manager, preparation());

        assertEquals(Collections.singletonList("success"), manager.outcomes);
        assertEquals(BACKEND, repository.preparedBackend);
        assertEquals(10737418240L, repository.preparedCapacity);
        Map<String, Object> first = manager.commands.get(0), second = manager.commands.get(1);
        assertEquals("prepare", first.get("action"));
        assertEquals(first.get("generation"), second.get("generation"));
        assertEquals("candidate:" + String.join("", Collections.nCopies(64, "a")), first.get("candidateId"));
        assertEquals(true, first.get("resetConfirmed"));
        assertTrue(repository.plans.contains("host1:prepareWritebackBackend:null"));
        assertEquals("the prepared backend reaches the Host in its next generation", "apply",
                manager.commands.get(manager.commands.size() - 1).get("path"));
    }

    @Test public void aPreparationWaitsForTheApplyInFlight() throws Exception {
        setup();
        MemoryHostOperationMsg pause = new MemoryHostOperationMsg();
        pause.setHostUuid("host1"); pause.setAction("pause"); pause.setAccountUuid("account"); pause.setUserUuid("user");
        manager.hostBus = bus(replies, manager);
        invokeHostOperation(manager, pause);
        assertEquals(1, manager.commands.size());
        MemoryAgentResponse prepared = reply("Succeeded", null, null);
        prepared.state.put("maintenance", Collections.singletonMap("backend_resource_uuid", BACKEND));
        manager.script("host1", "writeback-backends", prepared);
        invokeHostOperation(manager, preparation());
        assertEquals("nothing is prepared while an apply is in flight", 1, manager.commands.size());
        manager.held.poll().accept(reply("Succeeded", null, null));
        manager.deferred.poll().run();
        assertEquals("prepare", manager.commands.get(1).get("action"));
    }

    @Test public void preparationWithoutAReportedBackendFails() throws Exception {
        setup();
        manager.script("host1", "writeback-backends", reply("Succeeded", null, null));
        manager.hostBus = bus(replies, manager);
        invokeHostOperation(manager, preparation());
        assertEquals(Collections.singletonList("Failed: BACKEND_RESOURCE_UNREPORTED"), manager.outcomes);
        assertNull(repository.preparedBackend);
    }

    @Test public void aPreparationTheAgentRejectsIsAnInvalidPreparation() throws Exception {
        setup();
        manager.script("host1", "writeback-backends", reply("Failed", "WRITEBACK_CANDIDATE_NOT_UNIQUE", null));
        manager.hostBus = bus(replies, manager);
        invokeHostOperation(manager, preparation());
        assertEquals(Collections.singletonList("MEMORY_BACKEND_PREPARATION_INVALID: WRITEBACK_CANDIDATE_NOT_UNIQUE"),
                manager.outcomes);
    }

    @Test public void aPreparationTheAgentCouldNotCompleteIsAFailedOperation() throws Exception {
        setup();
        manager.script("host1", "writeback-backends", reply("Failed", "BACKEND_MAINTENANCE_FAILED", null));
        manager.hostBus = bus(replies, manager);
        invokeHostOperation(manager, preparation());
        assertEquals(Collections.singletonList("Failed: BACKEND_MAINTENANCE_FAILED"), manager.outcomes);
    }

    @Test public void reenablingZramResumesOnlyDrainedHosts() throws Exception {
        setup();
        repository.drained.add("host1");
        manager.standardConfigurationChanged("cluster", true);
        assertEquals(Arrays.asList("host1:resume:active", "host2:configure:null"), repository.plans);
        repository.plans.clear();
        manager.standardConfigurationChanged("cluster", false);
        assertEquals(Arrays.asList("host1:configure:null", "host2:configure:null"), repository.plans);
    }

    @Test public void aConfigurationChangeAHostCouldNotPlanIsPlannedByTheNextScan() throws Exception {
        setup();
        repository.drained.add("host1");
        repository.failingPlans.add("host1");
        manager.standardConfigurationChanged("cluster", true);
        assertEquals(Collections.singletonList("host2:configure:null"), repository.plans);
        repository.failingPlans.clear(); repository.plans.clear();
        // A connected Host does not wait for a reconnect; the retry still resumes the drained Host.
        manager.planUnplanned();
        assertEquals(Collections.singletonList("host1:resume:active"), repository.plans);
        repository.plans.clear();
        manager.planUnplanned();
        assertTrue(repository.plans.isEmpty());
    }

    @Test public void aGlobalChangeIsPlannedAndRetriedLikeAnyOther() throws Exception {
        setup();
        repository.failingTargets = 1;
        manager.standardConfigurationChanged(null, false);
        assertTrue(repository.plans.isEmpty());
        manager.planUnplanned();
        assertEquals(Arrays.asList("host1:configure:null", "host2:configure:null"), repository.plans);
    }

    @Test public void aWaitingReenableOfTheResourceIsCarriedByItsNextChange() throws Exception {
        setup();
        repository.drained.add("host1");
        repository.failingTargets = 1;
        manager.standardConfigurationChanged("cluster", true);
        manager.standardConfigurationChanged("cluster", false);
        assertEquals(Arrays.asList("host1:resume:active", "host2:configure:null"), repository.plans);
    }

    @Test public void aLaterZramDisableKeepsTheDrainedHostDrained() throws Exception {
        setup();
        repository.drained.add("host1");
        repository.failingPlans.add("host1");
        manager.standardConfigurationChanged("cluster", true);
        repository.failingPlans.clear(); repository.plans.clear();
        repository.zramOff.add("host1");
        manager.planUnplanned();
        assertEquals(Collections.singletonList("host1:configure:null"), repository.plans);
    }

    @Test public void anExpiredOrRenewedLicenseIsPlannedAtTheNextScan() throws Exception {
        setup();
        manager.planLicenseChange();
        assertTrue("the first scan only notes the license", repository.plans.isEmpty());
        licenseDeadline = 0;
        manager.planLicenseChange();
        assertEquals(Arrays.asList("host1:configure:null", "host2:configure:null"), repository.plans);
        repository.plans.clear();
        manager.planLicenseChange();
        assertTrue(repository.plans.isEmpty());
        licenseDeadline = Long.MAX_VALUE;
        manager.planLicenseChange();
        assertEquals(Arrays.asList("host1:configure:null", "host2:configure:null"), repository.plans);
    }

    @Test public void aHostOutsideTheClusterIsRejectedBeforeAnyPlan() throws Exception {
        setup();
        MemoryClusterOperationMsg msg = cluster("pause"); msg.setHostUuid("elsewhere");
        try {
            invoke(manager, msg);
            fail("a Host outside the Cluster must be rejected");
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertEquals("MEMORY_INVALID_REQUEST", ((MemoryOperationException) e.getCause()).getCode());
        }
        assertTrue(repository.plans.isEmpty());
    }

    private static void invokeHostOperation(MemoryOptimizationManager manager, MemoryHostOperationMsg msg) throws Exception {
        Method method = MemoryOptimizationManager.class.getDeclaredMethod("hostOperation", MemoryHostOperationMsg.class);
        method.setAccessible(true); method.invoke(manager, msg);
    }

    private static PluginRegistry licensedRegistry(java.util.function.LongSupplier deadline) {
        MemoryLicenseExtensionPoint license = deadline::getAsLong;
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
                    if ("submitTimeoutTask".equals(method.getName())) { ((Runnable) args[0]).run(); }
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
