package org.zstack.identity;

import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.identity.SuppressCredentialCheck;
import org.zstack.header.message.APIMessage;
import org.zstack.identity.rbac.RBACAPIRequestChecker;
import org.zstack.utils.BeanUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AuthorizationManager implements GlobalApiMessageInterceptor {
    private Set apiByPassAuthorizationCheck = new HashSet<>();

    @Override
    public List<Class> getMessageClassToIntercept() {
        // intercept all
        return null;
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.FRONT;
    }

    void init() {
        apiByPassAuthorizationCheck = BeanUtils.reflections.getTypesAnnotatedWith(SuppressCredentialCheck.class);
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (apiByPassAuthorizationCheck.contains(msg.getClass())) {
            return msg;
        }

        List<APIRequestChecker> checkers = new ArrayList<>();
        checkers.add(new SessionAPIRequestChecker());
        checkers.add(new AccountAPIRequestChecker());
        checkers.add(new RBACAPIRequestChecker());
        checkers.add(new QuotaAPIRequestChecker());

        checkers.forEach(c->c.check(msg));

        return msg;
    }
}
