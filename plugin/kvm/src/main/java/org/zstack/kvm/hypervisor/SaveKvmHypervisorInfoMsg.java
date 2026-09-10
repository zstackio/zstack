package org.zstack.kvm.hypervisor;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.Message;
import org.zstack.kvm.KVMAgentCommands.VirtualizerInfoTO;

import java.util.List;

/**
 * Routing the hypervisor info write by the host uuid makes the write be handled by the management node that
 * owns the host, no matter which management node the report comes from.
 */
public class SaveKvmHypervisorInfoMsg extends Message implements HostMessage {
    private String hostUuid;
    private VirtualizerInfoTO hostInfo;
    private List<VirtualizerInfoTO> vmInfoList;

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public VirtualizerInfoTO getHostInfo() {
        return hostInfo;
    }

    public void setHostInfo(VirtualizerInfoTO hostInfo) {
        this.hostInfo = hostInfo;
    }

    public List<VirtualizerInfoTO> getVmInfoList() {
        return vmInfoList;
    }

    public void setVmInfoList(List<VirtualizerInfoTO> vmInfoList) {
        this.vmInfoList = vmInfoList;
    }
}
