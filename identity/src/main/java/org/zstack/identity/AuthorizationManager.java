package org.zstack.identity;

import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import org.zstack.identity.rbac.RBACAPIRequestChecker;

import java.util.ArrayList;
import java.util.List;

public class AuthorizationManager implements GlobalApiMessageInterceptor {
    @Override
    public List<Class> getMessageClassToIntercept() {
        // intercept all
        return null;
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.FRONT;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        List<APIRequestChecker> checkers = new ArrayList<>();
        checkers.add(new SessionAPIRequestChecker());
        checkers.add(new AccountAPIRequestChecker());
        checkers.add(new RBACAPIRequestChecker());

        checkers.forEach(c->c.check(msg));

        return msg;
    }
}
