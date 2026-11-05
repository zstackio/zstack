package org.zstack.identity.rbac;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
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
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

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

    @Autowired
    private DatabaseFacade dbf;

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
                logger.warn(String.format("failed to grant TPM APIs to customized role[uuid:%s]: %s", roleUuid, e.getMessage()), e);
            }
        }

        logger.info(String.format("granted TPM APIs to %d customized role(s) that can modify VMs", upgraded));
    }

    private boolean upgradeRole(String roleUuid) {
        List<RolePolicyStatementVO> vos = Q.New(RolePolicyStatementVO.class)
                .eq(RolePolicyStatementVO_.roleUuid, roleUuid)
                .list();

        List<PolicyStatement> statements = vos.stream()
                .map(vo -> PolicyStatement.fromJSON(vo.getStatement()))
                .collect(Collectors.toList());

        List<String> missing = new ArrayList<>();
        for (String tpmAction : TPM_ACTIONS) {
            if (!anyStatementMatches(statements, PolicyStatementEffect.Allow, tpmAction)
                    && !anyStatementMatches(statements, PolicyStatementEffect.Deny, tpmAction)) {
                missing.add(tpmAction);
            }
        }

        if (missing.isEmpty()) {
            return false;
        }

        for (int i = 0; i < vos.size(); i++) {
            PolicyStatement statement = statements.get(i);
            if (statement.getEffect() == null
                    || statement.getEffect() != PolicyStatementEffect.Allow
                    || !matches(statement, VM_PERMISSION_ANCHOR)) {
                continue;
            }

            missing.forEach(statement::addAction);
            final RolePolicyStatementVO vo = vos.get(i);
            vo.setStatement(JSONObjectUtil.toJsonString(statement));
            final String missingStr = missing.toString();
            new SQLBatch() {
                @Override
                protected void scripts() {
                    merge(vo);
                }
            }.execute();
            logger.info(String.format("granted %s to customized role[uuid:%s] statement[uuid:%s]", missingStr, roleUuid, vo.getUuid()));
            return true;
        }

        return false;
    }

    private static boolean anyStatementMatches(List<PolicyStatement> statements, PolicyStatementEffect effect, String api) {
        return statements.stream().anyMatch(s -> s.getEffect() == effect && matches(s, api));
    }

    private static boolean matches(PolicyStatement statement, String api) {
        if (statement.getActions() == null) {
            return false;
        }

        return statement.getActions().stream()
                .anyMatch(action -> matcher.match(PolicyUtils.apiNamePatternFromAction(action), api));
    }
}
