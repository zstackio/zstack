package org.zstack.header.vm;

import org.zstack.header.allocator.AllocateHostMsg;

public interface BeforeVmAllocateHostExtensionPoint {
    void beforeVmAllocateHost(AllocateHostMsg msg, VmInstanceSpec spec);
}
