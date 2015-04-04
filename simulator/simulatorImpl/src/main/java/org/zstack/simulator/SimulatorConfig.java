package org.zstack.simulator;

import org.zstack.header.host.TakeSnapshotOnHypervisorMsg;
import org.zstack.header.vm.VmInstance;
import org.zstack.header.vm.VmInstanceState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class SimulatorConfig {
    public volatile boolean migrateSuccess = true;
    public Map<String, Map<String, VmInstanceState>> vms = new HashMap<String, Map<String, VmInstanceState>>();
    public volatile boolean snapshotSuccess = true;
    public Map<String, List<TakeSnapshotOnHypervisorMsg>> snapshots = new HashMap<String, List<TakeSnapshotOnHypervisorMsg>>();

    public void putVm(String hostUuid, String vmUuid, VmInstanceState vmState) {
        Map<String, VmInstanceState> vmmap = vms.get(hostUuid);
        if (vmmap == null) {
            vmmap = new HashMap<String, VmInstanceState>();
            vms.put(hostUuid, vmmap);
        }

        vmmap.put(vmUuid, vmState);
    }

    public void removeVm(String hostUuid, String vmUuid) {
        Map<String, VmInstanceState> vmmap = vms.get(hostUuid);
        if (vmmap == null) {
            return;
        }

        vmmap.remove(vmUuid);
    }

    public VmInstanceState findVm(String vmUuid) {
        for (Map.Entry<String, Map<String, VmInstanceState>> e : vms.entrySet()) {
            for (Map.Entry<String, VmInstanceState> e1 : e.getValue().entrySet()) {
                if (e1.getKey().equals(vmUuid)) {
                    return e1.getValue();
                }
            }
        }

        return null;
    }

    public boolean containVm(String vmUuid) {
        return findVm(vmUuid) != null;
    }

    public Map<String, VmInstanceState> getVmOnHost(String hostUuid) {
        return vms.get(hostUuid);
    }
}
