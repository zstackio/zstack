package org.zstack.identity;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.IdentityErrors;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.identity.SharedResourceVO;
import org.zstack.header.identity.SharedResourceVO_;
import org.zstack.header.message.APIMessage;
import org.zstack.header.identity.rbac.RBACEntity;
import static org.zstack.core.Platform.*;

import javax.persistence.Tuple;
import java.lang.reflect.Field;
import java.util.*;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AccountAPIRequestChecker implements APIRequestChecker {
    @Autowired
    private AccountManager acntMgr;

    private RBACEntity rbacEntity;

    private static class CheckAccountAPIField {
        Field field;
        boolean checkAccount;
        boolean operationTarget;
        boolean isCollection;
    }

    private static Map<Class, List<CheckAccountAPIField>> checkAccountFields = new HashMap<>();

    static {
        APIMessage.apiParams.forEach((clz, cl)-> {
            List<CheckAccountAPIField> fields = new ArrayList<>();
            cl.forEach(fp -> {
                if (!fp.param.checkAccount() && !fp.param.operationTarget()) {
                    return;
                }

                fp.field.setAccessible(true);
                CheckAccountAPIField f = new CheckAccountAPIField();
                f.field = fp.field;
                f.checkAccount = fp.param.checkAccount();
                f.operationTarget = fp.param.operationTarget();

                if (!String.class.isAssignableFrom(f.field.getType()) && !Collection.class.isAssignableFrom(f.field.getType())) {
                    throw new CloudRuntimeException(String.format("@APIParam of %s.%s has checkAccount = true, however," +
                                    " the type of the field is not String or Collection but %s. " +
                                    "This field must be a resource UUID or a collection(e.g. List) of UUIDs",
                            clz.getName(), f.field.getName(), f.field.getType()));
                }

                f.isCollection = Collection.class.isAssignableFrom(f.field.getType());

                fields.add(f);
            });

            checkAccountFields.put(clz, fields);
        });
    }

    @Override
    public void check(RBACEntity entity) {
        rbacEntity = entity;

        if (acntMgr.isAdmin(rbacEntity.getApiMessage().getSession())) {
            return;
        }


        try {
            check();
        } catch (IllegalAccessException e) {
            throw new CloudRuntimeException(e);
        }
    }

    private void check() throws IllegalAccessException {
        List<CheckAccountAPIField> fields = checkAccountFields.get(rbacEntity.getApiMessage().getClass());
        if (fields == null || fields.isEmpty()) {
            return;
        }

        SessionInventory session = rbacEntity.getApiMessage().getSession();

        Set checkAccountResourceUuids = new HashSet();
        Set operationTargetResourceUuids = new HashSet();

        for (CheckAccountAPIField cf : fields) {
            Object value = cf.field.get(rbacEntity.getApiMessage());
            if (value == null) {
                continue;
            }

            if (cf.isCollection) {
                if (cf.operationTarget) {
                    operationTargetResourceUuids.addAll((Collection) value);
                } else if (cf.checkAccount) {
                    checkAccountResourceUuids.addAll((Collection) value);
                }
            } else {
                if (cf.operationTarget) {
                    operationTargetResourceUuids.add(value);
                } else if (cf.checkAccount) {
                    checkAccountResourceUuids.add(value);
                }
            }
        }

        if (checkAccountResourceUuids.isEmpty() && operationTargetResourceUuids.isEmpty()) {
            return;
        }

        new SQLBatch() {
            @Override
            protected void scripts() {
                if (!checkAccountResourceUuids.isEmpty()) {
                    // rule out resources that shared as public
                    List<String> shared = q(SharedResourceVO.class).select(SharedResourceVO_.resourceUuid)
                            .in(SharedResourceVO_.resourceUuid, checkAccountResourceUuids)
                            .eq(SharedResourceVO_.toPublic, true).listValues();
                    checkAccountResourceUuids.removeAll(shared);
                }

                List<String> toCheck = new ArrayList<>();
                toCheck.addAll(checkAccountResourceUuids);
                toCheck.addAll(operationTargetResourceUuids);

                if (toCheck.isEmpty()) {
                    return;
                }

                List<Tuple> ts = sql(" select avo.name ,arrf.accountUuid ,arrf.resourceUuid ,arrf.resourceType " +
                                "from AccountResourceRefVO arrf ,AccountVO avo " +
                                "where arrf.resourceUuid in (:resourceUuids) and avo.uuid = arrf.accountUuid",Tuple.class)
                        .param("resourceUuids", toCheck).list();

                ts.forEach(t -> {
                    String resourceOwnerName = t.get(0, String.class);
                    String resourceOwnerAccountUuid = t.get(1, String.class);
                    String resourceUuid = t.get(2, String.class);
                    String resourceType = t.get(3, String.class);
                    if (!session.getAccountUuid().equals(resourceOwnerAccountUuid)) {
                        throw new OperationFailureException(err(IdentityErrors.PERMISSION_DENIED, "operation denied. The resource[uuid: %s, type: %s,ownerAccountName:%s, ownerAccountUuid:%s] doesn't belong to the account[uuid: %s]",
                                resourceUuid, resourceType, resourceOwnerName, resourceOwnerAccountUuid, session.getAccountUuid()
                        ));
                    }
                });
            }
        }.execute();
    }
}
