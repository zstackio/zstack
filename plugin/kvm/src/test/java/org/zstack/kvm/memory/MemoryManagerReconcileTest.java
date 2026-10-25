package org.zstack.kvm.memory;

import org.junit.Test;
import org.zstack.header.core.Completion;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.sql.Timestamp;
import java.util.function.Consumer;
import java.lang.reflect.Proxy;
import static org.junit.Assert.*;

public class MemoryManagerReconcileTest {
    static class Repository extends MemoryRepository {
        String status;
        String reason;
        MemoryTaskVO original;
        MemoryTaskVO parent;
        final Map<String, MemoryTaskVO> tasks = new HashMap<>();
        int fourArgResults;
        int fiveArgResults;
        MemoryAgentResponse resultResponse;
        MemoryAgentResponse resultObservation;
        int unobservedPreparationSubmits;
        APIUpdateMemoryPolicyMsg submittedPreparation;
        MemoryAgentResponse submittedBackendObservation;
        int backendSubmits;
        String targetScope;
        String targetResource;
        int targetLookups;
        List<String> submissionTrace;
        @Override public List<String> targets(String scope, String resource) {
            targetLookups++; targetScope = scope; targetResource = resource;
            if (submissionTrace != null) { submissionTrace.add("targets"); }
            return Collections.singletonList("host");
        }
        @Override MemoryTaskInventory submit(APIUpdateMemoryPolicyMsg msg, MemoryAgentResponse observation) {
            backendSubmits++; submittedPreparation = msg; submittedBackendObservation = observation;
            if (submissionTrace != null) { submissionTrace.add("submit"); }
            return new MemoryTaskInventory();
        }
        @Override public MemoryTaskInventory submit(APIUpdateMemoryPolicyMsg msg) {
            unobservedPreparationSubmits++;
            return new MemoryTaskInventory();
        }
        @Override public void result(String id, String value, String reason, MemoryAgentResponse response) {
            fourArgResults++; status = value; this.reason = reason; resultResponse = response;
        }
        @Override public void result(String id, String value, String reason, MemoryAgentResponse response,
                                     MemoryAgentResponse observation) {
            fiveArgResults++; status = value; this.reason = reason;
            resultResponse = response; resultObservation = observation;
        }
        @Override MemoryTaskVO findTask(String uuid) {
            MemoryTaskVO task = tasks.get(uuid);
            if (task != null) { return task; }
            if (parent != null && parent.getUuid().equals(uuid)) { return parent; }
            if (original != null && original.getUuid().equals(uuid)) { return original; }
            return null;
        }
        @Override public java.util.List<MemoryStateVO> states(java.util.List<String> hosts, int start, int limit) {
            MemoryStateVO state = new MemoryStateVO(); state.setHostUuid("host");
            state.setState("{\"bootId\":\"boot-1\"}");
            return Collections.singletonList(state);
        }
    }
    static class Hooks extends MemoryLifecycleHooks {
        int calls;
        @Override public void reconcileMigrationHold(String vm, Completion completion) { calls++; completion.success(); }
    }
    static class Manager extends MemoryOptimizationManager {
        String returnedStatus = "Succeeded";
        boolean success = true;
        boolean expectReconcile = true;
        String expectedOperation = "original-operation";
        int calls;
        Map<String, Object> command;
        Map<String, Object> firstCommand;
        final List<String> actions = new ArrayList<>();
        boolean autoDrainFlow;
        boolean preparationFlow;
        boolean backendPreparationSelectionFlow;
        boolean observationFailure;
        boolean completionSupportProbe;
        MemoryAgentResponse completionSupport;
        MemoryAgentResponse backendInventory;
        int publishedEvents;
        List<String> submissionTrace;
        @Override void call(String host, String action, Map<String, Object> command, Consumer<MemoryAgentResponse> completion) {
            calls++;
            actions.add(action);
            this.command = command;
            if (firstCommand == null) { firstCommand = command; }
            if (completionSupportProbe && "state".equals(action)) {
                assertFalse("capability probing must not send an executor action", command.containsKey("reconcileAction"));
                if (observationFailure) { throw new IllegalStateException("state unavailable"); }
                completion.accept(completionSupport);
                return;
            }
            if (backendPreparationSelectionFlow) {
                assertEquals("host", host);
                assertEquals("writeback-backends", action);
                if (submissionTrace != null) { submissionTrace.add("inventory"); }
                completion.accept(backendInventory);
                return;
            }
            if (autoDrainFlow) {
                if ("apply".equals(action) || "reconcile".equals(action)) {
                    completion.accept(successResponse(host, command));
                    return;
                }
                if ("writeback-backends".equals(action)) {
                    if (observationFailure) { throw new IllegalStateException("observation unavailable"); }
                    completion.accept(writebackObservation(host));
                    return;
                }
                fail("automatic drain must not issue " + action);
            }
            if (preparationFlow) {
                if ("apply".equals(action)) {
                    completion.accept(successResponse(host, command));
                    return;
                }
                if ("state".equals(action)) {
                    if (observationFailure) { throw new IllegalStateException("observation unavailable"); }
                    completion.accept(stateObservation(host));
                    return;
                }
                fail("preparation must not issue " + action);
            }
            assertEquals(expectReconcile ? "reconcile" : "apply", action);
            if (expectedOperation != null) { assertEquals(expectedOperation, command.get("operationUuid")); }
            MemoryAgentResponse response = new MemoryAgentResponse();
            response.operationUuid = (String) command.get("operationUuid");
            response.status = returnedStatus;
            response.reasonCode = "Failed".equals(returnedStatus) ? "HOST_ADMISSION_NOT_SENT" : null;
            response.setSuccess(success);
            completion.accept(response);
        }

        private MemoryAgentResponse successResponse(String host, Map<String, Object> command) {
            MemoryAgentResponse response = new MemoryAgentResponse();
            response.hostUuid = host; response.operationUuid = (String) command.get("operationUuid");
            response.status = "Succeeded"; response.setSuccess(true);
            Object revision = command.get("desiredRevision");
            if (revision instanceof Number) { response.appliedRevision = ((Number) revision).longValue(); }
            return response;
        }

        private MemoryAgentResponse writebackObservation(String host) {
            MemoryAgentResponse response = new MemoryAgentResponse();
            response.hostUuid = host; response.status = "Succeeded"; response.setSuccess(true);
            response.state = new LinkedHashMap<>();
            response.state.put("hostUuid", host); response.state.put("bootId", "boot-1");
            response.state.put("status", "AVAILABLE"); response.state.put("poolGeneration", "pool-1");
            response.state.put("controlOperationUuid", "drain-operation");
            response.state.put("candidates", new ArrayList<>());
            response.state.put("canPrepareZramPool", true);
            response.state.put("zramPoolPreparationQuality", "Qualified");
            response.state.put("zramPoolPreparationObservedAt", System.currentTimeMillis());
            return response;
        }

        private MemoryAgentResponse stateObservation(String host) {
            MemoryAgentResponse response = writebackObservation(host);
            response.observationKind = "state"; response.sampleTime = System.currentTimeMillis();
            response.state.put("maintenanceProof", Collections.singletonMap("bootId", "boot-1"));
            response.bootId = "boot-1";
            return response;
        }
    }
    private void verify(boolean original, String returnedStatus, String expected, int calls, int holds) throws Exception {
        Manager manager = new Manager(); manager.returnedStatus = returnedStatus;
        Repository repository = new Repository(); Hooks hooks = new Hooks();
        for (String name : new String[]{"repository", "lifecycleHooks"}) {
            Field field = MemoryOptimizationManager.class.getDeclaredField(name); field.setAccessible(true);
            field.set(manager, "repository".equals(name) ? repository : hooks);
        }
        MemoryTaskVO task = new MemoryTaskVO(); task.setUuid("reconcile-operation");
        task.setHostUuid("host"); task.setResourceUuid("vm"); task.setScope("VM"); task.setAction("reconcile");
        if (original) task.setReconcileOperationUuid("original-operation");
        Method dispatch = MemoryOptimizationManager.class.getDeclaredMethod("dispatch", MemoryTaskVO.class, boolean.class);
        dispatch.setAccessible(true); dispatch.invoke(manager, task, false);
        assertEquals(expected, repository.status); assertEquals(calls, manager.calls); assertEquals(holds, hooks.calls);
    }
    @Test public void originalUnknownOperationCannotSucceedBecauseNoMigrationHoldExists() throws Exception {
        verify(true, "Unknown", "Unknown", 1, 0);
    }
    @Test public void resolvedOriginalOperationAlsoChecksMigrationHold() throws Exception {
        verify(true, "Succeeded", "Succeeded", 1, 1);
    }
    @Test public void standaloneMigrationHoldDoesNotQueryInventedAgentOperation() throws Exception {
        verify(false, "Succeeded", "Succeeded", 0, 1);
    }

    @Test public void serviceDefaultCapacitySideSurvivesCommandSerialization() throws Exception {
        Manager manager = new Manager(); manager.expectedOperation = "default-capacity";
        inject(manager, "repository", new Repository());
        MemoryTaskVO task = new MemoryTaskVO(); task.setUuid("default-capacity"); task.setHostUuid("host");
        task.setResourceUuid("host"); task.setScope("Host"); task.setAction("apply"); task.setDesiredRevision(2L);
        task.setPolicy("{\"schemaVersion\":1,\"zram\":{\"enabled\":true,\"logicalCapacityBytes\":8192,\"ramLimitBytes\":null}}");
        dispatch(manager, task, true);
        String wire = org.zstack.utils.gson.JSONObjectUtil.toJsonString(manager.command);
        assertTrue("the null side is dropped by serialization and must be named: " + wire,
                wire.contains("\"defaultCapacitySides\":[\"ramLimitBytes\"]"));

        task.setPolicy("{\"schemaVersion\":1,\"zram\":{\"enabled\":true,\"logicalCapacityBytes\":8192,\"ramLimitBytes\":4096}}");
        dispatch(manager, task, true);
        assertFalse(manager.command.containsKey("defaultCapacitySides"));
    }

    @Test public void unknownHostReconcileCarriesSealEvidenceWithoutInventingNotSent() throws Exception {
        Manager manager = new Manager(); Repository repository = new Repository();
        MemoryTaskVO original = new MemoryTaskVO(); original.setUuid("original-operation");
        original.setStatus("Unknown"); original.setReason("No confirmed Agent result");
        original.setCreateDate(new Timestamp(System.currentTimeMillis())); repository.original = original;
        inject(manager, "repository", repository);
        MemoryTaskVO task = new MemoryTaskVO(); task.setUuid("reconcile-operation"); task.setHostUuid("host");
        task.setResourceUuid("host"); task.setScope("Host"); task.setAction("reconcile");
        task.setReconcileOperationUuid("original-operation");
        manager.returnedStatus = "Failed"; manager.success = false;
        dispatch(manager, task);
        assertEquals("seal-if-never-issued", manager.command.get("reconcileAction"));
        assertEquals(original.getCreateDate().getTime(), manager.command.get("taskSubmittedAt"));
        assertEquals("boot-1", manager.command.get("bootId"));
        assertFalse(manager.command.containsKey("classification"));
        assertFalse(manager.command.containsKey("admissionConnected"));
        assertEquals("Failed", repository.status);
        assertEquals("HOST_ADMISSION_NOT_SENT", repository.reason);
    }

    @Test public void rejectedResumeUsesOnlyTheDedicatedAgentVerificationAction() throws Exception {
        Manager manager = new Manager(); Repository repository = new Repository();
        manager.expectedOperation = "rejected-resume";
        MemoryTaskVO original = new MemoryTaskVO(); original.setUuid("rejected-resume");
        original.setStatus("Failed"); original.setAction("resume"); original.setScope("Host");
        original.setReason("CONTROL_OPERATION_FENCED");
        original.setExpectedControlOperationUuid("prior-drain");
        original.setCreateDate(new Timestamp(System.currentTimeMillis())); repository.original = original;
        inject(manager, "repository", repository);
        MemoryTaskVO task = new MemoryTaskVO(); task.setUuid("reconcile-child"); task.setHostUuid("host");
        task.setResourceUuid("host"); task.setScope("Host"); task.setAction("reconcile");
        task.setExpectedControlOperationUuid("prior-drain"); task.setReconcileOperationUuid("rejected-resume");
        dispatch(manager, task);
        assertEquals("rejected-resume", manager.command.get("operationUuid"));
        assertEquals("verify-rejected-resume", manager.command.get("reconcileAction"));
        assertEquals("prior-drain", manager.command.get("expectedControlOperationUuid"));
        assertFalse(manager.command.containsKey("classification"));
        assertFalse(manager.command.containsKey("admissionConnected"));
        assertEquals("Succeeded", repository.status);
    }

    @Test public void successfulAgentWithoutReasonDoesNotBecomePlaceholderError() throws Exception {
        Manager manager = new Manager(); Repository repository = new Repository();
        inject(manager, "repository", repository);
        MemoryTaskVO task = new MemoryTaskVO(); task.setUuid("apply-operation");
        task.setHostUuid("host"); task.setScope("Host"); task.setAction("reconcile");
        manager.expectedOperation = "apply-operation";
        dispatch(manager, task);
        assertEquals("Succeeded", repository.status);
        assertNull("successful no-reason result must remain null", repository.reason);
    }

    @Test public void automaticDrainReadsWritebackInventoryBeforeFiveArgumentResult() throws Exception {
        Manager manager = new Manager(); manager.autoDrainFlow = true;
        Repository repository = new Repository();
        MemoryTaskVO parent = task("parent-apply", "apply", "Applying");
        MemoryTaskVO child = task("drain-operation", "drain", "Applying");
        child.setParentUuid(parent.getUuid());
        child.setPolicy("{\"target\":{\"zram\":{\"logicalCapacityBytes\":1048576}},"
                + "\"targetHash\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\","
                + "\"bootId\":\"01234567-89ab-cdef-0123-456789abcdef\","
                + "\"poolGeneration\":\"abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789\","
                + "\"device\":\"/dev/zram0\"}");
        child.setPolicyPlanHash(String.join("", Collections.nCopies(64, "a")));
        repository.tasks.put(parent.getUuid(), parent); repository.tasks.put(child.getUuid(), child);
        inject(manager, "repository", repository);
        injectLicensedRegistry(manager);

        dispatch(manager, child);

        assertEquals(java.util.Arrays.asList("apply", "writeback-backends"), manager.actions);
        assertEquals("auto drain must not be terminal before its read-only inventory", 0, repository.fourArgResults);
        assertEquals(1, repository.fiveArgResults);
        assertNotSame("drain receipt and inventory must remain separate", repository.resultResponse, repository.resultObservation);
        assertEquals("Succeeded", repository.status);
    }

    @Test public void automaticDrainObservationFailureRemainsUnknownWithoutTerminalReceipt() throws Exception {
        Manager manager = new Manager(); manager.autoDrainFlow = true; manager.observationFailure = true;
        Repository repository = new Repository();
        MemoryTaskVO parent = task("parent-apply-failed-observation", "apply", "Applying");
        MemoryTaskVO child = task("drain-failed-observation", "drain", "Applying");
        child.setParentUuid(parent.getUuid());
        child.setPolicy("{\"target\":{\"zram\":{\"logicalCapacityBytes\":1048576}},"
                + "\"targetHash\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\","
                + "\"bootId\":\"01234567-89ab-cdef-0123-456789abcdef\","
                + "\"poolGeneration\":\"abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789\","
                + "\"device\":\"/dev/zram0\"}");
        child.setPolicyPlanHash(String.join("", Collections.nCopies(64, "a")));
        repository.tasks.put(parent.getUuid(), parent); repository.tasks.put(child.getUuid(), child);
        inject(manager, "repository", repository); injectLicensedRegistry(manager);

        dispatch(manager, child);

        assertEquals(java.util.Arrays.asList("apply", "writeback-backends"), manager.actions);
        assertEquals("Unknown", repository.status);
        assertEquals(1, repository.fourArgResults);
        assertEquals(0, repository.fiveArgResults);
        assertTrue(repository.reason.contains("fresh Host observation"));
    }

    @Test public void automaticDrainPollUsesReconcileEndpointBeforeWritebackObservation() throws Exception {
        Manager manager = new Manager(); manager.autoDrainFlow = true;
        Repository repository = new Repository();
        MemoryTaskVO parent = task("parent-apply-poll", "apply", "Applying");
        MemoryTaskVO child = task("drain-poll", "drain", "Applying");
        child.setParentUuid(parent.getUuid());
        child.setPolicy("{\"target\":{\"zram\":{\"logicalCapacityBytes\":1048576}},"
                + "\"targetHash\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\","
                + "\"bootId\":\"01234567-89ab-cdef-0123-456789abcdef\","
                + "\"poolGeneration\":\"abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789\","
                + "\"device\":\"/dev/zram0\"}");
        child.setPolicyPlanHash(String.join("", Collections.nCopies(64, "a")));
        repository.tasks.put(parent.getUuid(), parent); repository.tasks.put(child.getUuid(), child);
        inject(manager, "repository", repository); injectLicensedRegistry(manager);

        dispatch(manager, child, true);

        assertEquals(java.util.Arrays.asList("reconcile", "writeback-backends"), manager.actions);
        assertEquals(1, repository.fiveArgResults);
    }

    @Test public void reconcileAliasQueriesOriginalAutomaticDrainBeforeWritebackObservation() throws Exception {
        Manager manager = new Manager(); manager.autoDrainFlow = true;
        Repository repository = new Repository();
        MemoryTaskVO root = task("auto-root-query", "apply", "Applying");
        MemoryTaskVO drain = task("auto-drain-unknown", "drain", "Unknown");
        drain.setParentUuid(root.getUuid());
        drain.setPolicy("{\"target\":{\"zram\":{\"logicalCapacityBytes\":1048576}},"
                + "\"targetHash\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\","
                + "\"bootId\":\"01234567-89ab-cdef-0123-456789abcdef\","
                + "\"poolGeneration\":\"abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789\","
                + "\"device\":\"/dev/zram0\"}");
        drain.setPolicyPlanHash(String.join("", Collections.nCopies(64, "a")));
        MemoryTaskVO queryParent = task("reconcile-parent", "reconcile", "Queued");
        MemoryTaskVO query = task("reconcile-child", "reconcile", "Applying");
        query.setParentUuid(queryParent.getUuid()); query.setReconcileOperationUuid(drain.getUuid());
        repository.tasks.put(root.getUuid(), root); repository.tasks.put(drain.getUuid(), drain);
        repository.tasks.put(queryParent.getUuid(), queryParent); repository.tasks.put(query.getUuid(), query);
        inject(manager, "repository", repository); injectLicensedRegistry(manager);

        dispatch(manager, query);

        assertEquals(java.util.Arrays.asList("reconcile", "writeback-backends"), manager.actions);
        assertEquals("drain-operation must be queried, never replayed", drain.getUuid(),
                manager.firstCommand.get("operationUuid"));
        assertEquals(0, repository.fourArgResults);
        assertEquals(1, repository.fiveArgResults);
        assertNotSame(repository.resultResponse, repository.resultObservation);
    }

    @Test public void reconcileAliasObservationFailureIsUnknownWithoutDrainReplay() throws Exception {
        Manager manager = new Manager(); manager.autoDrainFlow = true; manager.observationFailure = true;
        Repository repository = new Repository();
        MemoryTaskVO root = task("auto-root-query-failure", "apply", "Applying");
        MemoryTaskVO drain = task("auto-drain-query-failure", "drain", "Unknown");
        drain.setParentUuid(root.getUuid());
        drain.setPolicy("{\"target\":{\"zram\":{\"logicalCapacityBytes\":1048576}},"
                + "\"targetHash\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\","
                + "\"bootId\":\"01234567-89ab-cdef-0123-456789abcdef\","
                + "\"poolGeneration\":\"abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789\","
                + "\"device\":\"/dev/zram0\"}");
        drain.setPolicyPlanHash(String.join("", Collections.nCopies(64, "a")));
        MemoryTaskVO query = task("reconcile-child-failure", "reconcile", "Applying");
        query.setParentUuid("reconcile-parent-failure"); query.setReconcileOperationUuid(drain.getUuid());
        repository.tasks.put(root.getUuid(), root); repository.tasks.put(drain.getUuid(), drain);
        repository.tasks.put(query.getUuid(), query);
        inject(manager, "repository", repository); injectLicensedRegistry(manager);

        dispatch(manager, query);

        assertEquals(java.util.Arrays.asList("reconcile", "writeback-backends"), manager.actions);
        assertEquals("Unknown", repository.status);
        assertEquals(1, repository.fourArgResults);
        assertEquals(0, repository.fiveArgResults);
    }

    @Test public void ordinaryDrainWithEmptyPolicyDoesNotUseAutomaticWritebackFlow() throws Exception {
        Manager manager = new Manager(); manager.expectedOperation = "ordinary-drain"; manager.expectReconcile = false;
        Repository repository = new Repository();
        MemoryTaskVO child = task("ordinary-drain", "drain", "Applying");
        child.setPolicy("{}"); repository.original = child;
        inject(manager, "repository", repository);
        injectLicensedRegistry(manager);

        dispatch(manager, child);

        assertEquals(Collections.singletonList("apply"), manager.actions);
        assertEquals(1, repository.fourArgResults);
        assertEquals(0, repository.fiveArgResults);
    }

    @Test public void successfulWritebackPreparationReadsStateBeforeFiveArgumentResult() throws Exception {
        Manager manager = new Manager(); manager.preparationFlow = true;
        Repository repository = new Repository();
        MemoryTaskVO drain = task("drain-operation", "drain", "Succeeded");
        MemoryTaskVO preparation = task("prepare-operation", "prepareWritebackBackend", "Applying");
        preparation.setDesiredRevision(7L);
        MemoryTaskVO root = task("root-apply", "apply", "Applying");
        preparation.setParentUuid(root.getUuid());
        preparation.setExpectedControlOperationUuid(drain.getUuid());
        preparation.setPolicyPlanHash(String.join("", Collections.nCopies(64, "a")));
        drain.setParentUuid(root.getUuid()); drain.setPolicyPlanHash(preparation.getPolicyPlanHash());
        drain.setPolicy("{\"target\":{\"zram\":{\"logicalCapacityBytes\":1048576}},"
                + "\"targetHash\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\","
                + "\"bootId\":\"01234567-89ab-cdef-0123-456789abcdef\","
                + "\"poolGeneration\":\"abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789\","
                + "\"device\":\"/dev/zram0\"}");
        repository.tasks.put(root.getUuid(), root); repository.tasks.put(drain.getUuid(), drain);
        repository.tasks.put(preparation.getUuid(), preparation);
        inject(manager, "repository", repository);
        injectLicensedRegistry(manager);

        dispatch(manager, preparation);

        assertEquals(java.util.Arrays.asList("apply", "state"), manager.actions);
        assertEquals(0, repository.fourArgResults);
        assertEquals(1, repository.fiveArgResults);
        assertEquals("Succeeded", repository.status);
        assertTrue(repository.resultResponse.isSuccess());
        assertEquals("host", repository.resultResponse.hostUuid);
        assertEquals("Succeeded", repository.resultResponse.status);
        assertEquals(Long.valueOf(preparation.getDesiredRevision()), repository.resultResponse.appliedRevision);
        assertNotNull(repository.resultObservation);
        assertTrue(repository.resultObservation.isSuccess());
        assertEquals("host", repository.resultObservation.hostUuid);
        assertEquals("state", repository.resultObservation.observationKind);
        assertEquals("boot-1", repository.resultObservation.bootId);
        assertNotNull(repository.resultObservation.sampleTime);
        assertEquals("AVAILABLE", repository.resultObservation.state.get("status"));
        assertEquals("drain-operation", repository.resultObservation.state.get("controlOperationUuid"));
        assertTrue(repository.resultObservation.state.containsKey("maintenanceProof"));
    }

    @Test public void selectedBackendPreparationReadsInventoryBeforeRepositorySubmit() throws Exception {
        Manager manager = new Manager();
        manager.backendPreparationSelectionFlow = true;
        MemoryAgentResponse inventory = new MemoryAgentResponse();
        inventory.hostUuid = "host"; inventory.status = "Succeeded"; inventory.setSuccess(true);
        inventory.state = new LinkedHashMap<>();
        inventory.state.put("hostUuid", "host"); inventory.state.put("bootId", "boot-1");
        inventory.state.put("candidates", Collections.singletonList("selected-backend-candidate"));
        manager.backendInventory = inventory;
        Repository repository = new Repository();
        List<String> trace = new ArrayList<>();
        repository.submissionTrace = trace; manager.submissionTrace = trace;
        inject(manager, "repository", repository);
        inject(manager, "pluginRgty", licensedRegistry());
        inject(manager, "bus", busPublishing(manager));

        APIUpdateMemoryPolicyMsg msg = new APIUpdateMemoryPolicyMsg();
        msg.setScope("Host"); msg.setResourceUuid("host");
        msg.setAction("prepareWritebackBackend"); msg.setExpectedRevision(4L);
        msg.setPolicy("{}");
        MemoryBackendPreparation preparation = new MemoryBackendPreparation();
        preparation.setCandidateId("candidate:" + String.join("", Collections.nCopies(64, "a")));
        preparation.setExpectedHostBootId("01234567-89ab-cdef-0123-456789abcdef");
        preparation.setExpectedPoolGeneration(String.join("", Collections.nCopies(64, "b")));
        preparation.setBackendCapacityBytes(1073741824L); preparation.setResetConfirmed(true);
        msg.setBackendPreparation(preparation);

        invokeUpdate(manager, msg);

        assertEquals(1, repository.targetLookups);
        assertEquals("Host", repository.targetScope);
        assertEquals("host", repository.targetResource);
        assertEquals(Collections.singletonList("writeback-backends"), manager.actions);
        assertEquals(java.util.Arrays.asList("targets", "inventory", "submit"), trace);
        assertEquals(1, repository.backendSubmits);
        assertEquals(0, repository.unobservedPreparationSubmits);
        assertSame("the exact read-only inventory must be the submit precondition", inventory,
                repository.submittedBackendObservation);
        assertEquals("boot-1", repository.submittedBackendObservation.state.get("bootId"));
        assertEquals(Collections.singletonList("selected-backend-candidate"),
                repository.submittedBackendObservation.state.get("candidates"));
        assertSame(msg, repository.submittedPreparation);
        assertEquals(1, manager.publishedEvents);
    }

    @Test public void coldSelectedBackendPreparationReadsFreshStateBeforeTerminalResult() throws Exception {
        verifySelectedBackendPreparationReadsFreshState(false);
    }

    @Test public void manualDrainSelectedBackendPreparationReadsFreshStateBeforeTerminalResult() throws Exception {
        verifySelectedBackendPreparationReadsFreshState(true);
    }

    @Test public void legacyPreparationWithoutSelectedRootDoesNotReadStateOrAutoResume() throws Exception {
        Manager manager = new Manager(); manager.preparationFlow = true;
        Repository repository = new Repository();
        MemoryTaskVO legacyRoot = task("legacy-preparation-root", "prepareWritebackBackend", "Queued");
        legacyRoot.setHostUuid(null); legacyRoot.setResourceUuid("host");
        legacyRoot.setPolicy("{}");
        MemoryTaskVO preparation = task("legacy-preparation", "prepareWritebackBackend", "Applying");
        preparation.setDesiredRevision(7L); preparation.setParentUuid(legacyRoot.getUuid());
        preparation.setPolicy(selectedBackendPreparationRequest());
        preparation.setPolicyPlanHash(String.join("", Collections.nCopies(64, "c")));
        repository.tasks.put(legacyRoot.getUuid(), legacyRoot);
        repository.tasks.put(preparation.getUuid(), preparation);
        inject(manager, "repository", repository);
        injectLicensedRegistry(manager);

        dispatch(manager, preparation);

        assertEquals("legacy preparation without a selected-backend root must retain its existing terminal path",
                Collections.singletonList("apply"), manager.actions);
        assertEquals(1, repository.fourArgResults);
        assertEquals(0, repository.fiveArgResults);
        assertEquals("Succeeded", repository.status);
        assertNotNull(repository.resultResponse);
        assertNull(repository.resultObservation);
    }

    private void verifySelectedBackendPreparationReadsFreshState(boolean manualDrain) throws Exception {
        Manager manager = new Manager(); manager.preparationFlow = true;
        Repository repository = new Repository();
        MemoryTaskVO root = task("selected-preparation-root", "prepareWritebackBackend", "Queued");
        root.setHostUuid(null); root.setResourceUuid("host");
        root.setPolicy(selectedBackendMaintenancePolicy());
        MemoryTaskVO preparation = task("selected-preparation", "prepareWritebackBackend", "Applying");
        preparation.setDesiredRevision(7L); preparation.setParentUuid(root.getUuid());
        preparation.setPolicy(selectedBackendPreparationRequest());
        preparation.setPolicyPlanHash(String.join("", Collections.nCopies(64, "c")));
        repository.tasks.put(root.getUuid(), root);
        if (manualDrain) {
            MemoryTaskVO manualRoot = task("manual-drain-root", "drain", "Succeeded");
            manualRoot.setHostUuid(null); manualRoot.setResourceUuid("host");
            MemoryTaskVO drain = task("manual-drain", "drain", "Succeeded");
            drain.setParentUuid(manualRoot.getUuid());
            repository.tasks.put(manualRoot.getUuid(), manualRoot);
            preparation.setExpectedControlOperationUuid(drain.getUuid());
            repository.tasks.put(drain.getUuid(), drain);
        }
        repository.tasks.put(preparation.getUuid(), preparation);
        inject(manager, "repository", repository);
        injectLicensedRegistry(manager);

        dispatch(manager, preparation);

        assertEquals("successful selected preparation must be followed by a fresh state read",
                java.util.Arrays.asList("apply", "state"), manager.actions);
        assertEquals(0, repository.fourArgResults);
        assertEquals(1, repository.fiveArgResults);
        assertEquals("Succeeded", repository.status);
        assertTrue(repository.resultResponse.isSuccess());
        assertEquals(Long.valueOf(7L), repository.resultResponse.appliedRevision);
        assertNotNull(repository.resultObservation);
        assertTrue(repository.resultObservation.isSuccess());
        assertEquals("host", repository.resultObservation.hostUuid);
        assertEquals("boot-1", repository.resultObservation.bootId);
        assertNotNull(repository.resultObservation.sampleTime);
    }

    private static String selectedBackendMaintenancePolicy() {
        String fingerprint = String.join("", Collections.nCopies(64, "a"));
        String poolGeneration = String.join("", Collections.nCopies(64, "b"));
        String bootId = "01234567-89ab-cdef-0123-456789abcdef";
        return "{\"target\":{\"writeback\":{\"enabled\":false}},"
                + "\"targetHash\":\"b31e1cf3fb56abf046a34cad92addbe75204ad96849a02fdb5c3bff386fff070\","
                + "\"bootId\":\"" + bootId + "\",\"poolGeneration\":\"" + poolGeneration + "\","
                + "\"device\":\"/dev/vdb\",\"drainRequired\":false,"
                + "\"selectedBackend\":{\"identity\":\"" + fingerprint + "\","
                + "\"fingerprint\":\"" + fingerprint + "\",\"resourceUuid\":null,"
                + "\"type\":\"block\",\"device\":\"/dev/vdb\",\"capacityBytes\":1073741824,"
                + "\"hostBootId\":\"" + bootId + "\",\"eligible\":true,"
                + "\"writebackReady\":false,\"state\":\"NEEDS_OFFLINE_PREPARATION\","
                + "\"stableIdentity\":{\"bootId\":\"" + bootId + "\",\"device\":\"/dev/vdb\","
                + "\"byIdPath\":\"/dev/disk/by-id/virtio-selected\",\"majorMinor\":\"252:16\","
                + "\"serial\":\"selected\",\"wwn\":\"\",\"capacityBytes\":1073741824,"
                + "\"transport\":\"\",\"sysfsPath\":\"/sys/devices/virtio/block/vdb\","
                + "\"sysfsInode\":\"100\",\"rdev\":64528,\"nodeInode\":1000}},"
                + "\"backendPreparation\":" + selectedBackendPreparationRequest() + "}";
    }

    private static String selectedBackendPreparationRequest() {
        return "{\"candidateId\":\"candidate:"
                + String.join("", Collections.nCopies(64, "a")) + "\","
                + "\"expectedHostBootId\":\"01234567-89ab-cdef-0123-456789abcdef\","
                + "\"expectedPoolGeneration\":\""
                + String.join("", Collections.nCopies(64, "b")) + "\","
                + "\"backendCapacityBytes\":1073741824,\"resetConfirmed\":true}";
    }

    @Test public void automaticActivationApplyReadsStateBeforeFiveArgumentResult() throws Exception {
        automaticActivationReadsStateBeforeFiveArgumentResult("apply");
    }

    @Test public void automaticActivationClearOverrideReadsStateBeforeFiveArgumentResult() throws Exception {
        automaticActivationReadsStateBeforeFiveArgumentResult("clearOverride");
    }

    private void automaticActivationReadsStateBeforeFiveArgumentResult(String action) throws Exception {
        Manager manager = new Manager(); manager.preparationFlow = true;
        Repository repository = new Repository();
        MemoryTaskVO root = task("activation-root", "apply", "Applying");
        MemoryTaskVO drain = task("activation-drain", "drain", "Succeeded");
        drain.setParentUuid(root.getUuid());
        drain.setPolicy("{\"target\":{\"zram\":{\"logicalCapacityBytes\":1048576}},"
                + "\"targetHash\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\","
                + "\"bootId\":\"01234567-89ab-cdef-0123-456789abcdef\","
                + "\"poolGeneration\":\"abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789\","
                + "\"device\":\"/dev/zram0\"}");
        drain.setPolicyPlanHash(String.join("", Collections.nCopies(64, "a")));
        MemoryTaskVO activation = task("activation-" + action, action, "Applying");
        activation.setHostUuid("host");
        activation.setResourceUuid("host");
        activation.setParentUuid(root.getUuid());
        activation.setExpectedControlOperationUuid(drain.getUuid());
        // Activation is a new sibling plan; it must still be recognized by
        // the original automatic drain/control fence even when its plan hash
        // differs from the drain plan.
        activation.setPolicyPlanHash(String.join("", Collections.nCopies(64, "b")));
        repository.tasks.put(root.getUuid(), root);
        repository.tasks.put(drain.getUuid(), drain);
        repository.tasks.put(activation.getUuid(), activation);
        inject(manager, "repository", repository);
        injectLicensedRegistry(manager);

        dispatch(manager, activation);

        assertEquals(java.util.Arrays.asList("apply", "state"), manager.actions);
        assertEquals(0, repository.fourArgResults);
        assertEquals(1, repository.fiveArgResults);
        assertNotNull(repository.resultObservation);
        assertEquals("state", repository.resultObservation.observationKind);
    }

    private static MemoryTaskVO task(String uuid, String action, String status) {
        MemoryTaskVO task = new MemoryTaskVO(); task.setUuid(uuid); task.setHostUuid("host");
        task.setResourceUuid("host"); task.setScope("Host"); task.setAction(action); task.setStatus(status);
        if ("apply".equals(action) || "clearOverride".equals(action)) {
            task.setHostUuid(null); task.setResourceUuid(null); task.setParentUuid(null);
        }
        return task;
    }

    @Test public void configurationCompletionQueriesOriginalReceiptWithoutSealOrApply() throws Exception {
        for (String action : new String[]{"apply", "clearOverride", "prepareWritebackBackend"}) {
          for (String scope : new String[]{"Host", "Cluster", "Global"}) {
            if ("prepareWritebackBackend".equals(action) && !"Host".equals(scope)) { continue; }
            Manager manager = new Manager(); Repository repository = new Repository();
            manager.completionSupportProbe = true;
            manager.completionSupport = manager.stateObservation("host");
            manager.completionSupport.capabilities = Collections.singletonMap("readOnlyCompletedReconcile", true);
            MemoryTaskVO original = task("original-operation", action, "Unknown");
            original.setHostUuid("host"); original.setScope(scope);
            original.setResourceUuid("Global".equals(scope) ? "global" : "Cluster".equals(scope)
                    ? "234567890abcdef1234567890abcdef1" : "host");
            original.setDesiredRevision(6L); original.setReason("HOST_ADMISSION_NOT_SENT");
            repository.original = original;
            inject(manager, "repository", repository);
            MemoryTaskVO query = task("configuration-query", "reconcile", "Applying");
            query.setActorUuid("system:memory-config-reconcile");
            query.setReconcileOperationUuid(original.getUuid()); query.setDesiredRevision(6L);
            query.setExpectedControlOperationUuid("retained-control");
            manager.returnedStatus = "Unknown"; manager.success = false;
            dispatch(manager, query);
            assertEquals(java.util.Arrays.asList("state", "reconcile"), manager.actions);
            assertEquals("verify-completed", manager.command.get("reconcileAction"));
            assertEquals(original.getUuid(), manager.command.get("operationUuid"));
            assertEquals("boot-1", manager.command.get("bootId"));
            assertEquals(scope, manager.command.get("scope"));
            assertEquals(original.getResourceUuid(), manager.command.get("resourceUuid"));
            assertEquals("Host", query.getScope());
            assertEquals(6L, manager.command.get("desiredRevision"));
            assertEquals("retained-control", manager.command.get("expectedControlOperationUuid"));
            assertEquals(Collections.emptyMap(), manager.command.get("policy"));
            assertFalse(manager.command.containsKey("classification"));
            assertFalse(manager.command.containsKey("admissionConnected"));
            assertEquals("Unknown", repository.status);
            assertEquals(1, repository.fourArgResults);
          }
        }
    }

    @Test public void automaticCompletionRejectsChangedOriginalIdentityBeforeAgentQuery() throws Exception {
        for (String defect : new String[]{"missing", "host", "revision", "scope", "resource"}) {
            Manager manager = new Manager(); Repository repository = new Repository();
            MemoryTaskVO original = task("original-operation", "prepareWritebackBackend", "Unknown");
            original.setDesiredRevision(6L); repository.original = original;
            if ("missing".equals(defect)) { repository.original = null; }
            if ("host".equals(defect)) { original.setHostUuid("another-host"); }
            if ("revision".equals(defect)) { original.setDesiredRevision(7L); }
            if ("scope".equals(defect)) { original.setScope("VM"); }
            if ("resource".equals(defect)) { original.setResourceUuid(null); }
            inject(manager, "repository", repository);
            MemoryTaskVO query = task("configuration-query", "reconcile", "Applying");
            query.setActorUuid("system:memory-config-reconcile");
            query.setReconcileOperationUuid(original.getUuid()); query.setDesiredRevision(6L);
            dispatch(manager, query);
            assertEquals(defect, 0, manager.calls);
            assertEquals("Unknown", repository.status);
            assertEquals("READ_ONLY_COMPLETION_ORIGINAL_IDENTITY_INVALID", repository.reason);
        }
    }

    @Test public void automaticCompletionNeverProbesReconcileOnOldOrUnprovenAgent() throws Exception {
        for (String defect : new String[]{"old-agent", "false", "string", "wrong-host", "wrong-boot",
                "stale", "failed", "missing-state", "no-response", "transport"}) {
            Manager manager = new Manager(); Repository repository = new Repository();
            manager.completionSupportProbe = true;
            MemoryAgentResponse support = manager.stateObservation("host");
            support.capabilities = new LinkedHashMap<>();
            support.capabilities.put("readOnlyCompletedReconcile", true);
            if ("old-agent".equals(defect)) { support.capabilities = null; }
            if ("false".equals(defect)) { support.capabilities.put("readOnlyCompletedReconcile", false); }
            if ("string".equals(defect)) { support.capabilities.put("readOnlyCompletedReconcile", "true"); }
            if ("wrong-host".equals(defect)) { support.hostUuid = "another-host"; }
            if ("wrong-boot".equals(defect)) { support.bootId = "another-boot"; }
            if ("stale".equals(defect)) { support.sampleTime = 1L; }
            if ("failed".equals(defect)) { support.setSuccess(false); }
            if ("missing-state".equals(defect)) { support.state = null; }
            manager.completionSupport = "no-response".equals(defect) ? null : support;
            manager.observationFailure = "transport".equals(defect);
            MemoryTaskVO original = task("original-operation", "prepareWritebackBackend", "Unknown");
            original.setDesiredRevision(6L); repository.original = original;
            inject(manager, "repository", repository);
            MemoryTaskVO query = task("configuration-query", "reconcile", "Applying");
            query.setActorUuid("system:memory-config-reconcile");
            query.setReconcileOperationUuid(original.getUuid()); query.setDesiredRevision(6L);
            query.setExpectedControlOperationUuid("retained-control");
            dispatch(manager, query);
            assertEquals(defect, Collections.singletonList("state"), manager.actions);
            assertEquals(defect, "Unknown", repository.status);
            assertEquals(defect, 1, repository.fourArgResults);
            assertEquals(defect, 0, repository.fiveArgResults);
            if (!"transport".equals(defect)) {
                assertEquals(java.util.Arrays.asList("old-agent", "false", "string").contains(defect)
                        ? "READ_ONLY_COMPLETION_UNSUPPORTED" : "READ_ONLY_COMPLETION_OBSERVATION_UNQUALIFIED", repository.reason);
            }
        }
    }

    private static void injectLicensedRegistry(MemoryOptimizationManager manager) throws Exception {
        inject(manager, "pluginRgty", licensedRegistry());
    }

    private static PluginRegistry licensedRegistry() {
        MemoryLicenseExtensionPoint license = () -> System.currentTimeMillis() + 3600000L;
        return (PluginRegistry) Proxy.newProxyInstance(
                PluginRegistry.class.getClassLoader(), new Class<?>[]{PluginRegistry.class},
                (proxy, method, args) -> "getExtensionList".equals(method.getName())
                        && args != null && args.length == 1
                        && args[0] == MemoryLicenseExtensionPoint.class
                        ? Collections.singletonList(license) : Collections.emptyList());
    }

    private static CloudBus busPublishing(Manager manager) {
        return (CloudBus) Proxy.newProxyInstance(CloudBus.class.getClassLoader(),
                new Class<?>[]{CloudBus.class}, (proxy, method, args) -> {
                    if ("publish".equals(method.getName())) { manager.publishedEvents++; }
                    if (method.getReturnType() == boolean.class) { return false; }
                    if (method.getReturnType() == int.class) { return 0; }
                    if (method.getReturnType() == long.class) { return 0L; }
                    return null;
                });
    }

    private static void invokeUpdate(MemoryOptimizationManager manager, APIUpdateMemoryPolicyMsg msg) throws Exception {
        Method update = MemoryOptimizationManager.class.getDeclaredMethod("update", APIUpdateMemoryPolicyMsg.class);
        update.setAccessible(true); update.invoke(manager, msg);
    }

    @Test public void recoveryReconcileBindsOriginalRequestAndOnlyExactPreClaimRejection() throws Exception {
        for (String reason : new String[]{
                "Uncertain recovery is not proven: RECOVERY_REQUEST_INVALID",
                "RECOVERY_REQUEST_INVALID in transport stderr", "No confirmed Agent result"}) {
            Manager manager = new Manager(); Repository repository = new Repository();
            MemoryTaskVO original = new MemoryTaskVO(); original.setUuid("original-operation");
            original.setHostUuid("host"); original.setScope("Host"); original.setResourceUuid("host");
            original.setAction("recoverUncertain"); original.setStatus("Unknown");
            original.setReason(reason); original.setExpectedControlOperationUuid("original-control");
            original.setDesiredRevision(9); original.setParentUuid("original-parent");
            MemoryTaskVO parent = new MemoryTaskVO(); parent.setUuid("original-parent");
            parent.setScope("Host"); parent.setResourceUuid("host"); parent.setAction("recoverUncertain");
            parent.setExpectedControlOperationUuid("original-control");
            String requestHash = String.join("", Collections.nCopies(64, "a"));
            parent.setRequestHash(requestHash); repository.parent = parent;
            original.setCreateDate(new Timestamp(System.currentTimeMillis()));
            original.setPolicy("{\"expectedHostBootId\":\"boot-1\",\"expectedPoolGeneration\":\"generation\",\"confirmed\":true}");
            repository.original = original; inject(manager, "repository", repository);
            MemoryTaskVO task = new MemoryTaskVO(); task.setUuid("reconcile-child");
            task.setHostUuid("host"); task.setResourceUuid("host"); task.setScope("Host");
            task.setAction("reconcile"); task.setReconcileOperationUuid("original-operation");
            task.setDesiredRevision(10); // Do not bind the query child's later revision.
            manager.returnedStatus = "Unknown"; manager.success = false;
            dispatch(manager, task);
            assertEquals("original-control", manager.command.get("expectedControlOperationUuid"));
            assertEquals(9L, manager.command.get("desiredRevision"));
            assertNull("Host children do not carry the parent admission hash", original.getRequestHash());
            assertEquals(requestHash, manager.command.get("recoveryRequestHash"));
            assertEquals("boot-1", ((Map) manager.command.get("recovery")).get("expectedHostBootId"));
            assertEquals(Collections.emptyMap(), manager.command.get("policy"));
            assertFalse(manager.command.containsKey("admissionConnected"));
            if (reason.equals("Uncertain recovery is not proven: RECOVERY_REQUEST_INVALID")) {
                assertEquals("seal-rejected-recovery", manager.command.get("reconcileAction"));
                assertEquals("RECOVERY_REQUEST_INVALID", manager.command.get("classification"));
            } else {
                assertFalse(manager.command.containsKey("reconcileAction"));
                assertFalse(manager.command.containsKey("classification"));
            }
            parent.setExpectedControlOperationUuid("different-control");
            dispatch(manager, task);
            assertNull(manager.command.get("recoveryRequestHash"));
            assertFalse("an unrelated parent cannot prove the rejected request",
                    manager.command.containsKey("reconcileAction"));
        }
    }

    private static void inject(Object target, String name, Object value) throws Exception {
        Field field = MemoryOptimizationManager.class.getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }

    private static void dispatch(MemoryOptimizationManager manager, MemoryTaskVO task) throws Exception {
        dispatch(manager, task, false);
    }

    private static void dispatch(MemoryOptimizationManager manager, MemoryTaskVO task, boolean poll) throws Exception {
        Method dispatch = MemoryOptimizationManager.class.getDeclaredMethod("dispatch", MemoryTaskVO.class, boolean.class);
        dispatch.setAccessible(true); dispatch.invoke(manager, task, poll);
    }
}
