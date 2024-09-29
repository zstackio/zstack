package org.zstack.identity.rbac;

import org.zstack.core.cloudbus.CloudBusGson;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.identity.*;
import org.zstack.header.identity.rbac.PolicyMatcher;
import org.zstack.header.identity.rbac.RBACEntity;
import org.zstack.header.identity.rbac.SuppressRBACCheck;
import org.zstack.header.identity.role.RolePolicyStatementVO;
import org.zstack.identity.APIRequestChecker;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;

public class RBACAPIRequestChecker implements APIRequestChecker {
    protected static final CLogger logger = Utils.getLogger(RBACAPIRequestChecker.class);

    protected RBACEntity rbacEntity;
    protected PolicyMatcher policyMatcher = new PolicyMatcher();

    public boolean bypass(RBACEntity entity) {
        return entity.getApiMessage().getHeaders().containsKey(IdentityByPassCheck.NoRBACCheck.toString());
    }

    @Override
    public void check(RBACEntity entity) {
        rbacEntity = entity;
        if (rbacEntity.getApiMessage().getClass().isAnnotationPresent(SuppressRBACCheck.class)) {
            return;
        }

        if (PolicyUtils.isAdminOnlyAction(entity.getApiName()) && !AccountConstant.isAdminPermission(entity.getApiMessage().getSession())) {
            throw new OperationFailureException(err(IdentityErrors.PERMISSION_DENIED,
                    "request api[name: %s] is admin only, can not be executed by current user",
                    entity.getApiName()));
        }

        check();
    }

    public boolean evalStatement(String as, String msgName) {
        String ap = PolicyUtils.apiNamePatternFromAction(as);
        return policyMatcher.match(ap, msgName);
    }

    protected List<RolePolicyStatementVO> getPoliciesForAPI() {
        return new ArrayList<>(); // TODO
    }

    /**
     * rule evaluation order:
     * 3. if any user defined policy denies the API, deny
     * 4. if any user defined policy allows the API, allow
     * 5. then deny by default
     */
    protected void check() {
        List<RolePolicyStatementVO> policies = getPoliciesForAPI();

        if (evalAllowStatements(policies)) {
            // allowed
            return;
        }

        if (logger.isTraceEnabled()) {
            logger.trace(String.format("[RBAC]operation is denied by default, API:\n%s", jsonMessage()));
        }

        // no policies applied to the operation, deny by default
        throw new OperationFailureException(operr("operation[API:%s] is denied by default, please contact admin to correct it", rbacEntity.getApiMessage().getClass().getName()));
    }

    private String jsonMessage() {
        return CloudBusGson.toLogSafeJson(rbacEntity.getApiMessage());
    }

    protected boolean evalAllowStatements(List<RolePolicyStatementVO> policies) {
        // TODO
        return true;
    }

    public Map<String, Boolean> evalAPIPermission(List<Class> classes, SessionInventory session) {
        List<RolePolicyStatementVO> policies = getPoliciesForAPI();
        // TODO
        return new HashMap<>();
    }

}
