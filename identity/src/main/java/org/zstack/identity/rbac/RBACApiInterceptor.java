package org.zstack.identity.rbac;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.IdentityErrors;
import org.zstack.header.identity.PolicyStatement;
import org.zstack.header.identity.role.RoleIdentity;
import org.zstack.header.identity.role.RoleType;
import org.zstack.header.identity.role.RoleVO;
import org.zstack.header.identity.role.RoleVO_;
import org.zstack.header.identity.role.api.APIAddPolicyStatementsToRoleMsg;
import org.zstack.header.identity.role.api.APICreateRoleMsg;
import org.zstack.header.identity.role.api.APIDeleteRoleMsg;
import org.zstack.header.identity.role.api.APIUpdateRoleMsg;
import org.zstack.header.message.APIMessage;
import org.zstack.identity.CheckIfSessionCanOperationAdminPermission;
import org.zstack.utils.gson.JSONObjectUtil;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.err;
import static org.zstack.utils.CollectionDSL.list;

public class RBACApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIDeleteRoleMsg) {
            validate((APIDeleteRoleMsg) msg);
        } else if (msg instanceof APIUpdateRoleMsg) {
            validate((APIUpdateRoleMsg) msg);
        } else if (msg instanceof APIAddPolicyStatementsToRoleMsg) {
            validate((APIAddPolicyStatementsToRoleMsg) msg);
        } else if (msg instanceof APICreateRoleMsg) {
            validate((APICreateRoleMsg) msg);
        } 

        return msg;
    }

    private void validate(APICreateRoleMsg msg) {
        if (msg.getIdentity() == null){
            return;
        }

        RoleIdentity roleIdentity = RoleIdentity.valueOf(msg.getIdentity());

        if (msg.getStatements() == null) {
            return;
        }

        roleIdentity.getRoleIdentityValidators().forEach(validator -> validator.validateRolePolicy(roleIdentity, msg.getStatements()));
    }

    private void validate(APIAddPolicyStatementsToRoleMsg msg) {
        boolean sessionAccessToAdminActions = new CheckIfSessionCanOperationAdminPermission().check(msg.getSession());

        for (PolicyStatement s : msg.getStatements()) {
            if (s.getEffect() == null) {
                throw new ApiMessageInterceptionException(argerr("a statement must have effect field. Invalid statement[%s]", JSONObjectUtil.toJsonString(s)));
            }
            if (s.getActions() == null) {
                throw new ApiMessageInterceptionException(argerr("a statement must have action field. Invalid statement[%s]", JSONObjectUtil.toJsonString(s)));
            }
            if (s.getActions().isEmpty()) {
                throw new ApiMessageInterceptionException(argerr("a statement must have a non-empty action field. Invalid statement[%s]",
                        JSONObjectUtil.toJsonString(s)));
            }

            if (sessionAccessToAdminActions) {
                continue;
            }

            if (s.getActions() != null) {
                s.getActions().forEach(as -> {
                    if (PolicyUtils.isAdminOnlyAction(as)) {
                        throw new OperationFailureException(err(IdentityErrors.PERMISSION_DENIED, "normal accounts can't create admin-only action polices[%s]", as));
                    }
                });
            }
        }
    }

    private void validate(APIUpdateRoleMsg msg) {
        if (Q.New(RoleVO.class).in(RoleVO_.type, list(RoleType.Predefined, RoleType.System)).eq(RoleVO_.uuid, msg.getUuid()).isExists()) {
            throw new ApiMessageInterceptionException(argerr("cannot update a system or predefined role"));
        }

        RoleVO vo = Q.New(RoleVO.class).eq(RoleVO_.uuid, msg.getRoleUuid()).find();

        if (vo.getIdentity() == null) {
            return;
        }

        RoleIdentity roleIdentity = RoleIdentity.valueOf(vo.getIdentity());
        roleIdentity.getRoleIdentityValidators().forEach(validator -> validator.validateRolePolicy(roleIdentity, msg.getStatements()));
    }


    private void validate(APIDeleteRoleMsg msg) {
        if (Q.New(RoleVO.class).in(RoleVO_.type, list(RoleType.Predefined, RoleType.System)).eq(RoleVO_.uuid, msg.getUuid()).isExists()) {
            throw new ApiMessageInterceptionException(argerr("cannot delete a system or predefined role"));
        }
    }
}
