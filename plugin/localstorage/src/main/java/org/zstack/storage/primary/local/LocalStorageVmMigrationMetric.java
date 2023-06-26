package org.zstack.storage.primary.local;

import org.zstack.compute.vm.VmMigrationMetric;

public class LocalStorageVmMigrationMetric implements VmMigrationMetric {
    @Override
    public boolean isCapable(String srcPrimaryStorageType) {
        return LocalStorageConstants.LOCAL_STORAGE_TYPE.equals(srcPrimaryStorageType);
    }

    @Override
    public boolean isSupportWithSharedBlock() {
        return false;
    }
}
