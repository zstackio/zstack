package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageType;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageType;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.storage.backup.sftp.GetSftpBackupStorageDownloadCredentialMsg;
import org.zstack.storage.backup.sftp.GetSftpBackupStorageDownloadCredentialReply;
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 7/1/2015.
 */
public class LocalStorageKvmSftpBackupStorageMediatorImpl implements LocalStorageBackupStorageMediator {
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;

    public static final String UPLOAD_BIT_PATH = "/localstorage/sftp/upload";
    public static final String DOWNLOAD_BIT_PATH = "/localstorage/sftp/download";

    public static class SftpDownloadBitsCmd extends LocalStorageKvmBackend.AgentCommand {
        private String sshKey;
        private String hostname;
        private String backupStorageInstallPath;
        private String primaryStorageInstallPath;

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

    public static class SftpDownloadBitsRsp extends LocalStorageKvmBackend.AgentResponse {

    }

    public static class SftpUploadBitsCmd extends LocalStorageKvmBackend.AgentCommand {
        private String primaryStorageInstallPath;
        private String backupStorageInstallPath;
        private String hostname;
        private String sshKey;

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

    public static class SftpUploadBitsRsp extends LocalStorageKvmBackend.AgentResponse {

    }

    public void downloadBits(final PrimaryStorageInventory pinv, BackupStorageInventory bsinv, final String backupStorageInstallPath, final String primaryStorageInstallPath, final String hostUuid, final Completion completion) {
        GetSftpBackupStorageDownloadCredentialMsg gmsg = new GetSftpBackupStorageDownloadCredentialMsg();
        gmsg.setBackupStorageUuid(bsinv.getUuid());
        bus.makeTargetServiceIdByResourceUuid(gmsg, BackupStorageConstant.SERVICE_ID, bsinv.getUuid());
        bus.send(gmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                final GetSftpBackupStorageDownloadCredentialReply greply = reply.castReply();
                SftpDownloadBitsCmd cmd = new SftpDownloadBitsCmd();
                cmd.setHostname(greply.getHostname());
                cmd.setSshKey(greply.getSshKey());
                cmd.setBackupStorageInstallPath(backupStorageInstallPath);
                cmd.setPrimaryStorageInstallPath(primaryStorageInstallPath);

                KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                msg.setHostUuid(hostUuid);
                msg.setPath(DOWNLOAD_BIT_PATH);
                msg.setCommand(cmd);
                msg.setCommandTimeout(LocalStorageGlobalProperty.KVM_SftpDownloadBitsCmd_TIMEOUT);
                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
                bus.send(msg, new CloudBusCallBack(completion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            completion.fail(reply.getError());
                            return;
                        }

                        KVMHostAsyncHttpCallReply kr = reply.castReply();
                        SftpDownloadBitsRsp rsp = kr.toResponse(SftpDownloadBitsRsp.class);
                        if (!rsp.isSuccess()) {
                            completion.fail(errf.stringToOperationError(
                                    String.format("failed to download bits from the SFTP backup storage[hostname:%s, path: %s] to the local primary storage[uuid:%s, path: %s], %s",
                                            greply.getHostname(), backupStorageInstallPath, pinv.getUuid(), primaryStorageInstallPath, rsp.getError())
                            ));
                            return;
                        }

                        completion.success();
                    }
                });
            }
        });
    }

    @Override
    public void uploadBits(final PrimaryStorageInventory pinv, BackupStorageInventory bsinv, final String backupStorageInstallPath, final String primaryStorageInstallPath, final String hostUuid, final Completion completion) {
        GetSftpBackupStorageDownloadCredentialMsg gmsg = new GetSftpBackupStorageDownloadCredentialMsg();
        gmsg.setBackupStorageUuid(bsinv.getUuid());
        bus.makeTargetServiceIdByResourceUuid(gmsg, BackupStorageConstant.SERVICE_ID, bsinv.getUuid());
        bus.send(gmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                final GetSftpBackupStorageDownloadCredentialReply r = reply.castReply();
                SftpUploadBitsCmd cmd = new SftpUploadBitsCmd();
                cmd.setPrimaryStorageInstallPath(primaryStorageInstallPath);
                cmd.setBackupStorageInstallPath(backupStorageInstallPath);
                cmd.setHostname(r.getHostname());
                cmd.setSshKey(r.getSshKey());

                KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                msg.setCommand(cmd);
                msg.setPath(UPLOAD_BIT_PATH);
                msg.setHostUuid(hostUuid);
                msg.setCommandTimeout(LocalStorageGlobalProperty.KVM_SftpUploadBitsCmd_TIMEOUT);
                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
                bus.send(msg, new CloudBusCallBack(completion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            completion.fail(reply.getError());
                            return;
                        }

                        KVMHostAsyncHttpCallReply kr = reply.castReply();
                        SftpUploadBitsRsp rsp = kr.toResponse(SftpUploadBitsRsp.class);
                        if (!rsp.isSuccess()) {
                            completion.fail(errf.stringToOperationError(
                                    String.format("failed to upload bits from the local storage[uuid:%s, path:%s] to the SFTP backup storage[hostname:%s, path:%s], %s",
                                            pinv.getUuid(), primaryStorageInstallPath, r.getHostname(), backupStorageInstallPath, rsp.getError())
                            ));
                            return;
                        }

                        completion.success();
                    }
                });
            }
        });
    }

    @Override
    public PrimaryStorageType getSupportedPrimaryStorageType() {
        return LocalStorageFactory.type;
    }

    @Override
    public BackupStorageType getSupportedBackupStorageType() {
        return new BackupStorageType(SftpBackupStorageConstant.SFTP_BACKUP_STORAGE_TYPE);
    }

    @Override
    public List<HypervisorType> getSupportedHypervisorTypes() {
        return list(new HypervisorType(KVMConstant.KVM_HYPERVISOR_TYPE));
    }
}
