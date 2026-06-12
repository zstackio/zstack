package org.zstack.storage.primary.smp;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.kvm.KvmOperationEndpointSelector.Endpoint;
import org.zstack.storage.backup.sftp.GetSftpBackupStorageDownloadCredentialMsg;
import org.zstack.storage.backup.sftp.GetSftpBackupStorageDownloadCredentialReply;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_STORAGE_PRIMARY_SMP_10012;

/**
 * @ Author : yh.w
 * @ Date   : Created in 17:08 2023/7/7
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE, dependencyCheck = true)
public class SftpBackupStorageKvmUploader extends BackupStorageKvmUploader {
    @Autowired
    private CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    private SMPPrimaryStorageFactory primaryStorageFactory;

    private PrimaryStorageInventory pinv;
    private String bsUuid;

    public static final String UPLOAD_BITS_TO_SFTP_BACKUPSTORAGE_PATH = "/sharedmountpointprimarystorage/sftp/upload";

    public SftpBackupStorageKvmUploader(PrimaryStorageInventory ps, String backupStorageUuid) {
        pinv = ps;
        bsUuid = backupStorageUuid;
    }

    public static SftpBackupStorageKvmUploader createUploader(PrimaryStorageInventory ps, String bsUuid) {
        return new SftpBackupStorageKvmUploader(ps, bsUuid);
    }

    @Override
    public void uploadBits(final String imageUuid, final String bsPath, final String psPath, final ReturnValueCompletion<String> completion) {
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
                KvmBackend.SftpUploadBitsCmd cmd = new KvmBackend.SftpUploadBitsCmd();
                cmd.primaryStorageInstallPath = psPath;
                cmd.backupStorageInstallPath = bsPath;
                cmd.hostname = r.getHostname();
                cmd.username = r.getUsername();
                cmd.sshKey = r.getSshKey();
                cmd.sshPort = r.getSshPort();
                cmd.primaryStorageUuid = pinv.getUuid();

                KvmAgentCommandDispatcher dispatcher;
                try {
                    dispatcher = KvmAgentCommandDispatcher.createForOperation(
                            pinv.getUuid(),
                            "smp-sftp-upload",
                            getCandidateHostUuids(),
                            Arrays.asList(Endpoint.backupStorage("sftp backup storage", bsUuid, r.getHostname())),
                            ORG_ZSTACK_STORAGE_PRIMARY_SMP_10012);
                } catch (OperationFailureException e) {
                    completion.fail(e.getErrorCode());
                    return;
                }

                dispatcher.go(UPLOAD_BITS_TO_SFTP_BACKUPSTORAGE_PATH, cmd, KvmBackend.AgentRsp.class, new ReturnValueCompletion<KvmBackend.AgentRsp>(completion) {
                    @Override
                    public void success(KvmBackend.AgentRsp returnValue) {
                        completion.success(bsPath);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
            }
        });
    }

    private List<String> getCandidateHostUuids() {
        List<String> ret = new ArrayList<String>();
        for (HostInventory host : primaryStorageFactory.getConnectedHostForOperation(pinv)) {
            ret.add(host.getUuid());
        }
        return ret;
    }
}
