package org.zstack.identity.rbac;

import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.identity.role.RoleType;
import org.zstack.header.identity.role.RoleVO;
import org.zstack.header.identity.role.RoleVO_;
import org.zstack.header.identity.role.api.APIDeleteRoleMsg;
import org.zstack.header.message.APIMessage;

import static org.zstack.core.Platform.argerr;

import static org.zstack.utils.CollectionDSL.list;

public class RBACApiInterceptor implements ApiMessageInterceptor {
    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIDeleteRoleMsg) {
            validate((APIDeleteRoleMsg) msg);
        }

        return msg;
    }

    private void validate(APIDeleteRoleMsg msg) {
        if (Q.New(RoleVO.class).in(RoleVO_.type, list(RoleType.Predefined, RoleType.System)).eq(RoleVO_.uuid, msg.getUuid()).isExists()) {
            throw new ApiMessageInterceptionException(argerr("cannot delete a system or predefined role"));
        }
    }
}
