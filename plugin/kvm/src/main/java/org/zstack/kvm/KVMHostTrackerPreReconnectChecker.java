package org.zstack.kvm;

import org.zstack.compute.host.HostTrackerPreReconnectChecker;
import org.zstack.core.db.Q;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.Tuple;
import java.util.concurrent.TimeUnit;

public class KVMHostTrackerPreReconnectChecker implements HostTrackerPreReconnectChecker {
    @Override
    public Boolean canDoReconnect(String hostUuid) {
        Tuple t = Q.New(KVMHostVO.class).select(KVMHostVO_.managementIp, KVMHostVO_.port).findTuple();
        if (t == null) {
            return null;
        }

        String ip = t.get(0, String.class);
        int port = t.get(1, Integer.class);

        return NetworkUtils.isRemotePortOpen(ip, port, (int) TimeUnit.SECONDS.toMillis(2));
    }

    @Override
    public String getHypervisorType() {
        return KVMConstant.KVM_HYPERVISOR_TYPE;
    }
}
