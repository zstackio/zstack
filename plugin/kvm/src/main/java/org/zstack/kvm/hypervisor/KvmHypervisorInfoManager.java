package org.zstack.kvm.hypervisor;

import static org.zstack.kvm.KVMAgentCommands.GetVirtualizerInfoRsp;
import static org.zstack.kvm.KVMAgentCommands.VirtualizerInfoTO;

import java.util.List;

public interface KvmHypervisorInfoManager {
    void save(GetVirtualizerInfoRsp rsp);
    void saveHostInfo(VirtualizerInfoTO info);
    void saveVmInfo(VirtualizerInfoTO info);

    /**
     * Saves the reported hypervisor information on the management node that owns the host.
     *
     * @param hostUuid the host uuid used to route the message, must not be null
     * @param hostInfo the host hypervisor information, null when only vm information is reported
     * @param vmInfoList the vm hypervisor information list, null when only host information is reported
     */
    void saveOnHostOwnerNode(String hostUuid, VirtualizerInfoTO hostInfo, List<VirtualizerInfoTO> vmInfoList);

    void clean(String uuid);

    void refreshMetadata();
}
