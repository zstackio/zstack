package org.zstack.identity.rbac;

import org.zstack.core.cloudbus.CloudBusGson;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.*;
import org.zstack.header.identity.rbac.PolicyMatcher;
import org.zstack.header.identity.rbac.RBAC;
import org.zstack.header.identity.rbac.RBACEntity;
import org.zstack.header.identity.rbac.SuppressRBACCheck;
import org.zstack.identity.APIRequestChecker;
import org.zstack.identity.rbac.datatype.Entity;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.Field;
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

    protected List<PolicyInventory> getPoliciesForAPI() {
        return RBACManager.getPoliciesByAPI(rbacEntity.getApiMessage());
    }

    /**
     * rule evaluation order:
     * 3. if any user defined policy denies the API, deny
     * 4. if any user defined policy allows the API, allow
     * 5. then deny by default
     */
    protected void check() {
        List<PolicyInventory> polices = getPoliciesForAPI();
        Map<PolicyInventory, List<PolicyStatement>> denyStatements = RBACManager.collectDenyStatements(polices);
        Map<PolicyInventory, List<PolicyStatement>> allowStatements = RBACManager.collectAllowedStatements(polices);

        evalDenyStatements(denyStatements);

        if (evalAllowStatements(allowStatements)) {
            // allowed
            return;
        }

        if (logger.isTraceEnabled()) {
            logger.trace(String.format("[RBAC]operation is denied by default, API:\n%s", jsonMessage()));
        }

        // no polices applied to the operation, deny by default
        throw new OperationFailureException(operr("operation[API:%s] is denied by default, please contact admin to correct it", rbacEntity.getApiMessage().getClass().getName()));
    }

    private String jsonMessage() {
        return CloudBusGson.toLogSafeJson(rbacEntity.getApiMessage());
    }

    protected boolean evalAllowStatements(Map<PolicyInventory, List<PolicyStatement>> policies) {
        Set<String> apiNeedToCheck = new HashSet<>();
        apiNeedToCheck.add(rbacEntity.getApiName());
        apiNeedToCheck.addAll(rbacEntity.getAdditionalApisToCheck());

        for (Map.Entry<PolicyInventory, List<PolicyStatement>> e : policies.entrySet()) {
            PolicyInventory policy = e.getKey();
            for (PolicyStatement statement : e.getValue()) {
                if (!isPrincipalMatched(statement.getPrincipals())) {
                    continue;
                }

                for (String as : statement.getActions()) {
                    apiNeedToCheck.removeIf(api -> evalAllowStatement(as, api));

                    if (apiNeedToCheck.isEmpty()) {
                        if (logger.isTraceEnabled()) {
                            logger.trace(String.format("[RBAC] policy[name:%s, uuid:%s]'s statement[%s] allows the API:\n%s", policy.getName(),
                                    policy.getUuid(), as, jsonMessage()));
                        }
                        return true;
                    }
                }
            }
        }

        return false;
    }

    protected boolean evalAllowStatement(String as, String targetApiName) {
        String ap = PolicyUtils.apiNamePatternFromAction(as, true);
        return RBAC.checkAPIPermission(rbacEntity.getApiMessage(), policyMatcher.match(ap, targetApiName));
    }

    protected boolean isPrincipalMatched(List<String> principals) {
        // if not principals specified, means the statement applies for all accounts/users
        // if principals specified, check if they matches current account/user
        if (principals != null && !principals.isEmpty()) {
            for (String s : principals) {
                String[] ss = s.split(":", 2);
                String principal = ss[0];
                String uuidRegex = ss[1];

                if (rbacEntity.getApiMessage().getSession().isAccountSession() && AccountConstant.PRINCIPAL_ACCOUNT.equals(principal)) {
                    if (checkAccountPrincipal(uuidRegex)) {
                        return true;
                    }
                } else if (AccountConstant.isAdminPermission(rbacEntity.getApiMessage().getSession())) {
                    if (checkAccountPrincipal(uuidRegex)) {
                        return true;
                    }
                } else if (rbacEntity.getApiMessage().getSession().isUserSession() && AccountConstant.PRINCIPAL_USER.equals(principal)) {
                    if (checkUserPrincipal(uuidRegex)) {
                        return true;
                    }
                } else {
                    throw new CloudRuntimeException(String.format("unknown principal[%s]", principal));
                }
            }

            return false;
        } else {
            return true;
        }
    }

    private boolean additionalApiMatch(String apiName) {
        return rbacEntity.getAdditionalApisToCheck().stream().anyMatch(api -> policyMatcher.match(apiName, api));
    }

    protected void evalDenyStatements(Map<PolicyInventory, List<PolicyStatement>> denyPolices) {
        // action string format is:
        // api-full-name:optional-api-field-list-split-by-comma
        denyPolices.forEach((p, sts)-> sts.forEach(st-> {
            if (!isPrincipalMatched(st.getPrincipals())) {
                return;
            }

            st.getActions().forEach(statement-> {
                String[] ss = statement.split(":",2);
                String apiName = ss[0];
                String apiFields = null;
                if (ss.length > 1) {
                    apiFields = ss[1];
                }

                if (!policyMatcher.match(apiName, rbacEntity.getApiName()) && !additionalApiMatch(apiName)) {
                    // the statement not matching this API and not additional api need to check
                    return;
                }

                // the statement matching this API

                if (apiFields == null) {
                    // no API fields specified, the API is denied by this statement
                    if (logger.isTraceEnabled()) {
                        logger.trace(String.format("[RBAC] policy[name:%s, uuid:%s]'s statement[%s] denies the API:\n%s", p.getName(),
                                p.getUuid(), statement, jsonMessage()));
                    }

                    throw new OperationFailureException(operr("the operation is denied by the policy[name:%s uuid:%s]", p.getName(), p.getUuid()));
                }

                Entity entity = Entity.getEntity(rbacEntity.getApiMessage().getClass());

                for (String fname : apiFields.split(",")) {
                    Field field = entity.getFields().get(fname);
                    try {
                        if (field != null && field.get(rbacEntity.getApiMessage()) != null) {
                            if (logger.isTraceEnabled()) {
                                logger.trace(String.format("[RBAC] policy[name:%s, uuid:%s]'s statement[%s] denies the API:\n%s", p.getName(),
                                        p.getUuid(), statement, jsonMessage()));
                            }
                            throw new OperationFailureException(operr("the operation is denied by the policy[name:%s, uuid:%s], field[%s] is not permitted to set", p.getName(), p.getUuid(), fname));
                        }
                    } catch (IllegalAccessException e) {
                        throw new CloudRuntimeException(e);
                    }
                }
            });
        }));
    }

    protected boolean checkUserPrincipal(String uuidRegex) {
        return policyMatcher.match(uuidRegex, rbacEntity.getApiMessage().getSession().getUserUuid());
    }

    protected boolean checkAccountPrincipal(String uuidRegex) {
        return policyMatcher.match(uuidRegex, rbacEntity.getApiMessage().getSession().getAccountUuid());
    }

    public Map<String, Boolean> evalAPIPermission(List<Class> classes, SessionInventory session) {
        List<PolicyInventory> policies = RBACManager.getPoliciesBySession(session);
        Map<PolicyInventory, List<PolicyStatement>> denyStatements = RBACManager.collectDenyStatements(policies);
        Map<PolicyInventory, List<PolicyStatement>> allowStatements = RBACManager.collectAllowedStatements(policies);

        Map<String, Boolean> ret = new HashMap<>();
        classes.forEach(clz -> {
            for (List<PolicyStatement> dss : denyStatements.values()) {
                for (PolicyStatement ds : dss) {
                    for (String as : ds.getActions()) {
                        if (evalStatement(as, clz.getName())) {
                            ret.put(clz.getName(), false);
                            return;
                        }
                    }
                }
            }

            for (List<PolicyStatement> ass : allowStatements.values()) {
                for (PolicyStatement as : ass) {
                    for (String a : as.getActions()) {
                        if (evalStatement(a, clz.getName())) {
                            ret.put(clz.getName(), true);
                            return;
                        }
                    }
                }
            }

            ret.put(clz.getName(), false);
        });

        return ret;
    }

}
