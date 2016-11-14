package org.zstack.storage.ceph.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.core.*;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.image.APICreateDataVolumeTemplateFromVolumeMsg;
import org.zstack.header.image.APICreateRootVolumeTemplateFromRootVolumeMsg;
import org.zstack.header.image.APICreateRootVolumeTemplateFromVolumeSnapshotMsg;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.primary.VolumeSnapshotCapability.VolumeSnapshotArrangementType;
import org.zstack.header.storage.snapshot.VolumeSnapshotConstant;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.vm.APICreateVmInstanceMsg;
import org.zstack.header.vm.VmAccountPerference;
import org.zstack.header.vm.VmInstanceSpec.ImageSpec;
import org.zstack.header.volume.*;
import org.zstack.kvm.*;
import org.zstack.kvm.KvmSetupSelfFencerExtensionPoint.KvmCancelSelfFencerParam;
import org.zstack.kvm.KvmSetupSelfFencerExtensionPoint.KvmSetupSelfFencerParam;
import org.zstack.storage.backup.sftp.GetSftpBackupStorageDownloadCredentialMsg;
import org.zstack.storage.backup.sftp.GetSftpBackupStorageDownloadCredentialReply;
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant;
import org.zstack.storage.ceph.*;
import org.zstack.storage.ceph.CephMonBase.PingResult;
import org.zstack.storage.ceph.backup.CephBackupStorageVO;
import org.zstack.storage.ceph.backup.CephBackupStorageVO_;
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonBase.PingOperationFailure;
import org.zstack.storage.primary.PrimaryStorageBase;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
    @Autowired
    private ApiTimeoutManager timeoutMgr;
    @Autowired
    private CephImageCacheCleaner imageCacheCleaner;

    class ReconnectMonLock {
        AtomicBoolean hold = new AtomicBoolean(false);

        boolean lock() {
            return hold.compareAndSet(false, true);
        }

        void unlock() {
            hold.set(false);
        }
    }

    ReconnectMonLock reconnectMonLock = new ReconnectMonLock();

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

    public static class GetVolumeSizeCmd extends AgentCommand {
        public String volumeUuid;
        public String installPath;
    }

    public static class GetVolumeSizeRsp extends AgentResponse {
        public Long size;
        public Long actualSize;
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

    @ApiTimeout(apiClasses = {APICreateVmInstanceMsg.class})
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
        String username;
        int sshPort;
        String backupStorageInstallPath;
        String primaryStorageInstallPath;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public int getSshPort() {
            return sshPort;
        }

        public void setSshPort(int sshPort) {
            this.sshPort = sshPort;
        }

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

    @ApiTimeout(apiClasses = {
            APICreateRootVolumeTemplateFromRootVolumeMsg.class,
            APICreateDataVolumeTemplateFromVolumeMsg.class
    })
    public static class SftpUpLoadCmd extends AgentCommand {
        String primaryStorageInstallPath;
        String backupStorageInstallPath;
        String hostname;
        String username;
        String sshKey;
        int sshPort;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public int getSshPort() {
            return sshPort;
        }

        public void setSshPort(int sshPort) {
            this.sshPort = sshPort;
        }

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

    @ApiTimeout(apiClasses = {
            APICreateVolumeSnapshotMsg.class,
            APICreateVmInstanceMsg.class
    })
    public static class CreateSnapshotCmd extends AgentCommand {
        boolean skipOnExisting;
        String snapshotPath;
        String volumeUuid;

        public String getVolumeUuid() {
            return volumeUuid;
        }

        public void setVolumeUuid(String volumeUuid) {
            this.volumeUuid = volumeUuid;
        }

        public boolean isSkipOnExisting() {
            return skipOnExisting;
        }

        public void setSkipOnExisting(boolean skipOnExisting) {
            this.skipOnExisting = skipOnExisting;
        }

        public String getSnapshotPath() {
            return snapshotPath;
        }

        public void setSnapshotPath(String snapshotPath) {
            this.snapshotPath = snapshotPath;
        }
    }

    public static class CreateSnapshotRsp extends AgentResponse {
        Long size;
        Long actualSize;

        public Long getActualSize() {
            return actualSize;
        }

        public void setActualSize(Long actualSize) {
            this.actualSize = actualSize;
        }

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

    @ApiTimeout(apiClasses = {APICreateVmInstanceMsg.class})
    public static class ProtectSnapshotCmd extends AgentCommand {
        String snapshotPath;
        boolean ignoreError;

        public boolean isIgnoreError() {
            return ignoreError;
        }

        public void setIgnoreError(boolean ignoreError) {
            this.ignoreError = ignoreError;
        }

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

    @ApiTimeout(apiClasses = {
            APICreateRootVolumeTemplateFromRootVolumeMsg.class,
            APICreateDataVolumeTemplateFromVolumeMsg.class,
            APICreateDataVolumeFromVolumeSnapshotMsg.class,
            APICreateRootVolumeTemplateFromVolumeSnapshotMsg.class
    })
    public static class CpCmd extends AgentCommand {
        String resourceUuid;
        String srcPath;
        String dstPath;
    }

    public static class CpRsp extends AgentResponse {
        Long size;
        Long actualSize;
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

    public static class CreateKvmSecretCmd extends KVMAgentCommands.AgentCommand {
        String userKey;
        String uuid;

        public String getUserKey() {
            return userKey;
        }

        public void setUserKey(String userKey) {
            this.userKey = userKey;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }
    }

    public static class CreateKvmSecretRsp extends AgentResponse {

    }

    public static class DeletePoolCmd extends AgentCommand {
        List<String> poolNames;

        public List<String> getPoolNames() {
            return poolNames;
        }

        public void setPoolNames(List<String> poolNames) {
            this.poolNames = poolNames;
        }
    }

    public static class DeletePoolRsp extends AgentResponse {
    }

    public static class KvmSetupSelfFencerCmd extends AgentCommand {
        public String heartbeatImagePath;
        public String hostUuid;
        public long interval;
        public int maxAttempts;
        public int storageCheckerTimeout;
        public String userKey;
        public List<String> monUrls;
    }

    public static class KvmCancelSelfFencerCmd extends AgentCommand {
        public String hostUuid;
    }

    public static class GetFactsCmd extends AgentCommand {
        public String monUuid;
    }

    public static class GetFactsRsp extends AgentResponse {
        public String fsid;
        public String monAddr;
    }

    public static class DeleteImageCacheCmd extends AgentCommand {
        public String imagePath;
        public String snapshotPath;
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
    public static final String KVM_CREATE_SECRET_PATH = "/vm/createcephsecret";
    public static final String DELETE_POOL_PATH = "/ceph/primarystorage/deletepool";
    public static final String GET_VOLUME_SIZE_PATH = "/ceph/primarystorage/getvolumesize";
    public static final String KVM_HA_SETUP_SELF_FENCER = "/ha/ceph/setupselffencer";
    public static final String KVM_HA_CANCEL_SELF_FENCER = "/ha/ceph/cancelselffencer";
    public static final String GET_FACTS = "/ceph/primarystorage/facts";
    public static final String DELETE_IMAGE_CACHE = "/ceph/primarystorage/deleteimagecache";
    public static final String SET_ROOT_PASSWORD = "/ceph/primarystorage/setrootpassword";

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
        String backupStorageInstallPath;
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

        abstract boolean deleteWhenRollabackDownload();
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
                int sshport;
                String sftpHostname;
                String username;

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
                                    sshport = greply.getSshPort();
                                    sftpHostname = greply.getHostname();
                                    username = greply.getUsername();
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
                            cmd.username = username;
                            cmd.sshKey = sshkey;
                            cmd.sshPort = sshport;
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
                String username;
                int sshPort;
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
                                    username = returnValue.getUsername();
                                    sshPort = returnValue.getSshPort();
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
                            cmd.setUsername(username);
                            cmd.setSshKey(sshKey);
                            cmd.setSshPort(sshPort);
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

        @Override
        boolean deleteWhenRollabackDownload() {
            return true;
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
        void download(final ReturnValueCompletion<String> completion) {
            checkParam();

            final DownloadParam dparam = (DownloadParam) param;
            if (ImageMediaType.DataVolumeTemplate.toString().equals(dparam.image.getInventory().getMediaType())) {
                CpCmd cmd = new CpCmd();
                cmd.srcPath = dparam.image.getSelectedBackupStorage().getInstallPath();
                cmd.dstPath = dparam.installPath;
                httpCall(CP_PATH, cmd, CpRsp.class, new ReturnValueCompletion<CpRsp>(completion) {
                    @Override
                    public void success(CpRsp returnValue) {
                        completion.success(dparam.installPath);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
            } else {
                completion.success(dparam.image.getSelectedBackupStorage().getInstallPath());
            }
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

        @Override
        boolean deleteWhenRollabackDownload() {
            return false;
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

    private void createEmptyVolume(final InstantiateVolumeOnPrimaryStorageMsg msg) {
        final CreateEmptyVolumeCmd cmd = new CreateEmptyVolumeCmd();
        cmd.installPath = VolumeType.Root.toString().equals(msg.getVolume().getType()) ?
                makeRootVolumeInstallPath(msg.getVolume().getUuid()) : makeDataVolumeInstallPath(msg.getVolume().getUuid());
        cmd.size = msg.getVolume().getSize();

        final InstantiateVolumeOnPrimaryStorageReply reply = new InstantiateVolumeOnPrimaryStorageReply();

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
                vol.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
                reply.setVolume(vol);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(APICleanUpImageCacheOnPrimaryStorageMsg msg) {
        APICleanUpImageCacheOnPrimaryStorageEvent evt = new APICleanUpImageCacheOnPrimaryStorageEvent(msg.getId());
        imageCacheCleaner.cleanup(msg.getUuid());
        bus.publish(evt);
    }

    @Override
    protected void handle(final InstantiateVolumeOnPrimaryStorageMsg msg) {
        if (msg instanceof InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg) {
            createVolumeFromTemplate((InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg) msg);
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
                        String __name__ = "allocate-primary-storage-capacity-for-image-cache";

                        boolean s = false;

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            AllocatePrimaryStorageMsg amsg = new AllocatePrimaryStorageMsg();
                            amsg.setRequiredPrimaryStorageUuid(self.getUuid());
                            amsg.setSize(image.getInventory().getActualSize());
                            amsg.setPurpose(PrimaryStorageAllocationPurpose.DownloadImage.toString());
                            amsg.setNoOverProvisioning(true);
                            bus.makeLocalServiceId(amsg, PrimaryStorageConstant.SERVICE_ID);
                            bus.send(amsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        trigger.fail(reply.getError());
                                    } else {
                                        s = true;
                                        trigger.next();
                                    }
                                }
                            });
                        }

                        @Override
                        public void rollback(FlowRollback trigger, Map data) {
                            if (s) {
                                ReturnPrimaryStorageCapacityMsg rmsg = new ReturnPrimaryStorageCapacityMsg();
                                rmsg.setNoOverProvisioning(true);
                                rmsg.setPrimaryStorageUuid(self.getUuid());
                                rmsg.setDiskSize(image.getInventory().getActualSize());
                                bus.makeLocalServiceId(rmsg, PrimaryStorageConstant.SERVICE_ID);
                                bus.send(rmsg);
                            }

                            trigger.rollback();
                        }
                    });

                    flow(new Flow() {
                        String __name__ = "download-from-backup-storage";

                        boolean deleteOnRollback;

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            DownloadParam param = new DownloadParam();
                            param.image = image;
                            param.installPath = makeCacheInstallPath(image.getInventory().getUuid());
                            BackupStorageMediator mediator = getBackupStorageMediator(image.getSelectedBackupStorage().getBackupStorageUuid());
                            mediator.param = param;

                            deleteOnRollback = mediator.deleteWhenRollabackDownload();
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
                        public void rollback(FlowRollback trigger, Map data) {
                            if (deleteOnRollback && cachePath != null) {
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
                            snapshotPath = String.format("%s@%s", cachePath, image.getInventory().getUuid());
                            CreateSnapshotCmd cmd = new CreateSnapshotCmd();
                            cmd.skipOnExisting = true;
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
                        public void rollback(FlowRollback trigger, Map data) {
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
                            cmd.ignoreError = true;
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
                            cvo.setSize(image.getInventory().getActualSize());
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

    private void createVolumeFromTemplate(final InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg msg) {
        final ImageInventory img = msg.getTemplateSpec().getInventory();

        final InstantiateVolumeOnPrimaryStorageReply reply = new InstantiateVolumeOnPrimaryStorageReply();
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-root-volume-%s", msg.getVolume().getUuid()));
        chain.then(new ShareFlow() {
            String cloneInstallPath;
            String volumePath = makeRootVolumeInstallPath(msg.getVolume().getUuid());
            ImageCacheVO cache;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "download-image-to-cache";

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
                        vol.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
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

    @Override
    protected void handle(final SyncVolumeSizeOnPrimaryStorageMsg msg) {
        final SyncVolumeSizeOnPrimaryStorageReply reply = new SyncVolumeSizeOnPrimaryStorageReply();
        final VolumeVO vol = dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class);

        String installPath = vol.getInstallPath();
        GetVolumeSizeCmd cmd = new GetVolumeSizeCmd();
        cmd.fsId = getSelf().getFsid();
        cmd.uuid = self.getUuid();
        cmd.volumeUuid = msg.getVolumeUuid();
        cmd.installPath = installPath;

        httpCall(GET_VOLUME_SIZE_PATH, cmd, GetVolumeSizeRsp.class, new ReturnValueCompletion<GetVolumeSizeRsp>(msg) {
            @Override
            public void success(GetVolumeSizeRsp rsp) {
                // current ceph has no way to get actual size
                long asize = rsp.actualSize == null ? vol.getActualSize() : rsp.actualSize;
                reply.setActualSize(asize);
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

    protected <T extends AgentResponse> void httpCall(final String path, final AgentCommand cmd, final Class<T> retClass, final ReturnValueCompletion<T> callback) {
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

                base.httpCall(path, cmd, retClass, new ReturnValueCompletion<T>(callback) {
                    @Override
                    public void success(T ret) {
                        if (!ret.success) {
                            callback.fail(errf.stringToOperationError(ret.error));
                            return;
                        }

                        if (!(cmd instanceof InitCmd)) {
                            updateCapacityIfNeeded(ret);
                        }
                        callback.success(ret);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        errorCodes.add(errorCode);
                        call();
                    }
                });
            }
        }

        new HttpCaller().call();
    }

    private void updateCapacityIfNeeded(AgentResponse rsp) {
        if (rsp.totalCapacity != null && rsp.availableCapacity != null) {
            new CephCapacityUpdater().update(getSelf().getFsid(), rsp.totalCapacity, rsp.availableCapacity);
        }
    }

    private void connect(final boolean newAdded, final Completion completion) {
        final List<CephPrimaryStorageMonBase> mons = CollectionUtils.transformToList(getSelf().getMons(),
                new Function<CephPrimaryStorageMonBase, CephPrimaryStorageMonVO>() {
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
                                String.format("unable to connect to the ceph primary storage[uuid:%s]." +
                                                " Failed to connect all ceph mons. Errors are %s",
                                        self.getUuid(), JSONObjectUtil.toJsonString(errorCodes))
                        ));
                    } else {
                        // reload because mon status changed
                        self = dbf.reload(self);
                        trigger.next();
                    }

                    return;
                }

                final CephPrimaryStorageMonBase base = it.next();
                base.connect(new Completion(trigger) {
                    @Override
                    public void success() {
                        connect(trigger);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        errorCodes.add(errorCode);

                        if (newAdded) {
                            // the mon fails to connect, remove it
                            dbf.remove(base.getSelf());
                        }

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
                    String __name__ = "check-mon-integrity";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        final Map<String, String> fsids = new HashMap<String, String>();

                        final List<CephPrimaryStorageMonBase> mons = CollectionUtils.transformToList(getSelf().getMons(), new Function<CephPrimaryStorageMonBase, CephPrimaryStorageMonVO>() {
                            @Override
                            public CephPrimaryStorageMonBase call(CephPrimaryStorageMonVO arg) {
                                return arg.getStatus() == MonStatus.Connected ? new CephPrimaryStorageMonBase(arg) : null;
                            }
                        });

                        DebugUtils.Assert(!mons.isEmpty(), "how can be no connected MON !!!???");

                        final AsyncLatch latch = new AsyncLatch(mons.size(), new NoErrorCompletion(trigger) {
                            @Override
                            public void done() {
                                Set<String> set = new HashSet<String>();
                                set.addAll(fsids.values());

                                if (set.size() != 1) {
                                    StringBuilder sb = new StringBuilder("the fsid returned by mons are mismatching, it seems the mons belong to different ceph clusters:\n");
                                    for (CephPrimaryStorageMonBase mon : mons) {
                                        String fsid = fsids.get(mon.getSelf().getUuid());
                                        sb.append(String.format("%s (mon ip) --> %s (fsid)\n", mon.getSelf().getHostname(), fsid));
                                    }

                                    throw new OperationFailureException(errf.stringToOperationError(sb.toString()));
                                }

                                // check if there is another ceph setup having the same fsid
                                String fsId = set.iterator().next();

                                SimpleQuery<CephPrimaryStorageVO> q = dbf.createQuery(CephPrimaryStorageVO.class);
                                q.add(CephPrimaryStorageVO_.fsid, Op.EQ, fsId);
                                q.add(CephPrimaryStorageVO_.uuid, Op.NOT_EQ, self.getUuid());
                                CephPrimaryStorageVO otherCeph = q.find();
                                if (otherCeph != null) {
                                    throw new OperationFailureException(errf.stringToOperationError(
                                            String.format("there is another CEPH primary storage[name:%s, uuid:%s] with the same" +
                                                            " FSID[%s], you cannot add the same CEPH setup as two different primary storage",
                                                    otherCeph.getName(), otherCeph.getUuid(), fsId)
                                    ));
                                }

                                trigger.next();
                            }
                        });

                        for (final CephPrimaryStorageMonBase mon : mons) {
                            GetFactsCmd cmd = new GetFactsCmd();
                            cmd.uuid = self.getUuid();
                            cmd.monUuid = mon.getSelf().getUuid();
                            mon.httpCall(GET_FACTS, cmd, GetFactsRsp.class, new ReturnValueCompletion<GetFactsRsp>(latch) {
                                @Override
                                public void success(GetFactsRsp rsp) {
                                    if (!rsp.success) {
                                        // one mon cannot get the facts, directly error out
                                        trigger.fail(errf.stringToOperationError(rsp.error));
                                        return;
                                    }

                                    CephPrimaryStorageMonVO monVO = mon.getSelf();
                                    fsids.put(monVO.getUuid(), rsp.fsid);
                                    monVO.setMonAddr(rsp.monAddr == null ? monVO.getHostname() : rsp.monAddr);
                                    dbf.update(monVO);

                                    latch.ack();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    // one mon cannot get the facts, directly error out
                                    trigger.fail(errorCode);
                                }
                            });
                        }

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
                        if (newAdded) {
                            self = dbf.reload(self);
                            if (!getSelf().getMons().isEmpty()) {
                                dbf.removeCollection(getSelf().getMons(), CephPrimaryStorageMonVO.class);
                            }
                        }

                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void connectHook(ConnectParam param, final Completion completion) {
        connect(param.isNewAdded(), completion);
    }

    @Override
    protected void pingHook(final Completion completion) {
        final List<CephPrimaryStorageMonBase> mons = CollectionUtils.transformToList(getSelf().getMons(), new Function<CephPrimaryStorageMonBase, CephPrimaryStorageMonVO>() {
            @Override
            public CephPrimaryStorageMonBase call(CephPrimaryStorageMonVO arg) {
                return new CephPrimaryStorageMonBase(arg);
            }
        });

        final List<ErrorCode> errors = new ArrayList<ErrorCode>();

        class Ping {
            private AtomicBoolean replied = new AtomicBoolean(false);

            @AsyncThread
            private void reconnectMon(final CephPrimaryStorageMonBase mon, boolean delay) {
                if (!CephGlobalConfig.PRIMARY_STORAGE_MON_AUTO_RECONNECT.value(Boolean.class)) {
                    logger.debug(String.format("do not reconnect the ceph primary storage mon[uuid:%s] as the global config[%s] is set to false",
                            self.getUuid(), CephGlobalConfig.PRIMARY_STORAGE_MON_AUTO_RECONNECT.getCanonicalName()));
                    return;
                }

                // there has been a reconnect in process
                if (!reconnectMonLock.lock()) {
                    logger.debug(String.format("ignore this call, reconnect ceph primary storage mon[uuid:%s] is in process", self.getUuid()));
                    return;
                }

                final NoErrorCompletion releaseLock = new NoErrorCompletion() {
                    @Override
                    public void done() {
                        reconnectMonLock.unlock();
                    }
                };

                try {
                    if (delay) {
                        try {
                            TimeUnit.SECONDS.sleep(CephGlobalConfig.PRIMARY_STORAGE_MON_RECONNECT_DELAY.value(Long.class));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    mon.connect(new Completion() {
                        @Override
                        public void success() {
                            logger.debug(String.format("successfully reconnected the mon[uuid:%s] of the ceph primary" +
                                    " storage[uuid:%s, name:%s]", mon.getSelf().getUuid(), self.getUuid(), self.getName()));
                            releaseLock.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            //TODO
                            logger.warn(String.format("failed to reconnect the mon[uuid:%s] of the ceph primary" +
                                    " storage[uuid:%s, name:%s], %s", mon.getSelf().getUuid(), self.getUuid(), self.getName(), errorCode));
                            releaseLock.done();
                        }
                    });
                } catch (Throwable t) {
                    releaseLock.done();
                    logger.warn(t.getMessage(), t);
                }
            }

            void ping() {
                // this is only called when all mons are disconnected
                final AsyncLatch latch = new AsyncLatch(mons.size(), new NoErrorCompletion() {
                    @Override
                    public void done() {
                        if (!replied.compareAndSet(false, true)) {
                            return;
                        }

                        ErrorCode err = errf.stringToOperationError(String.format("failed to ping the ceph primary storage[uuid:%s, name:%s]",
                                self.getUuid(), self.getName()), errors);
                        completion.fail(err);
                    }
                });

                for (final CephPrimaryStorageMonBase mon : mons) {
                    mon.ping(new ReturnValueCompletion<PingResult>(latch) {
                        private void thisMonIsDown(ErrorCode err) {
                            //TODO
                            logger.warn(String.format("cannot ping mon[uuid:%s] of the ceph primary storage[uuid:%s, name:%s], %s",
                                    mon.getSelf().getUuid(), self.getUuid(), self.getName(), err));
                            errors.add(err);
                            mon.changeStatus(MonStatus.Disconnected);
                            reconnectMon(mon, true);
                            latch.ack();
                        }

                        @Override
                        public void success(PingResult res) {
                            if (res.success) {
                                // as long as there is one mon working, the primary storage works
                                pingSuccess();

                                if (mon.getSelf().getStatus() == MonStatus.Disconnected) {
                                    reconnectMon(mon, false);
                                }

                            } else if (PingOperationFailure.UnableToCreateFile.toString().equals(res.failure)) {
                                // as long as there is one mon saying the ceph not working, the primary storage goes down
                                logger.warn(String.format("the ceph primary storage[uuid:%s, name:%s] is down, as one mon[uuid:%s] reports" +
                                        " an operation failure[%s]", self.getUuid(), self.getName(), mon.getSelf().getUuid(), res.error));
                                ErrorCode errorCode = errf.stringToOperationError(res.error);
                                errors.add(errorCode);
                                primaryStorageDown();
                            } else if (!res.success || PingOperationFailure.MonAddrChanged.toString().equals(res.failure)) {
                                // this mon is down(success == false, operationFailure == false), but the primary storage may still work as other mons may work
                                ErrorCode errorCode = errf.stringToOperationError(res.error);
                                thisMonIsDown(errorCode);
                            } else {
                                throw new CloudRuntimeException("should not be here");
                            }
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            thisMonIsDown(errorCode);
                        }
                    });
                }
            }

            // this is called once a mon return an operation failure
            private void primaryStorageDown() {
                if (!replied.compareAndSet(false, true)) {
                    return;
                }

                // set all mons to be disconnected
                for (CephPrimaryStorageMonBase base : mons) {
                    base.changeStatus(MonStatus.Disconnected);
                }

                ErrorCode err = errf.stringToOperationError(String.format("failed to ping the ceph primary storage[uuid:%s, name:%s]",
                        self.getUuid(), self.getName()), errors);
                completion.fail(err);
            }


            private void pingSuccess() {
                if (!replied.compareAndSet(false, true)) {
                    return;
                }

                completion.success();
            }
        }

        new Ping().ping();
    }

    @Override
    protected void syncPhysicalCapacity(ReturnValueCompletion<PhysicalCapacityUsage> completion) {
        PrimaryStorageCapacityVO cap = dbf.findByUuid(self.getUuid(), PrimaryStorageCapacityVO.class);
        PhysicalCapacityUsage usage = new PhysicalCapacityUsage();
        usage.availablePhysicalSize = cap.getAvailablePhysicalCapacity();
        usage.totalPhysicalSize = cap.getTotalPhysicalCapacity();
        completion.success(usage);
    }

    @Override
    protected void handle(APIReconnectPrimaryStorageMsg msg) {
        final APIReconnectPrimaryStorageEvent evt = new APIReconnectPrimaryStorageEvent(msg.getId());
        self.setStatus(PrimaryStorageStatus.Connecting);
        dbf.update(self);
        connect(false, new Completion(msg) {
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
        } else if (msg instanceof APIUpdateCephPrimaryStorageMonMsg) {
            handle((APIUpdateCephPrimaryStorageMonMsg) msg);
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

    private void handle(APIUpdateCephPrimaryStorageMonMsg msg) {
        final APIUpdateMonToCephPrimaryStorageEvent evt = new APIUpdateMonToCephPrimaryStorageEvent(msg.getId());
        CephPrimaryStorageMonVO monvo = dbf.findByUuid(msg.getMonUuid(), CephPrimaryStorageMonVO.class);
        if (msg.getHostname() != null) {
            monvo.setHostname(msg.getHostname());
        }
        if (msg.getMonPort() != null && msg.getMonPort() > 0 && msg.getMonPort() <= 65535) {
            monvo.setMonPort(msg.getMonPort());
        }
        if (msg.getSshPort() != null && msg.getSshPort() > 0 && msg.getSshPort() <= 65535) {
            monvo.setSshPort(msg.getSshPort());
        }
        if (msg.getSshUsername() != null) {
            monvo.setSshUsername(msg.getSshUsername());
        }
        if (msg.getSshPassword() != null) {
            monvo.setSshPassword(msg.getSshPassword());
        }
        dbf.update(monvo);
        evt.setInventory(CephPrimaryStorageInventory.valueOf((dbf.reload(getSelf()))));
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
                            monvo.setMonAddr(monvo.getHostname());
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
                    public void rollback(FlowRollback trigger, Map data) {
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

                        final List<ErrorCode> errorCodes = new ArrayList<ErrorCode>();
                        final AsyncLatch latch = new AsyncLatch(bases.size(), new NoErrorCompletion(trigger) {
                            @Override
                            public void done() {
                                if (!errorCodes.isEmpty()) {
                                    trigger.fail(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR, "unable to connect mons", errorCodes));
                                } else {
                                    trigger.next();
                                }
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
                                    errorCodes.add(errorCode);
                                    latch.ack();
                                }
                            });
                        }
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "check-mon-integrity";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        List<CephPrimaryStorageMonBase> bases = CollectionUtils.transformToList(monVOs, new Function<CephPrimaryStorageMonBase, CephPrimaryStorageMonVO>() {
                            @Override
                            public CephPrimaryStorageMonBase call(CephPrimaryStorageMonVO arg) {
                                return new CephPrimaryStorageMonBase(arg);
                            }
                        });

                        final List<ErrorCode> errors = new ArrayList<ErrorCode>();

                        final AsyncLatch latch = new AsyncLatch(bases.size(), new NoErrorCompletion(trigger) {
                            @Override
                            public void done() {
                                // one fail, all fail
                                if (!errors.isEmpty()) {
                                    trigger.fail(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR, "unable to add mon to ceph primary storage", errors));
                                } else {
                                    trigger.next();
                                }
                            }
                        });

                        for (final CephPrimaryStorageMonBase base : bases) {
                            GetFactsCmd cmd = new GetFactsCmd();
                            cmd.uuid = self.getUuid();
                            cmd.monUuid = base.getSelf().getUuid();
                            base.httpCall(GET_FACTS, cmd, GetFactsRsp.class, new ReturnValueCompletion<GetFactsRsp>(latch) {
                                @Override
                                public void success(GetFactsRsp rsp) {
                                    if (!rsp.isSuccess()) {
                                        errors.add(errf.stringToOperationError(rsp.getError()));
                                    } else {
                                        String fsid = rsp.fsid;
                                        if (!getSelf().getFsid().equals(fsid)) {
                                            errors.add(errf.stringToOperationError(
                                                    String.format("the mon[ip:%s] returns a fsid[%s] different from the current fsid[%s] of the cep cluster," +
                                                            "are you adding a mon not belonging to current cluster mistakenly?", base.getSelf().getHostname(), fsid, getSelf().getFsid())
                                            ));
                                        }
                                    }

                                    latch.ack();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    errors.add(errorCode);
                                    latch.ack();
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
        } else if (msg instanceof CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg) {
            handle((CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg) {
            handle((BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg) msg);
        } else if (msg instanceof CreateKvmSecretMsg) {
            handle((CreateKvmSecretMsg) msg);
        } else if (msg instanceof UploadBitsToBackupStorageMsg) {
            handle((UploadBitsToBackupStorageMsg) msg);
        } else if (msg instanceof SetupSelfFencerOnKvmHostMsg) {
            handle((SetupSelfFencerOnKvmHostMsg) msg);
        } else if (msg instanceof CancelSelfFencerOnKvmHostMsg) {
            handle((CancelSelfFencerOnKvmHostMsg) msg);
        } else if (msg instanceof DeleteImageCacheOnPrimaryStorageMsg) {
            handle((DeleteImageCacheOnPrimaryStorageMsg) msg);
        } else if (msg instanceof ReInitRootVolumeFromTemplateOnPrimaryStorageMsg) {
            handle((ReInitRootVolumeFromTemplateOnPrimaryStorageMsg) msg);
        } else if (msg instanceof SetRootPasswordMsg) {
            handle((SetRootPasswordMsg) msg);
        } else {
            super.handleLocalMessage(msg);
        }
    }

    public static class SetPasswordCmd extends AgentCommand {
        public String cephInstallPath;
        public String vmUuid;
        public String account;
        public String password;
    }

    public static class SetPasswordRsp extends AgentResponse {
        public String cephInstallPath;
        public String vmUuid;
        public String account;
        public String password;
    }

    private void handle(SetRootPasswordMsg msg) {
        final SetRootPasswordReply reply = new SetRootPasswordReply();
        CephVolumeOperate cephvo = new CephVolumeOperate();
        SimpleQuery<CephPrimaryStorageMonVO> q = dbf.createQuery(CephPrimaryStorageMonVO.class);
        q.add(CephPrimaryStorageMonVO_.primaryStorageUuid, SimpleQuery.Op.EQ, msg.getPrimaryStorageUuid());
        Set<CephPrimaryStorageMonVO> monVos = new HashSet(q.list());
        cephvo.setMonsSet(monVos);
        CephPrimaryStorageMonBase monBase = cephvo.chooseTargetVmUuid();

        SetPasswordCmd setPasswordCmd = new SetPasswordCmd();
        setPasswordCmd.cephInstallPath = msg.getQcowFile();
        setPasswordCmd.vmUuid = msg.getVmAccountPerference().getVmUuid();
        setPasswordCmd.account = msg.getVmAccountPerference().getUserAccount();
        setPasswordCmd.password = msg.getVmAccountPerference().getAccountPassword();

        logger.debug(String.format("set root password, send http to %s", monBase.getSelf().getHostname()));
        monBase.httpCall(SET_ROOT_PASSWORD, setPasswordCmd, SetPasswordRsp.class, new ReturnValueCompletion<SetPasswordRsp>(reply) {
            @Override
            public void success(SetPasswordRsp rsp) {
                if (!rsp.isSuccess()) {
                    reply.setError(errf.stringToOperationError(rsp.getError()));
                }
                reply.setQcowFile(rsp.cephInstallPath);
                reply.setVmAccountPerference(new VmAccountPerference(rsp.vmUuid, rsp.account, rsp.password));
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(DeleteImageCacheOnPrimaryStorageMsg msg) {
        DeleteImageCacheOnPrimaryStorageReply reply = new DeleteImageCacheOnPrimaryStorageReply();

        DeleteImageCacheCmd cmd = new DeleteImageCacheCmd();
        cmd.setFsId(getSelf().getFsid());
        cmd.setUuid(self.getUuid());
        cmd.imagePath = msg.getInstallPath().split("@")[0];
        cmd.snapshotPath = msg.getInstallPath();
        httpCall(DELETE_IMAGE_CACHE, cmd, AgentResponse.class, new ReturnValueCompletion<AgentResponse>(msg) {
            @Override
            public void success(AgentResponse rsp) {
                if (!rsp.isSuccess()) {
                    reply.setError(errf.stringToOperationError(rsp.getError()));
                }

                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(CancelSelfFencerOnKvmHostMsg msg) {
        KvmCancelSelfFencerParam param = msg.getParam();
        KvmCancelSelfFencerCmd cmd = new KvmCancelSelfFencerCmd();
        cmd.uuid = self.getUuid();
        cmd.fsId = getSelf().getFsid();
        cmd.hostUuid = param.getHostUuid();

        CancelSelfFencerOnKvmHostReply reply = new CancelSelfFencerOnKvmHostReply();
        new KvmCommandSender(param.getHostUuid()).send(cmd, KVM_HA_CANCEL_SELF_FENCER, wrapper -> {
            AgentResponse rsp = wrapper.getResponse(AgentResponse.class);
            return rsp.isSuccess() ? null : errf.stringToOperationError(rsp.getError());
        }, new ReturnValueCompletion<KvmResponseWrapper>(msg) {
            @Override
            public void success(KvmResponseWrapper w) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final SetupSelfFencerOnKvmHostMsg msg) {
        KvmSetupSelfFencerParam param = msg.getParam();
        KvmSetupSelfFencerCmd cmd = new KvmSetupSelfFencerCmd();
        cmd.uuid = self.getUuid();
        cmd.fsId = getSelf().getFsid();
        cmd.hostUuid = param.getHostUuid();
        cmd.interval = param.getInterval();
        cmd.maxAttempts = param.getMaxAttempts();
        cmd.storageCheckerTimeout = param.getStorageCheckerTimeout();
        cmd.userKey = getSelf().getUserKey();
        cmd.heartbeatImagePath = String.format("%s/ceph-primary-storage-%s-heartbeat-file", getSelf().getRootVolumePoolName(), self.getUuid());
        cmd.monUrls = CollectionUtils.transformToList(getSelf().getMons(), new Function<String, CephPrimaryStorageMonVO>() {
            @Override
            public String call(CephPrimaryStorageMonVO arg) {
                return String.format("%s:%s", arg.getMonAddr(), arg.getMonPort());
            }
        });

        final SetupSelfFencerOnKvmHostReply reply = new SetupSelfFencerOnKvmHostReply();
        new KvmCommandSender(param.getHostUuid()).send(cmd, KVM_HA_SETUP_SELF_FENCER, new KvmCommandFailureChecker() {
            @Override
            public ErrorCode getError(KvmResponseWrapper wrapper) {
                AgentResponse rsp = wrapper.getResponse(AgentResponse.class);
                return rsp.isSuccess() ? null : errf.stringToOperationError(rsp.getError());
            }
        }, new ReturnValueCompletion<KvmResponseWrapper>(msg) {
            @Override
            public void success(KvmResponseWrapper wrapper) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }


    private void handle(final UploadBitsToBackupStorageMsg msg) {
        SimpleQuery<BackupStorageVO> q = dbf.createQuery(BackupStorageVO.class);
        q.select(BackupStorageVO_.type);
        q.add(BackupStorageVO_.uuid, Op.EQ, msg.getBackupStorageUuid());
        String bsType = q.findValue();

        if (!CephConstants.CEPH_BACKUP_STORAGE_TYPE.equals(bsType)) {
            throw new OperationFailureException(errf.stringToOperationError(
                    String.format("unable to upload bits to the backup storage[type:%s], we only support CEPH", bsType)
            ));
        }

        final UploadBitsToBackupStorageReply reply = new UploadBitsToBackupStorageReply();

        CpCmd cmd = new CpCmd();
        cmd.fsId = getSelf().getFsid();
        cmd.srcPath = msg.getPrimaryStorageInstallPath();
        cmd.dstPath = msg.getBackupStorageInstallPath();
        httpCall(CP_PATH, cmd, CpRsp.class, new ReturnValueCompletion<CpRsp>(msg) {
            @Override
            public void success(CpRsp rsp) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final CreateKvmSecretMsg msg) {
        final CreateKvmSecretReply reply = new CreateKvmSecretReply();
        createSecretOnKvmHosts(msg.getHostUuids(), new Completion(msg) {
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

    private void handle(BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg msg) {
        BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply reply = new BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply();
        reply.setError(errf.stringToOperationError("backing up snapshots to backup storage is a depreciated feature, which will be removed in future version"));
        bus.reply(msg, reply);
    }

    private void handle(final CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg msg) {
        final CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply reply = new CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply();

        final String volPath = makeDataVolumeInstallPath(msg.getVolumeUuid());
        VolumeSnapshotInventory sp = msg.getSnapshot();
        CpCmd cmd = new CpCmd();
        cmd.resourceUuid = msg.getSnapshot().getVolumeUuid();
        cmd.srcPath = sp.getPrimaryStorageInstallPath();
        cmd.dstPath = volPath;
        httpCall(CP_PATH, cmd, CpRsp.class, new ReturnValueCompletion<CpRsp>(msg) {
            @Override
            public void success(CpRsp rsp) {
                reply.setInstallPath(volPath);
                reply.setSize(rsp.size);

                // current ceph has no way to get the actual size
                long asize = rsp.actualSize == null ? 1 : rsp.actualSize;
                reply.setActualSize(asize);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final RevertVolumeFromSnapshotOnPrimaryStorageMsg msg) {
        final RevertVolumeFromSnapshotOnPrimaryStorageReply reply = new RevertVolumeFromSnapshotOnPrimaryStorageReply();

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

    private void handle(final ReInitRootVolumeFromTemplateOnPrimaryStorageMsg msg) {
        final ReInitRootVolumeFromTemplateOnPrimaryStorageReply reply = new ReInitRootVolumeFromTemplateOnPrimaryStorageReply();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("re-init-root-volume-%s", msg.getVolume().getUuid()));
        chain.then(new ShareFlow() {

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "delete-current-root-volume";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        DeleteCmd cmd = new DeleteCmd();
                        cmd.installPath = msg.getVolume().getInstallPath();

                        httpCall(DELETE_PATH, cmd, DeleteRsp.class, new ReturnValueCompletion<DeleteRsp>(trigger) {
                            @Override
                            public void fail(ErrorCode err) {
                                trigger.fail(err);
                            }

                            @Override
                            public void success(DeleteRsp ret) {
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "clone-image";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        CloneCmd cmd = new CloneCmd();
                        cmd.srcPath = makeCacheInstallPath(msg.getVolume().getRootImageUuid());
                        cmd.dstPath = makeRootVolumeInstallPath(msg.getVolume().getUuid());

                        httpCall(CLONE_PATH, cmd, CloneRsp.class, new ReturnValueCompletion<CloneRsp>(trigger) {
                            @Override
                            public void success(CloneRsp returnValue) {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        // use the same path as before
                        reply.setNewVolumeInstallPath(makeRootVolumeInstallPath(msg.getVolume().getUuid()));
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
        cmd.volumeUuid = sp.getVolumeUuid();
        cmd.snapshotPath = spPath;
        httpCall(CREATE_SNAPSHOT_PATH, cmd, CreateSnapshotRsp.class, new ReturnValueCompletion<CreateSnapshotRsp>(msg) {
            @Override
            public void success(CreateSnapshotRsp rsp) {
                // current ceph has no way to get actual size
                long asize = rsp.getActualSize() == null ? 1 : rsp.getActualSize();
                sp.setSize(asize);
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
    public void attachHook(String clusterUuid, Completion completion) {
        SimpleQuery<ClusterVO> q = dbf.createQuery(ClusterVO.class);
        q.select(ClusterVO_.hypervisorType);
        q.add(ClusterVO_.uuid, Op.EQ, clusterUuid);
        String hvType = q.findValue();
        if (KVMConstant.KVM_HYPERVISOR_TYPE.equals(hvType)) {
            attachToKvmCluster(clusterUuid, completion);
        } else {
            completion.success();
        }
    }

    private void createSecretOnKvmHosts(List<String> hostUuids, final Completion completion) {
        final CreateKvmSecretCmd cmd = new CreateKvmSecretCmd();
        cmd.setUserKey(getSelf().getUserKey());
        String suuid = CephSystemTags.KVM_SECRET_UUID.getTokenByResourceUuid(self.getUuid(), CephSystemTags.KVM_SECRET_UUID_TOKEN);
        DebugUtils.Assert(suuid != null, String.format("cannot find system tag[%s] for ceph primary storage[uuid:%s]", CephSystemTags.KVM_SECRET_UUID.getTagFormat(), self.getUuid()));
        cmd.setUuid(suuid);

        List<KVMHostAsyncHttpCallMsg> msgs = CollectionUtils.transformToList(hostUuids, new Function<KVMHostAsyncHttpCallMsg, String>() {
            @Override
            public KVMHostAsyncHttpCallMsg call(String huuid) {
                KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                msg.setCommand(cmd);
                msg.setPath(KVM_CREATE_SECRET_PATH);
                msg.setHostUuid(huuid);
                msg.setNoStatusCheck(true);
                msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, huuid);
                return msg;
            }
        });

        bus.send(msgs, new CloudBusListCallBack(completion) {
            @Override
            public void run(List<MessageReply> replies) {
                for (MessageReply r : replies) {
                    if (!r.isSuccess()) {
                        completion.fail(r.getError());
                        return;
                    }

                    KVMHostAsyncHttpCallReply kr = r.castReply();
                    CreateKvmSecretRsp rsp = kr.toResponse(CreateKvmSecretRsp.class);
                    if (!rsp.isSuccess()) {
                        completion.fail(errf.stringToOperationError(rsp.getError()));
                        return;
                    }
                }

                completion.success();
            }
        });
    }

    private void attachToKvmCluster(String clusterUuid, Completion completion) {
        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
        q.select(HostVO_.uuid);
        q.add(HostVO_.clusterUuid, Op.EQ, clusterUuid);
        q.add(HostVO_.status, Op.EQ, HostStatus.Connected);
        List<String> hostUuids = q.listValue();
        if (hostUuids.isEmpty()) {
            completion.success();
        } else {
            createSecretOnKvmHosts(hostUuids, completion);
        }
    }

    @Override
    public void deleteHook() {
        if (CephGlobalConfig.PRIMARY_STORAGE_DELETE_POOL.value(Boolean.class)) {
            DeletePoolCmd cmd = new DeletePoolCmd();
            cmd.poolNames = list(getSelf().getImageCachePoolName(), getSelf().getDataVolumePoolName(), getSelf().getRootVolumePoolName());
            FutureReturnValueCompletion completion = new FutureReturnValueCompletion();
            httpCall(DELETE_POOL_PATH, cmd, DeletePoolRsp.class, completion);
            completion.await(TimeUnit.MINUTES.toMillis(30));
            if (!completion.isSuccess()) {
                throw new OperationFailureException(completion.getErrorCode());
            }
        }
        dbf.removeCollection(getSelf().getMons(), CephPrimaryStorageMonVO.class);
    }
}
