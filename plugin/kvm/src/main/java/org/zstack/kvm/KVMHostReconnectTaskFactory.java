package org.zstack.kvm;

import org.zstack.compute.host.HostReconnectTask;
import org.zstack.compute.host.HostReconnectTaskFactory;
import org.zstack.core.Platform;
import org.zstack.header.core.NoErrorCompletion;

public class KVMHostReconnectTaskFactory implements HostReconnectTaskFactory {
    @Override
    public HostReconnectTask createTask(String uuid, NoErrorCompletion completion) {
        return Platform.New(() -> new KVMReconnectHostTask(uuid, completion));
    }

    @Override
    public String getHypervisorType() {
        return KVMConstant.KVM_HYPERVISOR_TYPE;
    }
}
