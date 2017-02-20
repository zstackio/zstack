package org.zstack.simulator.storage.backup;

import org.zstack.core.Platform;
import org.zstack.header.core.Completion;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.backup.*;
import org.zstack.storage.backup.BackupStorageBase;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

public class SimulatorBackupStorage extends BackupStorageBase {
    private static final CLogger logger = Utils.getLogger(SimulatorBackupStorage.class);

    public SimulatorBackupStorage(BackupStorageVO self) {
        super(self);
    }

    @Override
    public void deleteHook() {
        logger.debug(String.format("SimulatorBackupStorage[uuid:%s] gets deleted", self.getUuid()));
    }

    @Override
    public void changeStateHook(BackupStorageStateEvent evt, BackupStorageState nextState) {
        logger.debug(String.format("SimulatorBackupStorage[uuid:%s] changes state from %s to %s", self.getUuid(), self.getState(), nextState));
    }

    @Override
    public void detachHook(Completion completion) {
        logger.debug(String.format("SimulatorBackupStorage[uuid:%s] detached", self.getUuid()));
        completion.success();
    }

    @Override
    public void attachHook(String zoneUuid, Completion completion) {
        logger.debug(String.format("SimulatorBackupStorage[uuid:%s] attached", self.getUuid()));
        completion.success();
    }

    @Override
    protected void exceptionIfImageSizeGreaterThanAvailableCapacity(String url) {
        // To override the behavior of the base class, thus avoid real HEAD request.
    }

    @Override
    protected void handle(GetImageSizeOnBackupStorageMsg msg) {
        GetImageSizeOnBackupStorageReply reply = new GetImageSizeOnBackupStorageReply();
        reply.setSize(233);
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(DownloadImageMsg msg) {
        ImageInventory inv = msg.getImageInventory();
        DownloadImageReply reply = new DownloadImageReply();
        reply.setMd5sum(Platform.getUuid());
        reply.setInstallPath(Utils.getPathUtil().join(self.getUrl(), inv.getName()));
        reply.setSize(100);
        reply.setActualSize(100L);
        reply.setFormat("simulator");
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(DownloadVolumeMsg msg) {
        DownloadVolumeReply reply = new DownloadVolumeReply();
        reply.setMd5sum(Platform.getUuid());
        reply.setInstallPath(Utils.getPathUtil().join(self.getUrl(), msg.getVolume().getName()));
        reply.setSize(100);
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(DeleteBitsOnBackupStorageMsg msg) {
        DeleteBitsOnBackupStorageReply reply = new DeleteBitsOnBackupStorageReply();
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(BackupStorageAskInstallPathMsg msg) {
        BackupStorageAskInstallPathReply reply = new BackupStorageAskInstallPathReply();
        reply.setInstallPath(String.format("/%s/%s/%s.img", msg.getImageMediaType(), msg.getImageUuid(), msg.getImageUuid()));
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(SyncImageSizeOnBackupStorageMsg msg) {
        throw new CloudRuntimeException("not supported yet");
    }

    @Override
    protected void connectHook(boolean newAdded, Completion completion) {
        completion.success();
    }

    @Override
    protected void pingHook(Completion completion) {
        completion.success();
    }

    @Override
    public List<ImageInventory> scanImages() {
        return null;
    }
}
