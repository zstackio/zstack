package org.zstack.kvm.hypervisor;

import static org.zstack.kvm.KVMAgentCommands.GetVirtualizerInfoRsp;
import static org.zstack.kvm.KVMAgentCommands.VirtualizerInfoTO;

import java.util.List;

public interface KvmHypervisorInfoManager {
    void save(GetVirtualizerInfoRsp rsp);
    void saveHostInfo(VirtualizerInfoTO info);
    void saveVmInfo(VirtualizerInfoTO info);

    void saveOnHostOwnerNode(String hostUuid, VirtualizerInfoTO hostInfo, List<VirtualizerInfoTO> vmInfoList);

    void clean(String uuid);

    void refreshMetadata();
}
