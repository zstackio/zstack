package org.zstack.kvm.memory;

import org.junit.Test;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.Action;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.identity.rbac.RBAC;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.APIParam;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.identity.AccountManager;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class MemoryReadAuthorizationTest {
    private static final String OWN_VM = "vm-owned";
    private static final String SHARED_VM = "vm-shared";
    private static final String PRIVATE_SHARED_VM = "vm-private-shared";
    private static final String OTHER_VM = "vm-other";
    private static final Map<MemoryOptimizationManager, AtomicBoolean> LEGACY_DENIALS = new WeakHashMap<>();
    private static final Map<MemoryOptimizationManager, AtomicBoolean> PLATFORM_CHECKS = new WeakHashMap<>();

    @Test public void vmOwnerCanReadSingleAndBatchAndAccessibleSharedVm() {
        MemoryOptimizationManager manager = managerFor(Arrays.asList(OWN_VM, SHARED_VM, PRIVATE_SHARED_VM));
        authorize(manager, single(OWN_VM, "tenant"));
        authorize(manager, batch(Arrays.asList(OWN_VM, SHARED_VM), "tenant"));
        authorize(manager, single(PRIVATE_SHARED_VM, "tenant"));
        authorize(manager, batch(Arrays.asList(OWN_VM, PRIVATE_SHARED_VM), "tenant"));
        assertTrue("manager must reuse the platform's account resource visibility", PLATFORM_CHECKS.get(manager).get());
    }

    @Test public void projectUserSessionDoesNotReenterLegacyUserPolicyEvaluation() {
        MemoryOptimizationManager manager = managerFor(Collections.singletonList(OWN_VM));
        APIGetVmMemoryOptimizationMsg single = single(OWN_VM, "project-account");
        single.getSession().setUserUuid("iam2-virtual-id");
        assertFalse(single.getSession().isAccountSession());
        authorize(manager, single);
        APIGetVmMemoryOptimizationsMsg batch = batch(Collections.singletonList(OWN_VM), "project-account");
        batch.getSession().setUserUuid("iam2-virtual-id");
        authorize(manager, batch);
        assertTrue(PLATFORM_CHECKS.get(manager).get());
    }

    @Test public void vmReadRejectsOtherAndMixedBatch() {
        MemoryOptimizationManager manager = managerFor(Collections.singletonList(OWN_VM));
        assertDenied(() -> authorize(manager, single(OTHER_VM, "tenant")));
        assertDenied(() -> authorize(manager, batch(Arrays.asList(OWN_VM, OTHER_VM), "tenant")));
        assertDenied(() -> authorize(manager, single("vm-nonexistent", "tenant")));
    }

    @Test public void systemAdminCanReadVmStatistics() {
        MemoryOptimizationManager manager = managerFor(null);
        authorize(manager, single(OTHER_VM, "admin"));
        authorize(manager, batch(Arrays.asList(OWN_VM, OTHER_VM), "admin"));
    }

    @Test public void failedResourceScopeLookupDoesNotGrantVmRead() throws Exception {
        MemoryOptimizationManager manager = managerFor(Collections.singletonList(OWN_VM));
        AccountManager unavailable = (AccountManager) Proxy.newProxyInstance(
                AccountManager.class.getClassLoader(), new Class<?>[]{AccountManager.class},
                (proxy, method, args) -> { throw new IllegalStateException("resource scope unavailable"); });
        Field field = MemoryOptimizationManager.class.getDeclaredField("accountManager");
        field.setAccessible(true); field.set(manager, unavailable);
        assertDenied(() -> authorize(manager, single(OWN_VM, "project-account")));
        assertDenied(() -> authorize(manager, batch(Collections.singletonList(OWN_VM), "project-account")));
    }

    @Test public void unauthenticatedVmAndInfrastructureReadsAreRejected() {
        MemoryOptimizationManager manager = managerFor(Arrays.asList(OWN_VM));
        APIGetVmMemoryOptimizationMsg noSession = new APIGetVmMemoryOptimizationMsg();
        noSession.setVmUuid(OWN_VM);
        assertDenied(() -> authorize(manager, noSession));
        APIQueryMemoryStateMsg state = new APIQueryMemoryStateMsg();
        state.setSession(session("tenant"));
        assertDenied(() -> authorize(manager, state));
    }

    @Test public void infrastructureAndPolicyReadsRemainAdminOnlyAtRuntime() {
        MemoryOptimizationManager tenantManager = managerFor(Collections.singletonList("host-1"));
        for (APIMessage api : adminReadMessages("tenant")) {
            assertDenied(api.getClass().getSimpleName(), () -> authorize(tenantManager, api));
        }
        MemoryOptimizationManager adminManager = managerFor(null);
        for (APIMessage api : adminReadMessages("admin")) {
            authorize(adminManager, api);
        }
    }

    @Test public void apiMetadataMatchesInfrastructureAndVmAuthorizationBoundary() throws Exception {
        for (Class<?> type : new Class<?>[]{APIQueryMemoryStateMsg.class, APIQueryMemoryTaskMsg.class,
                APIGetMemorySummaryMsg.class, APIQueryHostMemoryOperationsMsg.class,
                APIGetMemoryStatesMsg.class, APIGetMemoryTasksMsg.class, APIGetHostMemoryOperationsMsg.class,
                APIGetHostMemoryWritebackBackendsMsg.class, APIGetMemoryPolicyMsg.class}) {
            Action action = type.getAnnotation(Action.class);
            assertNotNull(type.getSimpleName(), action);
            assertTrue(type.getSimpleName(), action.adminOnly());
        }
        for (Class<?> type : new Class<?>[]{APIGetVmMemoryOptimizationMsg.class, APIGetVmMemoryOptimizationsMsg.class}) {
            Action action = type.getAnnotation(Action.class);
            assertNotNull(type.getSimpleName(), action);
            assertFalse(type.getSimpleName(), action.adminOnly());
            assertEquals(type.getSimpleName(), VmInstanceConstant.ACTION_CATEGORY, action.category());
            Field vmField = type.getDeclaredField(type == APIGetVmMemoryOptimizationMsg.class ? "vmUuid" : "vmUuids");
            APIParam param = vmField.getAnnotation(APIParam.class);
            assertEquals(type.getSimpleName(), VmInstanceVO.class, param.resourceType());
            assertTrue(type.getSimpleName(), param.checkAccount());
        }

        int before = RBAC.permissions.size();
        new org.zstack.kvm.RBACInfo().permissions();
        assertEquals(before + 1, RBAC.permissions.size());
        RBAC.Permission permission = RBAC.permissions.get(before);
        for (Class<?> type : new Class<?>[]{APIGetVmMemoryOptimizationMsg.class, APIGetVmMemoryOptimizationsMsg.class}) {
            assertTrue(type.getSimpleName(), permission.getNormalAPIs().contains(type.getName()));
            assertFalse(type.getSimpleName(), permission.getAdminOnlyAPIs().contains(type.getName()));
        }
        for (Class<?> type : new Class<?>[]{APIQueryMemoryStateMsg.class, APIGetMemoryPolicyMsg.class,
                APIUpdateMemoryPolicyMsg.class, APIGetMemoryStatesMsg.class, APIGetMemoryTasksMsg.class,
                APIGetHostMemoryOperationsMsg.class}) {
            assertTrue(type.getSimpleName(), permission.getAdminOnlyAPIs().contains(type.getName()));
        }
    }

    private static List<APIMessage> adminReadMessages(String account) {
        SessionInventory session = session(account);
        APIQueryMemoryStateMsg state = new APIQueryMemoryStateMsg(); state.setSession(session);
        APIQueryMemoryTaskMsg task = new APIQueryMemoryTaskMsg(); task.setSession(session);
        APIGetMemorySummaryMsg summary = new APIGetMemorySummaryMsg(); summary.setSession(session);
        APIQueryHostMemoryOperationsMsg operations = new APIQueryHostMemoryOperationsMsg(); operations.setSession(session);
        APIGetHostMemoryWritebackBackendsMsg backends = new APIGetHostMemoryWritebackBackendsMsg(); backends.setSession(session);
        APIGetMemoryPolicyMsg policy = new APIGetMemoryPolicyMsg(); policy.setSession(session);
        return Arrays.<APIMessage>asList(state, task, summary, operations, backends, policy);
    }

    private static APIGetVmMemoryOptimizationMsg single(String uuid, String account) {
        APIGetVmMemoryOptimizationMsg msg = new APIGetVmMemoryOptimizationMsg();
        msg.setVmUuid(uuid); msg.setSession(session(account)); return msg;
    }

    private static APIGetVmMemoryOptimizationsMsg batch(List<String> uuids, String account) {
        APIGetVmMemoryOptimizationsMsg msg = new APIGetVmMemoryOptimizationsMsg();
        msg.setVmUuids(uuids); msg.setSession(session(account)); return msg;
    }

    private static SessionInventory session(String account) {
        SessionInventory session = new SessionInventory(); session.setAccountUuid(account); return session;
    }

    static void authorize(MemoryOptimizationManager manager, APIMessage api) {
        try {
            Method checker = MemoryOptimizationManager.class.getDeclaredMethod("checkReadAuthorization", APIMessage.class);
            checker.setAccessible(true);
            checker.invoke(manager, api);
        } catch (NoSuchMethodException legacyManager) {
            // Exercise the pre-change service path for a real red baseline. The
            // old manager's Host-only gate replies with MEMORY_PERMISSION_DENIED
            // for a tenant VM read before reaching its handler.
            AtomicBoolean denied = LEGACY_DENIALS.get(manager);
            denied.set(false);
            manager.handleMessage(api);
            if (denied.get()) {
                throw new MemoryOperationException("MEMORY_PERMISSION_DENIED", "Legacy manager rejected the VM-scoped request");
            }
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) { throw (RuntimeException) cause; }
            throw new AssertionError(cause);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    static MemoryOptimizationManager managerFor(List<String> accessible) {
        final MemoryOptimizationManager[] managerReference = new MemoryOptimizationManager[1];
        AccountManager accountManager = (AccountManager) Proxy.newProxyInstance(
                AccountManager.class.getClassLoader(), new Class<?>[]{AccountManager.class}, (proxy, method, args) -> {
                    if ("getResourceUuidsCanAccessByAccount".equals(method.getName())) {
                        Class<?> resourceType = (Class<?>) args[1];
                        if (resourceType == VmInstanceVO.class) {
                            PLATFORM_CHECKS.get(managerReference[0]).set(true);
                            return accessible;
                        }
                        if (resourceType == HostVO.class) {
                            return accessible == null ? null : Collections.singletonList("host-1");
                        }
                    }
                    if ("checkApiMessagePermission".equals(method.getName())) {
                        throw new AssertionError("A service must not reenter legacy UserVO policy authorization after the unified API authorization backend");
                    }
                    if (method.getReturnType() == boolean.class) { return false; }
                    if (method.getReturnType() == int.class) { return 0; }
                    if (method.getReturnType() == long.class) { return 0L; }
                    return null;
                });
        MemoryOptimizationManager manager = new MemoryOptimizationManager();
        managerReference[0] = manager;
        try {
            PLATFORM_CHECKS.put(manager, new AtomicBoolean());
            Field field = MemoryOptimizationManager.class.getDeclaredField("accountManager");
            field.setAccessible(true); field.set(manager, accountManager);
            AtomicBoolean denied = new AtomicBoolean();
            LEGACY_DENIALS.put(manager, denied);
            CloudBus bus = (CloudBus) Proxy.newProxyInstance(CloudBus.class.getClassLoader(),
                    new Class<?>[]{CloudBus.class}, (proxy, method, args) -> {
                        if ("replyErrorByMessageType".equals(method.getName())) { denied.set(true); }
                        if (method.getReturnType() == boolean.class) { return false; }
                        if (method.getReturnType() == int.class) { return 0; }
                        if (method.getReturnType() == long.class) { return 0L; }
                        return null;
                    });
            Field busField = MemoryOptimizationManager.class.getDeclaredField("bus");
            busField.setAccessible(true); busField.set(manager, bus);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        return manager;
    }

    private static void assertDenied(Runnable call) { assertDenied("permission should be denied", call); }
    private static void assertDenied(String message, Runnable call) {
        try { call.run(); fail(message); }
        catch (MemoryOperationException expected) { assertEquals("MEMORY_PERMISSION_DENIED", expected.getCode()); }
    }
}
