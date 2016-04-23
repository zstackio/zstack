package org.zstack.storage.primary.smp;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageAskInstallPathMsg;
import org.zstack.header.storage.backup.BackupStorageAskInstallPathReply;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.header.storage.backup.DeleteBitsOnBackupStorageMsg;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.CreateTemplateFromVolumeSnapshotExtensionPoint;
import org.zstack.header.volume.VolumeFormat;
import org.zstack.storage.backup.BackupStorageCapacityUpdater;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by xing5 on 2016/3/26.
 */
public class SMPPrimaryStorageFactory implements PrimaryStorageFactory, CreateTemplateFromVolumeSnapshotExtensionPoint {
    private static final CLogger logger = Utils.getLogger(SMPPrimaryStorageFactory.class);

    public static final PrimaryStorageType type = new PrimaryStorageType(SMPConstants.SMP_TYPE);

    static {
        type.setSupportPingStorageGateway(true);
        type.setSupportHeartbeatFile(true);
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

            @AfterDone
            List<Runnable> returnCapacityToBackupStorage = new ArrayList<Runnable>();

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                final ParamOut out = (ParamOut) data.get(ParamOut.class);
                final List<String> bsUuids = paramIn.getSelectedBackupStorageUuids();
                List<ErrorCode> errors = new ArrayList<ErrorCode>();
                final List<UploadBitsToBackupStorageMsg> msgs = new ArrayList<UploadBitsToBackupStorageMsg>();

                for (final String bsUuid : bsUuids) {
                    BackupStorageAskInstallPathMsg ask = new BackupStorageAskInstallPathMsg();
                    ask.setImageUuid(paramIn.getImage().getUuid());
                    ask.setBackupStorageUuid(bsUuid);
                    ask.setImageMediaType(paramIn.getImage().getMediaType());
                    bus.makeTargetServiceIdByResourceUuid(ask, BackupStorageConstant.SERVICE_ID, bsUuid);
                    MessageReply ar = bus.call(ask);
                    if (!ar.isSuccess()) {
                        errors.add(ar.getError());

                        returnCapacityToBackupStorage.add(new Runnable() {
                            @Override
                            public void run() {
                                BackupStorageCapacityUpdater updater = new BackupStorageCapacityUpdater(bsUuid);
                                updater.increaseAvailableCapacity(out.getActualSize());
                            }
                        });

                        continue;
                    }

                    String bsInstallPath = ((BackupStorageAskInstallPathReply)ar).getInstallPath();

                    UploadBitsToBackupStorageMsg msg = new UploadBitsToBackupStorageMsg();
                    msg.setPrimaryStorageUuid(paramIn.getPrimaryStorageUuid());
                    msg.setHypervisorType(hvType.toString());
                    msg.setPrimaryStorageInstallPath(paramIn.getSnapshot().getPrimaryStorageInstallPath());
                    msg.setBackupStorageUuid(bsUuid);
                    msg.setBackupStorageInstallPath(bsInstallPath);
                    bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, paramIn.getPrimaryStorageUuid());
                    msgs.add(msg);
                }

                if (msgs.isEmpty()) {
                    trigger.fail(errf.stringToOperationError(
                            String.format("failed to get install path on all backup storage%s", bsUuids), errors
                    ));
                    return;
                }

                bus.send(msgs, new CloudBusListCallBack(trigger) {
                    @Override
                    public void run(List<MessageReply> replies) {
                        List<ErrorCode> errors = new ArrayList<ErrorCode>();
                        for (MessageReply reply : replies) {
                            final UploadBitsToBackupStorageMsg msg = msgs.get(replies.indexOf(reply));
                            if (!reply.isSuccess()) {
                                errors.add(reply.getError());

                                returnCapacityToBackupStorage.add(new Runnable() {
                                    @Override
                                    public void run() {
                                        BackupStorageCapacityUpdater updater = new BackupStorageCapacityUpdater(msg.getBackupStorageUuid());
                                        updater.increaseAvailableCapacity(out.getActualSize());
                                    }
                                });

                                continue;
                            }

                            BackupStorageResult res = new BackupStorageResult();
                            res.setBackupStorageUuid(msg.getBackupStorageUuid());
                            res.setInstallPath(msg.getBackupStorageInstallPath());
                            out.getBackupStorageResult().add(res);
                        }

                        if (out.getBackupStorageResult().isEmpty()) {
                            trigger.fail(errf.stringToOperationError(
                                    String.format("failed to upload to all backup storage%s", bsUuids), errors
                            ));
                        } else {
                            trigger.next();
                        }
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                final ParamOut out = (ParamOut) data.get(ParamOut.class);
                if (!out.getBackupStorageResult().isEmpty()) {
                    for (BackupStorageResult res : out.getBackupStorageResult()) {
                        DeleteBitsOnBackupStorageMsg msg = new DeleteBitsOnBackupStorageMsg();
                        msg.setInstallPath(res.getInstallPath());
                        msg.setBackupStorageUuid(res.getBackupStorageUuid());
                        bus.makeTargetServiceIdByResourceUuid(msg, BackupStorageConstant.SERVICE_ID, res.getBackupStorageUuid());
                        bus.send(msg);
                    }
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
}
