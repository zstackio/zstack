package org.zstack.compute.vm;

public interface VmClockSyncExtensionPoint {
    void clockSync(String resourceUuid, Boolean isSyncAfterVMResume, Integer intervalInSeconds);
}
