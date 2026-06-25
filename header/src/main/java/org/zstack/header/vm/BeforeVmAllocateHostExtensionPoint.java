package org.zstack.header.vm;

import org.zstack.header.allocator.AllocateHostMsg;

public interface BeforeVmAllocateHostExtensionPoint {
    /**
     * Invoked before host allocation message delivery so extensions can adjust allocation context.
     *
     * @param msg host allocation message that will be sent
     * @param spec current VM allocation specification
     */
    void beforeVmAllocateHost(AllocateHostMsg msg, VmInstanceSpec spec);
}
