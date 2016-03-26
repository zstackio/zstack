package org.zstack.storage.primary.smp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.vm.VmInstanceSpec.ImageSpec;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.storage.backup.sftp.GetSftpBackupStorageDownloadCredentialMsg;
import org.zstack.storage.backup.sftp.GetSftpBackupStorageDownloadCredentialReply;
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant;
import org.zstack.storage.primary.PrimaryStorageCapacityUpdater;
import org.zstack.storage.primary.PrimaryStoragePathMaker;
import org.zstack.storage.primary.PrimaryStoragePhysicalCapacityManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import javax.persistence.TypedQuery;
import java.io.File;
import java.util.*;

/**
 * Created by xing5 on 2016/3/26.
 */
public class KvmBackend extends HypervisorBackend {
    private static final CLogger logger = Utils.getLogger(KvmBackend.class);

    @Autowired
    protected ApiTimeoutManager timeoutManager;
    @Autowired
    protected PrimaryStorageOverProvisioningManager ratioMgr;
    @Autowired
    protected PrimaryStoragePhysicalCapacityManager physicalCapacityMgr;
    @Autowired
    protected PluginRegistry pluginRgty;

    public static class AgentCmd {
    }

    public static class AgentRsp {
        public boolean success = true;
        public String error;
        public Long totalCapacity;
        public Long availableCapacity;
    }

    public static class ConnectCmd extends AgentCmd {
        public String uuid;
        public String mountPoint;
    }


    private static class CreateVolumeFromCacheCmd extends AgentCmd {
        public String templatePathInCache;
        public String installPath;
        public String volumeUuid;
    }

    public static class DeleteBitsCmd extends AgentCmd {
        public String path;
    }

    public static class CreateTemplateFromVolumeCmd extends AgentCmd {
        public String installPath;
        public String volumePath;
    }

    public static class SftpUploadBitsCmd extends AgentCmd {
        public String primaryStorageInstallPath;
        public String backupStorageInstallPath;
        public String hostname;
        public String sshKey;
    }

    public static final String CONNECT_PATH = "/sharedmountpointpirmarystorage/connect";
    public static final String CREATE_VOLUME_FROM_CACHE_PATH = "/sharedmountpointpirmarystorage/createrootvolume";
    public static final String DELETE_BITS_PATH = "/sharedmountpointpirmarystorage/deletebits";
    public static final String CREATE_TEMPLATE_FROM_VOLUME_PATH = "/sharedmountpointpirmarystorage/createtemplatefromvolume";
    public static final String UPLOAD_BITS_TO_SFTP_BACKUPSTORAGE_PATH = "/sharedmountpointpirmarystorage/sftp/uploads";

    public KvmBackend(PrimaryStorageVO self) {
        super(self);
    }


    private String findConnectedHostByClusterUuid(String clusterUuid) {
        return findConnectedHostByClusterUuid(clusterUuid, true);
    }

    private String findConnectedHostByClusterUuid(String clusterUuid, boolean excptionOnNotFound) {
        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
        q.select(HostVO_.uuid);
        q.add(HostVO_.clusterUuid, Op.EQ, clusterUuid);
        q.add(HostVO_.status, Op.EQ, HostStatus.Connected);
        q.setLimit(200);
        List<String> hostUuids = q.listValue();
        if (hostUuids.isEmpty() && excptionOnNotFound) {
            throw new OperationFailureException(errf.stringToOperationError(
                    String.format("no connected host found in the cluster[uuid:%s]", clusterUuid)
            ));
        }

        Collections.shuffle(hostUuids);
        return hostUuids.get(0);
    }

    private <T extends AgentRsp> void httpCall(String path, final String hostUuid, AgentCmd cmd, final Class<T> rspType, final ReturnValueCompletion<T> completion) {
        httpCall(path, hostUuid, cmd, false, rspType, completion);
    }

    private <T extends AgentRsp> void httpCall(String path, final String hostUuid, AgentCmd cmd, boolean noCheckStatus, final Class<T> rspType, final ReturnValueCompletion<T> completion) {
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hostUuid);
        msg.setPath(path);
        msg.setNoStatusCheck(noCheckStatus);
        msg.setCommand(cmd);
        msg.setCommandTimeout(timeoutManager.getTimeout(cmd.getClass(), "5m"));
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply r = reply.castReply();
                T rsp = r.toResponse(rspType);
                if (!rsp.success) {
                    completion.fail(errf.stringToOperationError(rsp.error));
                    return;
                }

                if (rsp.totalCapacity != null && rsp.availableCapacity != null) {
                    new PrimaryStorageCapacityUpdater(self.getUuid()).update(null, null, rsp.totalCapacity, rsp.availableCapacity);
                }

                completion.success(rsp);
            }
        });
    }

    private void connect(String hostUuid, final Completion completion) {
        ConnectCmd cmd = new ConnectCmd();
        cmd.uuid = self.getUuid();
        cmd.mountPoint = self.getMountPath();

        httpCall(CONNECT_PATH, hostUuid, cmd, true, AgentRsp.class, new ReturnValueCompletion<AgentRsp>(completion) {
            @Override
            public void success(AgentRsp rsp) {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void attachHook(String clusterUuid, final Completion completion) {
        String huuid = findConnectedHostByClusterUuid(clusterUuid);
        connect(huuid, completion);
    }

    @Override
    void handle(InstantiateVolumeMsg msg, ReturnValueCompletion<InstantiateVolumeReply> completion) {
        if (msg instanceof InstantiateRootVolumeFromTemplateMsg) {
            createRootVolume((InstantiateRootVolumeFromTemplateMsg)msg, completion);
        } else {
            createEmptyVolume(msg.getVolume(), msg.getDestHost().getUuid(), completion);
        }
    }

    public String makeRootVolumeInstallUrl(VolumeInventory vol) {
        return PathUtil.join(self.getMountPath(), PrimaryStoragePathMaker.makeRootVolumeInstallPath(vol));
    }

    public String makeDataVolumeInstallUrl(String volUuid) {
        return PathUtil.join(self.getMountPath(), PrimaryStoragePathMaker.makeDataVolumeInstallPath(volUuid));
    }

    public String makeCachedImageInstallUrl(ImageInventory iminv) {
        return PathUtil.join(self.getMountPath(), PrimaryStoragePathMaker.makeCachedImageInstallPath(iminv));
    }

    public String makeTemplateFromVolumeInWorkspacePath(String imageUuid) {
        return PathUtil.join(self.getMountPath(), "templateWorkspace", String.format("image-%s", imageUuid), String.format("%s.qcow2", imageUuid));
    }

    public String makeSnapshotInstallPath(VolumeInventory vol, VolumeSnapshotInventory snapshot) {
        String volPath;
        if (VolumeType.Data.toString().equals(vol.getType())) {
            volPath = makeDataVolumeInstallUrl(vol.getUuid());
        } else {
            volPath = makeRootVolumeInstallUrl(vol);
        }
        File volDir = new File(volPath).getParentFile();
        return PathUtil.join(volDir.getAbsolutePath(), "snapshots", String.format("%s.qcow2", snapshot.getUuid()));
    }

    public String makeSnapshotWorkspacePath(String imageUuid) {
        return PathUtil.join(
                self.getMountPath(),
                PrimaryStoragePathMaker.makeImageFromSnapshotWorkspacePath(imageUuid),
                String.format("%s.qcow2", imageUuid)
        );
    }

    class ImageCache {
        ImageInventory image;
        BackupStorageInventory backupStorage;
        String hostUuid;
        String primaryStorageInstallPath;
        String backupStorageInstallPath;

        void download(final ReturnValueCompletion<String> completion) {

        }
    }

    private void createEmptyVolume(VolumeInventory volume, String hostUuid, ReturnValueCompletion<InstantiateVolumeReply> completion) {
    }

    private void createRootVolume(InstantiateRootVolumeFromTemplateMsg msg, final ReturnValueCompletion<InstantiateVolumeReply> completion) {
        final ImageSpec ispec = msg.getTemplateSpec();
        final ImageInventory image = ispec.getInventory();

        if (!ImageMediaType.RootVolumeTemplate.toString().equals(image.getMediaType())) {
            createEmptyVolume(msg.getVolume(), msg.getDestHost().getUuid(), completion);
            return;
        }

        SimpleQuery<BackupStorageVO> q = dbf.createQuery(BackupStorageVO.class);
        q.add(BackupStorageVO_.uuid, Op.EQ, ispec.getSelectedBackupStorage().getBackupStorageUuid());
        BackupStorageVO bs = q.find();

        final BackupStorageInventory bsInv = BackupStorageInventory.valueOf(bs);
        final VolumeInventory volume = msg.getVolume();
        final String hostUuid = msg.getDestHost().getUuid();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("kvm-smp-storage-create-root-volume-from-image-%s", image.getUuid()));
        chain.then(new ShareFlow() {
            String pathInCache = makeCachedImageInstallUrl(image);
            String installPath;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "download-image-to-cache";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        ImageCache cache = new ImageCache();
                        cache.backupStorage = bsInv;
                        cache.backupStorageInstallPath = ispec.getSelectedBackupStorage().getInstallPath();
                        cache.primaryStorageInstallPath = pathInCache;
                        cache.hostUuid = hostUuid;
                        cache.image = image;
                        cache.download(new ReturnValueCompletion<String>(trigger) {
                            @Override
                            public void success(String returnValue) {
                                pathInCache = returnValue;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "create-template-from-cache";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        installPath = makeRootVolumeInstallUrl(volume);

                        CreateVolumeFromCacheCmd cmd = new CreateVolumeFromCacheCmd();
                        cmd.installPath = installPath;
                        cmd.templatePathInCache = pathInCache;
                        cmd.volumeUuid = volume.getUuid();

                        httpCall(CREATE_VOLUME_FROM_CACHE_PATH, hostUuid, cmd, AgentRsp.class, new ReturnValueCompletion<AgentRsp>(trigger) {
                            @Override
                            public void success(AgentRsp rsp) {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
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

    class Do {
        private List<String> hostUuids;
        private List<ErrorCode> errors = new ArrayList<ErrorCode>();

        @Transactional(readOnly = true)
        private void findConnectedHosts() {
            String sql = "select h.uuid from HostVO h, PrimaryStorageClusterRefVO ref where ref.clusterUuid = h.clusterUuid and" +
                    " ref.primaryStorageUuid = :psUuid and h.status = :status and h.hypervisorType = :htype";
            TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
            q.setParameter("psUuid", self.getUuid());
            q.setParameter("status", HostStatus.Connected.toString());
            q.setParameter("htype", KVMConstant.KVM_HYPERVISOR_TYPE);
            q.setMaxResults(50);
            hostUuids = q.getResultList();
            Collections.shuffle(hostUuids);
        }

        void go(String path, AgentCmd cmd, ReturnValueCompletion<AgentRsp> completion) {
            findConnectedHosts();
            if (hostUuids.isEmpty()) {
                throw new OperationFailureException(errf.stringToOperationError(
                        String.format("cannot find any connected host to perform the operation, it seems all KVM hosts" +
                                " in the clusters attached with the shared mount point storage[uuid:%s] are disconnected",
                                self.getUuid())
                ));
            }

            doCommand(hostUuids.iterator(), path, cmd, completion);
        }

        private void doCommand(final Iterator<String> it, final String path, final AgentCmd cmd, final ReturnValueCompletion<AgentRsp> completion) {
            if (!it.hasNext()) {
                completion.fail(errf.stringToOperationError(
                        String.format("the operation failed on all hosts, errors are %s", errors)
                ));
                return;
            }

            final String hostUuid = it.next();
            httpCall(path, hostUuid, cmd, AgentRsp.class, new ReturnValueCompletion<AgentRsp>(completion) {
                @Override
                public void success(AgentRsp rsp) {
                    completion.success(rsp);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    if (!SysErrors.HTTP_ERROR.toString().equals(errorCode.getCode()) &&
                            !HostErrors.HOST_IS_DISCONNECTED.toString().equals(errorCode.getCode())) {
                        completion.fail(errorCode);
                        return;
                    }

                    logger.warn(String.format("failed to do the command[%s] on the kvm host[uuid:%s], %s, try next one",
                            cmd.getClass(), hostUuid, errorCode));
                    doCommand(it, path, cmd, completion);
                }
            });
        }
    }


    @Override
    void handle(DeleteVolumeOnPrimaryStorageMsg msg, final ReturnValueCompletion<DeleteVolumeOnPrimaryStorageReply> completion) {
        deleteBits(msg.getVolume().getInstallPath(), new Completion(completion) {
            @Override
            public void success() {
                DeleteVolumeOnPrimaryStorageReply reply = new DeleteVolumeOnPrimaryStorageReply();
                completion.success(reply);
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
    void handle(DeleteBitsOnPrimaryStorageMsg msg, ReturnValueCompletion<DeleteBitsOnPrimaryStorageReply> completion) {

    }

    @Override
    void handle(DownloadIsoToPrimaryStorageMsg msg, ReturnValueCompletion<DownloadIsoToPrimaryStorageReply> completion) {

    }

    @Override
    void handle(DeleteIsoFromPrimaryStorageMsg msg, ReturnValueCompletion<DeleteIsoFromPrimaryStorageReply> completion) {

    }

    @Override
    void handle(TakeSnapshotMsg msg, ReturnValueCompletion<TakeSnapshotReply> completion) {

    }

    @Override
    void handle(DeleteSnapshotOnPrimaryStorageMsg msg, ReturnValueCompletion<DeleteSnapshotOnPrimaryStorageReply> completion) {

    }

    @Override
    void handle(RevertVolumeFromSnapshotOnPrimaryStorageMsg msg, ReturnValueCompletion<RevertVolumeFromSnapshotOnPrimaryStorageReply> completion) {

    }

    @Override
    void handle(BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg msg, ReturnValueCompletion<BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply> completion) {

    }

    @Override
    void handle(CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg msg, ReturnValueCompletion<CreateTemplateFromVolumeSnapshotOnPrimaryStorageReply> completion) {

    }

    @Override
    void handle(CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg msg, ReturnValueCompletion<CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply> completion) {

    }

    @Override
    void handle(MergeVolumeSnapshotOnPrimaryStorageMsg msg, ReturnValueCompletion<MergeVolumeSnapshotOnPrimaryStorageReply> completion) {

    }

    @Override
    void downloadImageToCache(ImageInventory img, ReturnValueCompletion<String> completion) {

    }

    @Override
    void deleteBits(String path, final Completion completion) {
        DeleteBitsCmd cmd = new DeleteBitsCmd();
        cmd.path = path;
        new Do().go(DELETE_BITS_PATH, cmd, new ReturnValueCompletion<AgentRsp>(completion) {
            @Override
            public void success(AgentRsp rsp) {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(final CreateTemplateFromVolumeOnPrimaryStorageMsg msg, final ReturnValueCompletion<CreateTemplateFromVolumeOnPrimaryStorageReply> completion) {
        final CreateTemplateFromVolumeOnPrimaryStorageReply reply = new CreateTemplateFromVolumeOnPrimaryStorageReply();
        final VolumeInventory volume = msg.getVolumeInventory();
        final ImageInventory image = msg.getImageInventory();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-template-%s-from-volume-%s", image.getUuid(), volume.getUuid()));
        chain.then(new ShareFlow() {
            String temporaryTemplatePath = makeTemplateFromVolumeInWorkspacePath(msg.getImageInventory().getUuid());
            String backupStorageInstallPath;

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "create-temporary-template";

                    boolean success = false;

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        CreateTemplateFromVolumeCmd cmd = new CreateTemplateFromVolumeCmd();
                        cmd.volumePath = volume.getInstallPath();
                        cmd.installPath = temporaryTemplatePath;
                        new Do().go(CREATE_TEMPLATE_FROM_VOLUME_PATH, cmd, new ReturnValueCompletion<AgentRsp>(trigger) {
                            @Override
                            public void success(AgentRsp returnValue) {
                                success = true;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (success) {
                            deleteBits(temporaryTemplatePath, new Completion() {
                                @Override
                                public void success() {
                                    // pass
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    //TODO
                                    logger.warn(String.format("failed to delete %s, %s", temporaryTemplatePath, errorCode));
                                }
                            });
                        }

                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "upload-to-backup-storage";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        BackupStorageAskInstallPathMsg bmsg = new BackupStorageAskInstallPathMsg();
                        bmsg.setBackupStorageUuid(msg.getBackupStorageUuid());
                        bmsg.setImageMediaType(image.getMediaType());
                        bmsg.setImageUuid(image.getUuid());
                        bus.makeTargetServiceIdByResourceUuid(bmsg, BackupStorageConstant.SERVICE_ID, msg.getBackupStorageUuid());
                        MessageReply br = bus.call(bmsg);
                        if (!br.isSuccess()) {
                            trigger.fail(br.getError());
                            return;
                        }

                        backupStorageInstallPath = ((BackupStorageAskInstallPathReply) br).getInstallPath();

                        BackupStorageKvmUploader uploader = getBackupStorageKvmUploader(msg.getBackupStorageUuid());
                        uploader.uploadBits(backupStorageInstallPath, temporaryTemplatePath, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "delete-temporary-template-on-primary-storage";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        deleteBits(temporaryTemplatePath, new Completion(trigger) {
                            @Override
                            public void success() {
                                // pass
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                //TODO: cleanup
                                logger.warn(String.format("failed to delete %s on shared mount point primary storage[uuid: %s], %s; need a cleanup", temporaryTemplatePath, self.getUuid(), errorCode));
                            }
                        });

                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        reply.setFormat(volume.getFormat());
                        reply.setTemplateBackupStorageInstallPath(backupStorageInstallPath);
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

    class SftpBackupStorageKvmUploader extends BackupStorageKvmUploader {
        private final String bsUuid;

        public SftpBackupStorageKvmUploader(String backupStorageUuid) {
            bsUuid = backupStorageUuid;
        }

        @Override
        public void uploadBits(final String bsPath, final String psPath, final Completion completion) {
            GetSftpBackupStorageDownloadCredentialMsg gmsg = new GetSftpBackupStorageDownloadCredentialMsg();
            gmsg.setBackupStorageUuid(bsUuid);
            bus.makeTargetServiceIdByResourceUuid(gmsg, BackupStorageConstant.SERVICE_ID, bsUuid);
            bus.send(gmsg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        completion.fail(reply.getError());
                        return;
                    }

                    final GetSftpBackupStorageDownloadCredentialReply r = reply.castReply();
                    SftpUploadBitsCmd cmd = new SftpUploadBitsCmd();
                    cmd.primaryStorageInstallPath = psPath;
                    cmd.backupStorageInstallPath = bsPath;
                    cmd.hostname = r.getHostname();
                    cmd.sshKey = r.getSshKey();

                    new Do().go(UPLOAD_BITS_TO_SFTP_BACKUPSTORAGE_PATH, cmd, new ReturnValueCompletion<AgentRsp>(completion) {
                        @Override
                        public void success(AgentRsp returnValue) {
                            completion.success();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            completion.fail(errorCode);
                        }
                    });
                }
            });
        }
    }

    private BackupStorageKvmUploader getBackupStorageKvmUploader(String backupStorageUuid) {
        SimpleQuery<BackupStorageVO> q = dbf.createQuery(BackupStorageVO.class);
        q.select(BackupStorageVO_.type);
        q.add(BackupStorageVO_.uuid, Op.EQ, backupStorageUuid);
        String bsType = q.findValue();

        if (SftpBackupStorageConstant.SFTP_BACKUP_STORAGE_TYPE.equals(bsType)) {
            return new SftpBackupStorageKvmUploader(backupStorageUuid);
        } else {
            for (BackupStorageKvmFactory f : pluginRgty.getExtensionList(BackupStorageKvmFactory.class)) {
                if (bsType.equals(f.getBackupStorageType())) {
                    return f.createUploader(getSelfInventory(), backupStorageUuid);
                }
            }

            throw new CloudRuntimeException(String.format("cannot find any BackupStorageKvmFactory for the type[%s]", bsType));
        }
    }


    @Override
    void handleHypervisorSpecificMessage(SMPPrimaryStorageHypervisorSpecificMessage msg) {
        if (msg instanceof InitKvmHostMsg) {
            handle((InitKvmHostMsg) msg);
        } else {
            bus.dealWithUnknownMessage((Message)msg);
        }
    }

    @Override
    void connectByClusterUuid(String clusterUuid, Completion completion) {
        String hostUuid = findConnectedHostByClusterUuid(clusterUuid, false);
        if (hostUuid == null) {
            completion.success();
            return;
        }

        connect(hostUuid, completion);
    }

    private void handle(final InitKvmHostMsg msg) {
        final InitKvmHostReply reply = new InitKvmHostReply();
        connect(msg.getHostUuid(), new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }
}
