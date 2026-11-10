package org.zstack.identity.rbac;

import org.zstack.core.db.Q;
import org.zstack.core.Platform;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.PolicyStatement;
import org.zstack.header.identity.PolicyStatementEffect;
import org.zstack.header.identity.rbac.PolicyMatcher;
import org.zstack.header.identity.role.RolePolicyStatementVO;
import org.zstack.header.identity.role.RolePolicyStatementVO_;
import org.zstack.header.identity.role.RoleType;
import org.zstack.header.identity.role.RoleVO;
import org.zstack.header.identity.role.RoleVO_;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.tpm.api.APIAddTpmMsg;
import org.zstack.header.tpm.api.APIRemoveTpmMsg;
import org.zstack.header.vm.APIUpdateVmInstanceMsg;
import org.zstack.identity.IdentityGlobalProperty;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.LockModeType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// Runs only on the first start after upgrade (install.sh sets the flag once), so admins can still revoke TPM later.
public class CustomizedRoleTpmPermissionUpgrader implements ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(CustomizedRoleTpmPermissionUpgrader.class);

    static final String VM_PERMISSION_ANCHOR = APIUpdateVmInstanceMsg.class.getName();
    static final List<String> TPM_ACTIONS = Arrays.asList(
            APIAddTpmMsg.class.getName(),
            APIRemoveTpmMsg.class.getName()
    );

    private static final PolicyMatcher matcher = new PolicyMatcher();

    @Override
    public void managementNodeReady() {
        if (!IdentityGlobalProperty.UPGRADE_CUSTOMIZED_ROLE_TPM_PERMISSION) {
            return;
        }

        upgrade();
    }

    public void upgrade() {
        List<String> roleUuids = Q.New(RoleVO.class)
                .select(RoleVO_.uuid)
                .eq(RoleVO_.type, RoleType.Customized)
                .listValues();

        int upgraded = 0;
        for (String roleUuid : roleUuids) {
            try {
                if (upgradeRole(roleUuid)) {
                    upgraded++;
                }
            } catch (Exception e) {
                throw new CloudRuntimeException(String.format(
                        "failed to grant TPM APIs to customized role[uuid:%s]", roleUuid), e);
            }
        }

        logger.info(String.format("granted TPM APIs to %d customized role(s) that can modify VMs", upgraded));
    }

    private boolean upgradeRole(String roleUuid) {
        return new SQLBatchWithReturn<Boolean>() {
            @Override
            protected Boolean scripts() {
                // Serialize concurrent management-node upgrades before reading this role's statements.
                RoleVO role = databaseFacade.getEntityManager().find(RoleVO.class, roleUuid, LockModeType.PESSIMISTIC_WRITE);
                if (role == null) {
                    return false;
                }
                List<RolePolicyStatementVO> vos = q(RolePolicyStatementVO.class)
                        .eq(RolePolicyStatementVO_.roleUuid, roleUuid).list();
                List<PolicyStatement> statements = vos.stream()
                        .map(vo -> PolicyStatement.fromJSON(vo.getStatement())).collect(Collectors.toList());

                if (statements.stream().noneMatch(s -> s.getEffect() == PolicyStatementEffect.Allow
                        && matches(s, VM_PERMISSION_ANCHOR))) {
                    return false;
                }
                List<PolicyStatement> vmDenies = statements.stream()
                        .filter(CustomizedRoleTpmPermissionUpgrader::isFullVmUpdateDeny).collect(Collectors.toList());
                if (vmDenies.stream().anyMatch(CustomizedRoleTpmPermissionUpgrader::isUnscoped)) {
                    return false;
                }
                int changed = 0;
                for (int i = 0; i < vos.size(); i++) {
                    PolicyStatement statement = statements.get(i);
                    if (statement.getEffect() != PolicyStatementEffect.Allow || !matches(statement, VM_PERMISSION_ANCHOR)) {
                        continue;
                    }
                    List<String> missing = TPM_ACTIONS.stream().filter(action -> !matches(statement, action)
                            && statements.stream().noneMatch(s -> isUnscoped(s)
                            && s.getEffect() == PolicyStatementEffect.Deny && matches(s, action)))
                            .collect(Collectors.toList());
                    if (missing.isEmpty()) {
                        continue;
                    }
                    RolePolicyStatementVO vo = vos.get(i);
                    List<String> required = new ArrayList<>();
                    if (statement.getRequiredActions() != null) {
                        required.addAll(statement.getRequiredActions());
                    }
                    required.add(VM_PERMISSION_ANCHOR);
                    PolicyStatement grant = PolicyStatement.builder().name("tpm-upgrade-" + vo.getUuid())
                            .effect(PolicyStatementEffect.Allow)
                            .actions(missing.stream().map(PolicyUtils::conditionalAction).collect(Collectors.toList()))
                            .principals(statement.getPrincipals()).resources(statement.getResources())
                            .requiredActions(required.stream().distinct().sorted().collect(Collectors.toList())).build();
                    if (statements.stream().anyMatch(s -> sameGrant(s, grant))) {
                        continue;
                    }
                    RolePolicyStatementVO added = new RolePolicyStatementVO();
                    added.setUuid(Platform.getUuid());
                    added.setRoleUuid(roleUuid);
                    added.setStatement(grant.toJSON());
                    persist(added);
                    changed++;
                }
                if (changed > 0) {
                    logger.info(String.format("extended TPM permissions in %d statement(s) of customized role[uuid:%s]",
                            changed, roleUuid));
                }
                return changed > 0;
            }
        }.execute();
    }

    private static boolean isUnscoped(PolicyStatement statement) {
        return statement.getPrincipals() == null || statement.getPrincipals().isEmpty();
    }

    private static boolean sameGrant(PolicyStatement existing, PolicyStatement grant) {
        PolicyStatement normalized = PolicyStatement.fromJSON(existing.toJSON());
        normalized.setName(grant.getName());
        return normalized.toJSON().equals(grant.toJSON());
    }

    private static boolean isFullVmUpdateDeny(PolicyStatement statement) {
        // Field-level denies still permit other VM updates.
        return statement.getEffect() == PolicyStatementEffect.Deny
                && statement.getActions() != null
                && statement.getActions().stream().anyMatch(action -> !action.contains(":")
                && matcher.match(action, VM_PERMISSION_ANCHOR));
    }

    private static boolean matches(PolicyStatement statement, String api) {
        if (statement.getActions() == null) {
            return false;
        }

        return statement.getActions().stream()
                .anyMatch(action -> matcher.match(PolicyUtils.apiNamePatternFromAction(action), api));
    }
}
