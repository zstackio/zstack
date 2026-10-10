package org.zstack.kvm.memory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;
import org.zstack.core.componentloader.PluginRegistry;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.Consumer;

import static org.junit.Assert.*;

/**
 * Contract tests for configuration-only Cloud License handling.
 *
 * These tests intentionally exercise the management-side contract only:
 * Agent commands must remain ordinary memory commands and an expired License
 * must not tear down an already applied runtime.
 */
public class MemoryLicenseConfigurationTest {
    private static final String HOST = "0123456789abcdef0123456789abcdef";
    private static final String TASK = "abcdef0123456789abcdef0123456789";

    private static void inject(Object target, String name, Object value) throws Exception {
        Field field = MemoryOptimizationManager.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static PluginRegistry licenseRegistry(long deadline) {
        MemoryLicenseExtensionPoint provider = () -> deadline;
        return (PluginRegistry) Proxy.newProxyInstance(
                PluginRegistry.class.getClassLoader(), new Class<?>[]{PluginRegistry.class},
                (proxy, method, args) -> {
                    if ("getExtensionList".equals(method.getName())) {
                        return Collections.singletonList(provider);
                    }
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == int.class) return 0;
                    if (method.getReturnType() == long.class) return 0L;
                    return null;
                });
    }

    @Test
    public void capabilitiesExposeOnlyFourKsmBusinessFields() throws Exception {
        RecordingManager manager = new RecordingManager();
        inject(manager, "repository", new RecordingRepository());
        inject(manager, "pluginRgty", licenseRegistry(System.currentTimeMillis() + 300_000));
        MemoryPolicyInventory inventory = new MemoryPolicyInventory();
        inventory.setScope("Global"); inventory.setResourceUuid("global");
        Method describe = MemoryOptimizationManager.class.getDeclaredMethod("describe", MemoryPolicyInventory.class);
        describe.setAccessible(true); describe.invoke(manager, inventory);
        Set<String> actual = new HashSet<>();
        for (String name : inventory.getFieldCapabilities().keySet()) {
            if (name.startsWith("ksm.")) { actual.add(name); }
        }
        assertEquals(new HashSet<>(Arrays.asList("ksm.enabled", "ksm.zeroPagesEnabled",
                "ksm.pagesToScan", "ksm.sleepMillis")), actual);
    }

    @Test
    public void everyPublicPolicyFieldHasDiscoveryMetadata() throws Exception {
        RecordingManager manager = new RecordingManager();
        inject(manager, "repository", new RecordingRepository());
        inject(manager, "pluginRgty", licenseRegistry(System.currentTimeMillis() + 300_000));
        MemoryPolicyInventory inventory = new MemoryPolicyInventory();
        inventory.setScope("Global"); inventory.setResourceUuid("global");
        Method describe = MemoryOptimizationManager.class.getDeclaredMethod("describe", MemoryPolicyInventory.class);
        describe.setAccessible(true); describe.invoke(manager, inventory);
        for (java.lang.reflect.Field field : MemoryPolicyConfig.class.getFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) { continue; }
            if (field.getType().getName().equals(MemoryPolicyConfig.Ksm.class.getName())
                    || field.getType().getName().equals(MemoryPolicyConfig.Zram.class.getName())
                    || field.getType().getName().equals(MemoryPolicyConfig.Writeback.class.getName())) {
                for (java.lang.reflect.Field nested : field.getType().getFields()) {
                    assertTrue(field.getName() + "." + nested.getName(),
                            inventory.getFieldCapabilities().containsKey(field.getName() + "." + nested.getName()));
                }
            } else if (!"participation".equals(field.getName())) {
                assertTrue(field.getName(), inventory.getFieldCapabilities().containsKey(field.getName()));
            }
        }
        assertTrue(inventory.getFieldCapabilities().get("zram.logicalCapacityBytes").basicUi);
        assertTrue(inventory.getFieldCapabilities().get("zram.ramLimitBytes").basicUi);
        assertFalse(inventory.getFieldCapabilities().get("zram.selectionMode").basicUi);
        assertEquals("pages", inventory.getFieldCapabilities().get("ksm.pagesToScan").unit);
        assertEquals("milliseconds", inventory.getFieldCapabilities().get("ksm.sleepMillis").unit);
        assertFalse(inventory.getFieldCapabilities().get("ksm.pagesToScan").basicUi);
        assertFalse(inventory.getFieldCapabilities().get("ksm.sleepMillis").basicUi);
        assertFalse(inventory.getFieldCapabilities().containsKey("participation"));

        inventory.setScope("VM");
        describe.invoke(manager, inventory);
        assertTrue(inventory.getFieldCapabilities().containsKey("participation"));
    }

    @Test
    public void applyDoesNotPersistOrSendAnAgentLicensePermit() throws Exception {
        RecordingRepository repository = new RecordingRepository();
        RecordingManager manager = new RecordingManager();
        inject(manager, "repository", repository);
        inject(manager, "pluginRgty", licenseRegistry(System.currentTimeMillis() + 300_000));

        MemoryTaskVO task = new MemoryTaskVO();
        task.setUuid(TASK);
        task.setHostUuid(HOST);
        task.setScope("Host");
        task.setResourceUuid(HOST);
        task.setAction("apply");
        task.setPolicy("{}");
        task.setDesiredRevision(1L);

        Method dispatch = MemoryOptimizationManager.class.getDeclaredMethod(
                "dispatch", MemoryTaskVO.class, boolean.class);
        dispatch.setAccessible(true);
        dispatch.invoke(manager, task, false);

        assertEquals("Succeeded", repository.resultStatus);
        assertEquals("No task permit must be persisted", 0, repository.recordTaskPermitCalls);
        assertEquals(HOST, manager.command.get("hostUuid"));
        assertEquals(TASK, manager.command.get("operationUuid"));
        assertFalse("Agent command must not contain commercial authentication", manager.command.containsKey("licensePermit"));
    }

    @Test
    public void ordinaryApplyUsesEffectiveModeForNullAndEmptyPlanHashes() throws Exception {
        String fullPolicy = "{\"schemaVersion\":1,\"ksm\":{\"enabled\":true,\"zeroPagesEnabled\":false,\"pagesToScan\":64,\"sleepMillis\":20}}";
        for (String planHash : Arrays.asList(null, "")) {
            RecordingRepository repository = new RecordingRepository();
            RecordingManager manager = manager(repository, System.currentTimeMillis() + 300_000);
            MemoryTaskVO task = task("apply");
            task.setDesiredRevision(7L);
            task.setPolicy(fullPolicy);
            task.setPolicyPlanHash(planHash);

            dispatch(manager, task, false);

            JsonObject wire = wireCommand(manager.command);
            assertPolicyMode(wire, "effective");
            assertEquals("apply", wire.get("action").getAsString());
            assertDispatchIdentity(wire, task, 7L);
            assertEquals(new JsonParser().parse(fullPolicy).getAsJsonObject(), wire.getAsJsonObject("policy"));
        }
    }

    @Test
    public void clearOverrideUsesEffectiveModeAndKeepsApplyWireActionAndGsonKsmShape() throws Exception {
        List<String> policies = Arrays.asList(
                "{\"schemaVersion\":1,\"ksm\":{}}",
                "{\"schemaVersion\":1}");
        for (String policy : policies) {
            RecordingRepository repository = new RecordingRepository();
            RecordingManager manager = manager(repository, System.currentTimeMillis() + 300_000);
            MemoryTaskVO task = task("clearOverride");
            task.setDesiredRevision(9L);
            task.setPolicy(policy);

            dispatch(manager, task, false);

            JsonObject wire = wireCommand(manager.command);
            assertPolicyMode(wire, "effective");
            assertEquals("apply", wire.get("action").getAsString());
            assertDispatchIdentity(wire, task, 9L);
            assertEquals(new JsonParser().parse(policy).getAsJsonObject(), wire.getAsJsonObject("policy"));
        }
    }

    @Test
    public void bootstrapAndConnectHostPlanTasksUseDeltaRegardlessOfFullPolicyBody() throws Exception {
        for (String actor : Arrays.asList("system:memory-fresh-cloud-bootstrap", "system:memory-host-connect-policy")) {
            RecordingRepository repository = new RecordingRepository();
            RecordingManager manager = manager(repository, System.currentTimeMillis() + 300_000);
            MemoryTaskVO task = task("apply");
            task.setParentUuid("11111111111111111111111111111111");
            task.setActorUuid(actor);
            task.setDesiredRevision(11L);
            task.setPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":true,\"pagesToScan\":128}}");
            task.setPolicyPlanHash("persisted-plan-hash");

            dispatch(manager, task, false);

            JsonObject wire = wireCommand(manager.command);
            assertPolicyMode(wire, "delta");
            assertEquals("apply", wire.get("action").getAsString());
            assertDispatchIdentity(wire, task, 11L);
            assertEquals(new JsonParser().parse(task.getPolicy()).getAsJsonObject(), wire.getAsJsonObject("policy"));
        }
    }

    @Test
    public void repeatedDispatchKeepsModeFromTaskEvenWhenLiveStatePlanHashChanges() throws Exception {
        RecordingRepository repository = new RecordingRepository();
        MemoryStateVO state = new MemoryStateVO();
        state.setHostUuid(HOST);
        state.setPolicyPlanHash("live-state-plan-hash");
        repository.states = Collections.singletonList(state);
        RecordingManager manager = manager(repository, System.currentTimeMillis() + 300_000);
        MemoryTaskVO task = task("apply");
        task.setPolicyPlanHash("persisted-task-plan-hash");

        dispatch(manager, task, false);
        state.setPolicyPlanHash(null);
        dispatch(manager, task, true);

        assertEquals(2, manager.commands.size());
        assertPolicyMode(wireCommand(manager.commands.get(0)), "delta");
        assertPolicyMode(wireCommand(manager.commands.get(1)), "delta");
    }

    @Test
    public void safetyAndReconcileDispatchDoNotCarryPolicyMode() throws Exception {
        RecordingRepository repository = new RecordingRepository();
        RecordingManager manager = manager(repository, System.currentTimeMillis() + 300_000);
        MemoryTaskVO pause = task("pause");
        pause.setPolicyPlanHash("must-not-enable-mode-for-safety-action");
        dispatch(manager, pause, false);
        assertFalse(manager.command.containsKey("policyMode"));

        MemoryTaskVO reconcile = task("reconcile");
        reconcile.setPolicyPlanHash("must-not-enable-mode-for-reconcile");
        dispatch(manager, reconcile, false);
        assertFalse(manager.command.containsKey("policyMode"));
    }

    @Test
    public void expiredLicenseDoesNotDisableOrRenewAlreadyAppliedRuntime() throws Exception {
        RecordingRepository repository = new RecordingRepository();
        MemoryStateVO state = new MemoryStateVO();
        state.setHostUuid(HOST);
        state.setPermitAuthorized(true);
        state.setPermitDeadline(System.currentTimeMillis() - 1);
        repository.states = Collections.singletonList(state);
        RecordingManager manager = new RecordingManager();
        inject(manager, "repository", repository);
        inject(manager, "pluginRgty", licenseRegistry(System.currentTimeMillis() - 1));
        inject(manager, "lifecycleHooks", new MemoryLifecycleHooks() {
            @Override void reconcileObservedBindings(String host, MemoryAgentResponse response) { }
        });

        Method observe = MemoryOptimizationManager.class.getDeclaredMethod("observeRuntime");
        observe.setAccessible(true);
        observe.invoke(manager);

        assertEquals("Expired License must not issue a runtime disable", 0, repository.authorizePermitCalls);
        assertEquals("Expired License must not trigger a lease renewal", 0, manager.leaseCalls);
    }

    @Test
    public void missingHostEvidenceRequiresPreflightAndDoesNotAdvertiseLifecycleExecution() throws Exception {
        RecordingRepository repository = new RecordingRepository();
        RecordingManager manager = new RecordingManager();
        inject(manager, "repository", repository);
        inject(manager, "pluginRgty", licenseRegistry(System.currentTimeMillis() + 300_000));
        MemoryPolicyInventory inventory = new MemoryPolicyInventory();
        inventory.setScope("Host"); inventory.setResourceUuid(HOST);
        Method describe = MemoryOptimizationManager.class.getDeclaredMethod("describe", MemoryPolicyInventory.class);
        describe.setAccessible(true);
        describe.invoke(manager, inventory);
        assertTrue(inventory.getAllowedActions().containsAll(Arrays.asList(
                "preview", "apply", "clearOverride", "prepareWritebackBackend", "prepareZramPool")));
        assertTrue(inventory.getPreflightRequiredActions().containsAll(Arrays.asList(
                "apply", "clearOverride", "prepareWritebackBackend", "prepareZramPool")));
        assertTrue(inventory.getAllowedActions().containsAll(inventory.getPreflightRequiredActions()));
        assertFalse(inventory.getAllowedActions().contains("resume"));
        assertFalse(inventory.getAllowedActions().contains("pause"));
        assertFalse(inventory.getAllowedActions().contains("drain"));
        assertTrue(inventory.getPreflightRequiredActions().contains("prepareWritebackBackend"));
        assertTrue(inventory.getPreflightRequiredActions().contains("prepareZramPool"));
    }

    @Test
    public void policyApiResponseDoesNotAdvertiseResumeForUnknownActiveTask() throws Exception {
        RecordingRepository repository = new RecordingRepository();
        MemoryStateVO state = new MemoryStateVO();
        state.setHostUuid(HOST);
        state.setStatus("Unknown");
        state.setActiveTaskUuid(TASK);
        state.setControlOperationUuid("control-1");
        state.setDesiredRevision(3);
        state.setAppliedRevision(3L);
        state.setLastSampleTime(System.currentTimeMillis());
        state.setCapabilities("{\"supported\":true,\"zram\":true,\"writeback\":true}");
        state.setState("{\"managed\":true,\"bootId\":\"boot-1\",\"poolGeneration\":\"pool-1\","
                + "\"lastConfirmedOperationUuid\":\"control-1\",\"lastConfirmedAppliedRevision\":3,"
                + "\"lifecycle\":{\"paused\":true,\"activeState\":\"paused\"},"
                + "\"host_zram\":{\"pool_generation\":\"pool-1\",\"quality\":\"native_host_device\","
                + "\"device\":\"/dev/zram0\",\"observed_at\":\"" + java.time.Instant.now() + "\"}}");
        repository.states = Collections.singletonList(state);
        RecordingManager manager = manager(repository, System.currentTimeMillis() + 300_000);
        final APIGetMemoryPolicyReply[] received = new APIGetMemoryPolicyReply[1];
        inject(manager, "bus", (org.zstack.core.cloudbus.CloudBus) Proxy.newProxyInstance(
                org.zstack.core.cloudbus.CloudBus.class.getClassLoader(),
                new Class<?>[]{org.zstack.core.cloudbus.CloudBus.class},
                (proxy, method, args) -> {
                    if ("reply".equals(method.getName())) { received[0] = (APIGetMemoryPolicyReply) args[1]; }
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == int.class) return 0;
                    if (method.getReturnType() == long.class) return 0L;
                    return null;
                }));
        APIGetMemoryPolicyMsg request = new APIGetMemoryPolicyMsg();
        request.setScope("Host"); request.setResourceUuid(HOST);
        Method policy = MemoryOptimizationManager.class.getDeclaredMethod("policy", APIGetMemoryPolicyMsg.class);
        policy.setAccessible(true); policy.invoke(manager, request);

        assertNotNull(received[0]);
        MemoryPolicyInventory inventory = received[0].getInventory();
        assertNotNull(inventory);
        assertFalse(inventory.getAllowedActions().contains("resume"));
        assertFalse(inventory.getAllowedActions().contains("pause"));
        assertFalse(inventory.getAllowedActions().contains("drain"));
        assertTrue(inventory.getAllowedActions().containsAll(inventory.getPreflightRequiredActions()));
    }

    @Test
    public void policyApiResponseKeepsGlobalAndClusterApplyWorkflowWithPreflightMarkers() throws Exception {
        for (String scope : Arrays.asList("Global", "Cluster")) {
            RecordingRepository repository = new RecordingRepository();
            RecordingManager manager = manager(repository, System.currentTimeMillis() + 300_000);
            final APIGetMemoryPolicyReply[] received = new APIGetMemoryPolicyReply[1];
            inject(manager, "bus", (org.zstack.core.cloudbus.CloudBus) Proxy.newProxyInstance(
                    org.zstack.core.cloudbus.CloudBus.class.getClassLoader(),
                    new Class<?>[]{org.zstack.core.cloudbus.CloudBus.class},
                    (proxy, method, args) -> {
                        if ("reply".equals(method.getName())) { received[0] = (APIGetMemoryPolicyReply) args[1]; }
                        if (method.getReturnType() == boolean.class) return false;
                        if (method.getReturnType() == int.class) return 0;
                        if (method.getReturnType() == long.class) return 0L;
                        return null;
                    }));
            APIGetMemoryPolicyMsg request = new APIGetMemoryPolicyMsg();
            request.setScope(scope); request.setResourceUuid("Global".equals(scope) ? "global" : "cluster-1");
            Method policy = MemoryOptimizationManager.class.getDeclaredMethod("policy", APIGetMemoryPolicyMsg.class);
            policy.setAccessible(true); policy.invoke(manager, request);

            assertNotNull(received[0]);
            MemoryPolicyInventory inventory = received[0].getInventory();
            assertTrue(inventory.getAllowedActions().contains("apply"));
            assertTrue(inventory.getPreflightRequiredActions().contains("apply"));
            assertTrue(inventory.getAllowedActions().containsAll(inventory.getPreflightRequiredActions()));
            if ("Global".equals(scope)) {
                assertFalse(inventory.getAllowedActions().contains("clearOverride"));
            } else {
                assertTrue(inventory.getAllowedActions().contains("clearOverride"));
                assertTrue(inventory.getPreflightRequiredActions().contains("clearOverride"));
            }
        }
    }

    @Test
    public void policySourceReflectsMixedInheritedFieldSources() throws Exception {
        RecordingManager manager = new RecordingManager();
        inject(manager, "repository", new RecordingRepository());
        inject(manager, "pluginRgty", licenseRegistry(System.currentTimeMillis() + 300_000));
        MemoryPolicyInventory inventory = new MemoryPolicyInventory();
        inventory.setScope("Host"); inventory.setResourceUuid(HOST); inventory.setRevision(0);
        Map<String, String> fieldSources = new LinkedHashMap<>();
        fieldSources.put("ksm.enabled", "Cluster:cluster-1");
        fieldSources.put("ksm.pagesToScan", "Global:global");
        inventory.setFieldSources(fieldSources);

        Method describe = MemoryOptimizationManager.class.getDeclaredMethod("describe", MemoryPolicyInventory.class);
        describe.setAccessible(true); describe.invoke(manager, inventory);

        assertEquals("Mixed", inventory.getSource());
    }

    @Test
    public void policySourceNamesSingleClusterLayerAndDoesNotTreatDefaultAsOverride() throws Exception {
        RecordingManager manager = new RecordingManager();
        inject(manager, "repository", new RecordingRepository());
        inject(manager, "pluginRgty", licenseRegistry(System.currentTimeMillis() + 300_000));
        Method describe = MemoryOptimizationManager.class.getDeclaredMethod("describe", MemoryPolicyInventory.class);
        describe.setAccessible(true);

        MemoryPolicyInventory cluster = new MemoryPolicyInventory();
        cluster.setScope("Host"); cluster.setResourceUuid(HOST); cluster.setRevision(0);
        cluster.setFieldSources(Collections.singletonMap("ksm.enabled", "Cluster:cluster-1"));
        describe.invoke(manager, cluster);
        assertEquals("Cluster", cluster.getSource());

        MemoryPolicyInventory hostAndCluster = new MemoryPolicyInventory();
        hostAndCluster.setScope("Host"); hostAndCluster.setResourceUuid(HOST); hostAndCluster.setRevision(0);
        Map<String, String> hostAndClusterSources = new LinkedHashMap<>();
        hostAndClusterSources.put("ksm.enabled", "Host:" + HOST);
        hostAndClusterSources.put("ksm.zeroPagesEnabled", "Cluster:cluster-1");
        hostAndCluster.setFieldSources(hostAndClusterSources);
        describe.invoke(manager, hostAndCluster);
        assertEquals("Mixed", hostAndCluster.getSource());

        MemoryPolicyInventory defaults = new MemoryPolicyInventory();
        defaults.setScope("Host"); defaults.setResourceUuid(HOST); defaults.setRevision(0);
        defaults.setFieldSources(Collections.singletonMap("schemaVersion", "Default"));
        describe.invoke(manager, defaults);
        assertEquals("Default", defaults.getSource());

        defaults.setFieldSources(Collections.emptyMap());
        describe.invoke(manager, defaults);
        assertEquals("Default", defaults.getSource());
    }

    @Test
    public void globalPolicyDoesNotAdvertiseClearOverride() throws Exception {
        RecordingManager manager = new RecordingManager();
        inject(manager, "repository", new RecordingRepository());
        inject(manager, "pluginRgty", licenseRegistry(System.currentTimeMillis() + 300_000));
        MemoryPolicyInventory inventory = new MemoryPolicyInventory();
        inventory.setScope("Global"); inventory.setResourceUuid("global");

        Method describe = MemoryOptimizationManager.class.getDeclaredMethod("describe", MemoryPolicyInventory.class);
        describe.setAccessible(true); describe.invoke(manager, inventory);

        assertFalse("Global has no parent override to clear", inventory.getAllowedActions().contains("clearOverride"));
    }

    @Test
    public void expiredLicenseNeverAdvertisesConfigurationActionsOnReadOnlyHost() throws Exception {
        RecordingRepository repository = new RecordingRepository();
        RecordingManager manager = new RecordingManager();
        inject(manager, "repository", repository);
        inject(manager, "pluginRgty", licenseRegistry(System.currentTimeMillis() - 1));
        MemoryPolicyInventory inventory = new MemoryPolicyInventory();
        inventory.setScope("Host"); inventory.setResourceUuid(HOST);
        Method describe = MemoryOptimizationManager.class.getDeclaredMethod("describe", MemoryPolicyInventory.class);
        describe.setAccessible(true);
        describe.invoke(manager, inventory);
        assertEquals(Collections.singletonList("preview"), inventory.getAllowedActions());
    }

    @Test
    public void synchronousDispatchExceptionConvergesToUnknownInsteadOfStuckApplying() throws Exception {
        RecordingRepository repository = new RecordingRepository();
        RecordingManager manager = new RecordingManager();
        manager.throwOnApply = true;
        inject(manager, "repository", repository);
        inject(manager, "pluginRgty", licenseRegistry(System.currentTimeMillis() + 300_000));
        dispatch(manager, task("apply"), false);
        assertEquals("Unknown", repository.resultStatus);
    }

    @Test
    public void alreadyDispatchedTaskMayFinishAfterLicenseExpires() throws Exception {
        RecordingRepository repository = new RecordingRepository();
        RecordingManager manager = new RecordingManager();
        inject(manager, "repository", repository);
        inject(manager, "pluginRgty", licenseRegistry(System.currentTimeMillis() - 1));
        dispatch(manager, task("apply"), true);
        assertEquals("Succeeded", repository.resultStatus);
    }

    @Test
    public void materializedHostReconcilePolicyIsNotTreatedAsNewConfiguration() throws Exception {
        RecordingRepository repository = new RecordingRepository();
        RecordingManager manager = new RecordingManager();
        inject(manager, "repository", repository);
        inject(manager, "pluginRgty", licenseRegistry(System.currentTimeMillis() - 1));
        MemoryTaskVO task = task("reconcile");
        // Repository stores the effective Host policy in the child task,
        // even when the admitted reconcile request has an empty payload.
        task.setPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":true}}");
        dispatch(manager, task, false);
        assertTrue("reconcile must still observe the host after expiry", manager.applyCalled);
        assertEquals("Succeeded", repository.resultStatus);
        assertEquals(Collections.emptyMap(), manager.command.get("policy"));
    }

    @Test
    public void queuedConfigurationIsRejectedBeforeAgentDispatchWhenLicenseExpires() throws Exception {
        RecordingRepository repository = new RecordingRepository();
        RecordingManager manager = new RecordingManager();
        inject(manager, "repository", repository);
        inject(manager, "pluginRgty", licenseRegistry(System.currentTimeMillis() - 1));
        dispatch(manager, task("apply"), false);
        assertEquals("Failed", repository.resultStatus);
        assertFalse(manager.applyCalled);
    }

    private static MemoryTaskVO task(String action) {
        MemoryTaskVO task = new MemoryTaskVO();
        task.setUuid(TASK); task.setHostUuid(HOST); task.setScope("Host");
        task.setResourceUuid(HOST); task.setAction(action); task.setPolicy("{}");
        task.setDesiredRevision(1L);
        return task;
    }

    private static RecordingManager manager(RecordingRepository repository, long licenseDeadline) throws Exception {
        RecordingManager manager = new RecordingManager();
        inject(manager, "repository", repository);
        inject(manager, "pluginRgty", licenseRegistry(licenseDeadline));
        return manager;
    }

    private static JsonObject wireCommand(Map<String, Object> command) {
        return new JsonParser().parse(new Gson().toJson(command)).getAsJsonObject();
    }

    private static void assertDispatchIdentity(JsonObject wire, MemoryTaskVO task, long revision) {
        assertEquals(HOST, wire.get("hostUuid").getAsString());
        assertEquals(TASK, wire.get("operationUuid").getAsString());
        assertEquals(task.getScope(), wire.get("scope").getAsString());
        assertEquals(task.getResourceUuid(), wire.get("resourceUuid").getAsString());
        assertEquals(revision, wire.get("desiredRevision").getAsLong());
        assertEquals("policy-" + revision, wire.get("targetSnapshotGeneration").getAsString());
    }

    private static void assertPolicyMode(JsonObject wire, String expected) {
        assertTrue("Active dispatch must include policyMode", wire.has("policyMode") && !wire.get("policyMode").isJsonNull());
        assertEquals(expected, wire.get("policyMode").getAsString());
    }

    private static void dispatch(RecordingManager manager, MemoryTaskVO task, boolean poll) throws Exception {
        Method dispatch = MemoryOptimizationManager.class.getDeclaredMethod("dispatch", MemoryTaskVO.class, boolean.class);
        dispatch.setAccessible(true);
        dispatch.invoke(manager, task, poll);
    }

    private static class RecordingRepository extends MemoryRepository {
        int recordTaskPermitCalls;
        int authorizePermitCalls;
        String resultStatus;
        List<MemoryStateVO> states = Collections.emptyList();
        MemoryPolicyInventory policy = new MemoryPolicyInventory();

        @Override public boolean recordTaskPermit(String taskUuid, String operationUuid, long deadline) {
            recordTaskPermitCalls++;
            return true;
        }
        @Override public void authorizeTaskPermit(String taskUuid) { }
        @Override public void result(String id, String status, String reason, MemoryAgentResponse response) {
            resultStatus = status;
        }
        @Override public List<String> targets(String scope, String resource) {
            return Collections.singletonList(HOST);
        }
        @Override public List<MemoryStateVO> allStates(List<String> hosts) {
            return states;
        }
        @Override public List<MemoryStateVO> states(List<String> hosts, int start, int limit) {
            return states;
        }
        @Override public MemoryPolicyInventory getPolicy(String scope, String resource) {
            policy.setScope(scope); policy.setResourceUuid(resource);
            return policy;
        }
        @Override boolean canResumeControl(String hostUuid, String controlUuid) { return false; }
        @Override boolean canReconcileRejectedResume(String hostUuid, String controlUuid) { return false; }
        @Override public MemoryCloudBootstrapVO freshCloudBootstrap() { return null; }
        @Override MemoryTaskVO findTask(String uuid) { return null; }
        @Override public void observe(String hostUuid, MemoryAgentResponse response) { }
        @Override public void authorizePermit(String hostUuid, boolean allowed, long deadline) {
            authorizePermitCalls++;
        }
    }

    private static class RecordingManager extends MemoryOptimizationManager {
        Map<String, Object> command = Collections.emptyMap();
        final List<Map<String, Object>> commands = new ArrayList<>();
        int leaseCalls;
        boolean throwOnApply;
        boolean applyCalled;

        @Override void call(String host, String action, Map<String, Object> command,
                            Consumer<MemoryAgentResponse> callback) {
            if ("apply".equals(action) || "reconcile".equals(action)) {
                applyCalled = true;
                if (throwOnApply) { throw new IllegalStateException("transport unavailable"); }
                this.command = new LinkedHashMap<>(command);
                this.commands.add(this.command);
                MemoryAgentResponse response = new MemoryAgentResponse();
                response.operationUuid = TASK;
                response.status = "Succeeded";
                response.appliedRevision = 1L;
                callback.accept(response);
            } else if ("lease".equals(action)) {
                leaseCalls++;
            } else if ("state".equals(action)) {
                MemoryAgentResponse response = new MemoryAgentResponse();
                response.operationUuid = command.get("operationUuid") instanceof String
                        ? (String) command.get("operationUuid") : "state";
                response.status = "Succeeded";
                response.state = Collections.emptyMap();
                callback.accept(response);
            }
        }
    }
}
