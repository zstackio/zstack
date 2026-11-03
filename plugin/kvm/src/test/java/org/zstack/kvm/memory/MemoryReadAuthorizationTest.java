package org.zstack.kvm.memory;

import org.junit.Test;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.APIMessage;
import org.zstack.identity.AccountManager;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class MemoryReadAuthorizationTest {
    @Test public void infrastructureAndPolicyReadsRemainAdminOnlyAtRuntime() {
        MemoryOptimizationManager tenantManager = managerFor(Collections.singletonList("host-1"));
        for (APIMessage api : adminReadMessages("tenant")) {
            try {
                tenantManager.checkReadAuthorization(api);
                fail(api.getClass().getSimpleName() + " must be denied to a tenant");
            } catch (MemoryOperationException expected) {
                assertEquals("MEMORY_PERMISSION_DENIED", expected.getCode());
            }
        }
        MemoryOptimizationManager adminManager = managerFor(null);
        for (APIMessage api : adminReadMessages("admin")) {
            adminManager.checkReadAuthorization(api);
        }
    }

    @Test public void aFailedAccessQueryIsReportedAsItselfNotAsDenied() throws Exception {
        MemoryOptimizationManager manager = new MemoryOptimizationManager();
        Field field = MemoryOptimizationManager.class.getDeclaredField("accountManager");
        field.setAccessible(true);
        field.set(manager, Proxy.newProxyInstance(AccountManager.class.getClassLoader(), new Class<?>[]{AccountManager.class},
                (proxy, method, args) -> { throw new IllegalStateException("database unavailable"); }));
        try {
            manager.checkReadAuthorization(adminReadMessages("tenant").get(0));
            fail("a failed access query must not pass the check");
        } catch (IllegalStateException expected) {
            assertEquals("database unavailable", expected.getMessage());
        }
    }

    private static List<APIMessage> adminReadMessages(String account) {
        SessionInventory session = new SessionInventory();
        session.setAccountUuid(account);
        APIQueryHostMemoryStateMsg state = new APIQueryHostMemoryStateMsg(); state.setSession(session);
        APIGetMemorySummaryMsg summary = new APIGetMemorySummaryMsg(); summary.setSession(session);
        APIGetHostMemoryWritebackBackendsMsg backends = new APIGetHostMemoryWritebackBackendsMsg(); backends.setSession(session);
        return Arrays.<APIMessage>asList(state, summary, backends);
    }

    /** accessible == null is an administrator; otherwise the account sees only the listed Hosts. */
    static MemoryOptimizationManager managerFor(List<String> accessible) {
        AccountManager accountManager = (AccountManager) Proxy.newProxyInstance(
                AccountManager.class.getClassLoader(), new Class<?>[]{AccountManager.class}, (proxy, method, args) -> {
                    if ("getResourceUuidsCanAccessByAccount".equals(method.getName()) && args[1] == HostVO.class) {
                        return accessible;
                    }
                    if (method.getReturnType() == boolean.class) { return false; }
                    return null;
                });
        MemoryOptimizationManager manager = new MemoryOptimizationManager();
        try {
            Field field = MemoryOptimizationManager.class.getDeclaredField("accountManager");
            field.setAccessible(true); field.set(manager, accountManager);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        return manager;
    }
}
