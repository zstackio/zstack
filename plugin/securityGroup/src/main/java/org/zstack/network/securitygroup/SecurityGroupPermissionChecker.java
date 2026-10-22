package org.zstack.network.securitygroup;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.IdentityErrors;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.identity.SharedResourceVO;
import org.zstack.header.identity.SharedResourceVO_;
import org.zstack.identity.AccountManager;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

public class SecurityGroupPermissionChecker {
    @Autowired
    private AccountManager acntMgr;

    /**
     * Checks use of an existing group through admin access, ownership, or a read sharing grant.
     */
    boolean canUse(String accountUuid, String securityGroupUuid) {
        if (accountUuid == null || !Q.New(SecurityGroupVO.class).eq(SecurityGroupVO_.uuid, securityGroupUuid).isExists()) {
            return false;
        }

        if (AccountConstant.isAdminPermission(accountUuid) || accountUuid.equals(acntMgr.getOwnerAccountUuidOfResource(securityGroupUuid))) {
            return true;
        }

        return !SQL.New("select s.id from SharedResourceVO s where s.resourceUuid = :sgUuid" +
                        " and (s.toPublic = true or s.receiverAccountUuid = :accountUuid) and s.permission in (:permissions)", Long.class)
                .param("sgUuid", securityGroupUuid).param("accountUuid", accountUuid)
                .param("permissions", java.util.Arrays.asList(SharedResourceVO.PERMISSION_READ, SharedResourceVO.PERMISSION_READ | SharedResourceVO.PERMISSION_WRITE)).limit(1).list().isEmpty();
    }

    /**
     * Rejects any group that the resource account has no permission to use.
     */
    void checkUse(String accountUuid, List<String> securityGroupUuids) {
        for (String uuid : securityGroupUuids) {
            if (!canUse(accountUuid, uuid)) {
                throw new OperationFailureException(err(ORG_ZSTACK_NETWORK_SECURITYGROUP_10133, IdentityErrors.PERMISSION_DENIED,
                        "account[uuid:%s] has no permission to use security group[uuid:%s]", accountUuid, uuid));
            }
        }
    }

    /**
     * Identifies an admin-owned group that the resource account has no permission to use.
     */
    public boolean isHiddenAdminSecurityGroup(String accountUuid, String securityGroupUuid) {
        return AccountConstant.isAdminPermission(acntMgr.getOwnerAccountUuidOfResource(securityGroupUuid))
                && !canUse(accountUuid, securityGroupUuid);
    }

    /**
     * Returns hidden admin groups in their first-occurrence order in the input.
     */
    Set<String> getHiddenAdminSecurityGroupUuids(String accountUuid, Collection<String> securityGroupUuids) {
        Set<String> hidden = new LinkedHashSet<>();
        for (String uuid : new LinkedHashSet<>(securityGroupUuids)) {
            if (isHiddenAdminSecurityGroup(accountUuid, uuid)) {
                hidden.add(uuid);
            }
        }
        return hidden;
    }

    /**
     * Checks requested permissions and trusted backend inheritance for each L3.
     * Inherited groups must still belong to admin and appear in the final configuration on that L3.
     */
    void validateCreateSecurityGroupSelection(String accountUuid, Map<String, List<String>> groups,
                                             Map<String, List<String>> requested, Map<String, List<String>> inherited) {
        if (requested == null || inherited == null) {
            throw new OperationFailureException(operr(ORG_ZSTACK_NETWORK_SECURITYGROUP_10146, "security group selection source is missing"));
        }

        Set<String> checked = requested.values().stream().flatMap(Collection::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        // Explicit selections still require permission when the same UUID is also inherited.
        checkUse(accountUuid, new ArrayList<>(checked));
        for (Map.Entry<String, List<String>> entry : groups.entrySet()) {
            List<String> inheritedUuids = inherited.getOrDefault(entry.getKey(), Collections.emptyList());
            for (String uuid : inheritedUuids) {
                if (!AccountConstant.isAdminPermission(acntMgr.getOwnerAccountUuidOfResource(uuid)) || !entry.getValue().contains(uuid)) {
                    throw new OperationFailureException(operr(ORG_ZSTACK_NETWORK_SECURITYGROUP_10143,
                            "security group[uuid:%s] cannot be inherited on L3 network[uuid:%s]", uuid, entry.getKey()));
                }
            }
            List<String> toCheck = entry.getValue().stream().filter(uuid -> !inheritedUuids.contains(uuid))
                    .filter(checked::add).collect(Collectors.toList());
            checkUse(accountUuid, toCheck);
        }
    }

    /**
     * Checks group use for the VM owner, allowing admin to manage bindings of its own groups.
     */
    void checkBindingUse(SessionInventory session, String vmAccountUuid, List<String> securityGroupUuids) {
        List<String> toCheck = new ArrayList<>(securityGroupUuids);
        if (AccountConstant.isAdminPermission(session)) {
            toCheck.removeIf(uuid -> session.getAccountUuid().equals(acntMgr.getOwnerAccountUuidOfResource(uuid)));
        }
        checkUse(vmAccountUuid, toCheck);
    }

    /**
     * Requires the caller to be admin or the owner of the group.
     */
    void checkManage(SessionInventory session, String securityGroupUuid) {
        if (!AccountConstant.isAdminPermission(session) && !session.getAccountUuid().equals(acntMgr.getOwnerAccountUuidOfResource(securityGroupUuid))) {
            throw new OperationFailureException(err(ORG_ZSTACK_NETWORK_SECURITYGROUP_10134, IdentityErrors.PERMISSION_DENIED,
                    "account[uuid:%s] has no permission to modify security group[uuid:%s]", session.getAccountUuid(), securityGroupUuid));
        }
    }

    /**
     * Requires an existing admin-owned group before its sharing scope can be changed.
     */
    void checkShareable(String securityGroupUuid) {
        if (!Q.New(SecurityGroupVO.class).eq(SecurityGroupVO_.uuid, securityGroupUuid).isExists()) {
            throw new OperationFailureException(operr(ORG_ZSTACK_NETWORK_SECURITYGROUP_10140, "security group[uuid:%s] does not exist", securityGroupUuid));
        }
        if (!AccountConstant.isAdminPermission(acntMgr.getOwnerAccountUuidOfResource(securityGroupUuid))) {
            throw new OperationFailureException(operr(ORG_ZSTACK_NETWORK_SECURITYGROUP_10135, "only security groups owned by admin can be shared, security group[uuid:%s] is not owned by admin", securityGroupUuid));
        }
    }

    /**
     * Returns whether any sharing record exists for the group.
     */
    boolean isShared(String securityGroupUuid) {
        return Q.New(SharedResourceVO.class).eq(SharedResourceVO_.resourceUuid, securityGroupUuid).isExists();
    }

    /**
     * Evaluates use permission under the requested final sharing scope.
     */
    private boolean canUseAfterSharing(String accountUuid, String ownerAccountUuid, boolean targetPublic, List<String> targetAccounts) {
        return AccountConstant.isAdminPermission(accountUuid) || (accountUuid != null && accountUuid.equals(ownerAccountUuid))
                || targetPublic || targetAccounts.contains(accountUuid);
    }

    /**
     * Rejects scope reductions that remove permission from accounts with NIC or external rule references.
     * Existing hidden admin bindings are not treated as recalled sharing grants.
     */
    void checkSharingReferences(String securityGroupUuid, boolean targetPublic, List<String> targetAccounts) {
        String ownerAccountUuid = acntMgr.getOwnerAccountUuidOfResource(securityGroupUuid);
        for (VmNicSecurityGroupRefVO ref : Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.securityGroupUuid, securityGroupUuid).<VmNicSecurityGroupRefVO>list()) {
            String accountUuid = acntMgr.getOwnerAccountUuidOfResource(ref.getVmInstanceUuid());
            if (canUse(accountUuid, securityGroupUuid) && !canUseAfterSharing(accountUuid, ownerAccountUuid, targetPublic, targetAccounts)) {
                throw new OperationFailureException(operr(ORG_ZSTACK_NETWORK_SECURITYGROUP_10136, "cannot revoke security group[uuid:%s] from account[uuid:%s], vm[uuid:%s] nic[uuid:%s] still uses it",
                        securityGroupUuid, accountUuid, ref.getVmInstanceUuid(), ref.getVmNicUuid()));
            }
        }

        for (SecurityGroupRuleVO rule : Q.New(SecurityGroupRuleVO.class).eq(SecurityGroupRuleVO_.remoteSecurityGroupUuid, securityGroupUuid)
                .notEq(SecurityGroupRuleVO_.securityGroupUuid, securityGroupUuid).<SecurityGroupRuleVO>list()) {
            String accountUuid = acntMgr.getOwnerAccountUuidOfResource(rule.getSecurityGroupUuid());
            if (canUse(accountUuid, securityGroupUuid) && !canUseAfterSharing(accountUuid, ownerAccountUuid, targetPublic, targetAccounts)) {
                throw new OperationFailureException(operr(ORG_ZSTACK_NETWORK_SECURITYGROUP_10149, "cannot revoke security group[uuid:%s] from account[uuid:%s], security group[uuid:%s] rule[uuid:%s] still references it",
                        securityGroupUuid, accountUuid, rule.getSecurityGroupUuid(), rule.getUuid()));
            }
        }
    }

    /**
     * Blocks deletion of shared groups with NIC bindings or external rule references.
     * Private groups retain the existing deletion behavior.
     */
    void checkDelete(String securityGroupUuid) {
        if (!isShared(securityGroupUuid)) {
            return;
        }
        VmNicSecurityGroupRefVO ref = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.securityGroupUuid, securityGroupUuid).limit(1).find();
        if (ref != null) {
            throw new OperationFailureException(operr(ORG_ZSTACK_NETWORK_SECURITYGROUP_10137, "cannot delete shared security group[uuid:%s], vm[uuid:%s] nic[uuid:%s] still uses it",
                    securityGroupUuid, ref.getVmInstanceUuid(), ref.getVmNicUuid()));
        }
        SecurityGroupRuleVO rule = Q.New(SecurityGroupRuleVO.class).eq(SecurityGroupRuleVO_.remoteSecurityGroupUuid, securityGroupUuid)
                .notEq(SecurityGroupRuleVO_.securityGroupUuid, securityGroupUuid).limit(1).find();
        if (rule != null) {
            throw new OperationFailureException(operr(ORG_ZSTACK_NETWORK_SECURITYGROUP_10150, "cannot delete shared security group[uuid:%s], security group[uuid:%s] rule[uuid:%s] still references it",
                    securityGroupUuid, rule.getSecurityGroupUuid(), rule.getUuid()));
        }
    }

    /**
     * Requires the new VM owner to have use permission for every group bound to the VM.
     */
    void checkVmOwnerChange(String vmUuid, String targetAccountUuid) {
        List<String> sgUuids = Q.New(VmNicSecurityGroupRefVO.class).select(VmNicSecurityGroupRefVO_.securityGroupUuid)
                .eq(VmNicSecurityGroupRefVO_.vmInstanceUuid, vmUuid).listValues();
        for (String sgUuid : sgUuids) {
            if (!canUse(targetAccountUuid, sgUuid)) {
                throw new OperationFailureException(operr(ORG_ZSTACK_NETWORK_SECURITYGROUP_10138, "cannot transfer vm[uuid:%s] to account[uuid:%s], detach security group[uuid:%s] first because the target account has no permission to use it",
                        vmUuid, targetAccountUuid, sgUuid));
            }
        }
    }
}
