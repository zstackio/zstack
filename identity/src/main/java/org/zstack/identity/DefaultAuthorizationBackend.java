package org.zstack.identity;

import org.zstack.header.identity.SessionInventory;
import org.zstack.header.identity.extension.AuthorizationBackend;
import org.zstack.header.message.APIMessage;
import org.zstack.identity.rbac.OperationTargetAPIRequestChecker;
import org.zstack.identity.rbac.RBACAPIRequestChecker;

import java.util.ArrayList;
import java.util.List;

public class DefaultAuthorizationBackend implements AuthorizationBackend {
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

        checkers.forEach(c-> {
            if (!c.bypass(msg)) {
                c.check(msg);
            }
        });
        return msg;
    }
}
