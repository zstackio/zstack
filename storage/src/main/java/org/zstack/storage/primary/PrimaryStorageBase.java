package org.zstack.storage.primary;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.inventory.InventoryFacade;
import org.zstack.core.job.JobQueueFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.primary.PrimaryStorageCanonicalEvent.PrimaryStorageDeletedData;
import org.zstack.header.storage.primary.PrimaryStorageCanonicalEvent.PrimaryStorageStatusChangedData;
import org.zstack.header.storage.snapshot.ChangeVolumeSnapshotStatusReply;
import org.zstack.header.storage.snapshot.VolumeSnapshotConstant;
import org.zstack.header.storage.snapshot.VolumeSnapshotReportPrimaryStorageCapacityUsageMsg;
import org.zstack.header.storage.snapshot.VolumeSnapshotReportPrimaryStorageCapacityUsageReply;
import org.zstack.header.vm.StopVmInstanceMsg;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeReportPrimaryStorageCapacityUsageMsg;
import org.zstack.header.volume.VolumeReportPrimaryStorageCapacityUsageReply;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE, dependencyCheck = true)
public abstract class PrimaryStorageBase extends AbstractPrimaryStorage {
    private final static CLogger logger = Utils.getLogger(PrimaryStorageBase.class);

    protected PrimaryStorageVO self;

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected JobQueueFacade jobf;
    @Autowired
    protected PrimaryStorageExtensionPointEmitter extpEmitter;
    @Autowired
    protected InventoryFacade invf;
    @Autowired
    protected CascadeFacade casf;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    protected PrimaryStorageOverProvisioningManager ratioMgr;
    @Autowired
    protected EventFacade evtf;
    @Autowired
    protected PrimaryStoragePingTracker tracker;

    public static class PhysicalCapacityUsage {
        public long totalPhysicalSize;
        public long availablePhysicalSize;
    }

    public static class ConnectParam {
        private boolean newAdded;

        public boolean isNewAdded() {
            return newAdded;
        }

        public void setNewAdded(boolean newAdded) {
            this.newAdded = newAdded;
        }
    }

    protected abstract void handle(InstantiateVolumeOnPrimaryStorageMsg msg);

    protected abstract void handle(DeleteVolumeOnPrimaryStorageMsg msg);

    protected abstract void handle(CreateTemplateFromVolumeOnPrimaryStorageMsg msg);

    protected abstract void handle(DownloadDataVolumeToPrimaryStorageMsg msg);

    protected abstract void handle(DeleteBitsOnPrimaryStorageMsg msg);

    protected abstract void handle(DownloadIsoToPrimaryStorageMsg msg);

    protected abstract void handle(DeleteIsoFromPrimaryStorageMsg msg);

    protected abstract void handle(AskVolumeSnapshotCapabilityMsg msg);

    protected abstract void handle(SyncVolumeSizeOnPrimaryStorageMsg msg);

    protected abstract void connectHook(ConnectParam param, Completion completion);

    protected abstract void pingHook(Completion completion);

    protected abstract void syncPhysicalCapacity(ReturnValueCompletion<PhysicalCapacityUsage> completion);

    public PrimaryStorageBase(PrimaryStorageVO self) {
        this.self = self;
    }

    protected PrimaryStorageInventory getSelfInventory() {
        return PrimaryStorageInventory.valueOf(self);
    }

    protected String getSyncId() {
        return String.format("primaryStorage-%s", self.getUuid());
    }

    @Override
    public void attachHook(String clusterUuid, Completion completion) {
        completion.success();
    }

    @Override
    public void detachHook(String clusterUuid, Completion completion) {
        completion.success();
    }

    @Override
    public void deleteHook() {
    }

    @Override
    public void changeStateHook(PrimaryStorageStateEvent evt, PrimaryStorageState nextState) {
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            if (msg instanceof APIMessage) {
                handleApiMessage((APIMessage) msg);
            } else {
                handleLocalMessage(msg);
            }
        } catch (Exception e) {
            bus.logExceptionWithMessageDump(msg, e);
            bus.replyErrorByMessageType(msg, e);
        }
    }

    protected void handleLocalMessage(Message msg) {
        if (msg instanceof PrimaryStorageReportPhysicalCapacityMsg) {
            handle((PrimaryStorageReportPhysicalCapacityMsg) msg);
        } else if (msg instanceof InstantiateVolumeOnPrimaryStorageMsg) {
            handle((InstantiateVolumeOnPrimaryStorageMsg) msg);
        } else if (msg instanceof DeleteVolumeOnPrimaryStorageMsg) {
            handle((DeleteVolumeOnPrimaryStorageMsg) msg);
        } else if (msg instanceof CreateTemplateFromVolumeOnPrimaryStorageMsg) {
            handleBase((CreateTemplateFromVolumeOnPrimaryStorageMsg) msg);
        } else if (msg instanceof PrimaryStorageDeletionMsg) {
            handle((PrimaryStorageDeletionMsg) msg);
        } else if (msg instanceof DetachPrimaryStorageFromClusterMsg) {
            handle((DetachPrimaryStorageFromClusterMsg) msg);
        } else if (msg instanceof DownloadDataVolumeToPrimaryStorageMsg) {
            handleBase((DownloadDataVolumeToPrimaryStorageMsg) msg);
        } else if (msg instanceof DeleteBitsOnPrimaryStorageMsg) {
            handle((DeleteBitsOnPrimaryStorageMsg) msg);
        } else if (msg instanceof ConnectPrimaryStorageMsg) {
            handle((ConnectPrimaryStorageMsg) msg);
        } else if (msg instanceof DownloadIsoToPrimaryStorageMsg) {
            handleBase((DownloadIsoToPrimaryStorageMsg) msg);
        } else if (msg instanceof DeleteIsoFromPrimaryStorageMsg) {
            handle((DeleteIsoFromPrimaryStorageMsg) msg);
        } else if (msg instanceof AskVolumeSnapshotCapabilityMsg) {
            handle((AskVolumeSnapshotCapabilityMsg) msg);
        } else if (msg instanceof SyncVolumeSizeOnPrimaryStorageMsg) {
            handle((SyncVolumeSizeOnPrimaryStorageMsg) msg);
        } else if (msg instanceof PingPrimaryStorageMsg) {
            handle((PingPrimaryStorageMsg) msg);
        } else if (msg instanceof ChangePrimaryStorageStatusMsg) {
            handle((ChangePrimaryStorageStatusMsg) msg);
        } else if (msg instanceof ReconnectPrimaryStorageMsg) {
            handle((ReconnectPrimaryStorageMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    protected void handle(ReconnectPrimaryStorageMsg msg) {
        ReconnectPrimaryStorageReply reply = new ReconnectPrimaryStorageReply();
        doConnect(new ConnectParam(), new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(ChangePrimaryStorageStatusMsg msg) {
        changeStatus(PrimaryStorageStatus.valueOf(msg.getStatus()));
        ChangeVolumeSnapshotStatusReply reply = new ChangeVolumeSnapshotStatusReply();
        bus.reply(msg, reply);
    }

    private void handle(final PingPrimaryStorageMsg msg) {
        final PingPrimaryStorageReply reply = new PingPrimaryStorageReply();

        pingHook(new Completion(msg) {
            @Override
            public void success() {
                if (self.getStatus() == PrimaryStorageStatus.Disconnected) {
                    doConnect(new ConnectParam(), new NopeCompletion());
                }

                reply.setConnected(true);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                changeStatus(PrimaryStorageStatus.Disconnected);
                reply.setConnected(false);
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handleBase(DownloadIsoToPrimaryStorageMsg msg) {
        checkIfBackupStorageAttachedToMyZone(msg.getIsoSpec().getSelectedBackupStorage().getBackupStorageUuid());
        handle(msg);
    }

    private void doConnect(ConnectParam param, final Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return String.format("reconnect-primary-storage-%s", self.getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                changeStatus(PrimaryStorageStatus.Connecting);

                connectHook(param, new Completion(chain, completion) {
                    @Override
                    public void success() {
                        self = dbf.reload(self);
                        changeStatus(PrimaryStorageStatus.Connected);
                        logger.debug(String.format("successfully connected primary storage[uuid:%s]", self.getUuid()));

                        tracker.track(self.getUuid());

                        completion.success();
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        tracker.track(self.getUuid());

                        self = dbf.reload(self);
                        changeStatus(PrimaryStorageStatus.Disconnected);
                        logger.debug(String.format("failed to connect primary storage[uuid:%s], %s", self.getUuid(), errorCode));
                        completion.fail(errorCode);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(final ConnectPrimaryStorageMsg msg) {
        final ConnectPrimaryStorageReply reply = new ConnectPrimaryStorageReply();

        ConnectParam param = new ConnectParam();
        param.newAdded = msg.isNewAdded();

        doConnect(param, new Completion(msg) {
            @Override
            public void success() {
                reply.setConnected(true);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                if (msg.isNewAdded()) {
                    reply.setError(errorCode);
                } else {
                    reply.setConnected(false);
                }

                bus.reply(msg, reply);
            }
        });
    }

    private void handleBase(DownloadDataVolumeToPrimaryStorageMsg msg) {
        checkIfBackupStorageAttachedToMyZone(msg.getBackupStorageRef().getBackupStorageUuid());
        handle(msg);
    }

    @Transactional(readOnly = true)
    private void checkIfBackupStorageAttachedToMyZone(String bsUuid) {
        String sql = "select bs.uuid" +
                " from BackupStorageVO bs, BackupStorageZoneRefVO ref" +
                " where bs.uuid = ref.backupStorageUuid" +
                " and ref.zoneUuid = :zoneUuid" +
                " and bs.uuid = :bsUuid";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("zoneUuid", self.getZoneUuid());
        q.setParameter("bsUuid", bsUuid);
        if (q.getResultList().isEmpty()) {
            throw new OperationFailureException(errf.stringToOperationError(
                    String.format("backup storage[uuid:%s] is not attached to zone[uuid:%s] the primary storage[uuid:%s] belongs to",
                            bsUuid, self.getZoneUuid(), self.getUuid())
            ));
        }
    }

    private void handleBase(CreateTemplateFromVolumeOnPrimaryStorageMsg msg) {
        checkIfBackupStorageAttachedToMyZone(msg.getBackupStorageUuid());
        handle(msg);
    }

    private void handle(final DetachPrimaryStorageFromClusterMsg msg) {
        final DetachPrimaryStorageFromClusterReply reply = new DetachPrimaryStorageFromClusterReply();
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(final SyncTaskChain chain) {
                extpEmitter.beforeDetach(self, msg.getClusterUuid());
                detachHook(msg.getClusterUuid(), new Completion(msg, chain) {
                    @Override
                    public void success() {
                        SimpleQuery<PrimaryStorageClusterRefVO> q = dbf.createQuery(PrimaryStorageClusterRefVO.class);
                        q.add(PrimaryStorageClusterRefVO_.clusterUuid, Op.EQ, msg.getClusterUuid());
                        q.add(PrimaryStorageClusterRefVO_.primaryStorageUuid, Op.EQ, msg.getPrimaryStorageUuid());
                        List<PrimaryStorageClusterRefVO> refs = q.list();
                        dbf.removeCollection(refs, PrimaryStorageClusterRefVO.class);

                        self = dbf.reload(self);
                        extpEmitter.afterDetach(self, msg.getClusterUuid());

                        logger.debug(String.format("successfully detached primary storage[name: %s, uuid:%s]",
                                self.getName(), self.getUuid()));
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        extpEmitter.failToDetach(self, msg.getClusterUuid());
                        logger.warn(errorCode.toString());
                        reply.setError(errf.instantiateErrorCode(PrimaryStorageErrors.DETACH_ERROR, errorCode));
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("detach-primary-storage-%s-from-%s", self.getUuid(), msg.getClusterUuid());
            }
        });
    }

    private void handle(PrimaryStorageDeletionMsg msg) {
        PrimaryStorageInventory inv = PrimaryStorageInventory.valueOf(self);
        extpEmitter.beforeDelete(inv);
        deleteHook();
        extpEmitter.afterDelete(inv);

        tracker.untrack(self.getUuid());
        PrimaryStorageDeletionReply reply = new PrimaryStorageDeletionReply();
        bus.reply(msg, reply);
    }

    @Transactional
    private void updateCapacity(long total, long avail) {
        PrimaryStorageCapacityVO cvo = dbf.getEntityManager().find(PrimaryStorageCapacityVO.class,
                self.getUuid(), LockModeType.PESSIMISTIC_WRITE);
        DebugUtils.Assert(cvo != null, String.format("how can there is no PrimaryStorageCapacityVO[uuid:%s]", self.getUuid()));

        cvo.setTotalPhysicalCapacity(total);
        cvo.setAvailablePhysicalCapacity(avail);
        dbf.getEntityManager().merge(cvo);
    }

    private void handle(PrimaryStorageReportPhysicalCapacityMsg msg) {
        updateCapacity(msg.getTotalCapacity(), msg.getAvailableCapacity());
        bus.reply(msg, new MessageReply());
    }

    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIDeletePrimaryStorageMsg) {
            handle((APIDeletePrimaryStorageMsg) msg);
        } else if (msg instanceof APIChangePrimaryStorageStateMsg) {
            handle((APIChangePrimaryStorageStateMsg) msg);
        } else if (msg instanceof APIAttachPrimaryStorageToClusterMsg) {
            handle((APIAttachPrimaryStorageToClusterMsg) msg);
        } else if (msg instanceof APIDetachPrimaryStorageFromClusterMsg) {
            handle((APIDetachPrimaryStorageFromClusterMsg) msg);
        } else if (msg instanceof APIReconnectPrimaryStorageMsg) {
            handle((APIReconnectPrimaryStorageMsg) msg);
        } else if (msg instanceof APIUpdatePrimaryStorageMsg) {
            handle((APIUpdatePrimaryStorageMsg) msg);
        } else if (msg instanceof APISyncPrimaryStorageCapacityMsg) {
            handle((APISyncPrimaryStorageCapacityMsg) msg);
        } else if (msg instanceof APICleanUpImageCacheOnPrimaryStorageMsg) {
            handle((APICleanUpImageCacheOnPrimaryStorageMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    protected void handle(APICleanUpImageCacheOnPrimaryStorageMsg msg) {
        throw new OperationFailureException(errf.stringToOperationError("operation not supported"));
    }

    private void handle(final APISyncPrimaryStorageCapacityMsg msg) {
        final APISyncPrimaryStorageCapacityEvent evt = new APISyncPrimaryStorageCapacityEvent(msg.getId());

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("sync-capacity-of-primary-storage-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            Long volumeUsage;
            Long snapshotUsage;
            Long totalPhysicalSize;
            Long availablePhysicalSize;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "sync-capacity-used-by-volumes";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        VolumeReportPrimaryStorageCapacityUsageMsg msg = new VolumeReportPrimaryStorageCapacityUsageMsg();
                        msg.setPrimaryStorageUuid(self.getUuid());
                        bus.makeLocalServiceId(msg, VolumeConstant.SERVICE_ID);
                        bus.send(msg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                VolumeReportPrimaryStorageCapacityUsageReply r = reply.castReply();
                                volumeUsage = r.getUsedCapacity();
                                volumeUsage = ratioMgr.calculateByRatio(self.getUuid(), volumeUsage);
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "sync-capacity-used-by-volume-snapshots";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        VolumeSnapshotReportPrimaryStorageCapacityUsageMsg msg = new VolumeSnapshotReportPrimaryStorageCapacityUsageMsg();
                        msg.setPrimaryStorageUuid(self.getUuid());
                        bus.makeLocalServiceId(msg, VolumeSnapshotConstant.SERVICE_ID);
                        bus.send(msg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                // note: snapshot size is physical size,
                                // don't calculate over-provisioning here
                                VolumeSnapshotReportPrimaryStorageCapacityUsageReply r = reply.castReply();
                                snapshotUsage = r.getUsedSize();
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "sync-physical-capacity";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        syncPhysicalCapacity(new ReturnValueCompletion<PhysicalCapacityUsage>(trigger) {
                            @Override
                            public void success(PhysicalCapacityUsage returnValue) {
                                totalPhysicalSize = returnValue.totalPhysicalSize;
                                availablePhysicalSize = returnValue.availablePhysicalSize;
                                availablePhysicalSize = availablePhysicalSize < 0 ? 0 : availablePhysicalSize;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        writeToDb();
                        self = dbf.reload(self);
                        evt.setInventory(getSelfInventory());
                        bus.publish(evt);
                    }

                    private void writeToDb() {
                        PrimaryStorageCapacityUpdater updater = new PrimaryStorageCapacityUpdater(self.getUuid());
                        updater.run(new PrimaryStorageCapacityUpdaterRunnable() {
                            @Override
                            public PrimaryStorageCapacityVO call(PrimaryStorageCapacityVO cap) {
                                long avail = cap.getTotalCapacity() - volumeUsage - snapshotUsage;
                                cap.setAvailableCapacity(avail);
                                cap.setAvailablePhysicalCapacity(availablePhysicalSize);
                                cap.setTotalPhysicalCapacity(totalPhysicalSize);
                                return cap;
                            }
                        });
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setErrorCode(errCode);
                        bus.publish(evt);
                    }
                });
            }
        }).start();
    }

    protected PrimaryStorageVO updatePrimaryStorage(APIUpdatePrimaryStorageMsg msg) {
        boolean update = false;
        if (msg.getName() != null) {
            self.setName(msg.getName());
            update = true;
        }
        if (msg.getDescription() != null) {
            self.setDescription(msg.getDescription());
            update = true;
        }

        return update ? self : null;
    }

    private void handle(APIUpdatePrimaryStorageMsg msg) {
        PrimaryStorageVO vo = updatePrimaryStorage(msg);

        if (vo != null) {
            self = dbf.updateAndRefresh(vo);
        }

        APIUpdatePrimaryStorageEvent evt = new APIUpdatePrimaryStorageEvent(msg.getId());
        evt.setInventory(getSelfInventory());
        bus.publish(evt);
    }

    protected void changeStatus(PrimaryStorageStatus status) {
        if (status == self.getStatus()) {
            return;
        }

        PrimaryStorageStatus oldStatus = self.getStatus();
        self.setStatus(status);
        self = dbf.updateAndRefresh(self);

        PrimaryStorageStatusChangedData d = new PrimaryStorageStatusChangedData();
        d.setInventory(PrimaryStorageInventory.valueOf(self));
        d.setPrimaryStorageUuid(self.getUuid());
        d.setOldStatus(oldStatus.toString());
        d.setNewStatus(status.toString());
        evtf.fire(PrimaryStorageCanonicalEvent.PRIMARY_STORAGE_STATUS_CHANGED_PATH, d);

        logger.debug(String.format("the primary storage[uuid:%s, name:%s] changed status from %s to %s",
                self.getUuid(), self.getName(), oldStatus, status));
    }

    protected void handle(APIReconnectPrimaryStorageMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                final APIReconnectPrimaryStorageEvent evt = new APIReconnectPrimaryStorageEvent(msg.getId());
                doConnect(new ConnectParam(), new Completion(msg, chain) {
                    @Override
                    public void success() {
                        evt.setInventory(getSelfInventory());
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setErrorCode(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "reconnect-primary-storage";
            }
        });
    }

    // don't use chainTask for this method, the sub-sequential DetachPrimaryStorageFromClusterMsg
    // is in the queue
    protected void handle(final APIDetachPrimaryStorageFromClusterMsg msg) {
        final APIDetachPrimaryStorageFromClusterEvent evt = new APIDetachPrimaryStorageFromClusterEvent(msg.getId());

        try {
            extpEmitter.preDetach(self, msg.getClusterUuid());
        } catch (PrimaryStorageException e) {
            throw new OperationFailureException(errf.instantiateErrorCode(PrimaryStorageErrors.DETACH_ERROR, e.getMessage()));
        }

        String issuer = PrimaryStorageVO.class.getSimpleName();
        List<PrimaryStorageDetachStruct> ctx = new ArrayList<>();
        PrimaryStorageDetachStruct struct = new PrimaryStorageDetachStruct();
        struct.setClusterUuid(msg.getClusterUuid());
        struct.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
        ctx.add(struct);
        casf.asyncCascade(PrimaryStorageConstant.PRIMARY_STORAGE_DETACH_CODE, issuer, ctx, new Completion(msg) {
            @Override
            public void success() {
                self = dbf.reload(self);
                evt.setInventory(PrimaryStorageInventory.valueOf(self));
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setErrorCode(errorCode);
                bus.publish(evt);
            }
        });
    }

    protected void handle(final APIAttachPrimaryStorageToClusterMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(final SyncTaskChain chain) {
                attachCluster(msg, new NoErrorCompletion(msg, chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("attach-primary-storage-%s-to-cluster-%s", self.getUuid(), msg.getClusterUuid());
            }
        });

    }

    private void attachCluster(final APIAttachPrimaryStorageToClusterMsg msg, final NoErrorCompletion completion) {
        final APIAttachPrimaryStorageToClusterEvent evt = new APIAttachPrimaryStorageToClusterEvent(msg.getId());
        try {
            extpEmitter.preAttach(self, msg.getClusterUuid());
        } catch (PrimaryStorageException pe) {
            evt.setErrorCode(errf.instantiateErrorCode(PrimaryStorageErrors.ATTACH_ERROR, pe.getMessage()));
            bus.publish(evt);
            completion.done();
            return;
        }

        extpEmitter.beforeAttach(self, msg.getClusterUuid());
        attachHook(msg.getClusterUuid(), new Completion(msg, completion) {
            @Override
            public void success() {
                PrimaryStorageClusterRefVO ref = new PrimaryStorageClusterRefVO();
                ref.setClusterUuid(msg.getClusterUuid());
                ref.setPrimaryStorageUuid(self.getUuid());
                dbf.persist(ref);

                self = dbf.reload(self);
                extpEmitter.afterAttach(self, msg.getClusterUuid());

                PrimaryStorageInventory pinv = (PrimaryStorageInventory) invf.valueOf(self);
                evt.setInventory(pinv);
                logger.debug(String.format("successfully attached primary storage[name:%s, uuid:%s]",
                        pinv.getName(), pinv.getUuid()));
                bus.publish(evt);
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                extpEmitter.failToAttach(self, msg.getClusterUuid());
                evt.setErrorCode(errf.instantiateErrorCode(PrimaryStorageErrors.ATTACH_ERROR, errorCode));
                bus.publish(evt);
                completion.done();
            }
        });
    }

    private  void stopAllVms(List<String> vmUuids) {
        final List<StopVmInstanceMsg> msgs = new ArrayList<StopVmInstanceMsg>();
        for (String vmUuid : vmUuids) {
            StopVmInstanceMsg msg = new StopVmInstanceMsg();
            msg.setVmInstanceUuid(vmUuid);
            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vmUuid);
            msgs.add(msg);
        }

        bus.send(msgs, new CloudBusListCallBack() {
            @Override
            public void run(List<MessageReply> replies) {
                StringBuilder sb = new StringBuilder();
                boolean success = true;
                for (MessageReply r : replies) {
                    if (!r.isSuccess()) {
                        StopVmInstanceMsg msg = msgs.get(replies.indexOf(r));
                        String err = String.format("\nfailed to stop vm[uuid:%s] on primary storage[uuid:%s], %s",
                                msg.getVmInstanceUuid(), self.getUuid(), r.getError());
                        sb.append(err);
                        success = false;
                    }
                }

                if (!success) {
                    logger.warn(sb.toString());
                }

            }
        });

    }

    private List<String> getAllVmsUuid(String PrimaryStorageUuid) {
        String sql = "select vm.uuid from VmInstanceVO vm, VolumeVO vol where vol.primaryStorageUuid =:uuid and vol.vmInstanceUuid = vm.uuid";
        Query q = dbf.getEntityManager().createQuery(sql);
        q.setParameter("uuid", PrimaryStorageUuid);
        List<String> vmUUids= q.getResultList();
        return vmUUids;
    }

    protected void handle(APIChangePrimaryStorageStateMsg msg) {
        APIChangePrimaryStorageStateEvent evt = new APIChangePrimaryStorageStateEvent(msg.getId());

        PrimaryStorageState currState = self.getState();
        PrimaryStorageStateEvent event = PrimaryStorageStateEvent.valueOf(msg.getStateEvent());
        PrimaryStorageState nextState = AbstractPrimaryStorage.getNextState(currState, event);

        try {
            extpEmitter.preChange(self, event);
        } catch (PrimaryStorageException e) {
            evt.setErrorCode(errf.instantiateErrorCode(SysErrors.CHANGE_RESOURCE_STATE_ERROR, e.getMessage()));
            bus.publish(evt);
            return;
        }

        extpEmitter.beforeChange(self, event);
        if (PrimaryStorageStateEvent.maintain == event) {
            logger.warn(String.format("Primary Storage %s  will enter maintenance mode, ignore unknown status VMs", msg.getPrimaryStorageUuid()));
            List<String> vmUuids = getAllVmsUuid(msg.getPrimaryStorageUuid());
            //TODO: Add alert if some vms on disconnect host
            if ( vmUuids.size() != 0 ) {
                stopAllVms(vmUuids);
            }
        }
        changeStateHook(event, nextState);
        self.setState(nextState);
        self = dbf.updateAndRefresh(self);
        extpEmitter.afterChange(self, event, currState);
        evt.setInventory(PrimaryStorageInventory.valueOf(self));
        bus.publish(evt);
    }

    protected void handle(APIDeletePrimaryStorageMsg msg) {
        final APIDeletePrimaryStorageEvent evt = new APIDeletePrimaryStorageEvent(msg.getId());
        final String issuer = PrimaryStorageVO.class.getSimpleName();
        final List<PrimaryStorageInventory> ctx = PrimaryStorageInventory.valueOf(Arrays.asList(self));
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-primary-storage-%s", msg.getUuid()));
        if (msg.getDeletionMode() == APIDeleteMessage.DeletionMode.Permissive) {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_CHECK_CODE, issuer, ctx, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            }).then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_DELETE_CODE, issuer, ctx, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            });
        } else {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_FORCE_DELETE_CODE, issuer, ctx, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            });
        }

        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new NopeCompletion());
                bus.publish(evt);

                PrimaryStorageDeletedData d = new PrimaryStorageDeletedData();
                d.setPrimaryStorageUuid(self.getUuid());
                d.setInventory(PrimaryStorageInventory.valueOf(self));
                evtf.fire(PrimaryStorageCanonicalEvent.PRIMARY_STORAGE_DELETED_PATH, d);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                evt.setErrorCode(errf.instantiateErrorCode(SysErrors.DELETE_RESOURCE_ERROR, errCode));
                bus.publish(evt);
            }
        }).start();
    }

}
