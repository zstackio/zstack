package org.zstack.kvm;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.SQL;
import org.zstack.core.db.Q;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vm.VmStateChangedExtensionPoint;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeProtocol;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;
import javax.persistence.Tuple;

public class KvmVmActiveVolumeSyncPingTask implements KVMPingAgentNoFailureExtensionPoint, VmStateChangedExtensionPoint, Component {
    private static final CLogger logger = Utils.getLogger(KvmVmActiveVolumeSyncPingTask.class);
    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private PluginRegistry pluginRgty;

    private List<KvmVmActiveVolumeSyncExtensionPoint> exts;

    @Override
    public void vmStateChanged(VmInstanceInventory vm, VmInstanceState oldState, VmInstanceState newState) {
        if (newState != VmInstanceState.Stopped || oldState == VmInstanceState.Stopping
                || !KVMConstant.KVM_HYPERVISOR_TYPE.equals(vm.getHypervisorType())
                || vm.getLastHostUuid() == null) {
            return;
        }

        thdf.chainSubmit(new ChainTask(null) {
            @Override
            public String getSyncSignature() {
                return VmInstanceConstant.VM_SYNC_SIGNATURE_PREFIX + vm.getUuid();
            }

            @Override
            public String getName() {
                return String.format("deactivate-volumes-of-stopped-vm-%s", vm.getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                if (!Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm.getUuid())
                        .eq(VmInstanceVO_.state, VmInstanceState.Stopped)
                        .eq(VmInstanceVO_.lastHostUuid, vm.getLastHostUuid())
                        .notNull(VmInstanceVO_.rootVolumeUuid).isExists()) {
                    chain.next();
                    return;
                }

                HostVO hostVO = Q.New(HostVO.class).eq(HostVO_.uuid, vm.getLastHostUuid()).find();
                if (hostVO == null) {
                    chain.next();
                    return;
                }

                HostInventory host = HostInventory.valueOf(hostVO);
                List<Tuple> volumes = Q.New(VolumeVO.class)
                        .select(VolumeVO_.primaryStorageUuid, VolumeVO_.installPath)
                        .eq(VolumeVO_.vmInstanceUuid, vm.getUuid())
                        .eq(VolumeVO_.isShareable, false)
                        .eq(VolumeVO_.protocol, VolumeProtocol.Vhost.toString())
                        .in(VolumeVO_.type, Arrays.asList(VolumeType.Root, VolumeType.Data))
                        .notNull(VolumeVO_.primaryStorageUuid).notNull(VolumeVO_.installPath).listTuple();
                Map<String, List<String>> pathsByStorage = new HashMap<>();
                volumes.forEach(volume -> pathsByStorage.computeIfAbsent(volume.get(0, String.class),
                        uuid -> new ArrayList<>()).add(volume.get(1, String.class)));
                Map<PrimaryStorageInventory, List<String>> inactiveVolumePaths = new HashMap<>();
                pathsByStorage.forEach((uuid, paths) -> {
                    PrimaryStorageInventory ps = new PrimaryStorageInventory();
                    ps.setUuid(uuid);
                    inactiveVolumePaths.put(ps, paths);
                });

                new While<>(exts).each((ext, completion) -> ext.handleInactiveVolume(
                        host, inactiveVolumePaths, new Completion(completion) {
                            @Override
                            public void success() {
                                completion.done();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                completion.addError(errorCode);
                                completion.done();
                            }
                        })).run(new WhileDoneCompletion(chain) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errorCodeList.getCauses().isEmpty()) {
                            logger.warn(String.format("failed to deactivate volumes of stopped vm[uuid:%s] on host[uuid:%s]: %s",
                                    vm.getUuid(), host.getUuid(), errorCodeList));
                        }
                        chain.next();
                    }
                });
            }
        });
    }


    private void syncVolume(final HostInventory host, final NoErrorCompletion completion) {
        List<PrimaryStorageVO> psVOs = SQL.New("select ps from PrimaryStorageVO ps, PrimaryStorageClusterRefVO ref" +
                        " where ref.primaryStorageUuid = ps.uuid" +
                        " and ref.clusterUuid = :clusterUuid", PrimaryStorageVO.class)
                .param("clusterUuid", host.getClusterUuid()).list();

        Map<PrimaryStorageInventory, List<String>> storagePaths = new HashMap<>();
        for (KvmVmActiveVolumeSyncExtensionPoint ext : exts) {
            for (PrimaryStorageVO psVO : psVOs) {
                PrimaryStorageInventory psInv = PrimaryStorageInventory.valueOf(psVO);
                List<String> paths = ext.getStoragePathsForVolumeSync(host, psInv);
                if (CollectionUtils.isNotEmpty(paths)) {
                    storagePaths.computeIfAbsent(psInv, k -> new ArrayList<>()).addAll(paths);
                }
            }
        }

        if (storagePaths.isEmpty()) {
            completion.done();
            return;
        }

        Map<String, List<PrimaryStorageInventory>> psInvsByPath = new HashMap<>();
        storagePaths.forEach((psInv, paths) -> {
            paths.forEach(path -> {
                psInvsByPath.computeIfAbsent(path, k -> new ArrayList<>()).add(psInv);
            });
        });

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        KVMAgentCommands.VolumeSyncCmd cmd = new KVMAgentCommands.VolumeSyncCmd();
        cmd.setStoragePaths(storagePaths.values().stream().flatMap(Collection::stream).collect(Collectors.toList()));

        msg.setCommand(cmd);
        msg.setNoStatusCheck(true);
        msg.setHostUuid(host.getUuid());
        msg.setPath(KVMConstant.KVM_VOLUME_SYNC_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to sync volume on kvm host[uuid:%s, ip:%s], %s",
                            host.getUuid(), host.getManagementIp(), reply.getError()));
                    completion.done();
                    return;
                }
                KVMHostAsyncHttpCallReply r = reply.castReply();
                KVMAgentCommands.VolumeSyncRsp ret = r.toResponse(KVMAgentCommands.VolumeSyncRsp.class);
                if (!ret.isSuccess()) {
                    logger.warn(String.format("failed to sync volume on kvm host[uuid:%s, ip:%s], %s",
                            host.getUuid(), host.getManagementIp(), ret.getError()));
                    completion.done();
                    return;
                }

                if (ret.getInactiveVolumePaths() == null || ret.getInactiveVolumePaths().isEmpty() ||
                        ret.getInactiveVolumePaths().values().stream().allMatch(List::isEmpty)) {
                    completion.done();
                    return;
                }

                Map<PrimaryStorageInventory, List<String>> inactiveVolumePaths = new HashMap<>();
                ret.getInactiveVolumePaths().forEach((storagePath, volumePaths) -> {
                    psInvsByPath.get(storagePath).forEach(psInv ->
                            inactiveVolumePaths.computeIfAbsent(psInv, k -> new ArrayList<>()).addAll(volumePaths));
                });

                new While<>(exts).each((ext, wcomp) -> {
                    ext.handleInactiveVolume(host, inactiveVolumePaths, new Completion(wcomp) {
                        @Override
                        public void success() {
                            wcomp.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            wcomp.done();
                        }
                    });
                }).run(new WhileDoneCompletion(completion) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        completion.done();
                    }
                });
            }
        });
    }


    @Override
    public boolean start() {
        exts = pluginRgty.getExtensionList(KvmVmActiveVolumeSyncExtensionPoint.class);
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void kvmPingAgentNoFailure(KVMHostInventory host, NoErrorCompletion completion) {
        if (!KVMGlobalConfig.VM_SYNC_ON_HOST_PING.value(Boolean.class)) {
            completion.done();
            return;
        }

        thdf.chainSubmit(new ChainTask(null) {
            @Override
            public String getSyncSignature() {
                return String.format("sync-volume-state-after-ping-host-%s-success", host.getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                syncVolume(host, new NoErrorCompletion() {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
        completion.done();
    }
}
