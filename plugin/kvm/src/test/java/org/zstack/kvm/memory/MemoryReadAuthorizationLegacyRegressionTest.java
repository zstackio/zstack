package org.zstack.kvm.memory;

import org.junit.Test;
import org.zstack.header.identity.Action;
import org.zstack.header.identity.rbac.RBAC;
import org.zstack.header.message.APIParam;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceVO;

import java.lang.reflect.Field;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * Narrow red/green regression cases that can also run against the pre-F04
 * manager and message/RBAC snapshots without linking to the new helper method.
 */
public class MemoryReadAuthorizationLegacyRegressionTest {
    @Test public void vmOwnerReadIsNotRejectedByTheOldHostWideGate() {
        MemoryOptimizationManager manager = MemoryReadAuthorizationTest.managerFor(Collections.singletonList("vm-owned"));
        APIGetVmMemoryOptimizationMsg msg = new APIGetVmMemoryOptimizationMsg();
        msg.setVmUuid("vm-owned");
        org.zstack.header.identity.SessionInventory session = new org.zstack.header.identity.SessionInventory();
        session.setAccountUuid("tenant");
        msg.setSession(session);
        try {
            MemoryReadAuthorizationTest.authorize(manager, msg);
        } catch (MemoryOperationException denied) {
            fail("VM owner was rejected by the previous Host-wide permission gate: " + denied.getCode());
        }
    }

    @Test public void vmStatisticMetadataUsesStandardVmResourceReadAuthorization() throws Exception {
        for (Class<?> type : new Class<?>[]{APIGetVmMemoryOptimizationMsg.class, APIGetVmMemoryOptimizationsMsg.class}) {
            Action action = type.getAnnotation(Action.class);
            assertEquals(type.getSimpleName(), VmInstanceConstant.ACTION_CATEGORY, action.category());
            Field vmField = type.getDeclaredField(type == APIGetVmMemoryOptimizationMsg.class ? "vmUuid" : "vmUuids");
            APIParam parameter = vmField.getAnnotation(APIParam.class);
            assertEquals(type.getSimpleName(), VmInstanceVO.class, parameter.resourceType());
            assertTrue(type.getSimpleName(), parameter.checkAccount());
        }
    }

    @Test public void rbacWildcardExemptsOnlyTheTwoVmStatisticReads() {
        int before = RBAC.permissions.size();
        new org.zstack.kvm.RBACInfo().permissions();
        RBAC.Permission permission = RBAC.permissions.get(before);
        assertTrue(permission.getNormalAPIs().contains(APIGetVmMemoryOptimizationMsg.class.getName()));
        assertTrue(permission.getNormalAPIs().contains(APIGetVmMemoryOptimizationsMsg.class.getName()));
        assertTrue(permission.getAdminOnlyAPIs().contains(APIQueryMemoryStateMsg.class.getName()));
        assertTrue(permission.getAdminOnlyAPIs().contains(APIGetMemoryPolicyMsg.class.getName()));
    }
}
