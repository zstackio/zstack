package org.zstack.header.configuration.userconfig;

import org.zstack.header.storage.primary.PrimaryStorageAllocateConfig;

import java.util.List;

/**
 * Created by lining on 2019/4/17.
 */
public class DiskOfferingAllocateConfig {
    private PrimaryStorageAllocateConfig primaryStorage;
    private List<PrimaryStorageAllocateConfig> primaryStorages;

    public List<PrimaryStorageAllocateConfig> getPrimaryStorages() {
        return primaryStorages;
    }

    public void setPrimaryStorages(List<PrimaryStorageAllocateConfig> primaryStorages) {
        this.primaryStorages = primaryStorages;
    }

    public PrimaryStorageAllocateConfig getPrimaryStorage() {
        return primaryStorage;
    }

    public void setPrimaryStorage(PrimaryStorageAllocateConfig primaryStorage) {
        this.primaryStorage = primaryStorage;
    }
}
