package org.zstack.header.aspect;

import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.ResourceTypeMetadata;

import javax.persistence.EntityManager;
import java.util.Collections;
import java.util.List;

public class OwnedByAccountAspectHelper {

    /**
     * Extension point for receiving notifications when resource ownership is created.
     * Implementations should be registered via PluginRegistry (Spring XML).
     */
    public static interface ResourceOwnershipCreationNotifier {
        void notifyResourceOwnershipCreated(AccountResourceRefVO ref, EntityManager entityManager);
    }

    // Notifiers populated by PluginRegistry; set once during Component.start() phase.
    // Using static field because AOP aspects cannot participate in Spring DI.
    private static volatile List<ResourceOwnershipCreationNotifier> notifiers = Collections.emptyList();

    public static void setResourceOwnershipCreationNotifiers(List<ResourceOwnershipCreationNotifier> list) {
        notifiers = list != null ? list : Collections.emptyList();
    }

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

        // Notify all registered listeners — no try-catch so exceptions propagate
        // and the outer transaction rolls back, ensuring strong consistency between
        // AccountResourceRefVO and ExternalTenantResourceRefVO.
        // Pass the EntityManager so listeners persist within the same flush/TX context,
        // avoiding nested @DeadlockAutoRestart scopes that cause unretryable rollbacks.
        for (ResourceOwnershipCreationNotifier n : notifiers) {
            n.notifyResourceOwnershipCreated(ref, entityManager);
        }
    }
}
