package org.zstack.kvm.hypervisor;

import static org.zstack.kvm.KVMAgentCommands.GetVirtualizerInfoRsp;

public interface KvmHypervisorInfoManager {
    void save(GetVirtualizerInfoRsp rsp);
}
