package org.zstack.storage.primary.smp;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.kvm.KvmOperationEndpointSelector;
import org.zstack.kvm.KvmOperationEndpointSelector.Endpoint;
import org.zstack.kvm.KvmOperationEndpointSelector.Selection;
import org.zstack.storage.backup.sftp.GetSftpBackupStorageDownloadCredentialMsg;
import org.zstack.storage.backup.sftp.GetSftpBackupStorageDownloadCredentialReply;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_STORAGE_PRIMARY_SMP_10012;

/**
 * @ Author : yh.w
 * @ Date   : Created in 17:14 2023/7/7
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE, dependencyCheck = true)
public class SftpBackupStorageKvmDownloader extends BackupStorageKvmDownloader {
    @Autowired
    private CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    private SMPPrimaryStorageFactory primaryStorageFactory;

    private PrimaryStorageInventory pinv;
    private String bsUuid;

    public static final String DOWNLOAD_BITS_FROM_SFTP_BACKUPSTORAGE_PATH = "/sharedmountpointprimarystorage/sftp/download";

    public SftpBackupStorageKvmDownloader(PrimaryStorageInventory ps, String backupStorageUuid) {
        pinv = ps;
        bsUuid = backupStorageUuid;
    }

    public static SftpBackupStorageKvmDownloader createDownloader(PrimaryStorageInventory ps, String bsUuid) {
        return new SftpBackupStorageKvmDownloader(ps, bsUuid);
    }

    @Override
    public void downloadBits(final String bsPath, final String psPath, boolean isData, final Completion completion) {
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

                final GetSftpBackupStorageDownloadCredentialReply greply = reply.castReply();
                Selection selection;
                KvmAgentCommandDispatcher dispatcher;
                try {
                    selection = KvmOperationEndpointSelector.selectForTargetEndpoint(
                            "smp-sftp-download",
                            primaryStorageFactory.getConnectedHostForOperation(pinv),
                            new ArrayList<Endpoint>(),
                            KvmOperationEndpointSelector.backupStorageEndpoints("sftp backup storage", bsUuid, greply.getEndpointCandidates(), greply.getHostname()),
                            ORG_ZSTACK_STORAGE_PRIMARY_SMP_10012);
                    dispatcher = new KvmAgentCommandDispatcher(pinv.getUuid(), selection.getSelectedHostUuids());
                } catch (OperationFailureException e) {
                    completion.fail(e.getErrorCode());
                    return;
                }
                KvmBackend.SftpDownloadBitsCmd cmd = new KvmBackend.SftpDownloadBitsCmd();
                cmd.hostname = selection.getSelectedBackupStorageAddress();
                cmd.username = greply.getUsername();
                cmd.sshKey = greply.getSshKey();
                cmd.sshPort = greply.getSshPort();
                cmd.backupStorageInstallPath = bsPath;
                cmd.primaryStorageInstallPath = psPath;
                cmd.primaryStorageUuid = pinv.getUuid();

                dispatcher.go(DOWNLOAD_BITS_FROM_SFTP_BACKUPSTORAGE_PATH, cmd, KvmBackend.AgentRsp.class, new ReturnValueCompletion<KvmBackend.AgentRsp>(completion) {
                    @Override
                    public void success(KvmBackend.AgentRsp returnValue) {
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

    private List<String> getCandidateHostUuids() {
        List<String> ret = new ArrayList<String>();
        for (HostInventory host : primaryStorageFactory.getConnectedHostForOperation(pinv)) {
            ret.add(host.getUuid());
        }
        return ret;
    }
}
