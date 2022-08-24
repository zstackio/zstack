package org.zstack.identity;

import org.zstack.header.identity.*;
import org.zstack.header.message.APIMessage;

import java.util.List;
import java.util.Map;

public interface AccountManager {
    String getOwnerAccountUuidOfResource(String resourceUuid);

    boolean isResourceHavingAccountReference(Class entityClass);

    List<String> getResourceUuidsCanAccessByAccount(String accountUuid, Class resourceType);

    Map<Class, List<Quota>> getMessageQuotaMap();

    List<Quota> getQuotas();

    AccountResourceRefInventory changeResourceOwner(String resourceUuid, String newOwnerUuid);

    void checkApiMessagePermission(APIMessage msg);

    boolean isAdmin(SessionInventory session);

    void adminAdoptAllOrphanedResource(List<String> resourceUuid, String originAccountUuid);

    Class getBaseResourceType(Class clz);

    AccountInventory createAccount(CreateAccountMsg msg);
}
