package org.zstack.header.allocator;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.VmInstanceSpec;

public class AllocateHostAndPrimaryStorageMsg extends NeedReplyMessage {
    private VmInstanceSpec spec;

    public VmInstanceSpec getVmInstanceSpec() {
        return spec;
    }

    public void setVmInstanceSpec(VmInstanceSpec spec) {
        this.spec = spec;
    }
}

