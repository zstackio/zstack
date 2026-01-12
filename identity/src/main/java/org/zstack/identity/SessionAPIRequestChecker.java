package org.zstack.identity;

import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.identity.IdentityErrors;
import org.zstack.header.identity.rbac.RBACEntity;
import static org.zstack.core.Platform.*;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

public class SessionAPIRequestChecker implements APIRequestChecker {
    @Override
    public void check(RBACEntity entity) {
        if (entity.getApiMessage().getSession() == null) {
            throw new ApiMessageInterceptionException(err(ORG_ZSTACK_IDENTITY_10006, IdentityErrors.INVALID_SESSION,
                    "session of message[%s] is null", entity.getApiMessage().getMessageName()));
        }

        if (entity.getApiMessage().getSession().getUuid() == null) {
            throw new ApiMessageInterceptionException(err(ORG_ZSTACK_IDENTITY_10007, IdentityErrors.INVALID_SESSION,
                    "session uuid is null"));
        }

        Session.errorOnTimeout(entity.getApiMessage().getSession().getUuid());
        entity.getApiMessage().setSession(Session.getSession(entity.getApiMessage().getSession().getUuid()));
    }
}
