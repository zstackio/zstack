package org.zstack.identity.rbac;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.identity.rbac.PolicyMatcher;
import org.zstack.header.identity.rbac.RBACInfo;
import org.zstack.header.message.APIMessage;
import org.zstack.identity.APIRequestChecker;
import org.zstack.identity.AccountManager;

import static org.zstack.core.Platform.operr;

import javax.persistence.Tuple;
import java.util.*;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class OperationTargetAPIRequestChecker implements APIRequestChecker {
    protected APIMessage message;

    private static Map<Class, RBACInfo> rbacInfos = new HashMap<>();
    private static PolicyMatcher policyMatcher = new PolicyMatcher();

    @Autowired
    private AccountManager acntMgr;

    @Override
    public void check(APIMessage msg) {
        message = msg;
        check();
    }

    protected boolean isMatch(String as) {
        String ap = PolicyUtils.apiNamePatternFromAction(as);
        return policyMatcher.match(ap, message.getClass().getName());
    }

    private RBACInfo getRBACInfo() {
        return rbacInfos.computeIfAbsent(message.getClass(), x-> {
            for (RBACInfo rbacInfo : RBACInfo.getInfos()) {
                for (String s : rbacInfo.getNormalAPIs()) {
                    if (isMatch(s)) {
                        return rbacInfo;
                    }
                }
            }

            throw new CloudRuntimeException(String.format("cannot find RBACInfo for the API[%s]", message.getClass()));
        });
    }

    private void check() {
        if (RBACInfo.isAdminOnlyAPI(message.getClass().getName()) && AccountConstant.INITIAL_SYSTEM_ADMIN_UUID.equals(message.getSession().getAccountUuid())) {
            return;
        }

        RBACInfo info = getRBACInfo();

        new SQLBatch() {
            @Override
            protected void scripts() {
                APIMessage.getApiParams().get(message.getClass()).forEach(this::checkOperationTarget);
            }

            private void checkOperationTarget(APIMessage.FieldParam param) {
                Class resourceType = param.param.resourceType();
                if (resourceType == Object.class)  {
                    return;
                }

                if (!acntMgr.isResourceHavingAccountReference(resourceType)) {
                    return;
                }

                //TODO: remove
                if (info.getTargetResource() == null) {
                    return;
                }

                try {
                    if (resourceType.isAssignableFrom(info.getTargetResource())) {
                        checkIfTheAccountOwnTheResource(param);
                    } else {
                        checkIfTheAccountCanAccessTheResource(param);
                    }
                } catch (OperationFailureException oe) {
                    throw oe;
                } catch (Exception e) {
                    throw new CloudRuntimeException(e);
                }
            }


            private List<String> getResourceUuids(APIMessage.FieldParam param) throws IllegalAccessException {
                List<String> uuids = new ArrayList<>();
                if (String.class.isAssignableFrom(param.field.getType())) {
                    uuids.add((String) param.field.get(message));
                } else if (Collection.class.isAssignableFrom(param.field.getType())) {
                    uuids.addAll((Collection<? extends String>) param.field.get(message));
                } else {
                    throw new CloudRuntimeException(String.format("not supported field type[%s] for %s#%s", param.field.getType(), message.getClass(), param.field.getName()));
                }
                return uuids;
            }

            private void checkIfTheAccountCanAccessTheResource(APIMessage.FieldParam param) throws IllegalAccessException {
                List<String> uuids = getResourceUuids(param);

                Class resourceType = param.param.resourceType();
                List<Tuple> ts = q(AccountResourceRefVO.class).select(AccountResourceRefVO_.accountUuid, AccountResourceRefVO_.resourceUuid).in(AccountResourceRefVO_.resourceUuid, uuids)
                        .eq(AccountResourceRefVO_.resourceType, acntMgr.getBaseResourceType(resourceType).getSimpleName())
                        .groupBy(AccountResourceRefVO_.accountUuid)
                        .listTuple();

                ts.forEach(t -> {
                    String accountUuid = t.get(0, String.class);
                    String resourceUuid = t.get(1, String.class);
                    if (!message.getSession().getAccountUuid().equals(accountUuid)) {
                        throw new OperationFailureException(operr("permission denied, the account[uuid:%s] is not the owner of the resource[uuid:%s, type:%s]",
                                message.getSession().getAccountUuid(), resourceUuid, resourceType.getSimpleName()));
                    }
                });
            }

            private void checkIfTheAccountOwnTheResource(APIMessage.FieldParam param) throws IllegalAccessException {
                List<String> uuids = getResourceUuids(param);
                Class resourceType = param.param.resourceType();

                List<String> auuids = sql("select ref.resourceUuid from AccountResourceRefVO ref where ((ref.ownerAccountUuid = :accountUuid and ref.resourceType = :rtype)" +
                        " or ref.resourceUuid in (select sh.resourceUuid from SharedResourceVO sh where (sh.receiverAccountUuid = :accountUuid or sh.toPublic = 1) and sh.resourceType = :rtype))" +
                        " and ref.resourceUuid in (:uuids) group by ref.resourceUuid", String.class)
                        .param("accountUuid", message.getSession().getAccountUuid())
                        .param("rtype", acntMgr.getBaseResourceType(resourceType).getSimpleName())
                        .param("uuids", uuids)
                        .list();

                if (auuids.size() != uuids.size()) {
                    uuids.forEach(uuid -> {
                        if (!auuids.contains(uuid)) {
                            throw new OperationFailureException(operr("the account[uuid:%s] has no access to the resource[uuid:%s, type:%s]",
                                    message.getSession().getAccountUuid(), uuid, resourceType.getSimpleName()));
                        }
                    });
                }
            }
        }.execute();
    }
}
