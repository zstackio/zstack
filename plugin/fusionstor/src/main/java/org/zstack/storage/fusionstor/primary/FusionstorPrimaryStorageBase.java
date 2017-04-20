package org.zstack.storage.fusionstor.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBusCallBack;
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
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
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
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.vm.VmInstanceSpec.ImageSpec;
import org.zstack.header.volume.*;
import org.zstack.kvm.*;
import org.zstack.kvm.KvmSetupSelfFencerExtensionPoint.KvmSetupSelfFencerParam;
import org.zstack.storage.backup.sftp.GetSftpBackupStorageDownloadCredentialMsg;
import org.zstack.storage.backup.sftp.GetSftpBackupStorageDownloadCredentialReply;
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant;
import org.zstack.storage.fusionstor.*;
import org.zstack.storage.fusionstor.FusionstorMonBase.PingResult;
import org.zstack.storage.fusionstor.backup.FusionstorBackupStorageVO;
import org.zstack.storage.fusionstor.backup.FusionstorBackupStorageVO_;
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

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 7/28/2015.
 */
public class FusionstorPrimaryStorageBase extends PrimaryStorageBase {
    private static final CLogger logger = Utils.getLogger(FusionstorPrimaryStorageBase.class);

    @Autowired
    private RESTFacade restf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ApiTimeoutManager timeoutMgr;

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
        List<String> monHostnames;
        List<String> sshUsernames;
        List<String> sshPasswords;
        String fusionstorType;

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

    @ApiTimeout(apiClasses = {APICreateVolumeSnapshotMsg.class})
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

    public static class GetFactsCmd extends AgentCommand {
        public String monUuid;
    }

    public static class GetFactsRsp extends AgentResponse {
        public String fsid;
    }

    public static final String INIT_PATH = "/fusionstor/primarystorage/init";
    public static final String CREATE_VOLUME_PATH = "/fusionstor/primarystorage/volume/createempty";
    public static final String DELETE_PATH = "/fusionstor/primarystorage/delete";
    public static final String CLONE_PATH = "/fusionstor/primarystorage/volume/clone";
    public static final String FLATTEN_PATH = "/fusionstor/primarystorage/volume/flatten";
    public static final String SFTP_DOWNLOAD_PATH = "/fusionstor/primarystorage/sftpbackupstorage/download";
    public static final String SFTP_UPLOAD_PATH = "/fusionstor/primarystorage/sftpbackupstorage/upload";
    public static final String CREATE_SNAPSHOT_PATH = "/fusionstor/primarystorage/snapshot/create";
    public static final String DELETE_SNAPSHOT_PATH = "/fusionstor/primarystorage/snapshot/delete";
    public static final String PROTECT_SNAPSHOT_PATH = "/fusionstor/primarystorage/snapshot/protect";
    public static final String ROLLBACK_SNAPSHOT_PATH = "/fusionstor/primarystorage/snapshot/rollback";
    public static final String UNPROTECT_SNAPSHOT_PATH = "/fusionstor/primarystorage/snapshot/unprotect";
    public static final String CP_PATH = "/fusionstor/primarystorage/volume/cp";
    public static final String DELETE_POOL_PATH = "/fusionstor/primarystorage/deletepool";
    public static final String GET_VOLUME_SIZE_PATH = "/fusionstor/primarystorage/getvolumesize";
    public static final String KVM_HA_SETUP_SELF_FENCER = "/ha/fusionstor/setupselffencer";
    public static final String GET_FACTS = "/fusionstor/primarystorage/facts";

    private final Map<String, BackupStorageMediator> backupStorageMediators = new HashMap<String, BackupStorageMediator>();

    {
        backupStorageMediators.put(SftpBackupStorageConstant.SFTP_BACKUP_STORAGE_TYPE, new SftpBackupStorageMediator());
        backupStorageMediators.put(FusionstorConstants.FUSIONSTOR_BACKUP_STORAGE_TYPE, new FusionstorBackupStorageMediator());
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
            chain.setName(String.format("download-image-from-sftp-%s-to-fusionstor-%s", backupStorage.getUuid(), self.getUuid()));
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
                                    sftpHostname = greply.getHostname();
                                    username = greply.getUsername();
                                    sshport = greply.getSshPort();
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
            chain.setName(String.format("upload-image-fusionstor-%s-to-sftp-%s", self.getUuid(), backupStorage.getUuid()));
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

    class FusionstorBackupStorageMediator extends BackupStorageMediator {
        protected void checkParam() {
            super.checkParam();

            SimpleQuery<FusionstorBackupStorageVO> q = dbf.createQuery(FusionstorBackupStorageVO.class);
            q.select(FusionstorBackupStorageVO_.fsid);
            q.add(FusionstorBackupStorageVO_.uuid, Op.EQ, backupStorage.getUuid());
            String bsFsid = q.findValue();
            if (!getSelf().getFsid().equals(bsFsid)) {
                throw new OperationFailureException(errf.stringToOperationError(
                        String.format("the backup storage[uuid:%s, name:%s, fsid:%s] is not in the same fusionstor cluster" +
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
            chain.setName(String.format("upload-image-fusionstor-%s-to-fusionstor-%s", self.getUuid(), backupStorage.getUuid()));
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
        return String.format("fusionstor://%s/%s", getSelf().getRootVolumePoolName(), volUuid);
    }

    private String makeDataVolumeInstallPath(String volUuid) {
        return String.format("fusionstor://%s/%s", getSelf().getDataVolumePoolName(), volUuid);
    }

    private String makeResetImageRootVolumeInstallPath(String volUuid) {
        return String.format("fusionstor://%s/reset-image-%s-%s", getSelf().getRootVolumePoolName(), volUuid,
                System.currentTimeMillis());
    }

    private String makeCacheInstallPath(String uuid) {
        return String.format("fusionstor://%s/%s", getSelf().getImageCachePoolName(), uuid);
    }

    public FusionstorPrimaryStorageBase(PrimaryStorageVO self) {
        super(self);
    }

    protected FusionstorPrimaryStorageVO getSelf() {
        return (FusionstorPrimaryStorageVO) self;
    }

    protected FusionstorPrimaryStorageInventory getSelfInventory() {
        return FusionstorPrimaryStorageInventory.valueOf(getSelf());
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
            chain.setName(String.format("prepare-image-cache-fusionstor-%s", self.getUuid()));
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
                                IncreasePrimaryStorageCapacityMsg imsg = new IncreasePrimaryStorageCapacityMsg();
                                imsg.setNoOverProvisioning(true);
                                imsg.setPrimaryStorageUuid(self.getUuid());
                                imsg.setDiskSize(image.getInventory().getActualSize());
                                bus.makeLocalServiceId(imsg, PrimaryStorageConstant.SERVICE_ID);
                                bus.send(imsg);
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
                                httpCall(DELETE_PATH, cmd, DeleteRsp.class, new ReturnValueCompletion<DeleteRsp>(null) {
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
                                httpCall(DELETE_SNAPSHOT_PATH, cmd, DeleteSnapshotRsp.class, new ReturnValueCompletion<DeleteSnapshotRsp>(null) {
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
                    return String.format("fusionstor-p-%s-download-image-%s", self.getUuid(), image.getInventory().getUuid());
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
                // current fusionstor has no way to get actual size
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

        final List<FusionstorPrimaryStorageMonBase> mons = new ArrayList<FusionstorPrimaryStorageMonBase>();
        for (FusionstorPrimaryStorageMonVO monvo : getSelf().getMons()) {
            if (monvo.getStatus() == MonStatus.Connected) {
                mons.add(new FusionstorPrimaryStorageMonBase(monvo));
            }
        }

        if (mons.isEmpty()) {
            throw new OperationFailureException(errf.stringToOperationError(
                    String.format("all fusionstor mons of primary storage[uuid:%s] are not in Connected state", self.getUuid())
            ));
        }

        Collections.shuffle(mons);

        class HttpCaller {
            Iterator<FusionstorPrimaryStorageMonBase> it = mons.iterator();
            List<ErrorCode> errorCodes = new ArrayList<ErrorCode>();

            void call() {
                if (!it.hasNext()) {
                    callback.fail(errf.stringToOperationError(
                            String.format("all mons failed to execute http call[%s], errors are %s", path, JSONObjectUtil.toJsonString(errorCodes))
                    ));

                    return;
                }

                FusionstorPrimaryStorageMonBase base = it.next();

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
            new FusionstorCapacityUpdater().update(getSelf().getFsid(), rsp.totalCapacity, rsp.availableCapacity);
        }
    }

    private void connect(final boolean newAdded, final Completion completion) {
        final List<FusionstorPrimaryStorageMonBase> mons = CollectionUtils.transformToList(getSelf().getMons(), new Function<FusionstorPrimaryStorageMonBase, FusionstorPrimaryStorageMonVO>() {
            @Override
            public FusionstorPrimaryStorageMonBase call(FusionstorPrimaryStorageMonVO arg) {
                return new FusionstorPrimaryStorageMonBase(arg);
            }
        });

        class Connector {
            List<ErrorCode> errorCodes = new ArrayList<ErrorCode>();
            Iterator<FusionstorPrimaryStorageMonBase> it = mons.iterator();

            void connect(final FlowTrigger trigger) {
                if (!it.hasNext()) {
                    if (errorCodes.size() == mons.size()) {
                        trigger.fail(errf.stringToOperationError(
                                String.format("unable to connect to the fusionstor primary storage[uuid:%s]. Failed to connect all fusionstor mons. Errors are %s",
                                        self.getUuid(), JSONObjectUtil.toJsonString(errorCodes))
                        ));
                    } else {
                        // reload because mon status changed
                        PrimaryStorageVO vo = dbf.reload(self);
                        if (vo == null) {
                            if (newAdded){
                                if (!getSelf().getMons().isEmpty()) {
                                    dbf.removeCollection(getSelf().getMons(), FusionstorPrimaryStorageMonVO.class);
                                }
                            }
                            trigger.fail(operr("fusionstor primary storage[uuid:%s] may have been deleted.", self.getUuid()));
                        } else {
                            self = vo;
                            trigger.next();
                        }
                    }

                    return;
                }

                final FusionstorPrimaryStorageMonBase base = it.next();
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
        chain.setName(String.format("connect-fusionstor-primary-storage-%s", self.getUuid()));
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

                        final List<FusionstorPrimaryStorageMonBase> mons = CollectionUtils.transformToList(getSelf().getMons(), new Function<FusionstorPrimaryStorageMonBase, FusionstorPrimaryStorageMonVO>() {
                            @Override
                            public FusionstorPrimaryStorageMonBase call(FusionstorPrimaryStorageMonVO arg) {
                                return arg.getStatus() == MonStatus.Connected ? new FusionstorPrimaryStorageMonBase(arg) : null;
                            }
                        });

                        DebugUtils.Assert(!mons.isEmpty(), "how can be no connected MON !!!???");

                        final AsyncLatch latch = new AsyncLatch(mons.size(), new NoErrorCompletion(trigger) {
                            @Override
                            public void done() {
                                Set<String> set = new HashSet<String>();
                                set.addAll(fsids.values());

                                if (set.size() != 1) {
                                    StringBuilder sb = new StringBuilder("the fsid returned by mons are mismatching, it seems the mons belong to different fusionstor clusters:\n");
                                    for (FusionstorPrimaryStorageMonBase mon : mons) {
                                        String fsid = fsids.get(mon.getSelf().getUuid());
                                        sb.append(String.format("%s (mon ip) --> %s (fsid)\n", mon.getSelf().getHostname(), fsid));
                                    }

                                    throw new OperationFailureException(errf.stringToOperationError(sb.toString()));
                                }

                                // check if there is another fusionstor setup having the same fsid
                                String fsId = set.iterator().next();

                                SimpleQuery<FusionstorPrimaryStorageVO> q = dbf.createQuery(FusionstorPrimaryStorageVO.class);
                                q.add(FusionstorPrimaryStorageVO_.fsid, Op.EQ, fsId);
                                q.add(FusionstorPrimaryStorageVO_.uuid, Op.NOT_EQ, self.getUuid());
                                FusionstorPrimaryStorageVO otherFusion = q.find();
                                if (otherFusion != null) {
                                    throw new OperationFailureException(errf.stringToOperationError(
                                            String.format("there is another Fusionstor primary storage[name:%s, uuid:%s] with the same" +
                                                            " FSID[%s], you cannot add the same Fusionstor setup as two different primary storage",
                                                    otherFusion.getName(), otherFusion.getUuid(), fsId)
                                    ));
                                }

                                trigger.next();
                            }
                        });

                        for (final FusionstorPrimaryStorageMonBase mon : mons) {
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

                                    fsids.put(mon.getSelf().getUuid(), rsp.fsid);
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
                        List<String> monHostnames = new ArrayList<String>();
                        List<String> sshUsernames = new ArrayList<String>();
                        List<String> sshPasswords = new ArrayList<String>();

                        final List<FusionstorPrimaryStorageMonBase> mons = CollectionUtils.transformToList(getSelf().getMons(), new Function<FusionstorPrimaryStorageMonBase, FusionstorPrimaryStorageMonVO>() {
                            @Override
                            public FusionstorPrimaryStorageMonBase call(FusionstorPrimaryStorageMonVO arg) {
                                return new FusionstorPrimaryStorageMonBase(arg);
                            }
                        });

                        for (FusionstorPrimaryStorageMonBase mon : mons) {
                            monHostnames.add(mon.getSelf().getHostname());
                            sshUsernames.add(mon.getSelf().getSshUsername());
                            sshPasswords.add(mon.getSelf().getSshPassword());
                        }

                        Pool p = new Pool();
                        p.name = getSelf().getImageCachePoolName();
                        p.predefined = FusionstorSystemTags.PREDEFINED_PRIMARY_STORAGE_IMAGE_CACHE_POOL.hasTag(self.getUuid());
                        pools.add(p);

                        p = new Pool();
                        p.name = getSelf().getRootVolumePoolName();
                        p.predefined = FusionstorSystemTags.PREDEFINED_PRIMARY_STORAGE_ROOT_VOLUME_POOL.hasTag(self.getUuid());
                        pools.add(p);

                        p = new Pool();
                        p.name = getSelf().getDataVolumePoolName();
                        p.predefined = FusionstorSystemTags.PREDEFINED_PRIMARY_STORAGE_DATA_VOLUME_POOL.hasTag(self.getUuid());
                        pools.add(p);

                        cmd.pools = pools;
                        cmd.monHostnames = monHostnames;
                        cmd.sshUsernames = sshUsernames;
                        cmd.sshPasswords = sshPasswords;
                        cmd.fusionstorType = FusionstorGlobalProperty.FUSIONSTOR_PRIMARY_STORAGE_TYPE;

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

                                FusionstorCapacityUpdater updater = new FusionstorCapacityUpdater();
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
                            PrimaryStorageVO vo = dbf.reload(self);
                            if (vo != null) {
                                self = vo;
                            }
                            if (!getSelf().getMons().isEmpty()) {
                                dbf.removeCollection(getSelf().getMons(), FusionstorPrimaryStorageMonVO.class);
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
        final List<FusionstorPrimaryStorageMonBase> mons = CollectionUtils.transformToList(getSelf().getMons(), new Function<FusionstorPrimaryStorageMonBase, FusionstorPrimaryStorageMonVO>() {
            @Override
            public FusionstorPrimaryStorageMonBase call(FusionstorPrimaryStorageMonVO arg) {
                return new FusionstorPrimaryStorageMonBase(arg);
            }
        });

        final List<ErrorCode> errors = new ArrayList<ErrorCode>();

        class Ping {
            private AtomicBoolean replied = new AtomicBoolean(false);

            @AsyncThread
            private void reconnectMon(final FusionstorPrimaryStorageMonBase mon, boolean delay) {
                if (!FusionstorGlobalConfig.PRIMARY_STORAGE_MON_AUTO_RECONNECT.value(Boolean.class)) {
                    logger.debug(String.format("do not reconnect the fusionstor primary storage mon[uuid:%s] as the global config[%s] is set to false",
                            self.getUuid(), FusionstorGlobalConfig.PRIMARY_STORAGE_MON_AUTO_RECONNECT.getCanonicalName()));
                    return;
                }

                // there has been a reconnect in process
                if (!reconnectMonLock.lock()) {
                    logger.debug(String.format("ignore this call, reconnect fusionstor primary storage mon[uuid:%s] is in process", self.getUuid()));
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
                            TimeUnit.SECONDS.sleep(FusionstorGlobalConfig.PRIMARY_STORAGE_MON_RECONNECT_DELAY.value(Long.class));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    mon.connect(new Completion(releaseLock) {
                        @Override
                        public void success() {
                            logger.debug(String.format("successfully reconnected the mon[uuid:%s] of the fusionstor primary" +
                                    " storage[uuid:%s, name:%s]", mon.getSelf().getUuid(), self.getUuid(), self.getName()));
                            releaseLock.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            //TODO
                            logger.warn(String.format("failed to reconnect the mon[uuid:%s] of the fusionstor primary" +
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

                        ErrorCode err = errf.stringToOperationError(String.format("failed to ping the fusionstor primary storage[uuid:%s, name:%s]",
                                self.getUuid(), self.getName()), errors);
                        completion.fail(err);
                    }
                });

                for (final FusionstorPrimaryStorageMonBase mon : mons) {
                    mon.ping(new ReturnValueCompletion<PingResult>(latch) {
                        private void thisMonIsDown(ErrorCode err) {
                            //TODO
                            logger.warn(String.format("cannot ping mon[uuid:%s] of the fusionstor primary storage[uuid:%s, name:%s], %s",
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

                            } else if (res.operationFailure) {
                                // as long as there is one mon saying the fusionstor not working, the primary storage goes down
                                logger.warn(String.format("the fusionstor primary storage[uuid:%s, name:%s] is down, as one mon[uuid:%s] reports" +
                                        " an operation failure[%s]", self.getUuid(), self.getName(), mon.getSelf().getUuid(), res.error));
                                ErrorCode errorCode = errf.stringToOperationError(res.error);
                                errors.add(errorCode);
                                primaryStorageDown();
                            } else {
                                // this mon is down(success == false, operationFailure == false), but the primary storage may still work as other mons may work
                                ErrorCode errorCode = errf.stringToOperationError(res.error);
                                thisMonIsDown(errorCode);
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
                for (FusionstorPrimaryStorageMonBase base : mons) {
                    base.changeStatus(MonStatus.Disconnected);
                }

                ErrorCode err = errf.stringToOperationError(String.format("failed to ping the fusionstor primary storage[uuid:%s, name:%s]",
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
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    @Override
    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIAddMonToFusionstorPrimaryStorageMsg) {
            handle((APIAddMonToFusionstorPrimaryStorageMsg) msg);
        } else if (msg instanceof APIRemoveMonFromFusionstorPrimaryStorageMsg) {
            handle((APIRemoveMonFromFusionstorPrimaryStorageMsg) msg);
        } else if (msg instanceof APIUpdateFusionstorPrimaryStorageMonMsg) {
            handle((APIUpdateFusionstorPrimaryStorageMonMsg)msg);
        } else {
            super.handleApiMessage(msg);
        }
    }

    private void handle(APIUpdateFusionstorPrimaryStorageMonMsg msg) {
        final APIUpdateMonToFusionstorPrimaryStorageEvent evt = new APIUpdateMonToFusionstorPrimaryStorageEvent(msg.getId());
        FusionstorPrimaryStorageMonVO monvo = dbf.findByUuid(msg.getMonUuid(), FusionstorPrimaryStorageMonVO.class);
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
        evt.setInventory(FusionstorPrimaryStorageInventory.valueOf((dbf.reload(getSelf()))));
        bus.publish(evt);
    }

    private void handle(APIRemoveMonFromFusionstorPrimaryStorageMsg msg) {
        APIRemoveMonFromFusionstorPrimaryStorageEvent evt = new APIRemoveMonFromFusionstorPrimaryStorageEvent(msg.getId());

        SimpleQuery<FusionstorPrimaryStorageMonVO> q = dbf.createQuery(FusionstorPrimaryStorageMonVO.class);
        q.add(FusionstorPrimaryStorageMonVO_.hostname, Op.IN, msg.getMonHostnames());
        List<FusionstorPrimaryStorageMonVO> vos = q.list();

        dbf.removeCollection(vos, FusionstorPrimaryStorageMonVO.class);
        evt.setInventory(FusionstorPrimaryStorageInventory.valueOf(dbf.reload(getSelf())));
        bus.publish(evt);
    }

    private void handle(final APIAddMonToFusionstorPrimaryStorageMsg msg) {
        final APIAddMonToFusionstorPrimaryStorageEvent evt = new APIAddMonToFusionstorPrimaryStorageEvent(msg.getId());

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("add-mon-fusionstor-primary-storage-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            List<FusionstorPrimaryStorageMonVO> monVOs = new ArrayList<FusionstorPrimaryStorageMonVO>();

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "create-mon-in-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        for (String url : msg.getMonUrls()) {
                            FusionstorPrimaryStorageMonVO monvo = new FusionstorPrimaryStorageMonVO();
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
                    public void rollback(FlowRollback trigger, Map data) {
                        dbf.removeCollection(monVOs, FusionstorPrimaryStorageMonVO.class);
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "connect-mons";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        List<FusionstorPrimaryStorageMonBase> bases = CollectionUtils.transformToList(monVOs, new Function<FusionstorPrimaryStorageMonBase, FusionstorPrimaryStorageMonVO>() {
                            @Override
                            public FusionstorPrimaryStorageMonBase call(FusionstorPrimaryStorageMonVO arg) {
                                return new FusionstorPrimaryStorageMonBase(arg);
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

                        for (FusionstorPrimaryStorageMonBase base : bases) {
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
                        List<FusionstorPrimaryStorageMonBase> bases = CollectionUtils.transformToList(monVOs, new Function<FusionstorPrimaryStorageMonBase, FusionstorPrimaryStorageMonVO>() {
                            @Override
                            public FusionstorPrimaryStorageMonBase call(FusionstorPrimaryStorageMonVO arg) {
                                return new FusionstorPrimaryStorageMonBase(arg);
                            }
                        });

                        final List<ErrorCode> errors = new ArrayList<ErrorCode>();

                        final AsyncLatch latch = new AsyncLatch(bases.size(), new NoErrorCompletion(trigger) {
                            @Override
                            public void done() {
                                // one fail, all fail
                                if (!errors.isEmpty()) {
                                    trigger.fail(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR, "unable to add mon to fusionstor primary storage", errors));
                                } else {
                                    trigger.next();
                                }
                            }
                        });

                        for (final FusionstorPrimaryStorageMonBase base : bases) {
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
                        evt.setInventory(FusionstorPrimaryStorageInventory.valueOf(dbf.reload(getSelf())));
                        bus.publish(evt);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setError(errCode);
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
        } else {
            super.handleLocalMessage(msg);
        }
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
        cmd.heartbeatImagePath = String.format("%s/fusionstor-primary-storage-%s-heartbeat-file", getSelf().getRootVolumePoolName(), self.getUuid());
        cmd.monUrls = CollectionUtils.transformToList(getSelf().getMons(), new Function<String, FusionstorPrimaryStorageMonVO>() {
            @Override
            public String call(FusionstorPrimaryStorageMonVO arg) {
                return String.format("%s:%s", arg.getHostname(), arg.getMonPort());
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

        if (!FusionstorConstants.FUSIONSTOR_BACKUP_STORAGE_TYPE.equals(bsType)) {
            throw new OperationFailureException(errf.stringToOperationError(
                    String.format("unable to upload bits to the backup storage[type:%s], we only support FUSIONSTOR", bsType)
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

                // current fusionstor has no way to get the actual size
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

    protected  void handle(final RevertVolumeFromSnapshotOnPrimaryStorageMsg msg) {
        final RevertVolumeFromSnapshotOnPrimaryStorageReply reply = new RevertVolumeFromSnapshotOnPrimaryStorageReply();

        final FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("revert-volume-[uuid:%s]-from-snapshot-[uuid:%s]-on-fusionstor-primary-storage",
                msg.getVolume().getUuid(), msg.getSnapshot().getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                String originalVolumePath = msg.getVolume().getInstallPath();
                // get volume path from snapshot path, just split @
                String volumePath = msg.getSnapshot().getPrimaryStorageInstallPath().split("@")[0];

                flow(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        RollbackSnapshotCmd cmd = new RollbackSnapshotCmd();
                        cmd.snapshotPath = msg.getSnapshot().getPrimaryStorageInstallPath();
                        httpCall(ROLLBACK_SNAPSHOT_PATH, cmd, RollbackSnapshotRsp.class, new ReturnValueCompletion<RollbackSnapshotRsp>(msg) {
                            @Override
                            public void success(RollbackSnapshotRsp returnValue) {
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
                    String __name__ = "delete-origin-root-volume-which-has-no-snapshot";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        SimpleQuery<VolumeSnapshotVO> sq = dbf.createQuery(VolumeSnapshotVO.class);
                        sq.add(VolumeSnapshotVO_.primaryStorageInstallPath, Op.LIKE,
                                String.format("%s%%", originalVolumePath));
                        sq.count();
                        if (sq.count() == 0) {
                            DeleteCmd cmd = new DeleteCmd();
                            cmd.installPath = originalVolumePath;
                            httpCall(DELETE_PATH, cmd, DeleteRsp.class, new ReturnValueCompletion<DeleteRsp>(null) {
                                @Override
                                public void success(DeleteRsp returnValue) {
                                    logger.debug(String.format("successfully deleted %s", originalVolumePath));
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    //TODO GC
                                    logger.warn(String.format("unable to delete %s, %s. Need a cleanup",
                                            originalVolumePath, errorCode));
                                }
                            });
                        }
                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        reply.setNewVolumeInstallPath(volumePath);
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

    protected void handle(final ReInitRootVolumeFromTemplateOnPrimaryStorageMsg msg) {
        final ReInitRootVolumeFromTemplateOnPrimaryStorageReply reply = new ReInitRootVolumeFromTemplateOnPrimaryStorageReply();

        final FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("reimage-vm-root-volume-%s", msg.getVolume().getUuid()));
        chain.then(new ShareFlow() {
            String cloneInstallPath;
            String originalVolumePath = msg.getVolume().getInstallPath();
            String volumePath = makeResetImageRootVolumeInstallPath(msg.getVolume().getUuid());
            ImageCacheVO cache;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "clone-image";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        SimpleQuery<ImageCacheVO> sq = dbf.createQuery(ImageCacheVO.class);
                        sq.add(ImageCacheVO_.imageUuid, Op.EQ, msg.getVolume().getRootImageUuid());
                        sq.add(ImageCacheVO_.primaryStorageUuid, Op.EQ, msg.getPrimaryStorageUuid());
                        ImageCacheVO ivo = sq.find();

                        CloneCmd cmd = new CloneCmd();
                        cmd.srcPath = ivo.getInstallUrl();
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

                flow(new NoRollbackFlow() {
                    String __name__ = "delete-origin-root-volume-which-has-no-snapshot";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        SimpleQuery<VolumeSnapshotVO> sq = dbf.createQuery(VolumeSnapshotVO.class);
                        sq.add(VolumeSnapshotVO_.primaryStorageInstallPath, Op.LIKE,
                                String.format("%s%%", originalVolumePath));
                        sq.count();
                        if (sq.count() == 0) {
                            DeleteCmd cmd = new DeleteCmd();
                            cmd.installPath = originalVolumePath;
                            httpCall(DELETE_PATH, cmd, DeleteRsp.class, new ReturnValueCompletion<DeleteRsp>(null) {
                                @Override
                                public void success(DeleteRsp returnValue) {
                                    logger.debug(String.format("successfully deleted %s", originalVolumePath));
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    //TODO GC
                                    logger.warn(String.format("unable to delete %s, %s. Need a cleanup",
                                            originalVolumePath, errorCode));
                                }
                            });
                        }
                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        reply.setNewVolumeInstallPath(volumePath);
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
    protected void handle(final DeleteSnapshotOnPrimaryStorageMsg msg) {
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

    protected  void handle(MergeVolumeSnapshotOnPrimaryStorageMsg msg) {
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
                // current fusionstor has no way to get actual size
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
        completion.success();
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
        if (FusionstorGlobalConfig.PRIMARY_STORAGE_DELETE_POOL.value(Boolean.class)) {
            DeletePoolCmd cmd = new DeletePoolCmd();
            cmd.poolNames = list(getSelf().getImageCachePoolName(), getSelf().getDataVolumePoolName(), getSelf().getRootVolumePoolName());
            FutureReturnValueCompletion completion = new FutureReturnValueCompletion(null);
            httpCall(DELETE_POOL_PATH, cmd, DeletePoolRsp.class, completion);
            completion.await(TimeUnit.MINUTES.toMillis(30));
            if (!completion.isSuccess()) {
                throw new OperationFailureException(completion.getErrorCode());
            }
        }
        dbf.removeCollection(getSelf().getMons(), FusionstorPrimaryStorageMonVO.class);
    }
}
