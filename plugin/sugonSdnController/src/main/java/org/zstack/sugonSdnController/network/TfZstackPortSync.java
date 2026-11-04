package org.zstack.sugonSdnController.network;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.Q;
import org.zstack.core.thread.Task;
import org.zstack.core.thread.ThreadFacade;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class TfZstackPortSync {
    private static final long SYNC_INTERVAL_MILLIS = TimeUnit.DAYS.toMillis(1);
    private static final int MAX_DELETE_COUNT = 10;

    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    private TfPortService tfPortService;
    private final static CLogger logger = Utils.getLogger(TfZstackPortSync.class);
    private final List<String> excludeTypes = new ArrayList<String>(Arrays.asList("neutron:LOADBALANCER", "VIP", "BMS"));
    private final Map<String, Long> lastSyncTime = new ConcurrentHashMap<>();
    private final Set<String> runningSyncs = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    public void triggerSyncIfDue(String sdnControllerUuid) {
        if (sdnControllerUuid == null) {
            logger.warn("Port_Sync_Task: skip sync because sdn controller uuid is null.");
            return;
        }

        long now = System.currentTimeMillis();
        Long lastSync = lastSyncTime.get(sdnControllerUuid);
        if (lastSync != null && now - lastSync < SYNC_INTERVAL_MILLIS) {
            return;
        }
        if (!runningSyncs.add(sdnControllerUuid)) {
            return;
        }

        lastSyncTime.put(sdnControllerUuid, now);
        try {
            thdf.submit(new SyncPort(sdnControllerUuid));
        } catch (RuntimeException e) {
            lastSyncTime.remove(sdnControllerUuid);
            runningSyncs.remove(sdnControllerUuid);
            throw e;
        }
    }

    private class SyncPort implements Task<Void> {
        private final String sdnControllerUuid;

        private SyncPort(String sdnControllerUuid) {
            this.sdnControllerUuid = sdnControllerUuid;
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
                List<VirtualMachineInterface> tfPorts = tfPortService.getTfPortsDetail(sdnControllerUuid);
                if (tfPorts == null) {
                    return new HashSet<>();
                }
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
                return new HashSet<>();
            }
            HashSet<String> result = new HashSet<>(tfPortsUuid);
            result.removeAll(zstackPortsUuid);
            logger.debug(String.format("Port_Sync_Task: Fetch tf VirtualMachineInterface (%s) to delete.", result));
            return result;
        }

        @Override
        public Void call() {
            logger.info("Port_Sync_Task: begin.");
            try {
                HashSet<String> portsToDelete = getPortToDelete();
                int maxDeleteCount = MAX_DELETE_COUNT;
                for (String portUuid: portsToDelete) {
                    TfPortResponse response = tfPortService.deleteTfPort(sdnControllerUuid, portUuid);
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
            } finally {
                runningSyncs.remove(sdnControllerUuid);
            }
            return null;
        }
    }
}
