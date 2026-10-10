package org.zstack.kvm.memory;

import org.junit.Test;
import org.zstack.header.core.Completion;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Collections;
import java.sql.Timestamp;
import java.util.function.Consumer;
import java.lang.reflect.Proxy;
import static org.junit.Assert.*;

public class MemoryManagerReconcileTest {
    static class Repository extends MemoryRepository {
        String status;
        String reason;
        MemoryTaskVO original;
        @Override public void result(String id, String value, String reason, MemoryAgentResponse response) { status = value; this.reason = reason; }
        @Override MemoryTaskVO findTask(String uuid) { return original; }
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
        @Override void call(String host, String action, Map<String, Object> command, Consumer<MemoryAgentResponse> completion) {
            calls++;
            this.command = command;
            assertEquals(expectReconcile ? "reconcile" : "apply", action);
            if (expectedOperation != null) { assertEquals(expectedOperation, command.get("operationUuid")); }
            MemoryAgentResponse response = new MemoryAgentResponse();
            response.operationUuid = (String) command.get("operationUuid");
            response.status = returnedStatus;
            response.reasonCode = "Failed".equals(returnedStatus) ? "HOST_ADMISSION_NOT_SENT" : null;
            response.setSuccess(success);
            completion.accept(response);
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

    private static void inject(Object target, String name, Object value) throws Exception {
        Field field = MemoryOptimizationManager.class.getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }

    private static void dispatch(MemoryOptimizationManager manager, MemoryTaskVO task) throws Exception {
        Method dispatch = MemoryOptimizationManager.class.getDeclaredMethod("dispatch", MemoryTaskVO.class, boolean.class);
        dispatch.setAccessible(true); dispatch.invoke(manager, task, false);
    }
}
