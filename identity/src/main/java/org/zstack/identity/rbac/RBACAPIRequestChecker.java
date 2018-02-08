package org.zstack.identity.rbac;

import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.PolicyInventory;
import org.zstack.header.message.APIMessage;
import org.zstack.identity.APIRequestChecker;
import org.zstack.identity.rbac.datatype.Entity;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class RBACAPIRequestChecker implements APIRequestChecker {
    private static final CLogger logger = Utils.getLogger(RBACAPIRequestChecker.class);

    private APIMessage message;

    private List<PolicyInventory> getPolicies() {
        return new ArrayList<>();
    }

    @Override
    public void check(APIMessage msg) {
        message = msg;
        check();
    }

    private void check() {
        List<PolicyInventory> polices = getPolicies();
        List<PolicyInventory> denyStatements = collectDenyStatements(polices);
        List<PolicyInventory> allowStatements = collectDenyStatements(polices);

        evalDenyStatements(denyStatements);

        if (evalAllowStatements(allowStatements)) {
            // allowed
            return;
        }

        // no polices applied to the operation, deny by default
        throw new OperationFailureException(operr("operation is denied by default"));
    }

    private boolean evalAllowStatements(List<PolicyInventory> policies) {
        for (PolicyInventory policy : policies) {
            for (PolicyInventory.Statement statement : policy.getStatements()) {
                for (String as : statement.getActions()) {
                    if (evalAllowStatement(policy, as)) {
                        if (logger.isTraceEnabled()) {
                            logger.trace(String.format("[RBAC] policy[name:%s, uuid:%s]'s statement[%s] allows the API:\n%s", policy.getName(),
                                    policy.getUuid(), statement, JSONObjectUtil.toJsonString(message)));
                        }
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean evalAllowStatement(PolicyInventory p, String as) {
        String[] ss = as.split(":",2);
        String apiName = ss[0];

        Pattern pattern = Pattern.compile(apiName);
        return pattern.matcher(message.getClass().getName()).matches();
    }

    private void evalDenyStatements(List<PolicyInventory> denyPolices) {
        // action string format is:
        // api-full-name:optional-api-field-list-split-by-comma
        denyPolices.forEach(p -> p.getStatements().forEach(st -> st.getActions().forEach(statement -> {
            String[] ss = statement.split(":",2);
            String apiName = ss[0];
            String apiFields = null;
            if (ss.length > 1) {
                apiFields = ss[1];
            }

            Pattern pattern = Pattern.compile(apiName);
            if (!pattern.matcher(message.getClass().getName()).matches()) {
                // the statement not matching this API
                return;
            }

            // the statement matching this API

            if (apiFields == null) {
                // no API fields specified, the API is denied by this statement
                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("[RBAC] policy[name:%s, uuid:%s]'s statement[%s] denies the API:\n%s", p.getName(),
                            p.getUuid(), statement, JSONObjectUtil.toJsonString(message)));
                }

                throw new OperationFailureException(operr("the operation is denied by the policy[uuid:%s]", p.getUuid()));
            }

            Entity entity = Entity.getEntity(message.getClass());

            for (String fname : apiFields.split(",")) {
                Field field = entity.getFields().get(fname);
                try {
                    if (field != null && field.get(message) != null) {
                        if (logger.isTraceEnabled()) {
                            logger.trace(String.format("[RBAC] policy[name:%s, uuid:%s]'s statement[%s] denies the API:\n%s", p.getName(),
                                    p.getUuid(), statement, JSONObjectUtil.toJsonString(message)));
                        }
                        throw new OperationFailureException(operr("the operation is denied by the policy[uuid:%s], field[%s] is not permitted to set", p.getUuid(), fname));
                    }
                } catch (IllegalAccessException e) {
                    throw new CloudRuntimeException(e);
                }
            }
        })));
    }

    private List<PolicyInventory> collectDenyStatements(List<PolicyInventory> polices) {
        return null;
    }
}
