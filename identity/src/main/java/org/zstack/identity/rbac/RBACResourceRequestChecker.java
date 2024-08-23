package org.zstack.identity.rbac;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.*;
import org.zstack.header.identity.rbac.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APIResourceScope;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.identity.APIRequestChecker;
import org.zstack.identity.Account;

import javax.persistence.Tuple;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.*;
import static org.zstack.header.errorcode.SysErrors.*;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class RBACResourceRequestChecker implements APIRequestChecker {
    protected RBACEntity rbacEntity;

    @Override
    public void check(RBACEntity entity) {
        rbacEntity = entity;
        check();
    }

    protected RBAC.Permission getRBACInfo() {
        return RBAC.apiBuckets.get(rbacEntity.getApiMessage().getClass().getName()).permission;
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
        if (Account.isAdminPermission(rbacEntity.getApiMessage().getSession())) {
            return;
        }

        RBAC.Permission info = getRBACInfo();

        new SQLBatch() {
            @Override
            protected void scripts() {
                APIMessage.getApiParams().get(rbacEntity.getApiMessage().getClass()).forEach(this::checkOperationTarget);
            }

            private void checkOperationTarget(APIMessage.FieldParam param) {
                Class<?>[] resourceTypes = param.param.resourceType();
                if (resourceTypes.length == 0)  {
                    return;
                }

                if (info.getTargetResources().isEmpty()) {
                    return;
                }

                if (rbacEntity.getApiMessage() instanceof APISyncCallMessage) {
                    // no check to read api
                    return;
                }

                Class<?> resourceType = resourceTypes[0];
                try {
                    if (resourceType.equals(AccountVO.class)) {
                        checkAccountAPIParam(param);
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

            private void checkAccountAPIParam(APIMessage.FieldParam param) throws IllegalAccessException {
                APIResourceScope scope = apiResourceScope(param);
                if (scope != APIResourceScope.MustOwner) {
                    return;
                }
                List<String> uuids = getResourceUuids(param);
                String currentAccountUuid = rbacEntity.getApiMessage().getSession().getAccountUuid();
                if (uuids.stream().anyMatch(uuid -> !Objects.equals(uuid, currentAccountUuid))) {
                    String parameterName = param.field.getName();
                    throw new OperationFailureException(err(RESOURCE_NOT_ACCESSIBLE,
                            "permission denied: parameter[%s] must be yourself", parameterName));
                }
            }

            private void checkIfTheAccountOwnTheResource(APIMessage.FieldParam param) throws IllegalAccessException {
                List<String> uuids = getResourceUuids(param);
                removeSharedToPublicResources(uuids);
                if (uuids.isEmpty()) {
                    return;
                }

                Collection<AccountResourceBundle> bundles = getAccountResourceBundles(uuids);
                uuids.forEach(uuid -> {
                    Optional<AccountResourceBundle> opt = bundles.stream().filter(b -> b.accountUuid.equals(rbacEntity.getApiMessage().getSession().getAccountUuid()) && b.resourceUuid.equals(uuid)).findFirst();
                    if (!opt.isPresent()) {
                        String resourceType = Q.New(ResourceVO.class)
                                .eq(ResourceVO_.uuid, uuid)
                                .select(ResourceVO_.resourceType)
                                .findValue();
                        throw new OperationFailureException(err(RESOURCE_NOT_ACCESSIBLE,
                                "%s resource[uuid:%s] is not accessible for account[uuid:%s]",
                                resourceType, uuid, rbacEntity.getApiMessage().getSession().getAccountUuid()));
                    }
                });
            }

            private void removeSharedToPublicResources(List<String> uuids) {
                if (uuids.isEmpty()) {
                    return;
                }

                final List<String> sharedResourceUuidList = q(AccountResourceRefVO.class)
                        .in(AccountResourceRefVO_.resourceUuid, uuids)
                        .eq(AccountResourceRefVO_.type, AccessLevel.SharePublic)
                        .select(AccountResourceRefVO_.resourceUuid)
                        .listValues();
                uuids.removeAll(sharedResourceUuidList);
            }

            private Collection<AccountResourceBundle> getAccountResourceBundles(List<String> uuids) {
                List<Tuple> ts = q(AccountResourceRefVO.class)
                        .select(AccountResourceRefVO_.accountUuid, AccountResourceRefVO_.resourceUuid)
                        .in(AccountResourceRefVO_.resourceUuid, uuids)
                        .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                        .listTuple();

                ts.addAll(
                    q(AccountResourceRefVO.class)
                            .select(AccountResourceRefVO_.accountUuid, AccountResourceRefVO_.resourceUuid)
                            .in(AccountResourceRefVO_.resourceUuid, uuids)
                            .eq(AccountResourceRefVO_.type, AccessLevel.Share)
                            .eq(AccountResourceRefVO_.accountUuid, rbacEntity.getApiMessage().getSession().getAccountUuid())
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
                        throw new OperationFailureException(err(RESOURCE_NOT_ACCESSIBLE,
                                "permission denied, the account[uuid:%s] is not the owner of the tagged resource[uuid:%s, type:%s]",
                                rbacEntity.getApiMessage().getSession().getAccountUuid(), uuid, type));
                    }
                });
            }

            private void checkIfTheAccountCanAccessTheResource(APIMessage.FieldParam param) throws IllegalAccessException {
                List<String> uuids = getResourceUuids(param);

                final String accountUuid = rbacEntity.getApiMessage().getSession().getAccountUuid();
                AccessibleResourceChecker checker = AccessibleResourceChecker.forAccount(accountUuid)
                        .withScope(apiResourceScope(param));

                final Class<?>[] resourceTypes = param.param.resourceType();
                if (resourceTypes.length == 1) {
                    checker.withResourceType(resourceTypes[0]);
                }

                List<String> inaccessibleResources = checker.findOutAllInaccessibleResources(uuids);
                if (!inaccessibleResources.isEmpty()) {
                    throw new OperationFailureException(err(RESOURCE_NOT_ACCESSIBLE,
                            "the account[uuid:%s] has no access to the resources[uuid:%s, type:%s]",
                            accountUuid, inaccessibleResources,
                            Arrays.stream(resourceTypes).map(Class::getSimpleName).collect(Collectors.toList())));
                }

            }
        }.execute();
    }

    public APIResourceScope apiResourceScope(APIMessage.FieldParam param) {
        String scope = param.param.scope();
        if (!APIParam.SCOPE_AUTO.equals(scope)) {
            return APIResourceScope.valueOf(scope);
        }

        Class<?> resourceType = param.param.resourceType()[0];

        if (param.param.noOwnerCheck()) {
            return APIResourceScope.AllowedAll;
        }

        if (param.param.checkAccount() || param.param.operationTarget()) {
            return APIResourceScope.AllowedSharing;
        }

        return RBAC.isResourceGlobalReadable(resourceType) ?
                APIResourceScope.AllowedAll :
                APIResourceScope.AllowedSharing;
    }
}
