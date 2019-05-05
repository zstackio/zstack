package org.zstack.header.configuration.userconfig;

import org.zstack.header.storage.primary.PrimaryStorageAllocateConfig;

/**
 * Created by lining on 2019/4/17.
 */
public class DiskOfferingAllocateConfig {
    private PrimaryStorageAllocateConfig primaryStorage;

    public PrimaryStorageAllocateConfig getPrimaryStorage() {
        return primaryStorage;
    }

    public void setPrimaryStorage(PrimaryStorageAllocateConfig primaryStorage) {
        this.primaryStorage = primaryStorage;
    }
}
