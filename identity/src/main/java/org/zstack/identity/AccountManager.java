package org.zstack.identity;

import org.zstack.header.identity.Quota;

import java.util.List;
import java.util.Map;

public interface AccountManager {
    void createAccountResourceRef(String accountUuid, String resourceUuid, Class<?> resourceClass);
    
    String getOwnerAccountUuidOfResource(String resourceUuid);
    
    boolean isResourceHavingAccountReference(Class entityClass);

    List<String> getResourceUuidsCanAccessByAccount(String accountUuid, Class resourceType);

    Map<Class, Quota> getMessageQuotaMap();
}
