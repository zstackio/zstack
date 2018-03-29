package org.zstack.identity;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.Component;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.IdentityErrors;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.identity.SuppressCredentialCheck;
import org.zstack.header.identity.extension.AuthorizationBackend;
import org.zstack.header.message.APIMessage;
import org.zstack.identity.rbac.RBACAPIRequestChecker;
import org.zstack.utils.BeanUtils;

import static org.zstack.core.Platform.err;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AuthorizationManager implements GlobalApiMessageInterceptor, Component {
    private Set apiByPassAuthorizationCheck = new HashSet<>();

    @Autowired
    private PluginRegistry pluginRegistry;
    @Autowired
    private DefaultAuthorizationBackend defaultAuthorizationBackend;

    private List<AuthorizationBackend> authorizationBackends;

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

    private SessionInventory evaluateSession(APIMessage msg) {
        if (msg.getSession() == null) {
            throw new ApiMessageInterceptionException(err(IdentityErrors.INVALID_SESSION,
                    "session of message[%s] is null", msg.getMessageName()));
        }

        if (msg.getSession().getUuid() == null) {
            throw new ApiMessageInterceptionException(err(IdentityErrors.INVALID_SESSION,
                    "session uuid is null"));
        }

        Session session = new Session();
        session.errorOnTimeout(msg.getSession().getUuid());
        msg.setSession(session.getSession(msg.getSession().getUuid()));
        return msg.getSession();
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (apiByPassAuthorizationCheck.contains(msg.getClass())) {
            return msg;
        }

        SessionInventory session = evaluateSession(msg);
        AuthorizationBackend backend = defaultAuthorizationBackend;
        for (AuthorizationBackend b : authorizationBackends) {
            if (b.takeOverAuthorization(session)) {
                backend = b;
                break;
            }
        }

        return backend.authorize(msg);
    }

    @Override
    public boolean start() {
        authorizationBackends = pluginRegistry.getExtensionList(AuthorizationBackend.class);
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
