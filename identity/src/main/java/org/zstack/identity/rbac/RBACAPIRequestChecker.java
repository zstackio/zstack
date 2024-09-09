package org.zstack.identity.rbac;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBusGson;
import org.zstack.core.db.Q;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.identity.*;
import org.zstack.header.identity.rbac.RBACEntity;
import org.zstack.header.identity.rbac.SuppressRBACCheck;
import org.zstack.header.identity.role.RoleAccountRefVO;
import org.zstack.header.identity.role.RoleAccountRefVO_;
import org.zstack.header.identity.role.RolePolicyEffect;
import org.zstack.header.identity.role.RolePolicyVO;
import org.zstack.header.identity.role.RolePolicyVO_;
import org.zstack.header.message.APIMessage;
import org.zstack.identity.APIRequestChecker;
import org.zstack.identity.Account;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.*;

import static org.zstack.core.Platform.err;
import static org.zstack.header.errorcode.SysErrors.OPERATION_DENIED;
import static org.zstack.utils.CollectionUtils.*;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class RBACAPIRequestChecker implements APIRequestChecker {
    protected static final CLogger logger = Utils.getLogger(RBACAPIRequestChecker.class);

    protected APIMessage apiMessage;
    protected Class<?> apiClass;
    protected SessionInventory session;

    @Autowired(required = false)
    private ResourcePolicyCheckerFactory resourcePolicyCheckerFactory;
    private ResourcePolicyChecker resourcePolicyChecker;

    public boolean bypass(RBACEntity entity) {
        return entity.getApiMessage().getHeaders().containsKey(IdentityByPassCheck.NoRBACCheck.toString());
    }

    @Override
    public void check(RBACEntity entity) {
        apiMessage = entity.getApiMessage();
        apiClass = apiMessage.getClass();
        session = apiMessage.getSession();

        if (Account.isAdminPermission(session)) {
            return;
        }

        if (apiClass.isAnnotationPresent(SuppressRBACCheck.class)) {
            return;
        }

        if (PolicyUtils.isAdminOnlyAction(entity.getApiName())) {
            throw new OperationFailureException(err(OPERATION_DENIED,
                    "operation[API:%s] is denied: admin-only API",
                    entity.getApiName()));
        }

        if (!check()) {
            permissionDenied();
        }

//        entity.getAdditionalApisToCheck() TODO check addition apis | AdditionalApis need to convent to Class<?>
    }

    private void permissionDenied() {
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("[RBAC]operation is denied by default, API:%n%s", jsonMessage()));
        }

        // no policies applied to the operation, deny by default
        throw new OperationFailureException(err(OPERATION_DENIED,
                "operation[API:%s] is denied: no permission", apiClass.getName()));
    }

    protected List<RolePolicyVO> getPoliciesForAPI() {
        String accountUuid = session.getAccountUuid();
        String api = apiClass.getName();

        HashSet<String> apiPatterns = new HashSet<>(PolicyUtils.findAllMatchedApiPatterns(api));
        return Q.New(RoleAccountRefVO.class, RolePolicyVO.class)
                .table0()
                    .eq(RoleAccountRefVO_.accountUuid, accountUuid)
                    .eq(RoleAccountRefVO_.roleUuid).table1(RolePolicyVO_.roleUuid)
                .table1()
                    .in(RolePolicyVO_.actions, apiPatterns)
                    .selectThisTable()
                .list();
    }

    protected boolean check() {
        List<RolePolicyVO> policies = getPoliciesForAPI();
        return evalStatements(policies);
    }

    private String jsonMessage() {
        if (apiMessage == null) {
            JsonObject object = new JsonObject();
            object.addProperty("api", apiClass.getName());
            object.add("session", JSONObjectUtil.rehashObject(session, JsonElement.class));
            return object.toString();
        }

        return CloudBusGson.toLogSafeJson(apiMessage);
    }

    protected boolean evalStatements(List<RolePolicyVO> policies) {
        if (isEmpty(policies)) {
            return false;
        }

        final Map<String, List<RolePolicyVO>> rolePoliciesMap = groupBy(policies, RolePolicyVO::getRoleUuid);
        List<List<RolePolicyVO>> expendCheckList = new ArrayList<>();

        OUT_LOOP:
        for (List<RolePolicyVO> policiesToCheck : rolePoliciesMap.values()) {

            // first: no wildcards   (".header.vm.APIStartVmInstanceMsg")
            // second: end with "*"  (".header.vm.*")
            // last: end with "**"   (".header.vm.**") > (".header.**") > (".**") sort by string length

            // others:
            //    effect Exclude > Allow
            policiesToCheck.sort(Comparator
                .comparingInt((RolePolicyVO p) ->
                        (p.getActions().endsWith(".**") ? 2 : (p.getActions().endsWith(".*") ? 1 : 0)))
                .thenComparingInt((RolePolicyVO p) -> -p.getActions().length())
                .thenComparingInt((RolePolicyVO p) -> p.getEffect() == RolePolicyEffect.Exclude ? 0 : 1));

            boolean allPoliciesWithoutResources = policiesToCheck.stream()
                    .allMatch(policy -> isEmpty(policy.getResourceRefs()));

            if (allPoliciesWithoutResources) {
                for (RolePolicyVO policy : policiesToCheck) {
                    if (policy.getEffect() == RolePolicyEffect.Exclude) {
                        continue OUT_LOOP;
                    }
                    if (policy.getEffect() == RolePolicyEffect.Allow) {
                        return true;
                    }
                }
                continue;
            }

            Map<String, Boolean> resourceTypeAllowMap = new HashMap<>();

            RolePolicyVO last = new RolePolicyVO();
            last.setEffect(RolePolicyEffect.Exclude);
            policiesToCheck.add(last);

            for (RolePolicyVO policy : policiesToCheck) {
                if (policy.getEffect() == RolePolicyEffect.Exclude) { // or find the last
                    if (resourceTypeAllowMap.values().stream().allMatch(b -> b)) {
                        return true;
                    }
                    continue OUT_LOOP;
                }

                if (policy.getEffect() == RolePolicyEffect.Allow) {
                    if (isEmpty(policy.getResourceRefs())) {
                        return true;
                    }
                    resourceTypeAllowMap.put(policy.getResourceType(), matchResource(policy));
                }
            }
        }

        return false;
    }

    private boolean matchResource(RolePolicyVO policy) {
        if (resourcePolicyCheckerFactory == null) {
            return false;
        }
        if (resourcePolicyChecker == null) {
            resourcePolicyChecker = resourcePolicyCheckerFactory.build(APIMessage.getApiParams().get(apiClass), apiMessage);
        }

        return resourcePolicyChecker.matchResources(policy);
    }

    public Map<String, Boolean> evalAPIPermission(List<Class> classes, SessionInventory session) {
        this.session = session;

        Map<String, Boolean> apiClassNamePassMap = new HashMap<>();
        for (Class<?> apiClass : classes) {
            this.apiClass = apiClass;
            apiClassNamePassMap.put(apiClass.getName(), check());
        }

        return apiClassNamePassMap;
    }
}
