package org.zstack.header.storage.primary;

/**
 * Created by frank on 7/1/2015.
 */
public interface PrimaryStorageAllocatorStrategyExtensionPoint {
    String getPrimaryStorageAllocatorStrategyName(AllocatePrimaryStorageMsg msg);
}
