package org.zstack.storage.ceph.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.AsyncLatch;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.primary.CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg.SnapshotDownloadInfo;
import org.zstack.header.storage.primary.VolumeSnapshotCapability.VolumeSnapshotArrangementType;
import org.zstack.header.storage.snapshot.CreateTemplateFromVolumeSnapshotReply.CreateTemplateFromVolumeSnapshotResult;
import org.zstack.header.storage.snapshot.VolumeSnapshotConstant;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.vm.VmInstanceSpec.ImageSpec;
import org.zstack.header.volume.*;
import org.zstack.storage.backup.sftp.GetSftpBackupStorageDownloadCredentialMsg;
import org.zstack.storage.backup.sftp.GetSftpBackupStorageDownloadCredentialReply;
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant;
import org.zstack.storage.ceph.*;
import org.zstack.storage.ceph.backup.CephBackupStorageVO;
import org.zstack.storage.ceph.backup.CephBackupStorageVO_;
import org.zstack.storage.primary.PrimaryStorageBase;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 7/28/2015.
 */
public class CephPrimaryStorageBase extends PrimaryStorageBase {
    private static final CLogger logger = Utils.getLogger(CephPrimaryStorageBase.class);

    @Autowired
    private RESTFacade restf;
    @Autowired
    private ThreadFacade thdf;


    public static class AgentCommand {
        String fsId;
        String uuid;

        public String getFsId() {
            return fsId;
        }

        public void setFsId(String fsId) {
            this.fsId = fsId;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }
    }

    public static class AgentResponse {
        String error;
        boolean success = true;
        Long totalCapacity;
        Long availableCapacity;

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
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

    public static class Pool {
        String name;
        boolean predefined;
    }

    public static class InitCmd extends AgentCommand {
        List<Pool> pools;

        public List<Pool> getPools() {
            return pools;
        }

        public void setPools(List<Pool> pools) {
            this.pools = pools;
        }
    }

    public static class InitRsp extends AgentResponse {
        String fsid;
        String userKey;

        public String getUserKey() {
            return userKey;
        }

        public void setUserKey(String userKey) {
            this.userKey = userKey;
        }

        public String getFsid() {
            return fsid;
        }

        public void setFsid(String fsid) {
            this.fsid = fsid;
        }
    }

    public static class CreateEmptyVolumeCmd extends AgentCommand {
        String installPath;
        long size;

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }

    public static class CreateEmptyVolumeRsp extends AgentResponse {
    }

    public static class DeleteCmd extends AgentCommand {
        String installPath;

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }
    }

    public static class DeleteRsp extends AgentResponse {

    }

    public static class CloneCmd extends AgentCommand {
        String srcPath;
        String dstPath;

        public String getSrcPath() {
            return srcPath;
        }

        public void setSrcPath(String srcPath) {
            this.srcPath = srcPath;
        }

        public String getDstPath() {
            return dstPath;
        }

        public void setDstPath(String dstPath) {
            this.dstPath = dstPath;
        }
    }

    public static class CloneRsp extends AgentResponse {
    }

    public static class FlattenCmd extends AgentCommand {
        String path;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public static class FlattenRsp extends AgentResponse {

    }

    public static class SftpDownloadCmd extends AgentCommand {
        String sshKey;
        String hostname;
        String backupStorageInstallPath;
        String primaryStorageInstallPath;

        public String getSshKey() {
            return sshKey;
        }

        public void setSshKey(String sshKey) {
            this.sshKey = sshKey;
        }

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public String getBackupStorageInstallPath() {
            return backupStorageInstallPath;
        }

        public void setBackupStorageInstallPath(String backupStorageInstallPath) {
            this.backupStorageInstallPath = backupStorageInstallPath;
        }

        public String getPrimaryStorageInstallPath() {
            return primaryStorageInstallPath;
        }

        public void setPrimaryStorageInstallPath(String primaryStorageInstallPath) {
            this.primaryStorageInstallPath = primaryStorageInstallPath;
        }
    }

    public static class SftpDownloadRsp extends AgentResponse {
    }

    public static class SftpUpLoadCmd extends AgentCommand {
        String primaryStorageInstallPath;
        String backupStorageInstallPath;
        String hostname;
        String sshKey;

        public String getPrimaryStorageInstallPath() {
            return primaryStorageInstallPath;
        }

        public void setPrimaryStorageInstallPath(String primaryStorageInstallPath) {
            this.primaryStorageInstallPath = primaryStorageInstallPath;
        }

        public String getBackupStorageInstallPath() {
            return backupStorageInstallPath;
        }

        public void setBackupStorageInstallPath(String backupStorageInstallPath) {
            this.backupStorageInstallPath = backupStorageInstallPath;
        }

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public String getSshKey() {
            return sshKey;
        }

        public void setSshKey(String sshKey) {
            this.sshKey = sshKey;
        }
    }

    public static class SftpUploadRsp extends AgentResponse {
    }

    public static class CreateSnapshotCmd extends AgentCommand {
        String snapshotPath;

        public String getSnapshotPath() {
            return snapshotPath;
        }

        public void setSnapshotPath(String snapshotPath) {
            this.snapshotPath = snapshotPath;
        }
    }

    public static class CreateSnapshotRsp extends AgentResponse {
        long size;

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }

    public static class DeleteSnapshotCmd extends AgentCommand {
        String snapshotPath;

        public String getSnapshotPath() {
            return snapshotPath;
        }

        public void setSnapshotPath(String snapshotPath) {
            this.snapshotPath = snapshotPath;
        }
    }

    public static class DeleteSnapshotRsp extends AgentResponse {
    }

    public static class ProtectSnapshotCmd extends AgentCommand {
        String snapshotPath;

        public String getSnapshotPath() {
            return snapshotPath;
        }

        public void setSnapshotPath(String snapshotPath) {
            this.snapshotPath = snapshotPath;
        }
    }

    public static class ProtectSnapshotRsp extends AgentResponse {
    }

    public static class UnprotectedSnapshotCmd extends AgentCommand {
        String snapshotPath;

        public String getSnapshotPath() {
            return snapshotPath;
        }

        public void setSnapshotPath(String snapshotPath) {
            this.snapshotPath = snapshotPath;
        }
    }

    public static class UnprotectedSnapshotRsp extends AgentResponse {
    }

    public static class CpCmd extends AgentCommand {
        String srcPath;
        String dstPath;

        public String getSrcPath() {
            return srcPath;
        }

        public void setSrcPath(String srcPath) {
            this.srcPath = srcPath;
        }

        public String getDstPath() {
            return dstPath;
        }

        public void setDstPath(String dstPath) {
            this.dstPath = dstPath;
        }
    }

    public static class CpRsp extends AgentResponse {
        long size;

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }

    public static class RollbackSnapshotCmd extends AgentCommand {
        String snapshotPath;

        public String getSnapshotPath() {
            return snapshotPath;
        }

        public void setSnapshotPath(String snapshotPath) {
            this.snapshotPath = snapshotPath;
        }
    }

    public static class RollbackSnapshotRsp extends AgentResponse {
    }

    public static final String INIT_PATH = "/ceph/primarystorage/init";
    public static final String CREATE_VOLUME_PATH = "/ceph/primarystorage/volume/createempty";
    public static final String DELETE_PATH = "/ceph/primarystorage/delete";
    public static final String CLONE_PATH = "/ceph/primarystorage/volume/clone";
    public static final String FLATTEN_PATH = "/ceph/primarystorage/volume/flatten";
    public static final String SFTP_DOWNLOAD_PATH = "/ceph/primarystorage/sftpbackupstorage/download";
    public static final String SFTP_UPLOAD_PATH = "/ceph/primarystorage/sftpbackupstorage/upload";
    public static final String CREATE_SNAPSHOT_PATH = "/ceph/primarystorage/snapshot/create";
    public static final String DELETE_SNAPSHOT_PATH = "/ceph/primarystorage/snapshot/delete";
    public static final String PROTECT_SNAPSHOT_PATH = "/ceph/primarystorage/snapshot/protect";
    public static final String ROLLBACK_SNAPSHOT_PATH = "/ceph/primarystorage/snapshot/rollback";
    public static final String UNPROTECT_SNAPSHOT_PATH = "/ceph/primarystorage/snapshot/unprotect";
    public static final String CP_PATH = "/ceph/primarystorage/volume/cp";

    private final Map<String, BackupStorageMediator> backupStorageMediators = new HashMap<String, BackupStorageMediator>();

    {
        backupStorageMediators.put(SftpBackupStorageConstant.SFTP_BACKUP_STORAGE_TYPE, new SftpBackupStorageMediator());
        backupStorageMediators.put(CephConstants.CEPH_BACKUP_STORAGE_TYPE, new CephBackupStorageMediator());
    }


    abstract class MediatorParam {
    }

    class DownloadParam extends MediatorParam {
        ImageSpec image;
        String installPath;
    }

    class UploadParam extends MediatorParam {
        ImageInventory image;
        String primaryStorageInstallPath;
    }

    abstract class BackupStorageMediator {
        BackupStorageInventory backupStorage;
        MediatorParam param;

        protected void checkParam() {
            DebugUtils.Assert(backupStorage != null, "backupStorage cannot be null");
            DebugUtils.Assert(param != null, "param cannot be null");
        }

        abstract void download(ReturnValueCompletion<String> completion);

        abstract void upload(ReturnValueCompletion<String> completion);
    }

    class SftpBackupStorageMediator extends BackupStorageMediator {
        private void getSftpCredentials(final ReturnValueCompletion<GetSftpBackupStorageDownloadCredentialReply> completion) {
            GetSftpBackupStorageDownloadCredentialMsg gmsg = new GetSftpBackupStorageDownloadCredentialMsg();
            gmsg.setBackupStorageUuid(backupStorage.getUuid());
            bus.makeTargetServiceIdByResourceUuid(gmsg, BackupStorageConstant.SERVICE_ID, backupStorage.getUuid());
            bus.send(gmsg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        completion.fail(reply.getError());
                    } else {
                        completion.success((GetSftpBackupStorageDownloadCredentialReply) reply);
                    }
                }
            });
        }

        @Override
        void download(final ReturnValueCompletion<String> completion) {
            checkParam();
            final DownloadParam dparam = (DownloadParam) param;

            FlowChain chain = FlowChainBuilder.newShareFlowChain();
            chain.setName(String.format("download-image-from-sftp-%s-to-ceph-%s", backupStorage.getUuid(), self.getUuid()));
            chain.then(new ShareFlow() {
                String sshkey;
                String sftpHostname;

                @Override
                public void setup() {
                    flow(new NoRollbackFlow() {
                        String __name__ = "get-sftp-credentials";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            getSftpCredentials(new ReturnValueCompletion<GetSftpBackupStorageDownloadCredentialReply>(trigger) {
                                @Override
                                public void success(GetSftpBackupStorageDownloadCredentialReply greply) {
                                    sshkey = greply.getSshKey();
                                    sftpHostname = greply.getHostname();
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
                        String __name__ = "download-image";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            SftpDownloadCmd cmd = new SftpDownloadCmd();
                            cmd.backupStorageInstallPath = dparam.image.getSelectedBackupStorage().getInstallPath();
                            cmd.hostname = sftpHostname;
                            cmd.sshKey = sshkey;
                            cmd.primaryStorageInstallPath = dparam.installPath;

                            httpCall(SFTP_DOWNLOAD_PATH, cmd, SftpDownloadRsp.class, new ReturnValueCompletion<SftpDownloadRsp>(trigger) {
                                @Override
                                public void success(SftpDownloadRsp returnValue) {
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
                            completion.success(dparam.installPath);
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

        @Override
        void upload(final ReturnValueCompletion<String> completion) {
            checkParam();

            final UploadParam uparam = (UploadParam) param;

            FlowChain chain = FlowChainBuilder.newShareFlowChain();
            chain.setName(String.format("upload-image-ceph-%s-to-sftp-%s", self.getUuid(), backupStorage.getUuid()));
            chain.then(new ShareFlow() {
                String sshKey;
                String hostname;
                String backupStorageInstallPath;

                @Override
                public void setup() {
                    flow(new NoRollbackFlow() {
                        String __name__ = "get-sftp-credentials";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            getSftpCredentials(new ReturnValueCompletion<GetSftpBackupStorageDownloadCredentialReply>(trigger) {
                                @Override
                                public void success(GetSftpBackupStorageDownloadCredentialReply returnValue) {
                                    sshKey = returnValue.getSshKey();
                                    hostname = returnValue.getHostname();
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
                        String __name__ = "get-backup-storage-install-path";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            BackupStorageAskInstallPathMsg msg = new BackupStorageAskInstallPathMsg();
                            msg.setBackupStorageUuid(backupStorage.getUuid());
                            msg.setImageUuid(uparam.image.getUuid());
                            msg.setImageMediaType(uparam.image.getMediaType());
                            bus.makeTargetServiceIdByResourceUuid(msg, BackupStorageConstant.SERVICE_ID, backupStorage.getUuid());
                            bus.send(msg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        trigger.fail(reply.getError());
                                    } else {
                                        backupStorageInstallPath = ((BackupStorageAskInstallPathReply) reply).getInstallPath();
                                        trigger.next();
                                    }
                                }
                            });
                        }
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = "upload-to-backup-storage";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            SftpUpLoadCmd cmd = new SftpUpLoadCmd();
                            cmd.setBackupStorageInstallPath(backupStorageInstallPath);
                            cmd.setHostname(hostname);
                            cmd.setSshKey(sshKey);
                            cmd.setPrimaryStorageInstallPath(uparam.primaryStorageInstallPath);

                            httpCall(SFTP_UPLOAD_PATH, cmd, SftpUploadRsp.class, new ReturnValueCompletion<SftpUploadRsp>(trigger) {
                                @Override
                                public void success(SftpUploadRsp returnValue) {
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
                            completion.success(backupStorageInstallPath);
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
    }

    class CephBackupStorageMediator extends BackupStorageMediator {
        protected void checkParam() {
            super.checkParam();

            SimpleQuery<CephBackupStorageVO> q = dbf.createQuery(CephBackupStorageVO.class);
            q.select(CephBackupStorageVO_.fsid);
            q.add(CephBackupStorageVO_.uuid, Op.EQ, backupStorage.getUuid());
            String bsFsid = q.findValue();
            if (!getSelf().getFsid().equals(bsFsid)) {
                throw new OperationFailureException(errf.stringToOperationError(
                        String.format("the backup storage[uuid:%s, name:%s, fsid:%s] is not in the same ceph cluster" +
                                        " with the primary storage[uuid:%s, name:%s, fsid:%s]", backupStorage.getUuid(),
                                backupStorage.getName(), bsFsid, self.getUuid(), self.getName(), getSelf().getFsid())
                ));
            }
        }

        @Override
        void download(ReturnValueCompletion<String> completion) {
            checkParam();

            DownloadParam dparam = (DownloadParam) param;
            completion.success(dparam.image.getSelectedBackupStorage().getInstallPath());
        }

        @Override
        void upload(final ReturnValueCompletion<String> completion) {
            checkParam();

            final UploadParam uparam = (UploadParam) param;

            FlowChain chain = FlowChainBuilder.newShareFlowChain();
            chain.setName(String.format("upload-image-ceph-%s-to-ceph-%s", self.getUuid(), backupStorage.getUuid()));
            chain.then(new ShareFlow() {
                String backupStorageInstallPath;

                @Override
                public void setup() {
                    flow(new NoRollbackFlow() {
                        String __name__ = "get-backup-storage-install-path";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            BackupStorageAskInstallPathMsg msg = new BackupStorageAskInstallPathMsg();
                            msg.setBackupStorageUuid(backupStorage.getUuid());
                            msg.setImageUuid(uparam.image.getUuid());
                            msg.setImageMediaType(uparam.image.getMediaType());
                            bus.makeTargetServiceIdByResourceUuid(msg, BackupStorageConstant.SERVICE_ID, backupStorage.getUuid());
                            bus.send(msg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        trigger.fail(reply.getError());
                                    } else {
                                        backupStorageInstallPath = ((BackupStorageAskInstallPathReply) reply).getInstallPath();
                                        trigger.next();
                                    }
                                }
                            });
                        }
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = "cp-to-the-image";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            CpCmd cmd = new CpCmd();
                            cmd.srcPath = uparam.primaryStorageInstallPath;
                            cmd.dstPath = backupStorageInstallPath;
                            httpCall(CP_PATH, cmd, CpRsp.class, new ReturnValueCompletion<CpRsp>(trigger) {
                                @Override
                                public void success(CpRsp returnValue) {
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
                            completion.success(backupStorageInstallPath);
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
    }

    private BackupStorageMediator getBackupStorageMediator(String bsUuid) {
        BackupStorageVO bsvo = dbf.findByUuid(bsUuid, BackupStorageVO.class);
        BackupStorageMediator mediator = backupStorageMediators.get(bsvo.getType());
        if (mediator == null) {
            throw new CloudRuntimeException(String.format("cannot find BackupStorageMediator for type[%s]", bsvo.getType()));
        }

        mediator.backupStorage = BackupStorageInventory.valueOf(bsvo);
        return mediator;
    }

    private String makeRootVolumeInstallPath(String volUuid) {
        return String.format("ceph://%s/%s", getSelf().getRootVolumePoolName(), volUuid);
    }

    private String makeDataVolumeInstallPath(String volUuid) {
        return String.format("ceph://%s/%s", getSelf().getDataVolumePoolName(), volUuid);
    }

    private String makeCacheInstallPath(String uuid) {
        return String.format("ceph://%s/%s", getSelf().getImageCachePoolName(), uuid);
    }

    public CephPrimaryStorageBase(PrimaryStorageVO self) {
        super(self);
    }

    protected CephPrimaryStorageVO getSelf() {
        return (CephPrimaryStorageVO) self;
    }

    protected CephPrimaryStorageInventory getSelfInventory() {
        return CephPrimaryStorageInventory.valueOf(getSelf());
    }

    private void createEmptyVolume(final InstantiateVolumeMsg msg) {
        final CreateEmptyVolumeCmd cmd = new CreateEmptyVolumeCmd();
        cmd.installPath = VolumeType.Root.toString().equals(msg.getVolume().getType()) ?
                makeRootVolumeInstallPath(msg.getVolume().getUuid()) : makeDataVolumeInstallPath(msg.getVolume().getUuid());
        cmd.size = msg.getVolume().getSize();

        final InstantiateVolumeReply reply = new InstantiateVolumeReply();

        httpCall(CREATE_VOLUME_PATH, cmd, CreateEmptyVolumeRsp.class, new ReturnValueCompletion<CreateEmptyVolumeRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(CreateEmptyVolumeRsp ret) {
                VolumeInventory vol = msg.getVolume();
                vol.setInstallPath(cmd.getInstallPath());
                reply.setVolume(vol);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final InstantiateVolumeMsg msg) {
        if (msg instanceof InstantiateRootVolumeFromTemplateMsg) {
            createVolumeFromTemplate((InstantiateRootVolumeFromTemplateMsg) msg);
        } else {
            createEmptyVolume(msg);
        }
    }

    class DownloadToCache {
        ImageSpec image;

        private void doDownload(final ReturnValueCompletion<ImageCacheVO> completion) {
            SimpleQuery<ImageCacheVO> q = dbf.createQuery(ImageCacheVO.class);
            q.add(ImageCacheVO_.imageUuid, Op.EQ, image.getInventory().getUuid());
            q.add(ImageCacheVO_.primaryStorageUuid, Op.EQ, self.getUuid());
            ImageCacheVO cache = q.find();
            if (cache != null) {
                completion.success(cache);
                return;
            }

            final FlowChain chain = FlowChainBuilder.newShareFlowChain();
            chain.setName(String.format("prepare-image-cache-ceph-%s", self.getUuid()));
            chain.then(new ShareFlow() {
                String cachePath;
                String snapshotPath;

                @Override
                public void setup() {
                    flow(new Flow() {
                        String __name__ = "download-from-backup-storage";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            DownloadParam param = new DownloadParam();
                            param.image = image;
                            param.installPath = makeCacheInstallPath(image.getInventory().getUuid());
                            BackupStorageMediator mediator = getBackupStorageMediator(image.getSelectedBackupStorage().getBackupStorageUuid());
                            mediator.param = param;

                            mediator.download(new ReturnValueCompletion<String>(trigger) {
                                @Override
                                public void success(String path) {
                                    cachePath = path;
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }

                        @Override
                        public void rollback(FlowTrigger trigger, Map data) {
                            if (cachePath != null) {
                                DeleteCmd cmd = new DeleteCmd();
                                cmd.installPath = cachePath;
                                httpCall(DELETE_PATH, cmd, DeleteRsp.class, new ReturnValueCompletion<DeleteRsp>() {
                                    @Override
                                    public void success(DeleteRsp returnValue) {
                                        logger.debug(String.format("successfully deleted %s", cachePath));
                                    }

                                    @Override
                                    public void fail(ErrorCode errorCode) {
                                        //TODO
                                        logger.warn(String.format("unable to delete %s, %s. Need a cleanup", cachePath, errorCode));
                                    }
                                });
                            }

                            trigger.rollback();
                        }
                    });

                    flow(new Flow() {
                        String __name__ = "create-snapshot";

                        boolean needCleanup = false;

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            snapshotPath =  String.format("%s@%s", cachePath, image.getInventory().getUuid());
                            CreateSnapshotCmd cmd = new CreateSnapshotCmd();
                            cmd.snapshotPath = snapshotPath;
                            httpCall(CREATE_SNAPSHOT_PATH, cmd, CreateSnapshotRsp.class, new ReturnValueCompletion<CreateSnapshotRsp>(trigger) {
                                @Override
                                public void success(CreateSnapshotRsp returnValue) {
                                    needCleanup = true;
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }

                        @Override
                        public void rollback(FlowTrigger trigger, Map data) {
                            if (needCleanup) {
                                DeleteSnapshotCmd cmd = new DeleteSnapshotCmd();
                                cmd.snapshotPath = snapshotPath;
                                httpCall(DELETE_SNAPSHOT_PATH, cmd, DeleteSnapshotRsp.class, new ReturnValueCompletion<DeleteSnapshotRsp>() {
                                    @Override
                                    public void success(DeleteSnapshotRsp returnValue) {
                                        logger.debug(String.format("successfully deleted the snapshot %s", snapshotPath));
                                    }

                                    @Override
                                    public void fail(ErrorCode errorCode) {
                                        //TODO
                                        logger.warn(String.format("unable to delete the snapshot %s, %s. Need a cleanup", snapshotPath, errorCode));
                                    }
                                });
                            }

                            trigger.rollback();
                        }
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = "protect-snapshot";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            ProtectSnapshotCmd cmd = new ProtectSnapshotCmd();
                            cmd.snapshotPath = snapshotPath;
                            httpCall(PROTECT_SNAPSHOT_PATH, cmd, ProtectSnapshotRsp.class, new ReturnValueCompletion<ProtectSnapshotRsp>(trigger) {
                                @Override
                                public void success(ProtectSnapshotRsp returnValue) {
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
                            ImageCacheVO cvo = new ImageCacheVO();
                            cvo.setMd5sum("not calculated");
                            cvo.setSize(image.getInventory().getSize());
                            cvo.setInstallUrl(snapshotPath);
                            cvo.setImageUuid(image.getInventory().getUuid());
                            cvo.setPrimaryStorageUuid(self.getUuid());
                            cvo.setMediaType(ImageMediaType.valueOf(image.getInventory().getMediaType()));
                            cvo.setState(ImageCacheState.ready);
                            cvo = dbf.persistAndRefresh(cvo);

                            completion.success(cvo);
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

        void download(final ReturnValueCompletion<ImageCacheVO> completion) {
            thdf.chainSubmit(new ChainTask(completion) {
                @Override
                public String getSyncSignature() {
                    return String.format("ceph-p-%s-download-image-%s", self.getUuid(), image.getInventory().getUuid());
                }

                @Override
                public void run(final SyncTaskChain chain) {
                    doDownload(new ReturnValueCompletion<ImageCacheVO>(chain) {
                        @Override
                        public void success(ImageCacheVO returnValue) {
                            completion.success(returnValue);
                            chain.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            completion.fail(errorCode);
                            chain.next();
                        }
                    });
                }

                @Override
                public String getName() {
                    return getSyncSignature();
                }
            });
        }
    }

    private void createVolumeFromTemplate(final InstantiateRootVolumeFromTemplateMsg msg) {
        final ImageInventory img = msg.getTemplateSpec().getInventory();

        final InstantiateVolumeReply reply = new InstantiateVolumeReply();
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-root-volume-%s", msg.getVolume().getUuid()));
        chain.then(new ShareFlow() {
            String cloneInstallPath;
            String volumePath = makeRootVolumeInstallPath(msg.getVolume().getUuid());
            ImageCacheVO cache;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ ="download-image-to-cache";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        DownloadToCache downloadToCache = new DownloadToCache();
                        downloadToCache.image = msg.getTemplateSpec();
                        downloadToCache.download(new ReturnValueCompletion<ImageCacheVO>(trigger) {
                            @Override
                            public void success(ImageCacheVO returnValue) {
                                cloneInstallPath = returnValue.getInstallUrl();
                                cache = returnValue;
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
                    String __name__ = "clone-image";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        CloneCmd cmd = new CloneCmd();
                        cmd.srcPath = cloneInstallPath;
                        cmd.dstPath = volumePath;

                        httpCall(CLONE_PATH, cmd, CloneRsp.class, new ReturnValueCompletion<CloneRsp>(trigger) {
                            @Override
                            public void fail(ErrorCode err) {
                                trigger.fail(err);
                            }

                            @Override
                            public void success(CloneRsp ret) {
                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        VolumeInventory vol = msg.getVolume();
                        vol.setInstallPath(volumePath);
                        reply.setVolume(vol);

                        ImageCacheVolumeRefVO ref = new ImageCacheVolumeRefVO();
                        ref.setImageCacheId(cache.getId());
                        ref.setPrimaryStorageUuid(self.getUuid());
                        ref.setVolumeUuid(vol.getUuid());
                        dbf.persist(ref);

                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void handle(final DeleteVolumeOnPrimaryStorageMsg msg) {
        DeleteCmd cmd = new DeleteCmd();
        cmd.installPath = msg.getVolume().getInstallPath();

        final DeleteVolumeOnPrimaryStorageReply reply = new DeleteVolumeOnPrimaryStorageReply();

        httpCall(DELETE_PATH, cmd, DeleteRsp.class, new ReturnValueCompletion<DeleteRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(DeleteRsp ret) {
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final CreateTemplateFromVolumeOnPrimaryStorageMsg msg) {
        final CreateTemplateFromVolumeOnPrimaryStorageReply reply = new CreateTemplateFromVolumeOnPrimaryStorageReply();
        BackupStorageMediator mediator = getBackupStorageMediator(msg.getBackupStorageUuid());

        UploadParam param = new UploadParam();
        param.image = msg.getImageInventory();
        param.primaryStorageInstallPath = msg.getVolumeInventory().getInstallPath();
        mediator.param = param;
        mediator.upload(new ReturnValueCompletion<String>(msg) {
            @Override
            public void success(String returnValue) {
                reply.setTemplateBackupStorageInstallPath(returnValue);
                reply.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final DownloadDataVolumeToPrimaryStorageMsg msg) {
        final DownloadDataVolumeToPrimaryStorageReply reply = new DownloadDataVolumeToPrimaryStorageReply();

        BackupStorageMediator mediator = getBackupStorageMediator(msg.getBackupStorageRef().getBackupStorageUuid());
        ImageSpec spec = new ImageSpec();
        spec.setInventory(msg.getImage());
        spec.setSelectedBackupStorage(msg.getBackupStorageRef());
        DownloadParam param = new DownloadParam();
        param.image = spec;
        param.installPath = makeDataVolumeInstallPath(msg.getVolumeUuid());
        mediator.param = param;
        mediator.download(new ReturnValueCompletion<String>(msg) {
            @Override
            public void success(String returnValue) {
                reply.setInstallPath(returnValue);
                reply.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final DeleteBitsOnPrimaryStorageMsg msg) {
        DeleteCmd cmd = new DeleteCmd();
        cmd.installPath = msg.getInstallPath();

        final DeleteBitsOnPrimaryStorageReply reply = new DeleteBitsOnPrimaryStorageReply();

        httpCall(DELETE_PATH, cmd, DeleteRsp.class, new ReturnValueCompletion<DeleteRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(DeleteRsp ret) {
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final DownloadIsoToPrimaryStorageMsg msg) {
        final DownloadIsoToPrimaryStorageReply reply = new DownloadIsoToPrimaryStorageReply();
        DownloadToCache downloadToCache = new DownloadToCache();
        downloadToCache.image = msg.getIsoSpec();
        downloadToCache.download(new ReturnValueCompletion<ImageCacheVO>(msg) {
            @Override
            public void success(ImageCacheVO returnValue) {
                reply.setInstallPath(returnValue.getInstallUrl());
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(DeleteIsoFromPrimaryStorageMsg msg) {
        DeleteIsoFromPrimaryStorageReply reply = new DeleteIsoFromPrimaryStorageReply();
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(AskVolumeSnapshotCapabilityMsg msg) {
        AskVolumeSnapshotCapabilityReply reply = new AskVolumeSnapshotCapabilityReply();
        VolumeSnapshotCapability cap = new VolumeSnapshotCapability();
        cap.setSupport(true);
        cap.setArrangementType(VolumeSnapshotArrangementType.INDIVIDUAL);
        reply.setCapability(cap);
        bus.reply(msg, reply);
    }

    private <T extends AgentResponse> void httpCall(final String path, final AgentCommand cmd, Class<T> retClass, final ReturnValueCompletion<T> callback) {
        httpCall(path, cmd, retClass, callback, 5, TimeUnit.MINUTES);
    }

    protected String makeHttpPath(String ip, String path) {
        return String.format("http://%s:%s%s", ip, CephGlobalProperty.PRIMARY_STORAGE_AGENT_PORT, path);
    }

    private void updateCapacityIfNeeded(AgentResponse rsp) {
        if (rsp.totalCapacity != null && rsp.availableCapacity != null) {
            new CephCapacityUpdater().update(getSelf().getFsid(), rsp.totalCapacity, rsp.availableCapacity);
        }
    }

    private <T extends AgentResponse> void httpCall(final String path, final AgentCommand cmd, final Class<T> retClass, final ReturnValueCompletion<T> callback, final long timeout, final TimeUnit timeUnit) {
        cmd.setUuid(self.getUuid());
        cmd.setFsId(getSelf().getFsid());

        final List<CephPrimaryStorageMonBase> mons = new ArrayList<CephPrimaryStorageMonBase>();
        for (CephPrimaryStorageMonVO monvo : getSelf().getMons()) {
            if (monvo.getStatus() == MonStatus.Connected) {
                mons.add(new CephPrimaryStorageMonBase(monvo));
            }
        }

        if (mons.isEmpty()) {
            throw new OperationFailureException(errf.stringToOperationError(
                    String.format("all ceph mons of primary storage[uuid:%s] are not in Connected state", self.getUuid())
            ));
        }

        Collections.shuffle(mons);

        class HttpCaller {
            Iterator<CephPrimaryStorageMonBase> it = mons.iterator();
            List<ErrorCode> errorCodes = new ArrayList<ErrorCode>();

            void call() {
                if (!it.hasNext()) {
                    callback.fail(errf.stringToOperationError(
                            String.format("all mons failed to execute http call[%s], errors are %s", path, JSONObjectUtil.toJsonString(errorCodes))
                    ));

                    return;
                }

                CephPrimaryStorageMonBase base = it.next();

                restf.asyncJsonPost(makeHttpPath(base.getSelf().getHostname(), path), cmd, new JsonAsyncRESTCallback<T>(callback) {
                    @Override
                    public void fail(ErrorCode err) {
                        errorCodes.add(err);
                        call();
                    }

                    @Override
                    public void success(T ret) {
                        if (!ret.success) {
                            callback.fail(errf.stringToOperationError(ret.error));
                        } else {
                            if (!(cmd instanceof InitCmd)) {
                                updateCapacityIfNeeded(ret);
                            }
                            callback.success(ret);
                        }
                    }

                    @Override
                    public Class<T> getReturnClass() {
                        return retClass;
                    }
                }, timeUnit, timeout);
            }
        }

        new HttpCaller().call();
    }

    private void connect(final Completion completion) {
        final List<CephPrimaryStorageMonBase> mons = CollectionUtils.transformToList(getSelf().getMons(), new Function<CephPrimaryStorageMonBase, CephPrimaryStorageMonVO>() {
            @Override
            public CephPrimaryStorageMonBase call(CephPrimaryStorageMonVO arg) {
                return new CephPrimaryStorageMonBase(arg);
            }
        });

        class Connector {
            List<ErrorCode> errorCodes = new ArrayList<ErrorCode>();
            Iterator<CephPrimaryStorageMonBase> it = mons.iterator();

            void connect(final FlowTrigger trigger) {
                if (!it.hasNext()) {
                    if (errorCodes.size() == mons.size()) {
                        trigger.fail(errf.stringToOperationError(
                                String.format("unable to connect to the ceph primary storage[uuid:%s]. Failed to connect all ceph mons. Errors are %s",
                                        self.getUuid(), JSONObjectUtil.toJsonString(errorCodes))
                        ));
                    } else {
                        trigger.next();
                    }
                    return;
                }

                CephPrimaryStorageMonBase base = it.next();
                base.connect(new Completion(trigger) {
                    @Override
                    public void success() {
                        connect(trigger);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        errorCodes.add(errorCode);
                        connect(trigger);
                    }
                });
            }
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("connect-ceph-primary-storage-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "connect-monitor";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new Connector().connect(trigger);
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "init";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        InitCmd cmd = new InitCmd();
                        List<Pool> pools = new ArrayList<Pool>();

                        Pool p = new Pool();
                        p.name = getSelf().getImageCachePoolName();
                        p.predefined = CephSystemTags.PREDEFINED_PRIMARY_STORAGE_IMAGE_CACHE_POOL.hasTag(self.getUuid());
                        pools.add(p);

                        p = new Pool();
                        p.name = getSelf().getRootVolumePoolName();
                        p.predefined = CephSystemTags.PREDEFINED_PRIMARY_STORAGE_ROOT_VOLUME_POOL.hasTag(self.getUuid());
                        pools.add(p);

                        p = new Pool();
                        p.name = getSelf().getDataVolumePoolName();
                        p.predefined = CephSystemTags.PREDEFINED_PRIMARY_STORAGE_DATA_VOLUME_POOL.hasTag(self.getUuid());
                        pools.add(p);

                        cmd.pools = pools;

                        httpCall(INIT_PATH, cmd, InitRsp.class, new ReturnValueCompletion<InitRsp>(trigger) {
                            @Override
                            public void fail(ErrorCode err) {
                                trigger.fail(err);
                            }

                            @Override
                            public void success(InitRsp ret) {
                                if (getSelf().getFsid() == null) {
                                    getSelf().setFsid(ret.fsid);
                                }

                                getSelf().setUserKey(ret.userKey);
                                self = dbf.updateAndRefresh(self);

                                CephCapacityUpdater updater = new CephCapacityUpdater();
                                updater.update(ret.fsid, ret.totalCapacity, ret.availableCapacity, true);
                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
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

    @Override
    protected void connectHook(ConnectPrimaryStorageMsg msg, final Completion completion) {
        connect(completion);
    }

    @Override
    protected void syncPhysicalCapacity(ReturnValueCompletion<PhysicalCapacityUsage> completion) {
        PrimaryStorageCapacityVO cap = dbf.findByUuid(self.getUuid(), PrimaryStorageCapacityVO.class);
        PhysicalCapacityUsage usage = new PhysicalCapacityUsage();
        usage.availablePhysicalSize = cap.getAvailablePhysicalCapacity();
        usage.totalPhysicalSize =  cap.getTotalPhysicalCapacity();
        completion.success(usage);
    }

    @Override
    protected void handle(APIReconnectPrimaryStorageMsg msg) {
        final APIReconnectPrimaryStorageEvent evt = new APIReconnectPrimaryStorageEvent(msg.getId());
        self.setStatus(PrimaryStorageStatus.Connecting);
        dbf.update(self);
        connect(new Completion(msg) {
            @Override
            public void success() {
                self = dbf.reload(self);
                self.setStatus(PrimaryStorageStatus.Connected);
                self = dbf.updateAndRefresh(self);
                evt.setInventory(getSelfInventory());
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                self = dbf.reload(self);
                self.setStatus(PrimaryStorageStatus.Disconnected);
                self = dbf.updateAndRefresh(self);
                evt.setErrorCode(errorCode);
                bus.publish(evt);
            }
        });
    }

    @Override
    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIAddMonToCephPrimaryStorageMsg) {
            handle((APIAddMonToCephPrimaryStorageMsg) msg);
        } else if (msg instanceof APIRemoveMonFromCephPrimaryStorageMsg) {
            handle((APIRemoveMonFromCephPrimaryStorageMsg) msg);
        } else {
            super.handleApiMessage(msg);
        }
    }

    private void handle(APIRemoveMonFromCephPrimaryStorageMsg msg) {
        APIRemoveMonFromCephPrimaryStorageEvent evt = new APIRemoveMonFromCephPrimaryStorageEvent(msg.getId());

        SimpleQuery<CephPrimaryStorageMonVO> q = dbf.createQuery(CephPrimaryStorageMonVO.class);
        q.add(CephPrimaryStorageMonVO_.hostname, Op.IN, msg.getMonHostnames());
        List<CephPrimaryStorageMonVO> vos = q.list();

        dbf.removeCollection(vos, CephPrimaryStorageMonVO.class);
        evt.setInventory(CephPrimaryStorageInventory.valueOf(dbf.reload(getSelf())));
        bus.publish(evt);
    }

    private void handle(final APIAddMonToCephPrimaryStorageMsg msg) {
        final APIAddMonToCephPrimaryStorageEvent evt = new APIAddMonToCephPrimaryStorageEvent(msg.getId());

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("add-mon-ceph-primary-storage-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            List<CephPrimaryStorageMonVO> monVOs = new ArrayList<CephPrimaryStorageMonVO>();

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "create-mon-in-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        for (String url : msg.getMonUrls()) {
                            CephPrimaryStorageMonVO monvo = new CephPrimaryStorageMonVO();
                            MonUri uri = new MonUri(url);
                            monvo.setUuid(Platform.getUuid());
                            monvo.setStatus(MonStatus.Connecting);
                            monvo.setHostname(uri.getHostname());
                            monvo.setMonPort(uri.getMonPort());
                            monvo.setSshPort(uri.getSshPort());
                            monvo.setSshUsername(uri.getSshUsername());
                            monvo.setSshPassword(uri.getSshPassword());
                            monvo.setPrimaryStorageUuid(self.getUuid());
                            monVOs.add(monvo);
                        }

                        dbf.persistCollection(monVOs);
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowTrigger trigger, Map data) {
                        dbf.removeCollection(monVOs, CephPrimaryStorageMonVO.class);
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "connect-mons";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        List<CephPrimaryStorageMonBase> bases = CollectionUtils.transformToList(monVOs, new Function<CephPrimaryStorageMonBase, CephPrimaryStorageMonVO>() {
                            @Override
                            public CephPrimaryStorageMonBase call(CephPrimaryStorageMonVO arg) {
                                return new CephPrimaryStorageMonBase(arg);
                            }
                        });

                        final AsyncLatch latch = new AsyncLatch(bases.size(), new NoErrorCompletion(trigger) {
                            @Override
                            public void done() {
                                trigger.next();
                            }
                        });

                        for (CephPrimaryStorageMonBase base : bases) {
                            base.connect(new Completion(trigger) {
                                @Override
                                public void success() {
                                    latch.ack();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    // one fails, all fail
                                    trigger.fail(errorCode);
                                }
                            });
                        }
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        evt.setInventory(CephPrimaryStorageInventory.valueOf(dbf.reload(getSelf())));
                        bus.publish(evt);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setErrorCode(errCode);
                        bus.publish(evt);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void handleLocalMessage(Message msg) {
        if (msg instanceof TakeSnapshotMsg) {
            handle((TakeSnapshotMsg) msg);
        } else if (msg instanceof MergeVolumeSnapshotOnPrimaryStorageMsg) {
            handle((MergeVolumeSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof DeleteSnapshotOnPrimaryStorageMsg) {
            handle((DeleteSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof RevertVolumeFromSnapshotOnPrimaryStorageMsg) {
            handle((RevertVolumeFromSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg) {
            handle((CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg) {
            handle((CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg) {
            handle((BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg) msg);
        } else {
            super.handleLocalMessage(msg);
        }
    }

    private void handle(BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg msg) {
        BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply reply = new BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply();
        reply.setError(errf.stringToOperationError("backing up snapshots to backup storage is a depreciated feature, which will be removed in future version"));
        bus.reply(msg, reply);
    }

    private void handle(final CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg msg) {
        if (msg.isNeedDownload()) {
            throw new OperationFailureException(errf.stringToOperationError("downloading snapshots to create template is not supported"));
        }

        final CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply reply = new CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply();

        final String volPath = makeDataVolumeInstallPath(msg.getVolumeUuid());
        SnapshotDownloadInfo sp = msg.getSnapshots().get(0);
        CpCmd cmd = new CpCmd();
        cmd.srcPath = sp.getSnapshot().getPrimaryStorageInstallPath();
        cmd.dstPath = volPath;
        httpCall(CP_PATH, cmd, CpRsp.class, new ReturnValueCompletion<CpRsp>(msg) {
            @Override
            public void success(CpRsp rsp) {
                reply.setInstallPath(volPath);
                reply.setSize(rsp.size);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg msg) {
        if (msg.isNeedDownload()) {
            throw new OperationFailureException(errf.stringToOperationError("downloading snapshots to create template is not supported"));
        }

        final CreateTemplateFromVolumeSnapshotOnPrimaryStorageReply reply = new CreateTemplateFromVolumeSnapshotOnPrimaryStorageReply();

        final SnapshotDownloadInfo sp = msg.getSnapshotsDownloadInfo().get(0);
        final ImageInventory image = ImageInventory.valueOf(dbf.findByUuid(msg.getImageUuid(), ImageVO.class));

        List<BackupStorageMediator> mediators = CollectionUtils.transformToList(msg.getBackupStorage(), new Function<BackupStorageMediator, BackupStorageInventory>() {
            @Override
            public BackupStorageMediator call(BackupStorageInventory bs) {
                BackupStorageMediator mediator = getBackupStorageMediator(bs.getUuid());
                UploadParam param = new UploadParam();
                param.primaryStorageInstallPath = sp.getSnapshot().getPrimaryStorageInstallPath();
                param.image = image;
                mediator.param = param;
                return mediator;
            }
        });

        final List<CreateTemplateFromVolumeSnapshotResult> results = new ArrayList<CreateTemplateFromVolumeSnapshotResult>();
        final List<ErrorCode> errorCodes = new ArrayList<ErrorCode>();

        final AsyncLatch latch = new AsyncLatch(mediators.size(), new NoErrorCompletion() {
            @Override
            public void done() {
                if (results.isEmpty()) {
                    reply.setError(errf.stringToOperationError(String.format("uploading failed on all backup storage. An error list is %s", JSONObjectUtil.toJsonString(errorCodes))));
                } else {
                    reply.setResults(results);
                }

                bus.reply(msg, reply);
            }
        });

        for (final BackupStorageMediator m : mediators) {
            m.upload(new ReturnValueCompletion<String>() {
                @Override
                public void success(String returnValue) {
                    CreateTemplateFromVolumeSnapshotResult ret = new CreateTemplateFromVolumeSnapshotResult();
                    ret.setInstallPath(returnValue);
                    ret.setBackupStorageUuid(m.backupStorage.getUuid());
                    synchronized (results) {
                        results.add(ret);
                    }
                    latch.ack();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    synchronized (errorCodes) {
                        errorCodes.add(errorCode);
                    }
                    latch.ack();
                }
            });
        }
    }

    private void handle(final RevertVolumeFromSnapshotOnPrimaryStorageMsg msg) {
        final RevertVolumeFromSnapshotOnPrimaryStorageReply reply  = new RevertVolumeFromSnapshotOnPrimaryStorageReply();
        RollbackSnapshotCmd cmd = new RollbackSnapshotCmd();
        cmd.snapshotPath = msg.getSnapshot().getPrimaryStorageInstallPath();
        httpCall(ROLLBACK_SNAPSHOT_PATH, cmd, RollbackSnapshotRsp.class, new ReturnValueCompletion<RollbackSnapshotRsp>(msg) {
            @Override
            public void success(RollbackSnapshotRsp returnValue) {
                reply.setNewVolumeInstallPath(msg.getVolume().getInstallPath());
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final DeleteSnapshotOnPrimaryStorageMsg msg) {
        final DeleteSnapshotOnPrimaryStorageReply reply = new DeleteSnapshotOnPrimaryStorageReply();
        DeleteSnapshotCmd cmd = new DeleteSnapshotCmd();
        cmd.snapshotPath = msg.getSnapshot().getPrimaryStorageInstallPath();
        httpCall(DELETE_SNAPSHOT_PATH, cmd, DeleteSnapshotRsp.class, new ReturnValueCompletion<DeleteSnapshotRsp>(msg) {
            @Override
            public void success(DeleteSnapshotRsp returnValue) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(MergeVolumeSnapshotOnPrimaryStorageMsg msg) {
        MergeVolumeSnapshotOnPrimaryStorageReply reply = new MergeVolumeSnapshotOnPrimaryStorageReply();
        bus.reply(msg, reply);
    }

    private void handle(final TakeSnapshotMsg msg) {
        final TakeSnapshotReply reply = new TakeSnapshotReply();

        final VolumeSnapshotInventory sp = msg.getStruct().getCurrent();
        SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
        q.select(VolumeVO_.installPath);
        q.add(VolumeVO_.uuid, Op.EQ, sp.getVolumeUuid());
        String volumePath = q.findValue();

        final String spPath = String.format("%s@%s", volumePath, sp.getUuid());
        CreateSnapshotCmd cmd = new CreateSnapshotCmd();
        cmd.snapshotPath = spPath;
        httpCall(CREATE_SNAPSHOT_PATH, cmd, CreateSnapshotRsp.class, new ReturnValueCompletion<CreateSnapshotRsp>(msg) {
            @Override
            public void success(CreateSnapshotRsp rsp) {
                sp.setSize(rsp.getSize());
                sp.setPrimaryStorageUuid(self.getUuid());
                sp.setPrimaryStorageInstallPath(spPath);
                sp.setType(VolumeSnapshotConstant.STORAGE_SNAPSHOT_TYPE.toString());
                sp.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
                reply.setInventory(sp);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    public void deleteHook() {
        dbf.removeCollection(getSelf().getMons(), CephPrimaryStorageMonVO.class);
    }
}
