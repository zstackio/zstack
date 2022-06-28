package org.zstack.identity.rbac;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.*;
import org.zstack.header.identity.rbac.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.identity.APIRequestChecker;

import javax.persistence.Tuple;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class OperationTargetAPIRequestChecker implements APIRequestChecker {
    protected RBACEntity rbacEntity;

    private static Map<Class, RBAC.Permission> rbacInfos =  Collections.synchronizedMap(new HashMap<>());
    private static PolicyMatcher policyMatcher = new PolicyMatcher();


    @Override
    public void check(RBACEntity entity) {
        rbacEntity = entity;
        check();
    }

    protected boolean isMatch(String as) {
        String ap = PolicyUtils.apiNamePatternFromAction(as, true);
        return policyMatcher.match(ap, rbacEntity.getApiMessage().getClass().getName());
    }

    protected RBAC.Permission getRBACInfo() {
        return rbacInfos.computeIfAbsent(rbacEntity.getApiMessage().getClass(), x-> {
            for (RBAC.Permission permission : RBAC.permissions) {
                for (String s : permission.getNormalAPIs()) {
                    if (isMatch(s)) {
                        return permission;
                    }
                }

                for (String s : permission.getAdminOnlyAPIs()) {
                    if (isMatch(s)) {
                        return permission;
                    }
                }
            }

            throw new CloudRuntimeException(String.format("cannot find RBACInfo for the API[%s]", rbacEntity.getApiMessage().getClass()));
        });
    }

    private static class AccountResourceBundle {
        String accountUuid;
        String resourceUuid;
    }

    private Collection<AccountResourceBundle> toAccountResourceBundles(List<String> resourceUuids, List<Tuple> tss) {
        Map<String, AccountResourceBundle> m = new HashMap<>();

        tss.forEach(ts -> {
            String accountUuid = ts.get(0, String.class);
            String resUuid = ts.get(1, String.class);
            AccountResourceBundle b = new AccountResourceBundle();
            b.accountUuid = accountUuid;
            b.resourceUuid = resUuid;
            m.put(resUuid, b);
        });

        resourceUuids.forEach(ruuid -> {
            if (!m.containsKey(ruuid)) {
                AccountResourceBundle b = new AccountResourceBundle();
                b.accountUuid = AccountConstant.INITIAL_SYSTEM_ADMIN_UUID;
                b.resourceUuid = ruuid;
                m.put(ruuid, b);
            }
        });

        return m.values();
    }

    protected List<String> getResourceUuids(APIMessage.FieldParam param) throws IllegalAccessException {
        List<String> uuids = new ArrayList<>();
        if (param.param.noOwnerCheck()) {
            // do nothing
        } else if (String.class.isAssignableFrom(param.field.getType())) {
            String uuid = (String) param.field.get(rbacEntity.getApiMessage());
            if (uuid != null) {
                uuids.add(uuid);
            }
        } else if (Collection.class.isAssignableFrom(param.field.getType())) {
            Collection u = (Collection<? extends String>) param.field.get(rbacEntity.getApiMessage());
            if (u != null) {
                uuids.addAll(u);
            }
        } else {
            throw new CloudRuntimeException(String.format("not supported field type[%s] for %s#%s", param.field.getType(), rbacEntity.getApiMessage().getClass(), param.field.getName()));
        }

        return uuids;
    }

    private void check() {
        if (AccountConstant.INITIAL_SYSTEM_ADMIN_UUID.equals(rbacEntity.getApiMessage().getSession().getAccountUuid())) {
            return;
        }

        RBAC.Permission info = getRBACInfo();

        new SQLBatch() {
            @Override
            protected void scripts() {
                APIMessage.getApiParams().get(rbacEntity.getApiMessage().getClass()).forEach(this::checkOperationTarget);
            }

            private void checkOperationTarget(APIMessage.FieldParam param) {
                Class resourceType = param.param.resourceType();
                if (resourceType == Object.class)  {
                    return;
                }

                if (info.getTargetResources().isEmpty()) {
                    return;
                }

                if (rbacEntity.getApiMessage() instanceof APISyncCallMessage) {
                    // no check to read api
                    return;
                }

                try {
                    if (resourceType.equals(AccountVO.class)) {
                        checkIfTheAccountOperationItSelf(param);
                    } else if (info.getTargetResources().stream().anyMatch( it -> resourceType.isAssignableFrom(it))) {
                        checkIfTheAccountOwnTheResource(param);
                    } else if (resourceType.equals(SystemTagVO.class)) {
                        checkIfTheAccountOwnTheTaggedResource(param);
                    } else {
                        checkIfTheAccountCanAccessTheResource(param);
                    }
                } catch (OperationFailureException oe) {
                    throw oe;
                } catch (Exception e) {
                    throw new CloudRuntimeException(e);
                }
            }

            private void checkIfTheAccountOperationItSelf(APIMessage.FieldParam param) throws IllegalAccessException {
                List<String> uuids = getResourceUuids(param);

                Class resourceType = param.param.resourceType();

                String accountUuid = rbacEntity.getApiMessage().getSession().getAccountUuid();

                if (uuids.isEmpty()) {
                    throw new OperationFailureException(operr("permission denied, the account[uuid:%s] is not the owner of the resource[uuid:%s, type:%s]",
                            accountUuid, accountUuid, resourceType.getSimpleName()));
                }
            }

            private void checkIfTheAccountOwnTheResource(APIMessage.FieldParam param) throws IllegalAccessException {
                List<String> uuids = getResourceUuids(param);
                if (uuids.isEmpty()) {
                    return;
                }

                Class resourceType = param.param.resourceType();
                Collection<AccountResourceBundle> bundles = getAccountResourceBundles(uuids);
                uuids.forEach(uuid -> {
                    Optional<AccountResourceBundle> opt = bundles.stream().filter(b -> b.accountUuid.equals(rbacEntity.getApiMessage().getSession().getAccountUuid()) && b.resourceUuid.equals(uuid)).findFirst();
                    if (!opt.isPresent()) {
                        throw new OperationFailureException(operr("permission denied, the account[uuid:%s] is not the owner of the resource[uuid:%s, type:%s]",
                                rbacEntity.getApiMessage().getSession().getAccountUuid(), uuid, resourceType.getSimpleName()));
                    }
                });
            }

            private Collection<AccountResourceBundle> getAccountResourceBundles(List<String> uuids) {
                List<Tuple> ts = q(AccountResourceRefVO.class).select(AccountResourceRefVO_.accountUuid, AccountResourceRefVO_.resourceUuid)
                        .in(AccountResourceRefVO_.resourceUuid, uuids)
                        //.eq(AccountResourceRefVO_.resourceType, acntMgr.getBaseResourceType(resourceType).getSimpleName())
                        .listTuple();

                ts.addAll(
                        q(SharedResourceVO.class).select(SharedResourceVO_.receiverAccountUuid, SharedResourceVO_.resourceUuid)
                                .in(SharedResourceVO_.resourceUuid, uuids)
                                .eq(SharedResourceVO_.permission, SharedResourceVO.PERMISSION_WRITE)
                                .eq(SharedResourceVO_.receiverAccountUuid, rbacEntity.getApiMessage().getSession().getAccountUuid())
                                //.eq(SharedResourceVO_.resourceType, acntMgr.getBaseResourceType(resourceType).getSimpleName())
                                .listTuple()
                );

                return toAccountResourceBundles(uuids, ts);
            }

            private void checkIfTheAccountOwnTheTaggedResource(APIMessage.FieldParam param) throws IllegalAccessException {
                List<String> uuids = getResourceUuids(param);
                if (uuids.isEmpty()) {
                    return;
                }

                List<Tuple> taggedResource = q(SystemTagVO.class).select(SystemTagVO_.resourceUuid, SystemTagVO_.resourceType)
                        .in(SystemTagVO_.uuid, uuids).listTuple();
                if (taggedResource.isEmpty()) {
                    return;
                }

                List<String> taggedResUuids = taggedResource.stream()
                        .map(ts -> ts.get(0, String.class))
                        .collect(Collectors.toList());
                Collection<AccountResourceBundle> bundles = getAccountResourceBundles(taggedResUuids);

                taggedResource.forEach(tuple -> {
                    String uuid = tuple.get(0, String.class);
                    String type = tuple.get(1, String.class);
                    Optional<AccountResourceBundle> opt = bundles.stream()
                            .filter(b -> b.accountUuid.equals(rbacEntity.getApiMessage().getSession().getAccountUuid()) && b.resourceUuid.equals(uuid))
                            .findFirst();
                    if (!opt.isPresent()) {
                        throw new OperationFailureException(operr("permission denied, the account[uuid:%s] is not the owner of the tagged resource[uuid:%s, type:%s]",
                                rbacEntity.getApiMessage().getSession().getAccountUuid(), uuid, type));
                    }
                });
            }

            private void checkIfTheAccountCanAccessTheResource(APIMessage.FieldParam param) throws IllegalAccessException {
                Class resourceType = param.param.resourceType();

                if (RBAC.isResourceGlobalReadable(resourceType)) {
                    return;
                }
                if (!OwnedByAccount.class.isAssignableFrom(resourceType)){
                    return;
                }

                List<String> uuids = getResourceUuids(param);
                if (uuids.isEmpty()) {
                    return;
                }

                List<String> resourceWithNoAccess = CheckIfAccountCanAccessResource.check(uuids, rbacEntity.getApiMessage().getSession().getAccountUuid());
                if (!resourceWithNoAccess.isEmpty()) {
                    throw new OperationFailureException(operr("the account[uuid:%s] has no access to the resources[uuid:%s, type:%s]",
                            rbacEntity.getApiMessage().getSession().getAccountUuid(), resourceWithNoAccess, resourceType.getSimpleName()));
                }

            }
        }.execute();
    }
}
