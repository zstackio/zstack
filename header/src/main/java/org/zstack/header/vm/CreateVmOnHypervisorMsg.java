package org.zstack.header.vm;

import org.zstack.header.core.ApiTimeout;
import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

@ApiTimeout(apiClasses = {APICreateVmInstanceMsg.class})
public class CreateVmOnHypervisorMsg extends NeedReplyMessage implements HostMessage {
    private VmInstanceSpec vmSpec;

    @Override
    public String getHostUuid() {
        return vmSpec.getDestHost().getUuid();
    }

    public VmInstanceSpec getVmSpec() {
        return vmSpec;
    }

    public void setVmSpec(VmInstanceSpec vmSpec) {
        this.vmSpec = vmSpec;
    }
}
