package org.zstack.header.volume;

import org.zstack.header.storage.primary.PrimaryStorageFeature;

import java.util.Set;

public interface VolumePrimaryStorageRequirementExtensionPoint {
    Set<PrimaryStorageFeature> getRequirements(VolumePrimaryStorageAllocationContext context);
}
