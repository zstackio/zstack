package org.zstack.sugonSdnController.network;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.Q;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l2.L2NetworkVO_;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;
import org.zstack.sugonSdnController.controller.api.ObjectReference;
import org.zstack.sugonSdnController.controller.api.types.VirtualMachineInterface;
import org.zstack.sugonSdnController.controller.neutronClient.TfPortResponse;
import org.zstack.utils.StringDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class TfZstackPortSync implements ManagementNodeReadyExtensionPoint {

    @Autowired
    protected ThreadFacade thdf;
    private Future<Void> trackerThread = null;
    @Autowired
    private TfPortService tfPortService;
    private final static CLogger logger = Utils.getLogger(TfZstackPortSync.class);
    private final List<String> excludeTypes = new ArrayList<String>(Arrays.asList("neutron:LOADBALANCER", "VIP", "BMS"));

    @Override
    public void managementNodeReady() {
        if (trackerThread != null) {
            trackerThread.cancel(true);
        }
        trackerThread = thdf.submitPeriodicTask(new SyncPort());
    }

    private class SyncPort implements PeriodicTask {

        @Override
        public TimeUnit getTimeUnit() {
            return TimeUnit.DAYS;
        }

        @Override
        public long getInterval() {
            return 1;
        }

        @Override
        public String getName() {
            return "Period-Task-for-sync-port-between-tf-and-zstack";
        }

        private HashSet<String> getPortToDelete() {
            List<String> zstackPortsUuid = Q.New(VmNicVO.class).select(VmNicVO_.uuid).listValues();
            List<String> zstackL2NetworksUuid = Q.New(L2NetworkVO.class).select(L2NetworkVO_.uuid).listValues();
            List<String> tfPortsUuid = new ArrayList<>();
            try{
                List<VirtualMachineInterface> tfPorts = tfPortService.getTfPortsDetail();
                for (VirtualMachineInterface vmi : tfPorts) {
                    // skip port if it's network not in zstack
                    List<ObjectReference<ApiPropertyBase>>  tfNetworks = vmi.getVirtualNetwork();
                    if (!zstackL2NetworksUuid.contains(StringDSL.transToZstackUuid(tfNetworks.get(0).getUuid()))) {
                        continue;
                    }
                    // exclude the virtualmachineinterface
                    if (excludeTypes.contains(vmi.getDeviceOwner())) {
                        continue;
                    }
                    // exclude the virtualmachineinterface of tf lb
                    if (vmi.getName().startsWith("default-domain__")) {
                        continue;
                    }
                    tfPortsUuid.add(StringDSL.transToZstackUuid(vmi.getUuid()));
                }
            } catch (Exception e) {
                logger.error(String.format("Port_Sync_Task: Fetch tf VirtualMachineInterface failed: %s.", e));
                return null;
            }
            HashSet<String> result = new HashSet<>(tfPortsUuid);
            result.removeAll(zstackPortsUuid);
            logger.debug(String.format("Port_Sync_Task: Fetch tf VirtualMachineInterface (%s) to delete.", result));
            return result;
        }

        @Override
        public void run() {
            logger.info("Port_Sync_Task: begin.");
            try {
                HashSet<String> portsToDelete = getPortToDelete();
                int maxDeleteCount = 10;
                for (String portUuid: portsToDelete) {
                    TfPortResponse response = tfPortService.deleteTfPort(portUuid);
                    if (response.getCode() == 200) {
                        logger.info(String.format("Port_Sync_Task: VirtualMachineInterface: %s delete success.",
                                portUuid));
                    } else {
                        logger.warn(String.format("Port_Sync_Task: VirtualMachineInterface: %s delete failed," +
                                        " reason: %s.", portUuid, response.getMsg()));
                    }
                    maxDeleteCount --;
                    if (maxDeleteCount == 0) {
                        break;
                    }
                }
            } catch (Exception e) {
                logger.error(String.format("Port_Sync_Task failed: %s.", e));
            }
        }
    }
}
