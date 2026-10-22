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

    private static List<APIMessage> adminReadMessages(String account) {
        SessionInventory session = session(account);
        APIGetMemoryStatesMsg state = new APIGetMemoryStatesMsg(); state.setSession(session);
        APIGetMemoryTasksMsg task = new APIGetMemoryTasksMsg(); task.setSession(session);
        APIGetMemorySummaryMsg summary = new APIGetMemorySummaryMsg(); summary.setSession(session);
        APIGetHostMemoryWritebackBackendsMsg backends = new APIGetHostMemoryWritebackBackendsMsg(); backends.setSession(session);
        APIGetMemoryPolicyMsg policy = new APIGetMemoryPolicyMsg(); policy.setSession(session);
        return Arrays.<APIMessage>asList(state, task, summary, backends, policy);
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
