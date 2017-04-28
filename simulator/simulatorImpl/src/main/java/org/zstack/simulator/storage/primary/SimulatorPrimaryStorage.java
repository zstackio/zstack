package org.zstack.simulator.storage.primary;

import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.simulator.SimulatorConstant;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.primary.VolumeSnapshotCapability.VolumeSnapshotArrangementType;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.storage.primary.PrimaryStorageBase;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtils;


public class SimulatorPrimaryStorage extends PrimaryStorageBase {
    private static final CLogger logger = Utils.getLogger(SimulatorPrimaryStorage.class);
    private static final PathUtils putil = Utils.getPathUtil();

    public SimulatorPrimaryStorage(PrimaryStorageVO self) {
        super(self);
    }

    @Override
    public void deleteHook() {
        logger.debug(String.format("SimulatorPrimaryStorage[uuid:%s] gets deleted", self.getUuid()));
    }

    @Override
    public void changeStateHook(PrimaryStorageStateEvent evt, PrimaryStorageState nextState) {
        logger.debug(String.format("SimulatorPrimaryStorage[uuid:%s] changes state from %s to %s", self.getUuid(), self.getState(), nextState));
    }

    @Override
    public void attachHook(String clusterUuid, Completion completion) {
        logger.debug(String.format("SimulatorPrimaryStorage[uuid:%s] attached", self.getUuid()));
        completion.success();
    }

    @Override
    public void detachHook(String clusterUuid, Completion completion) {
        logger.debug(String.format("SimulatorPrimaryStorage[uuid:%s] detached from cluster[uuid:%s]", self.getUuid(), clusterUuid));
        completion.success();
    }

    private VolumeInventory instantiateVolume(VolumeInventory vol) {
        String root = self.getUrl();
        String path = putil.join(root, PrimaryStorageConstant.VM_FOLDER, vol.getUuid() + ".qcow2");
        vol.setInstallPath(path);
        return vol;
    }

    private void handle(InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg msg) {
        InstantiateVolumeOnPrimaryStorageReply reply = new InstantiateVolumeOnPrimaryStorageReply();
        VolumeInventory vol = instantiateVolume(msg.getVolume());
        vol.setFormat(SimulatorConstant.SIMULATOR_VOLUME_FORMAT_STRING);
        reply.setVolume(vol);
        logger.debug(String.format("Successfully created root volume[uuid:%s] on primary storage[uuid:%s]", msg.getVolume().getUuid(),
                msg.getPrimaryStorageUuid()));
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(DeleteVolumeOnPrimaryStorageMsg msg) {
        DeleteVolumeOnPrimaryStorageReply reply = new DeleteVolumeOnPrimaryStorageReply();
        logger.debug(String.format("Successfully deleted volume[uuid:%s] from primary storage[uuid:%s]", msg.getVolume().getUuid(), msg.getUuid()));
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(InstantiateVolumeOnPrimaryStorageMsg msg) {
        if (msg.getClass() == InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg.class) {
            handle((InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg) msg);
        } else {
            InstantiateVolumeOnPrimaryStorageReply reply = new InstantiateVolumeOnPrimaryStorageReply();
            VolumeInventory vol = instantiateVolume(msg.getVolume());
            vol.setFormat(SimulatorConstant.SIMULATOR_VOLUME_FORMAT_STRING);
            reply.setVolume(vol);
            logger.debug(String.format("Successfully created data volume[uuid:%s] on primary storage[uuid:%s]", msg.getVolume().getUuid(),
                    msg.getPrimaryStorageUuid()));
            bus.reply(msg, reply);
        }
    }

    @Override
    protected void handle(CreateTemplateFromVolumeOnPrimaryStorageMsg msg) {
        bus.dealWithUnknownMessage(msg);
    }

    @Override
    protected void handle(DownloadDataVolumeToPrimaryStorageMsg msg) {
        String path = putil.join(self.getUrl(), PrimaryStorageConstant.VM_FOLDER, msg.getVolumeUuid() + ".qcow2");
        DownloadDataVolumeToPrimaryStorageReply reply = new DownloadDataVolumeToPrimaryStorageReply();
        reply.setInstallPath(path);
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(DeleteBitsOnPrimaryStorageMsg msg) {
        DeleteBitsOnPrimaryStorageReply reply = new DeleteBitsOnPrimaryStorageReply();
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(DownloadIsoToPrimaryStorageMsg msg) {
        DownloadIsoToPrimaryStorageReply reply = new DownloadIsoToPrimaryStorageReply();
        reply.setInstallPath("/xxx.iso");
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(DeleteIsoFromPrimaryStorageMsg msg) {
        DeleteIsoFromPrimaryStorageReply reply = new DeleteIsoFromPrimaryStorageReply();
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(AskVolumeSnapshotCapabilityMsg msg) {
        AskVolumeSnapshotCapabilityReply reply = new AskVolumeSnapshotCapabilityReply();
        VolumeSnapshotCapability capability = new VolumeSnapshotCapability();
        capability.setArrangementType(VolumeSnapshotArrangementType.CHAIN);
        capability.setSupport(true);
        reply.setCapability(capability);
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(SyncVolumeSizeOnPrimaryStorageMsg msg) {
        SyncVolumeSizeOnPrimaryStorageReply reply = new SyncVolumeSizeOnPrimaryStorageReply();
        reply.setActualSize(0);
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(MergeVolumeSnapshotOnPrimaryStorageMsg msg) {
        MergeVolumeSnapshotOnPrimaryStorageReply reply = new MergeVolumeSnapshotOnPrimaryStorageReply();
        reply.setSuccess(true);
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(DeleteSnapshotOnPrimaryStorageMsg msg) {
        DeleteSnapshotOnPrimaryStorageReply reply = new DeleteSnapshotOnPrimaryStorageReply();
        reply.setSuccess(true);
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(RevertVolumeFromSnapshotOnPrimaryStorageMsg msg) {
        RevertVolumeFromSnapshotOnPrimaryStorageReply reply = new RevertVolumeFromSnapshotOnPrimaryStorageReply();
        reply.setSuccess(true);
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(ReInitRootVolumeFromTemplateOnPrimaryStorageMsg msg) {
        ReInitRootVolumeFromTemplateOnPrimaryStorageReply reply = new ReInitRootVolumeFromTemplateOnPrimaryStorageReply();
        reply.setSuccess(true);
        bus.reply(msg, reply);
    }

    @Override
    protected void connectHook(ConnectParam param, Completion completion) {
        completion.success();
    }

    @Override
    protected void pingHook(Completion completion) {
        completion.success();
    }

    @Override
    protected void syncPhysicalCapacity(ReturnValueCompletion<PhysicalCapacityUsage> completion) {
        PhysicalCapacityUsage usage = new PhysicalCapacityUsage();
        usage.availablePhysicalSize = self.getCapacity().getAvailablePhysicalCapacity();
        usage.totalPhysicalSize = self.getCapacity().getTotalPhysicalCapacity();
        completion.success(usage);
    }
}
