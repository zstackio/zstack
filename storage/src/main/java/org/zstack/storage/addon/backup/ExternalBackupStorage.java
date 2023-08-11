package org.zstack.storage.addon.backup;

import org.zstack.header.core.Completion;
import org.zstack.header.image.CancelDownloadImageMsg;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.addon.backup.BackupStorageController;
import org.zstack.header.storage.addon.backup.ExternalBackupStorageInventory;
import org.zstack.header.storage.addon.backup.ExternalBackupStorageVO;
import org.zstack.header.storage.backup.*;
import org.zstack.storage.backup.BackupStorageBase;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

// ExternalBackupStorage is an Addon Backup Storage Instance (`absi').
//
// It is here to serve as a bridge of message based control flow to
// external Backup Storage Interface (`bsi').
public class ExternalBackupStorage extends BackupStorageBase {
    private static final CLogger logger = Utils.getLogger(ExternalBackupStorage.class);

    private final BackupStorageController controller;

    public ExternalBackupStorage(ExternalBackupStorageVO vo, BackupStorageController controller) {
        super(vo);
        this.controller = controller;
    }

    private ExternalBackupStorageVO getSelf() {
        return (ExternalBackupStorageVO) self;
    }

    protected BackupStorageInventory getSelfInventory() {
        return ExternalBackupStorageInventory.valueOf(getSelf());
    }

    @Override
    protected void handle(final DownloadImageMsg msg) {
        bus.replyErrorByMessageType(msg, "not supported");
    }

    @Override
    protected void handle(final CancelDownloadImageMsg msg) {
        bus.replyErrorByMessageType(msg, "not supported");
    }

    @Override
    protected void handle(final DownloadVolumeMsg msg) {
        bus.replyErrorByMessageType(msg, "not supported");
    }

    @Override
    protected void connectHook(boolean newAdded, Completion completion) {
        logger.info(String.format("connecting bs[%s], newly added: %s", getSelf().getName(), newAdded));
        completion.success();
    }

    @Override
    protected void pingHook(final Completion completion) {
        logger.info(String.format("ping bs[%s]", getSelf().getName()));
        completion.success();
    }

    @Override
    public List<ImageInventory> scanImages() {
        return null;
    }

    @Override
    protected void handle(final DeleteBitsOnBackupStorageMsg msg) {
        bus.replyErrorByMessageType(msg, "not supported");
    }

    @Override
    protected void handle(GetImageSizeOnBackupStorageMsg msg) {
        bus.replyErrorByMessageType(msg, "not supported");
    }

    @Override
    protected void handle(BackupStorageAskInstallPathMsg msg) {
        bus.replyErrorByMessageType(msg, "not supported");
    }

    @Override
    protected void handle(final SyncImageSizeOnBackupStorageMsg msg) {
        bus.replyErrorByMessageType(msg, "not supported");
    }

    @Override
    protected void handle(GetLocalFileSizeOnBackupStorageMsg msg) {
        bus.replyErrorByMessageType(msg, "not supported");
    }

    @Override
    protected void handle(GetImageEncryptedOnBackupStorageMsg msg) {
        bus.replyErrorByMessageType(msg, "not supported");
    }

    @Override
    protected void handle(RestoreImagesBackupStorageMetadataToDatabaseMsg msg) {
        bus.replyErrorByMessageType(msg, "not supported");
    }
}
