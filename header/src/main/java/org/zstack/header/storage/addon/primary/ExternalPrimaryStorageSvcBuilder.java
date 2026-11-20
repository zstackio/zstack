package org.zstack.header.storage.addon.primary;

import org.zstack.header.core.ReturnValueCompletion;

import java.util.Optional;

public interface ExternalPrimaryStorageSvcBuilder {
    PrimaryStorageControllerSvc buildControllerSvc(ExternalPrimaryStorageVO vo);

    PrimaryStorageNodeSvc buildNodeSvc(ExternalPrimaryStorageVO vo);

    void discover(String url, String config, ReturnValueCompletion<AddonInfo> completion);

    /**
     * Apply a driver-specific metadata edit to the persisted configuration.
     * Empty means this request is a normal configuration replacement.
     * Metadata edits must not change connection or data-plane settings.
     */
    default Optional<String> applyMetadataUpdate(String storedConfig, String requestConfig) {
        return Optional.empty();
    }

    String getIdentity();
}
