package org.zstack.identity;

import java.util.List;

public interface AccountManager {
    void createAccountResourceRef(String accountUuid, String resourceUuid, Class<?> resourceClass);
    
    String getOwnerAccountUuidOfResource(String resourceUuid);
    
    boolean isResourceHavingAccountReference(Class entityClass);

    List<String> getResourceUuidsCanAccessByAccount(String accountUuid, Class resourceType);
}
