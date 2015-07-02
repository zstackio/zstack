package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.workflow.*;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.BackupStorageVO_;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.VmInstanceSpec.ImageSpec;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.identity.AccountManager;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.storage.primary.PrimaryStoragePathMaker;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by frank on 6/30/2015.
 */
public class LocalStorageKvmBackend extends LocalStorageHypervisorBackend {
    private final static CLogger logger = Utils.getLogger(LocalStorageKvmBackend.class);

    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private LocalStorageFactory localStorageFactory;

    public static class AgentCommand {
    }

    public static class AgentResponse {
        private Long totalCapacity;
        private Long availableCapacity;

        private boolean success = true;
        private String error;
        public boolean isSuccess() {
            return success;
        }
        public void setSuccess(boolean success) {
            this.success = success;
        }
        public String getError() {
            return error;
        }
        public void setError(String error) {
            this.error = error;
        }
        public Long getTotalCapacity() {
            return totalCapacity;
        }

        public void setTotalCapacity(Long totalCapacity) {
            this.totalCapacity = totalCapacity;
        }

        public Long getAvailableCapacity() {
            return availableCapacity;
        }

        public void setAvailableCapacity(Long availableCapacity) {
            this.availableCapacity = availableCapacity;
        }
    }

    public static class InitCmd extends AgentCommand {
        private String path;
        private String hostUuid;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public static class CreateEmptyVolumeCmd extends AgentCommand {
        private String installUrl;
        private long size;
        private String accountUuid;
        private String name;
        private String volumeUuid;

        public String getInstallUrl() {
            return installUrl;
        }

        public void setInstallUrl(String installUrl) {
            this.installUrl = installUrl;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public String getAccountUuid() {
            return accountUuid;
        }

        public void setAccountUuid(String accountUuid) {
            this.accountUuid = accountUuid;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVolumeUuid() {
            return volumeUuid;
        }

        public void setVolumeUuid(String volumeUuid) {
            this.volumeUuid = volumeUuid;
        }
    }

    public static class CreateEmptyVolumeRsp extends AgentResponse {
    }

    public static class GetPhysicalCapacityCmd extends AgentCommand {
        private String hostUuid;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }
    }

    public static class CreateVolumeFromCacheCmd extends AgentCommand {
        private String templatePathInCache;
        private String installUrl;
        private String volumeUuid;

        public String getTemplatePathInCache() {
            return templatePathInCache;
        }

        public void setTemplatePathInCache(String templatePathInCache) {
            this.templatePathInCache = templatePathInCache;
        }

        public String getInstallUrl() {
            return installUrl;
        }

        public void setInstallUrl(String installUrl) {
            this.installUrl = installUrl;
        }

        public String getVolumeUuid() {
            return volumeUuid;
        }

        public void setVolumeUuid(String volumeUuid) {
            this.volumeUuid = volumeUuid;
        }
    }

    public static class CreateVolumeFromCacheRsp extends AgentResponse {

    }

    public static class DeleteBitsCmd extends AgentCommand {
        private String path;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public static class DeleteBitsRsp extends AgentResponse {
    }

    public static final String INIT_PATH = "/localstorage/init";
    public static final String GET_PHYSICAL_CAPACITY_PATH = "/localstorage/getphysicalcapacity";
    public static final String CREATE_EMPTY_VOLUME_PATH = "/localstorage/volume/createempty";
    public static final String CREATE_VOLUME_FROM_CACHE_PATH = "/localstorage/volume/createvolumefromcache";
    public static final String DELETE_BITS_PATH = "/localstorage/delete";

    public LocalStorageKvmBackend(PrimaryStorageVO self) {
        super(self);
    }

    public String makeRootVolumeInstallUrl(VolumeInventory vol) {
        return PathUtil.join(self.getUrl(), PrimaryStoragePathMaker.makeRootVolumeInstallPath(vol));
    }

    public String makeDataVolumeInstallUrl(String volUuid) {
        return PathUtil.join(self.getUrl(), PrimaryStoragePathMaker.makeDataVolumeInstallPath(volUuid));
    }

    public String makeCachedImageInstallUrl(ImageInventory iminv) {
        return PathUtil.join(self.getUrl(), PrimaryStoragePathMaker.makeCachedImageInstallPath(iminv));
    }

    public String makeTemplateFromVolumeInWorkspacePath(String imageUuid) {
        return PathUtil.join(self.getUrl(), "templateWorkspace", String.format("image-%s", imageUuid), String.format("%s.qcow2", imageUuid));
    }

    @Override
    void syncPhysicalCapacityInCluster(List<ClusterInventory> clusters, final ReturnValueCompletion<PhysicalCapacityUsage> completion) {
        List<String> clusterUuids = CollectionUtils.transformToList(clusters, new Function<String, ClusterInventory>() {
            @Override
            public String call(ClusterInventory arg) {
                return arg.getUuid();
            }
        });

        final PhysicalCapacityUsage ret = new PhysicalCapacityUsage();

        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
        q.select(HostVO_.uuid);
        q.add(HostVO_.clusterUuid, Op.IN, clusterUuids);
        final List<String> hostUuids = q.listValue();

        if (hostUuids.isEmpty()) {
            completion.success(ret);
            return;
        }

        List<KVMHostAsyncHttpCallMsg> msgs = CollectionUtils.transformToList(hostUuids, new Function<KVMHostAsyncHttpCallMsg, String>() {
            @Override
            public KVMHostAsyncHttpCallMsg call(String arg) {
                GetPhysicalCapacityCmd cmd = new GetPhysicalCapacityCmd();
                cmd.setHostUuid(arg);

                KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                msg.setHostUuid(arg);
                msg.setCommand(cmd);
                msg.setPath(GET_PHYSICAL_CAPACITY_PATH);
                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, arg);
                return msg;
            }
        });

        bus.send(msgs, new CloudBusListCallBack(completion) {
            @Override
            public void run(List<MessageReply> replies) {
                for (MessageReply reply : replies) {
                    String hostUuid = hostUuids.get(replies.indexOf(reply));

                    if (!reply.isSuccess()) {
                        logger.warn(String.format("cannot get the physical capacity of local storage on the host[uuid:%s], %s", hostUuid, reply.getError()));
                        continue;
                    }

                    KVMHostAsyncHttpCallReply r = reply.castReply();
                    AgentResponse rsp = r.toResponse(AgentResponse.class);

                    if (!rsp.isSuccess()) {
                        logger.warn(String.format("cannot get the physical capacity of local storage on the host[uuid:%s], %s", hostUuid, rsp.getError()));
                        continue;
                    }

                    ret.totalPhysicalSize += rsp.getTotalCapacity();
                    ret.availablePhysicalSize += rsp.getAvailableCapacity();
                }

                completion.success(ret);
            }
        });
    }

    @Override
    void handle(InstantiateVolumeMsg msg, ReturnValueCompletion<InstantiateVolumeReply> completion) {
        if (msg instanceof  InstantiateRootVolumeFromTemplateMsg) {
            createRootVolume((InstantiateRootVolumeFromTemplateMsg) msg, completion);
        } else {
            createEmptyVolume(msg, completion);
        }
    }

    private void createEmptyVolume(final InstantiateVolumeMsg msg, final ReturnValueCompletion<InstantiateVolumeReply> completion) {
        final CreateEmptyVolumeCmd cmd = new CreateEmptyVolumeCmd();
        cmd.setAccountUuid(acntMgr.getOwnerAccountUuidOfResource(msg.getVolume().getUuid()));
        if (VolumeType.Root.toString().equals(msg.getVolume().getType())) {
            cmd.setInstallUrl(makeRootVolumeInstallUrl(msg.getVolume()));
        } else {
            cmd.setInstallUrl(makeDataVolumeInstallUrl(msg.getVolume().getUuid()));
        }
        cmd.setName(msg.getVolume().getName());
        cmd.setSize(msg.getVolume().getSize());
        cmd.setVolumeUuid(msg.getVolume().getUuid());

        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setHostUuid(msg.getDestHost().getUuid());
        kmsg.setPath(CREATE_EMPTY_VOLUME_PATH);
        kmsg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, msg.getDestHost().getUuid());
        bus.send(kmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                InstantiateVolumeReply r = new InstantiateVolumeReply();
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply kr = reply.castReply();
                CreateEmptyVolumeRsp rsp = kr.toResponse(CreateEmptyVolumeRsp.class);
                if (!rsp.isSuccess()) {
                    completion.fail(errf.stringToOperationError(
                            String.format("unable to create an empty volume[uuid:%s, name:%s] on the kvm host[uuid:%s], %s",
                                    msg.getVolume().getUuid(), msg.getVolume().getName(), msg.getDestHost().getUuid(), rsp.getError())
                    ));
                    return;
                }

                VolumeInventory vol = msg.getVolume();
                vol.setInstallPath(cmd.getInstallUrl());
                r.setVolume(vol);

                completion.success(r);
            }
        });
    }

    private String getHostUuidByResourceUuid(String resUuid, String resType) {
        SimpleQuery<LocalStorageResourceRefVO> q = dbf.createQuery(LocalStorageResourceRefVO.class);
        q.select(LocalStorageResourceRefVO_.hostUuid);
        q.add(LocalStorageResourceRefVO_.primaryStorageUuid, Op.EQ, self.getUuid());
        q.add(LocalStorageResourceRefVO_.resourceUuid, Op.EQ, resUuid);
        String hostUuid = q.findValue();

        if (hostUuid == null) {
            throw new CloudRuntimeException(String.format("resource[uuid:%s, type:%s] is not any on any host of local primary storage[uuid:%s]",
                    resUuid, resType, self.getUuid()));
        }

        return hostUuid;
    }

    private void createRootVolume(InstantiateRootVolumeFromTemplateMsg msg, final ReturnValueCompletion<InstantiateVolumeReply> completion) {
        final ImageSpec ispec = msg.getTemplateSpec();
        final ImageInventory image = ispec.getInventory();

        if (!ImageMediaType.RootVolumeTemplate.toString().equals(image.getMediaType())) {
            createEmptyVolume(msg, completion);
            return;
        }

        SimpleQuery<BackupStorageVO> q = dbf.createQuery(BackupStorageVO.class);
        q.add(BackupStorageVO_.uuid, Op.EQ, ispec.getSelectedBackupStorage().getBackupStorageUuid());
        BackupStorageVO bs = q.find();

        final BackupStorageInventory bsInv = BackupStorageInventory.valueOf(bs);
        final VolumeInventory volume = msg.getVolume();
        final String hostUuid = msg.getDestHost().getUuid();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("kvm-localstorage-create-root-volume-from-image-%s", image.getUuid()));
        chain.then(new ShareFlow() {
            String pathInCache = makeCachedImageInstallUrl(image);
            String installPath;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "download-image-to-cache";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        thdf.chainSubmit(new ChainTask(trigger) {
                            @Override
                            public String getSyncSignature() {
                                return String.format("localstorage-download-image-%s", image.getUuid());
                            }

                            @Override
                            public void run(final SyncTaskChain schain) {
                                LocalStorageBackupStorageMediator m = localStorageFactory.getBackupStorageMediator(KVMConstant.KVM_HYPERVISOR_TYPE, bsInv.getType());
                                m.downloadBits(getSelfInventory(), bsInv, ispec.getSelectedBackupStorage().getInstallPath(), pathInCache,
                                        hostUuid, new Completion(trigger, schain) {
                                    @Override
                                    public void success() {
                                        trigger.next();
                                        schain.next();
                                    }

                                    @Override
                                    public void fail(ErrorCode errorCode) {
                                        trigger.fail(errorCode);
                                        schain.next();
                                    }
                                });
                            }

                            @Override
                            public String getName() {
                                return "download-image-to-localstorage-cache";
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        installPath = makeRootVolumeInstallUrl(volume);

                        CreateVolumeFromCacheCmd cmd = new CreateVolumeFromCacheCmd();
                        cmd.setInstallUrl(installPath);
                        cmd.setTemplatePathInCache(pathInCache);
                        cmd.setVolumeUuid(volume.getUuid());

                        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
                        kmsg.setCommand(cmd);
                        kmsg.setHostUuid(hostUuid);
                        kmsg.setPath(CREATE_VOLUME_FROM_CACHE_PATH);
                        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, hostUuid);
                        bus.send(kmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                KVMHostAsyncHttpCallReply kr = reply.castReply();
                                CreateVolumeFromCacheRsp rsp = kr.toResponse(CreateVolumeFromCacheRsp.class);
                                if (!rsp.isSuccess()) {
                                    trigger.fail(errf.stringToOperationError(rsp.getError()));
                                    return;
                                }

                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        InstantiateVolumeReply reply = new InstantiateVolumeReply();
                        volume.setInstallPath(installPath);
                        reply.setVolume(volume);
                        completion.success(reply);
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void deleteBits(String path, String hostUuid, final Completion completion) {
        DeleteBitsCmd cmd = new DeleteBitsCmd();
        cmd.setPath(path);

        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setHostUuid(hostUuid);
        kmsg.setCommand(cmd);
        kmsg.setPath(DELETE_BITS_PATH);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(kmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply kr = reply.castReply();
                DeleteBitsRsp rsp = kr.toResponse(DeleteBitsRsp.class);
                if (!rsp.isSuccess()) {
                    completion.fail(errf.stringToOperationError(rsp.getError()));
                    return;
                }

                completion.success();
            }
        });
    }

    @Override
    void handle(DeleteVolumeOnPrimaryStorageMsg msg, final ReturnValueCompletion<DeleteVolumeOnPrimaryStorageReply> completion) {
        String hostUuid = getHostUuidByResourceUuid(msg.getVolume().getUuid(), VolumeVO.class.getSimpleName());
        deleteBits(msg.getVolume().getInstallPath(), hostUuid, new Completion(completion) {
            @Override
            public void success() {
                DeleteVolumeOnPrimaryStorageReply dreply = new DeleteVolumeOnPrimaryStorageReply();
                completion.success(dreply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(DownloadDataVolumeToPrimaryStorageMsg msg, ReturnValueCompletion<DownloadDataVolumeToPrimaryStorageReply> completion) {

    }

    @Override
    void handle(DeleteBitsOnPrimaryStorageMsg msg, final ReturnValueCompletion<DeleteBitsOnPrimaryStorageReply> completion) {
        String hostUuid = getHostUuidByResourceUuid(msg.getBitsUuid(), msg.getBitsType());
        deleteBits(msg.getInstallPath(), hostUuid, new Completion(completion) {
            @Override
            public void success() {
                DeleteBitsOnPrimaryStorageReply reply = new DeleteBitsOnPrimaryStorageReply();
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(DownloadIsoToPrimaryStorageMsg msg, final ReturnValueCompletion<DownloadIsoToPrimaryStorageReply> completion) {
        ImageSpec ispec = msg.getIsoSpec();
        SimpleQuery<BackupStorageVO> q = dbf.createQuery(BackupStorageVO.class);
        q.add(BackupStorageVO_.uuid, Op.EQ, ispec.getSelectedBackupStorage().getBackupStorageUuid());
        BackupStorageVO bsvo = q.find();

        SimpleQuery<HostVO> hq = dbf.createQuery(HostVO.class);
        hq.select(HostVO_.hypervisorType);
        hq.add(HostVO_.uuid, Op.EQ, msg.getDestHostUuid());
        String hvType = hq.findValue();

        final String installPath = makeCachedImageInstallUrl(ispec.getInventory());

        BackupStorageInventory bsinv = BackupStorageInventory.valueOf(bsvo);
        LocalStorageBackupStorageMediator m = localStorageFactory.getBackupStorageMediator(hvType, bsinv.getType());
        m.downloadBits(getSelfInventory(), bsinv, ispec.getSelectedBackupStorage().getInstallPath(), installPath, msg.getDestHostUuid(), new Completion(completion) {
            @Override
            public void success() {
                DownloadIsoToPrimaryStorageReply reply = new DownloadIsoToPrimaryStorageReply();
                reply.setInstallPath(installPath);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(DeleteIsoFromPrimaryStorageMsg msg, ReturnValueCompletion<DeleteIsoFromPrimaryStorageReply> completion) {
        // The ISO is in the image cache, no need to delete it
        DeleteIsoFromPrimaryStorageReply reply = new DeleteIsoFromPrimaryStorageReply();
        completion.success(reply);
    }

    @Override
    public void detachHook(String clusterUuid, final Completion completion) {
        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
        q.select(HostVO_.uuid);
        q.add(HostVO_.clusterUuid, Op.EQ, clusterUuid);
        final List<String> hostUuids = q.listValue();

        if (hostUuids.isEmpty()) {
            completion.success();
            return;
        }

        SimpleQuery<LocalStorageHostRefVO> refq = dbf.createQuery(LocalStorageHostRefVO.class);
        refq.add(LocalStorageHostRefVO_.primaryStorageUuid, Op.EQ, self.getUuid());
        refq.add(LocalStorageHostRefVO_.hostUuid, Op.IN, hostUuids);
        List<LocalStorageHostRefVO> refs = refq.list();
        if (!refs.isEmpty()) {
            dbf.removeCollection(refs, LocalStorageHostRefVO.class);

            long total = 0;
            for (LocalStorageHostRefVO ref : refs) {
                total += ref.getTotalCapacity();
            }

            // after detaching, total capacity on those hosts should be deducted
            // from both total and available capacity of the primary storage
            decreaseCapacity(total, total, null, null);
        }

        syncPhysicalCapacity(new ReturnValueCompletion<PhysicalCapacityUsage>(completion) {
            @Override
            public void success(PhysicalCapacityUsage returnValue) {
                setCapacity(null, null, returnValue.totalPhysicalSize, returnValue.availablePhysicalSize);
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("failed to sync the physical capacity on the local primary storage[uuid:%s], %s", self.getUuid(), errorCode));
                completion.success();
            }
        });

    }

    @Override
    public void attachHook(String clusterUuid, final Completion completion) {
        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
        q.select(HostVO_.uuid);
        q.add(HostVO_.clusterUuid, Op.EQ, clusterUuid);
        final List<String> hostUuids = q.listValue();

        if (hostUuids.isEmpty()) {
            completion.success();
            return;
        }

        List<KVMHostAsyncHttpCallMsg> msgs = CollectionUtils.transformToList(hostUuids, new Function<KVMHostAsyncHttpCallMsg, String>() {
            @Override
            public KVMHostAsyncHttpCallMsg call(String arg) {
                InitCmd cmd = new InitCmd();
                cmd.path = self.getUrl();
                cmd.hostUuid = arg;

                KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                msg.setCommand(cmd);
                msg.setPath(INIT_PATH);
                msg.setHostUuid(arg);
                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, arg);
                return msg;
            }
        });

        bus.send(msgs, new CloudBusListCallBack(completion) {
            @Override
            public void run(List<MessageReply> replies) {
                long total = 0;
                long avail = 0;
                List<LocalStorageHostRefVO> refs = new ArrayList<LocalStorageHostRefVO>();

                for (MessageReply reply : replies) {
                    String hostUuid = hostUuids.get(replies.indexOf(reply));
                    if (!reply.isSuccess()) {
                        logger.warn(String.format("cannot get the physical capacity of local storage on the host[uuid:%s], %s", hostUuid, reply.getError()));
                        continue;
                    }

                    KVMHostAsyncHttpCallReply r = reply.castReply();
                    AgentResponse rsp = r.toResponse(AgentResponse.class);
                    if (!rsp.isSuccess()) {
                        logger.warn(String.format("cannot get the physical capacity of local storage on the host[uuid:%s], %s", hostUuid, rsp.getError()));
                        continue;
                    }

                    total += rsp.getTotalCapacity();
                    avail += rsp.getAvailableCapacity();

                    LocalStorageHostRefVO ref = new LocalStorageHostRefVO();
                    ref.setPrimaryStorageUuid(self.getUuid());
                    ref.setHostUuid(hostUuid);
                    ref.setAvailablePhysicalCapacity(rsp.getAvailableCapacity());
                    ref.setAvailableCapacity(rsp.getAvailableCapacity());
                    ref.setTotalCapacity(rsp.getTotalCapacity());
                    ref.setTotalPhysicalCapacity(rsp.getTotalCapacity());
                    refs.add(ref);
                }

                dbf.persistCollection(refs);

                increaseCapacity(total, avail, total, avail);

                completion.success();
            }
        });
    }
}
