package org.zstack.kvm.efi;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.Q;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceAO_;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.additions.VmHostFileType;
import org.zstack.header.vm.additions.VmHostFileVO;
import org.zstack.header.vm.additions.VmHostFileVO_;
import org.zstack.kvm.KVMGlobalConfig;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class VmHostFileTracker implements Component {
    private static final CLogger logger = Utils.getLogger(VmHostFileTracker.class);

    @Autowired
    private ThreadFacade threadFacade;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ResourceDestinationMaker destinationMaker;

    private Future<Void> trackerThread;

    @Override
    public boolean start() {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            logger.info("VmHostFileTracker is disabled in unit test");
            return true;
        }

        submitTrackerTask();

        GlobalConfigUpdateExtensionPoint listener = (oldConfig, newConfig) -> {
            logger.debug(String.format("%s changed from %s to %s, restarting vm-host-file-tracker",
                    newConfig.getCanonicalName(), oldConfig.value(), newConfig.value()));
            submitTrackerTask();
        };

        KVMGlobalConfig.VM_HOST_FILE_SYNC_INTERVAL.installUpdateExtension(listener);
        KVMGlobalConfig.VM_HOST_FILE_SYNC_CONCURRENCY.installUpdateExtension(listener);

        return true;
    }

    @Override
    public boolean stop() {
        if (trackerThread != null) {
            trackerThread.cancel(true);
        }
        return true;
    }

    private synchronized void submitTrackerTask() {
        if (trackerThread != null) {
            trackerThread.cancel(true);
        }

        long interval = KVMGlobalConfig.VM_HOST_FILE_SYNC_INTERVAL.value(Long.class);
        trackerThread = threadFacade.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return interval;
            }

            @Override
            public String getName() {
                return "vm-host-file-tracker";
            }

            @Override
            public void run() {
                logger.info("VmHostFileTracker: starting periodic sync of VM host files");
                syncVmHostFiles();
            }
        });
    }

    private void syncVmHostFiles() {
        List<VmHostFileVO> hostFiles = Q.New(VmHostFileVO.class, VmInstanceVO.class)
                .table0()
                    .eq(VmHostFileVO_.vmInstanceUuid).table1(VmInstanceAO_.uuid)
                    .eq(VmHostFileVO_.hostUuid).table1(VmInstanceAO_.hostUuid)
                    .selectThisTable()
                .table1()
                    .eq(VmInstanceAO_.state, VmInstanceState.Running)
                .list();

        if (hostFiles.isEmpty()) {
            return;
        }

        Map<String, List<VmHostFileVO>> grouped = hostFiles.stream()
                .collect(Collectors.groupingBy(f -> f.getVmInstanceUuid() + "::" + f.getHostUuid()));
        List<List<VmHostFileVO>> groups = new ArrayList<>(grouped.values());

        int concurrency = KVMGlobalConfig.VM_HOST_FILE_SYNC_CONCURRENCY.value(Integer.class);

        new While<>(groups).step((group, whileCompletion) -> {
            VmHostFileVO first = group.get(0);
            String vmUuid = first.getVmInstanceUuid();
            String hostUuid = first.getHostUuid();

            if (!destinationMaker.isManagedByUs(vmUuid)) {
                whileCompletion.done();
                return;
            }

            SyncVmHostFilesFromHostMsg syncMsg = new SyncVmHostFilesFromHostMsg();
            syncMsg.setHostUuid(hostUuid);
            syncMsg.setVmUuid(vmUuid);

            for (VmHostFileVO file : group) {
                if (file.getType() == VmHostFileType.NvRam) {
                    syncMsg.setNvRamPath(file.getPath());
                } else if (file.getType() == VmHostFileType.TpmState) {
                    syncMsg.setTpmStateFolder(file.getPath());
                }
            }

            bus.makeLocalServiceId(syncMsg, VmInstanceConstant.SECURE_BOOT_SERVICE_ID);
            bus.send(syncMsg, new CloudBusCallBack(whileCompletion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(String.format("failed to sync VM host file[vmUuid=%s] from host[uuid=%s]: %s",
                                vmUuid, hostUuid, reply.getError().getReadableDetails()));
                    }
                    whileCompletion.done();
                }
            });
        }, concurrency).run(new WhileDoneCompletion(null) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                // periodic sync round finished, empty callback
            }
        });
    }
}
