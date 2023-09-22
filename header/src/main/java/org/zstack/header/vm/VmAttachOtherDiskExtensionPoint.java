package org.zstack.header.vm;

import org.zstack.header.core.Completion;

public interface VmAttachOtherDiskExtensionPoint {
    void attachOtherDiskToVm(APICreateVmInstanceMsg.DiskAO diskAO, String vmInstanceUuid, Completion completion);
    String getDiskType();
}
