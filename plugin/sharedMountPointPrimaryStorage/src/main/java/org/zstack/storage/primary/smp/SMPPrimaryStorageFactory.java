package org.zstack.storage.primary.smp;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.VmExpungeRootVolumeValidator;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageAskInstallPathMsg;
import org.zstack.header.storage.backup.BackupStorageAskInstallPathReply;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.header.storage.backup.DeleteBitsOnBackupStorageMsg;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.CreateTemplateFromVolumeSnapshotExtensionPoint;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.volume.VolumeFormat;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.storage.primary.PrimaryStorageCapacityUpdater;
import org.zstack.storage.snapshot.PostMarkRootVolumeAsSnapshotExtension;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;

/**
 * Created by xing5 on 2016/3/26.
 */
public class SMPPrimaryStorageFactory implements PrimaryStorageFactory, CreateTemplateFromVolumeSnapshotExtensionPoint, HostDeleteExtensionPoint, PrimaryStorageDetachExtensionPoint,
        PostMarkRootVolumeAsSnapshotExtension{
    private static final CLogger logger = Utils.getLogger(SMPPrimaryStorageFactory.class);

    public static final PrimaryStorageType type = new PrimaryStorageType(SMPConstants.SMP_TYPE);

    static {
        type.setSupportPingStorageGateway(true);
        type.setSupportHeartbeatFile(true);
        type.setOrder(699);
    }

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;

    @Override
    public PrimaryStorageType getPrimaryStorageType() {
        return type;
    }

    @Override
    public PrimaryStorageInventory createPrimaryStorage(PrimaryStorageVO vo, APIAddPrimaryStorageMsg msg) {
        vo.setMountPath(vo.getUrl());
        vo.setType(SMPConstants.SMP_TYPE);
        vo = dbf.persistAndRefresh(vo);
        return PrimaryStorageInventory.valueOf(vo);
    }

    @Override
    public PrimaryStorage getPrimaryStorage(PrimaryStorageVO vo) {
        return new SMPPrimaryStorageBase(vo);
    }

    @Override
    public PrimaryStorageInventory getInventory(String uuid) {
        return PrimaryStorageInventory.valueOf(dbf.findByUuid(uuid, PrimaryStorageVO.class));
    }

    @VmExpungeRootVolumeValidator.VmExpungeRootVolumeValidatorMethod
    static void vmExpungeRootVolumeValidator(String vmUuid, String volumeUuid) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                String psUuid = q(VolumeVO.class).select(VolumeVO_.primaryStorageUuid).eq(VolumeVO_.uuid, volumeUuid)
                        .findValue();

                if (psUuid == null) {
                    return;
                }

                if (!q(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, psUuid)
                        .eq(PrimaryStorageVO_.type, SMPConstants.SMP_TYPE)
                        .isExists()) {
                    // not SMP
                    return;
                }

                if (!q(PrimaryStorageClusterRefVO.class).eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, psUuid).isExists()) {
                    throw new OperationFailureException(operr("the SMP primary storage[uuid:%s] is not attached" +
                            " to any clusters, and cannot expunge the root volume[uuid:%s] of the VM[uuid:%s]", psUuid, vmUuid, volumeUuid));
                }
            }
        }.execute();
    }

    @Override
    public WorkflowTemplate createTemplateFromVolumeSnapshot(final ParamIn paramIn) {
        WorkflowTemplate template = new WorkflowTemplate();
        final HypervisorType hvType = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(paramIn.getSnapshot().getFormat());

        class Context {
            String temporaryInstallPath;
        }

        final Context ctx = new Context();

        template.setCreateTemporaryTemplate(new Flow() {
            String __name__ = "create-temporary-template";

            @Override
            public void run(final FlowTrigger trigger, final Map data) {
                CreateTemporaryVolumeFromSnapshotMsg msg = new CreateTemporaryVolumeFromSnapshotMsg();
                msg.setHypervisorType(hvType.toString());
                msg.setPrimaryStorageUuid(paramIn.getPrimaryStorageUuid());
                msg.setTemporaryVolumeUuid(paramIn.getImage().getUuid());
                msg.setSnapshot(paramIn.getSnapshot());
                bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, paramIn.getPrimaryStorageUuid());
                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                        } else {
                            ParamOut paramOut = (ParamOut) data.get(ParamOut.class);
                            CreateTemporaryVolumeFromSnapshotReply ar = reply.castReply();
                            ctx.temporaryInstallPath = ar.getInstallPath();
                            paramOut.setSize(ar.getSize());
                            paramOut.setActualSize(ar.getActualSize());
                            trigger.next();
                        }
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                if (ctx.temporaryInstallPath != null) {
                    DeleteBitsOnPrimaryStorageMsg msg = new DeleteBitsOnPrimaryStorageMsg();
                    msg.setHypervisorType(hvType.toString());
                    msg.setPrimaryStorageUuid(paramIn.getPrimaryStorageUuid());
                    msg.setInstallPath(ctx.temporaryInstallPath);
                    bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, paramIn.getPrimaryStorageUuid());
                    bus.send(msg);
                }

                trigger.rollback();
            }
        });

        template.setUploadToBackupStorage(new Flow() {
            String __name__ = "upload-to-backup-storage";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                final ParamOut out = (ParamOut) data.get(ParamOut.class);

                    BackupStorageAskInstallPathMsg ask = new BackupStorageAskInstallPathMsg();
                    ask.setImageUuid(paramIn.getImage().getUuid());
                    ask.setBackupStorageUuid(paramIn.getBackupStorageUuid());
                    ask.setImageMediaType(paramIn.getImage().getMediaType());
                    bus.makeTargetServiceIdByResourceUuid(ask, BackupStorageConstant.SERVICE_ID, paramIn.getBackupStorageUuid());
                    MessageReply ar = bus.call(ask);
                    if (!ar.isSuccess()) {
                        trigger.fail(ar.getError());
                        return;
                    }

                    String bsInstallPath = ((BackupStorageAskInstallPathReply)ar).getInstallPath();

                    UploadBitsToBackupStorageMsg msg = new UploadBitsToBackupStorageMsg();
                    msg.setPrimaryStorageUuid(paramIn.getPrimaryStorageUuid());
                    msg.setHypervisorType(hvType.toString());
                    msg.setPrimaryStorageInstallPath(paramIn.getSnapshot().getPrimaryStorageInstallPath());
                    msg.setBackupStorageUuid(paramIn.getBackupStorageUuid());
                    msg.setBackupStorageInstallPath(bsInstallPath);
                    bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, paramIn.getPrimaryStorageUuid());


                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                        } else {
                            UploadBitsToBackupStorageReply r = reply.castReply();
                            out.setBackupStorageInstallPath(r.getBackupStorageInstallPath());
                            trigger.next();
                        }
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                final ParamOut out = (ParamOut) data.get(ParamOut.class);
                if (out.getBackupStorageInstallPath() != null) {
                    DeleteBitsOnBackupStorageMsg msg = new DeleteBitsOnBackupStorageMsg();
                    msg.setInstallPath(out.getBackupStorageInstallPath());
                    msg.setBackupStorageUuid(paramIn.getBackupStorageUuid());
                    bus.makeTargetServiceIdByResourceUuid(msg, BackupStorageConstant.SERVICE_ID, paramIn.getBackupStorageUuid());
                    bus.send(msg);
                }

                trigger.rollback();
            }
        });

        template.setDeleteTemporaryTemplate(new NoRollbackFlow() {
            String __name__ = "delete-temporary-template";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                DeleteBitsOnPrimaryStorageMsg msg = new DeleteBitsOnPrimaryStorageMsg();
                msg.setInstallPath(ctx.temporaryInstallPath);
                msg.setPrimaryStorageUuid(paramIn.getPrimaryStorageUuid());
                msg.setHypervisorType(hvType.toString());
                bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, paramIn.getPrimaryStorageUuid());
                bus.send(msg);

                trigger.next();
            }
        });

        return template;
    }

    @Override
    public String createTemplateFromVolumeSnapshotPrimaryStorageType() {
        return SMPConstants.SMP_TYPE;
    }

    @Override
    public void preDeleteHost(HostInventory inventory) throws HostException {

    }

    @Override
    public void beforeDeleteHost(HostInventory inventory) {

    }

    @Override
    public void afterDeleteHost(HostInventory inventory) {
        String clusterUuid = inventory.getClusterUuid();

        if (Q.New(HostVO.class).eq(HostVO_.clusterUuid, clusterUuid).notEq(HostVO_.uuid, inventory.getUuid()).isExists()) {
            return;
        }

        final List<String> psUuids = getSMPPrimaryStorageInCluster(clusterUuid);
        if(psUuids == null || psUuids.isEmpty()) {
            return;
        }

        for (String psUuid : psUuids) {
            releasePrimaryStorageCapacity(psUuid);
        }
    }

    private void releasePrimaryStorageCapacity(String psUuid) {
        SMPRecalculatePrimaryStorageCapacityMsg msg = new SMPRecalculatePrimaryStorageCapacityMsg();
        msg.setPrimaryStorageUuid(psUuid);
        msg.setRelease(true);
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, psUuid);
        bus.send(msg);
    }

    private List<String> getSMPPrimaryStorageInCluster(String clusterUuid) {
        return SQL.New("select pri.uuid" +
                " from PrimaryStorageVO pri, PrimaryStorageClusterRefVO ref" +
                " where pri.uuid = ref.primaryStorageUuid" +
                " and ref.clusterUuid = :cuuid" +
                " and pri.type = :ptype")
                .param("cuuid", clusterUuid)
                .param("ptype", SMPConstants.SMP_TYPE)
                .list();
    }

    @Override
    public void preDetachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) throws PrimaryStorageException {
        return;
    }

    @Override
    public void beforeDetachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {
        return;
    }

    @Override
    public void failToDetachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {
        return;
    }

    @Override
    public void afterDetachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {

        PrimaryStorageVO vo = dbf.findByUuid(inventory.getUuid(), PrimaryStorageVO.class);
        if(null == vo){
            logger.warn(String.format("run afterRecalculatePrimaryStorageCapacity fail, not find ps[%s] db record", inventory.getUuid()));
            return;
        }

        SMPPrimaryStorageBase base = new SMPPrimaryStorageBase(vo);
        if(base.isUnmounted()){
            //base.resetDefaultCapacityWhenUnmounted();
            releasePrimaryStorageCapacity(inventory.getUuid());
        }
    }

    @Override
    public void afterMarkRootVolumeAsSnapshot(VolumeSnapshotInventory snapshot) {

    }
}
