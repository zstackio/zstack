package org.zstack.identity;

import java.util.List;

public interface AccountManager {
    void createAccountResourceRef(String accountUuid, String resourceUuid, Class<?> resourceClass);
    
    String getOwnerAccountUuidOfResource(String resrouceUuid);
    
    boolean isResourceHavingAccountReference(Class entityClass);

    List<String> getSiblingResourceUuids(String res1Uuid, String res1Type, String res2Type);
}
