package org.zstack.identity;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBusGson;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.identity.extension.AuthorizationBackend;
import org.zstack.header.identity.rbac.RBAC;
import org.zstack.header.message.APIMessage;
import org.zstack.identity.rbac.OperationTargetAPIRequestChecker;
import org.zstack.identity.rbac.RBACAPIRequestChecker;
import org.zstack.identity.rbac.RBACManager;
import org.zstack.header.identity.rbac.RBACEntity;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

public class DefaultAuthorizationBackend implements AuthorizationBackend {
    private static final CLogger logger = Utils.getLogger(DefaultAuthorizationBackend.class);

    @Autowired
    private RBACManager rbacManager;

    @Override
    public boolean takeOverAuthorization(SessionInventory session) {
        return true;
    }

    @Override
    public APIMessage authorize(APIMessage msg) {
        List<APIRequestChecker> checkers = new ArrayList<>();
        checkers.add(new RBACAPIRequestChecker());
        checkers.add(new OperationTargetAPIRequestChecker());
        checkers.add(new QuotaAPIRequestChecker());

        try {
            RBACEntity entity = RBAC.formatRBACEntity(new RBACEntity(msg));
            checkers.forEach(c -> {
                if (!c.bypass(entity)) {
                    c.check(entity);
                }
            });
        } catch (OperationFailureException e) {
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("%s, %s", e.getErrorCode(), CloudBusGson.toJson(msg)));
            }

            throw e;
        }

        return msg;
    }
}
