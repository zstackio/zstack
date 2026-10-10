package org.zstack.resourceconfig;

import javax.persistence.EntityManager;

/**
 * A same-transaction coordinator for resource-config changes that must commit
 * with related domain state. Validators must remain read-only; implementations
 * of this extension are invoked only by the actual write path.
 */
public interface ResourceConfigTransactionalMutationExtensionPoint {
    /** Whether a bulk request containing this config must use one transaction. */
    boolean requiresAtomicBulkTransaction();

    void beforeUpdate(EntityManager em, ResourceConfig config, String resourceUuid,
                      String resourceType, String oldValue, String newValue);

    void beforeDelete(EntityManager em, ResourceConfig config, String resourceUuid,
                      String resourceType, String oldValue);
}
