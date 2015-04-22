package org.zstack.storage.primary.iscsi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageType;
import org.zstack.header.storage.primary.ImageCacheInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageType;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.KVMConstant;
import org.zstack.storage.backup.sftp.GetSftpBackupStorageDownloadCredentialMsg;
import org.zstack.storage.backup.sftp.GetSftpBackupStorageDownloadCredentialReply;
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant;
import org.zstack.storage.primary.iscsi.IscsiFileSystemBackendPrimaryStorageCommands.DownloadBitsFromSftpBackupStorageCmd;
import org.zstack.storage.primary.iscsi.IscsiFileSystemBackendPrimaryStorageCommands.DownloadBitsFromSftpBackupStorageRsp;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 4/19/2015.
 */
public class IscsiFileSystemBackendPrimaryToSftpBackupStorageMediator implements IscsiFileSystemBackendPrimaryToBackupStorageMediator {
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private RESTFacade restf;

    @Override
    public void createVolumeFromImageCache(PrimaryStorageInventory primaryStorage, ImageCacheInventory image, VolumeInventory volume, ReturnValueCompletion<String> completion) {

    }

    private String makeHttpUrl(IscsiFileSystemBackendPrimaryStorageInventory inv, String path) {
        UriComponentsBuilder ub = UriComponentsBuilder.newInstance();
        ub.scheme("http");
        ub.host(inv.getHostname());
        ub.port(IscsiFileSystemBackendPrimaryStorageGlobalProperty.AGENT_PORT);
        ub.path(inv.getFilesystemType());
        ub.path(path);
        return ub.build().toUriString();
    }

    @Override
    public void downloadBits(final PrimaryStorageInventory pinv, BackupStorageInventory bsinv, final String backupStorageInstallPath, final String primaryStorageInstallPath, final Completion completion) {
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
                DownloadBitsFromSftpBackupStorageCmd cmd = new DownloadBitsFromSftpBackupStorageCmd();
                cmd.setHostname(greply.getHostname());
                cmd.setSshKey(greply.getSshKey());
                cmd.setPrimaryStorageInstallPath(primaryStorageInstallPath);
                cmd.setBackupStorageInstallPath(backupStorageInstallPath);

                restf.asyncJsonPost(makeHttpUrl((IscsiFileSystemBackendPrimaryStorageInventory) pinv, IscsiBtrfsPrimaryStorageConstants.DOWNLOAD_FROM_SFTP_PATH),
                        cmd, new JsonAsyncRESTCallback<DownloadBitsFromSftpBackupStorageRsp>() {
                            @Override
                            public void fail(ErrorCode err) {
                                completion.fail(err);
                            }

                            @Override
                            public void success(DownloadBitsFromSftpBackupStorageRsp ret) {
                                if (ret.isSuccess()) {
                                    completion.success();
                                } else {
                                    completion.fail(errf.stringToOperationError(ret.getError()));
                                }
                            }

                            @Override
                            public Class<DownloadBitsFromSftpBackupStorageRsp> getReturnClass() {
                                return DownloadBitsFromSftpBackupStorageRsp.class;
                            }
                        });
            }
        });
    }

    @Override
    public void uploadBits(PrimaryStorageInventory pinv, BackupStorageInventory bsinv, String backupStorageInstallPath, String primaryStorageInstallPath, Completion completion) {

    }

    @Override
    public String makeRootVolumeTemplateInstallPath(String backupStorageUuid, String imageUuid) {
        return null;
    }

    @Override
    public String makeVolumeSnapshotInstallPath(String backupStorageUuid, String snapshotUuid) {
        return null;
    }

    @Override
    public PrimaryStorageType getSupportedPrimaryStorageType() {
        return IscsiFileSystemBackendPrimaryStorageFactory.type;
    }

    @Override
    public BackupStorageType getSupportedBackupStorageType() {
        return BackupStorageType.valueOf(SftpBackupStorageConstant.SFTP_BACKUP_STORAGE_TYPE);
    }

    @Override
    public List<HypervisorType> getSupportedHypervisorTypes() {
        return list(HypervisorType.valueOf(KVMConstant.KVM_HYPERVISOR_TYPE));
    }
}
