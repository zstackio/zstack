package org.zstack.identity;

import org.zstack.header.identity.AccountResourceRefInventory;
import org.zstack.header.identity.Quota;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.APIMessage;

import java.util.List;
import java.util.Map;

public interface AccountManager {
    void createAccountResourceRef(String accountUuid, String resourceUuid, Class<?> resourceClass);

    String getOwnerAccountUuidOfResource(String resourceUuid);

    boolean isResourceHavingAccountReference(Class entityClass);

    List<String> getResourceUuidsCanAccessByAccount(String accountUuid, Class resourceType);

    Map<Class, List<Quota>> getMessageQuotaMap();

    List<Quota> getQuotas();

    AccountResourceRefInventory changeResourceOwner(String resourceUuid, String newOwnerUuid);

    void checkApiMessagePermission(APIMessage msg);

    boolean isAdmin(SessionInventory session);

    void adminAdoptAllOrphanedResource();
}
