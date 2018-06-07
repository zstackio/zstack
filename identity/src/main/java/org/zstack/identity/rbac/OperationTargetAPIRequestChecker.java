package org.zstack.identity.rbac;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.*;
import org.zstack.header.identity.rbac.PolicyMatcher;
import org.zstack.header.identity.rbac.RBAC;
import org.zstack.header.identity.rbac.RBACInfo;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.identity.APIRequestChecker;
import org.zstack.identity.AccountManager;
import static org.zstack.core.Platform.*;

import javax.persistence.Tuple;
import java.util.*;
import java.util.stream.Collectors;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class OperationTargetAPIRequestChecker implements APIRequestChecker {
    protected APIMessage message;

    private static Map<Class, RBACInfo> rbacInfos =  Collections.synchronizedMap(new HashMap<>());
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
            for (RBACInfo rbacInfo : RBAC.getRbacInfos()) {
                for (String s : rbacInfo.getNormalAPIs()) {
                    if (isMatch(s)) {
                        return rbacInfo;
                    }
                }

                for (String s : rbacInfo.getAdminOnlyAPIs()) {
                    if (isMatch(s)) {
                        return rbacInfo;
                    }
                }
            }

            throw new CloudRuntimeException(String.format("cannot find RBACInfo for the API[%s]", message.getClass()));
        });
    }

    private void check() {
        if (AccountConstant.INITIAL_SYSTEM_ADMIN_UUID.equals(message.getSession().getAccountUuid())) {
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

                if (info.getTargetResources().isEmpty()) {
                    return;
                }

                if (message instanceof APISyncCallMessage) {
                    // no check to read api
                    return;
                }

                try {
                    if (info.isTargetResource(resourceType)) {
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
                if (param.param.noOwnerCheck()) {
                    // do nothing
                } else if (String.class.isAssignableFrom(param.field.getType())) {
                    String uuid = (String) param.field.get(message);
                    if (uuid != null) {
                        uuids.add(uuid);
                    }
                } else if (Collection.class.isAssignableFrom(param.field.getType())) {
                    Collection u = (Collection<? extends String>) param.field.get(message);
                    if (u != null) {
                        uuids.addAll(u);
                    }
                } else {
                    throw new CloudRuntimeException(String.format("not supported field type[%s] for %s#%s", param.field.getType(), message.getClass(), param.field.getName()));
                }

                return uuids;
            }

            private void checkIfTheAccountOwnTheResource(APIMessage.FieldParam param) throws IllegalAccessException {
                List<String> uuids = getResourceUuids(param);
                if (uuids.isEmpty()) {
                    return;
                }

                Class resourceType = param.param.resourceType();
                List<Tuple> ts = q(AccountResourceRefVO.class).select(AccountResourceRefVO_.accountUuid, AccountResourceRefVO_.resourceUuid)
                        .in(AccountResourceRefVO_.resourceUuid, uuids)
                        //.eq(AccountResourceRefVO_.resourceType, acntMgr.getBaseResourceType(resourceType).getSimpleName())
                        .listTuple();

                ts.addAll(
                        q(SharedResourceVO.class).select(SharedResourceVO_.receiverAccountUuid, SharedResourceVO_.resourceUuid)
                                .in(SharedResourceVO_.resourceUuid, uuids)
                                .eq(SharedResourceVO_.permission, SharedResourceVO.PERMISSION_WRITE)
                                .eq(SharedResourceVO_.receiverAccountUuid, message.getSession().getAccountUuid())
                                //.eq(SharedResourceVO_.resourceType, acntMgr.getBaseResourceType(resourceType).getSimpleName())
                                .listTuple()
                );

                uuids.forEach(uuid -> {
                    Optional<Tuple> opt = ts.stream().filter(t -> t.get(0, String.class).equals(message.getSession().getAccountUuid()) && t.get(1, String.class).equals(uuid)).findFirst();
                    if (!opt.isPresent()) {
                        throw new OperationFailureException(operr("permission denied, the account[uuid:%s] is not the owner of the resource[uuid:%s, type:%s]",
                                message.getSession().getAccountUuid(), uuid, resourceType.getSimpleName()));
                    }
                });
            }

            private void checkIfTheAccountCanAccessTheResource(APIMessage.FieldParam param) throws IllegalAccessException {
                List<String> uuids = getResourceUuids(param);
                if (uuids.isEmpty()) {
                    return;
                }

                Class resourceType = param.param.resourceType();
                List<String> resourceWithNoAccess = new CheckIfAccountCanAccessResource().check(uuids, message.getSession().getAccountUuid());
                if (!resourceWithNoAccess.isEmpty()) {
                    throw new OperationFailureException(operr("the account[uuid:%s] has no access to the resources[uuid:%s, type:%s]",
                            message.getSession().getAccountUuid(), resourceWithNoAccess, resourceType.getSimpleName()));
                }
            }
        }.execute();
    }
}
