package org.zstack.resourceconfig;

import org.zstack.core.config.GlobalConfigException;

/**
 * Runs before a resource-scoped value is removed from the database.
 * Unlike delete extensions, this hook is allowed to veto the delete.
 */
public interface ResourceConfigDeleteValidatorExtensionPoint {
    void validateDelete(String resourceUuid, String oldValue) throws GlobalConfigException;
}
