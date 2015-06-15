package org.zstack.storage.snapshot;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.storage.snapshot.VolumeSnapshotStatus.StatusEvent;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.*;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VolumeSnapshotBase implements VolumeSnapshot {
    private static final CLogger logger = Utils.getLogger(VolumeSnapshotBase.class);
    
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    protected VolumeSnapshotVO self;

    protected VolumeSnapshotInventory getSelfInventory() {
        return VolumeSnapshotInventory.valueOf(self);
    }

    public VolumeSnapshotBase(VolumeSnapshotVO self) {
        this.self = self;
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        handleLocalMessage(msg);
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof VolumeSnapshotPrimaryStorageDeletionMsg) {
            handle((VolumeSnapshotPrimaryStorageDeletionMsg) msg);
        } else if (msg instanceof BackupVolumeSnapshotMsg) {
            handle((BackupVolumeSnapshotMsg) msg);
        } else if (msg instanceof VolumeSnapshotBackupStorageDeletionMsg) {
            handle((VolumeSnapshotBackupStorageDeletionMsg) msg);
        } else if (msg instanceof ChangeVolumeSnapshotStatusMsg) {
            handle((ChangeVolumeSnapshotStatusMsg) msg);
        } else if (msg instanceof APIUpdateVolumeSnapshotMsg) {
            handle((APIUpdateVolumeSnapshotMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIUpdateVolumeSnapshotMsg msg) {
        boolean update = false;
        if (msg.getName() != null) {
            self.setName(msg.getName());
            update = true;
        }
        if (msg.getDescription() != null) {
            self.setDescription(msg.getDescription());
            update = true;
        }
        if (update) {
            self = dbf.updateAndRefresh(self);
        }

        APIUpdateVolumeSnapshotEvent evt = new APIUpdateVolumeSnapshotEvent(msg.getId());
        evt.setInventory(getSelfInventory());
        bus.publish(evt);
    }

    private void handle(ChangeVolumeSnapshotStatusMsg msg) {
        StatusEvent evt = StatusEvent.valueOf(msg.getEvent());
        changeStatus(evt);

        ChangeVolumeSnapshotStatusReply reply = new ChangeVolumeSnapshotStatusReply();
        bus.reply(msg, reply);
    }

    private void handle(final VolumeSnapshotBackupStorageDeletionMsg msg) {
        final VolumeSnapshotBackupStorageDeletionReply reply = new VolumeSnapshotBackupStorageDeletionReply();
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-volume-snapshot-%s-from-backup-storage", self.getUuid()));

        for (final String bsUuid : msg.getBackupStorageUuids()) {
            final String installPath = CollectionUtils.find(self.getBackupStorageRefs(), new Function<String, VolumeSnapshotBackupStorageRefVO>() {
                @Override
                public String call(VolumeSnapshotBackupStorageRefVO arg) {
                    return arg.getBackupStorageUuid().equals(bsUuid) ? arg.getInstallPath() : null;
                }
            });

            DebugUtils.Assert(installPath!=null, String.format("why installPath is NULL for snapshot[uuid:%s] on backupStorage[uuid:%s]???", self.getUuid(), bsUuid));

            chain.then(new ShareFlow() {
                boolean success;

                @Override
                public void setup() {
                    flow(new NoRollbackFlow() {
                        String __name__ = String.format("delete-on-backup-storage-%s", bsUuid);
                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            DeleteBitsOnBackupStorageMsg dmsg = new DeleteBitsOnBackupStorageMsg();
                            dmsg.setInstallPath(installPath);
                            dmsg.setBackupStorageUuid(bsUuid);
                            bus.makeTargetServiceIdByResourceUuid(dmsg, BackupStorageConstant.SERVICE_ID, bsUuid);
                            bus.send(dmsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    success = reply.isSuccess();
                                    if (!success) {
                                        logger.warn(String.format("failed to delete snapshot[%s] on backup storage[uuid:%s], %s. the backup storage must clean up",
                                                self.getUuid(), bsUuid, reply.getError()));
                                    }

                                    VolumeSnapshotBackupStorageRefVO ref = CollectionUtils.find(self.getBackupStorageRefs(), new Function<VolumeSnapshotBackupStorageRefVO, VolumeSnapshotBackupStorageRefVO>() {
                                        @Override
                                        public VolumeSnapshotBackupStorageRefVO call(VolumeSnapshotBackupStorageRefVO arg) {
                                            return arg.getBackupStorageUuid().equals(bsUuid) ? arg : null;
                                        }
                                    });

                                    dbf.remove(ref);
                                    trigger.next();
                                }
                            });
                        }
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = String.format("return-capacity-to-backup-storage-%s", bsUuid);

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            if (!success) {
                                logger.warn(String.format("failed to delete volume snapshot[uuid:%s] from backup storage[uuid:%s], skip to return capacity[%s bytes]",
                                        self.getUuid(), bsUuid, self.getSize()));
                                trigger.next();
                                return;
                            }

                            ReturnBackupStorageMsg rmsg = new ReturnBackupStorageMsg();
                            rmsg.setSize(self.getSize());
                            rmsg.setBackupStorageUuid(bsUuid);
                            bus.makeTargetServiceIdByResourceUuid(rmsg, BackupStorageConstant.SERVICE_ID, bsUuid);
                            bus.send(rmsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        logger.warn(String.format("failed to return capacity to backup storage[uuid:%s] for volume snapshot[uuid:%s, size:%s], %s",
                                                bsUuid, self.getUuid(), self.getSize(), reply.getError()));
                                    }
                                    trigger.next();
                                }
                            });
                        }
                    });
                }
            });
        }

        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                bus.reply(msg, reply);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                bus.reply(msg, reply);
            }
        }).start();
    }

    private void handle(final BackupVolumeSnapshotMsg msg) {
        final BackupVolumeSnapshotReply reply = new BackupVolumeSnapshotReply();
        BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg bmsg = new BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg();
        bmsg.setPrimaryStorageUuid(self.getPrimaryStorageUuid());
        bmsg.setBackupStorage(msg.getBackupStorage());
        bmsg.setSnapshot(getSelfInventory());
        bus.makeTargetServiceIdByResourceUuid(bmsg, PrimaryStorageConstant.SERVICE_ID, self.getPrimaryStorageUuid());
        bus.send(bmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply rsp) {
                if (!rsp.isSuccess()) {
                    reply.setError(rsp.getError());
                } else {
                    BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply r = rsp.castReply();
                    VolumeSnapshotBackupStorageRefVO ref = new VolumeSnapshotBackupStorageRefVO();
                    ref.setInstallPath(r.getBackupStorageInstallPath());
                    ref.setVolumeSnapshotUuid(self.getUuid());
                    ref.setBackupStorageUuid(msg.getBackupStorage().getUuid());
                    dbf.persist(ref);
                }

                bus.reply(msg, reply);
            }
        });
    }

    private void changeStatus(VolumeSnapshotStatus.StatusEvent event) {
        self.setStatus(self.getStatus().nextState(event));
        dbf.update(self);
    }

    private void handle(final VolumeSnapshotPrimaryStorageDeletionMsg msg) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-volume-snapshot-%s-on-primary-storage", self.getUuid()));
        chain.then(new ShareFlow() {
            private void finish() {
                VolumeSnapshotPrimaryStorageDeletionReply dreply = new VolumeSnapshotPrimaryStorageDeletionReply();
                self.setPrimaryStorageInstallPath(null);
                self.setPrimaryStorageUuid(null);
                dbf.update(self);
                bus.reply(msg, dreply);
            }

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "delete-on-primary-storage";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        DeleteSnapshotOnPrimaryStorageMsg dmsg = new DeleteSnapshotOnPrimaryStorageMsg();
                        dmsg.setSnapshot(getSelfInventory());
                        bus.makeTargetServiceIdByResourceUuid(dmsg, PrimaryStorageConstant.SERVICE_ID, self.getPrimaryStorageUuid());
                        bus.send(dmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    trigger.setError(reply.getError());
                                }

                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "return-capacity-to-primary-storage";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ReturnPrimaryStorageCapacityMsg rmsg = new ReturnPrimaryStorageCapacityMsg();
                        rmsg.setDiskSize(self.getSize());
                        rmsg.setPrimaryStorageUuid(self.getPrimaryStorageUuid());
                        bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, rmsg.getPrimaryStorageUuid());
                        bus.send(rmsg);
                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        finish();
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        logger.warn(String.format("failed to delete snapshot[uuid:%s, name:%s] on primary storage[uuid:%s], the primary storage should cleanup",
                                self.getUuid(), self.getName(), self.getPrimaryStorageUuid()));
                        finish();
                    }
                });
            }
        }).start();
    }
}
