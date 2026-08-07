package org.zstack.networksecuritypolicyschedule;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.network.securitygroup.SecurityGroupInventory;
import org.zstack.network.securitygroup.SecurityGroupVO;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class NetworkSecurityPolicyScheduleCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final String NAME = NetworkSecurityPolicyScheduleVO.class.getSimpleName();

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private NetworkSecurityPolicyScheduleFacade scheduleFacade;

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(
                CascadeConstant.DELETION_DELETE_CODE,
                CascadeConstant.DELETION_FORCE_DELETE_CODE)
                && !NAME.equals(action.getRootIssuer())) {
            deleteSchedules(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_CLEANUP_CODE)) {
            for (NetworkSecurityPolicyScheduleInventory schedule : schedulesFromAction(action)) {
                dbf.eoCleanup(NetworkSecurityPolicyScheduleVO.class, schedule.getUuid());
            }
            completion.success();
        } else {
            completion.success();
        }
    }

    private void deleteSchedules(CascadeAction action, Completion completion) {
        List<NetworkSecurityPolicyScheduleInventory> schedules = schedulesFromAction(action);
        if (schedules.isEmpty()) {
            completion.success();
            return;
        }

        dbf.removeByPrimaryKeys(schedules.stream()
                .map(NetworkSecurityPolicyScheduleInventory::getUuid)
                .collect(Collectors.toList()), NetworkSecurityPolicyScheduleVO.class);
        completion.success();
    }

    private List<NetworkSecurityPolicyScheduleInventory> schedulesFromAction(CascadeAction action) {
        if (NAME.equals(action.getParentIssuer())) {
            List<NetworkSecurityPolicyScheduleInventory> schedules = action.getParentIssuerContext();
            return schedules == null ? Collections.emptyList() : schedules;
        }

        String resourceType;
        List<String> resourceUuids;
        if (SecurityGroupVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<SecurityGroupInventory> inventories = action.getParentIssuerContext();
            resourceType = NetworkSecurityPolicyScheduleConstant.SECURITY_GROUP_RESOURCE_TYPE;
            resourceUuids = inventories == null ? Collections.emptyList() : inventories.stream()
                    .map(SecurityGroupInventory::getUuid)
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }

        if (resourceUuids.isEmpty()) {
            return Collections.emptyList();
        }
        return NetworkSecurityPolicyScheduleInventory.valueOf(
                Q.New(NetworkSecurityPolicyScheduleVO.class)
                        .eq(NetworkSecurityPolicyScheduleVO_.resourceType, resourceType)
                        .in(NetworkSecurityPolicyScheduleVO_.resourceUuid, resourceUuids)
                        .list(), scheduleFacade.now());
    }

    @Override
    public List<String> getEdgeNames() {
        // SecurityGroupCascadeExtension may pass through VM/account actions without
        // security-group inventories. Accept those transitive parents as no-ops.
        return Arrays.asList(
                SecurityGroupVO.class.getSimpleName(),
                VmInstanceVO.class.getSimpleName(),
                AccountVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (!CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            return null;
        }

        List<NetworkSecurityPolicyScheduleInventory> schedules = schedulesFromAction(action);
        if (schedules.isEmpty()) {
            return null;
        }
        return action.copy()
                .setParentIssuer(NAME)
                .setParentIssuerContext(schedules);
    }
}
