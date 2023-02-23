package org.zstack.identity;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBusGson;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.Component;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.identity.IdentityByPassCheck;
import org.zstack.header.identity.IdentityErrors;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.identity.SuppressCredentialCheck;
import org.zstack.header.identity.extension.AuthorizationBackend;
import org.zstack.header.message.APIMessage;
import org.zstack.header.zql.ZQLQueryExtensionPoint;
import org.zstack.query.QueryHelper;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;

public class AuthorizationManager implements GlobalApiMessageInterceptor, Component, ZQLQueryExtensionPoint {
    private static final CLogger logger = Utils.getLogger(AuthorizationManager.class);

    private Set apiByPassAuthorizationCheck = new HashSet<>();

    @Autowired
    private PluginRegistry pluginRegistry;
    @Autowired
    private DefaultAuthorizationBackend defaultAuthorizationBackend;

    private List<AuthorizationBackend> authorizationBackends;

    private Cache<String, AuthorizationBackend> authorizationBackendsCache = CacheBuilder.newBuilder()
            .maximumSize(IdentityGlobalProperty.AUTHORIZATION_SESSION_CACHE_SIZE)
            .build();


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

        msg.setSession(Session.renewSession(msg.getSession().getUuid(), null));
        return msg.getSession();
    }

    private AuthorizationBackend findAuthorizationBackend(SessionInventory session) {
        AuthorizationBackend bkd = session.getUuid() == null ? null : authorizationBackendsCache.getIfPresent(session.getUuid());
        if (bkd == null) {
            bkd = defaultAuthorizationBackend;
            for (AuthorizationBackend b : authorizationBackends) {
                if (b.takeOverAuthorization(session)) {
                    bkd = b;
                    break;
                }
            }

            if (session.getUuid() != null) {
                authorizationBackendsCache.put(session.getUuid(), bkd);
            }
        }

        return bkd;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (apiByPassAuthorizationCheck.contains(msg.getClass())) {
            return msg;
        }

        SessionInventory session;
        if (msg.getHeaders().containsKey(IdentityByPassCheck.NoSessionEvaluation.toString())) {
            session = msg.getSession();
            DebugUtils.Assert(session != null, String.format("NoSessionEvaluation set but no session set in the message: %s", CloudBusGson.toJson(msg)));
        } else {
            session = evaluateSession(msg);
        }

        logger.trace(String.format("authorizing message[%s] with user[accountUuid:%s, uuid:%s] session",
                msg.getMessageName(),
                session.getAccountUuid(),
                session.getUserUuid()));
        return findAuthorizationBackend(session).authorize(msg);
    }

    @Override
    public boolean start() {
        authorizationBackends = pluginRegistry.getExtensionList(AuthorizationBackend.class);
        authorizationBackends.remove(defaultAuthorizationBackend);
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void beforeQueryExtensionPoint(List<Class> queryTargetInventoryClass, SessionInventory session) {
        if (session == null) {
            return;
        }

        List<Class> targetApis = queryTargetInventoryClass.stream()
                .map(clz -> QueryHelper.inventoryQueryMessageMap.get(clz))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        findAuthorizationBackend(session).validatePermission(targetApis, session);
    }

    public static ErrorCode createAdditionAuthErrorCode(String credentials, String... authentications) {
        JsonObject o = new JsonObject();
        JsonArray authArray = new JsonArray(authentications.length);
        Arrays.asList(authentications).forEach(authArray::add);
        o.add("authentications", authArray);
        o.addProperty("credentials", credentials);
        return err(IdentityErrors.NEED_ADDITION_AUTHENTICATION, "%s", o.toString());
    }
}
