package org.zstack.compute.vm;

public interface VmMigrationMetric {
    boolean isCapable(String srcPrimaryStorageType);
    boolean isSupportWithSharedBlock();
}
