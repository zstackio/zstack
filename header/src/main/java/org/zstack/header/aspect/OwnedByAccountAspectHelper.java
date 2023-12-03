package org.zstack.header.aspect;

import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.ResourceTypeMetadata;

import javax.persistence.EntityManager;

public class OwnedByAccountAspectHelper {
    public static void createAccountResourceRefVO(OwnedByAccount oa, EntityManager entityManager, Object entity) {
        AccountResourceRefVO ref = new AccountResourceRefVO();
        ref.setAccountUuid(oa.getAccountUuid());
        ref.setResourceType(ResourceTypeMetadata.getBaseResourceTypeFromConcreteType(entity.getClass()).getSimpleName());
        ref.setConcreteResourceType(entity.getClass().getName());
        ref.setResourceUuid(OwnedByAccount.getResourceUuid(entity));
        ref.setPermission(AccountConstant.RESOURCE_PERMISSION_WRITE);
        ref.setOwnerAccountUuid(oa.getAccountUuid());
        ref.setShared(false);

        entityManager.persist(ref);
    }
}
