package org.zstack.storage.cdp;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.http.HttpHeaders;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.core.db.TransactionalCallback;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.trash.StorageTrash;
import org.zstack.core.trash.TrashType;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.trash.CleanTrashResult;
import org.zstack.header.core.trash.InstallPathRecycleInventory;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.image.CancelDownloadImageMsg;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.backup.BackupStorageCanonicalEvents.BackupStorageStatusChangedData;
import org.zstack.header.storage.backup.BackupStorageErrors.Opaque;
import org.zstack.storage.backup.BackupStoragePingTracker;
import org.zstack.utils.CollectionDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.LockModeType;
import javax.persistence.Query;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public abstract class CdpBackupStorageBase extends AbstractCdpBackupStorage {
    private static final CLogger logger = Utils.getLogger(CdpBackupStorageBase.class);

    protected BackupStorageVO self;

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected GlobalConfigFacade gcf;
    @Autowired
    protected CdpBackupStorageExtensionPointEmitter extpEmitter;
    @Autowired
    protected CascadeFacade casf;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    protected BackupStoragePingTracker tracker;
    @Autowired
    protected EventFacade evtf;
    @Autowired
    protected RESTFacade restf;
    @Autowired
    protected StorageTrash trash;

    abstract protected void connectHook(boolean newAdd, Completion completion);

    protected static List<TrashType> trashLists = CollectionDSL.list(TrashType.MigrateImage);

    public CdpBackupStorageBase(BackupStorageVO self) {
        this.self = self;
    }

    public CdpBackupStorageBase() {
    }

    public void deleteHook() {
    }

    public void detachHook(Completion completion) {
        completion.success();
    }

    public void attachHook(String zoneUuid, Completion completion) {
        completion.success();
    }

    public void changeStateHook(BackupStorageStateEvent evt, BackupStorageState nextState) {
    }

    @Override
    public List<ImageInventory> scanImages() {
        return null;
    }

    private long getContentLength(String url) {
        HttpHeaders header;

        try {
            header = restf.syncHead(url);
        } catch (Exception e) {
            throw new OperationFailureException(operr("failed to get header of image url %s: %s", url, e.toString()));
        }

        if (header == null) {
            throw new OperationFailureException(operr("failed to get header of image url %s", url));
        }

        return header.getContentLength();
    }

    protected void exceptionIfImageSizeGreaterThanAvailableCapacity(String url) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }

        url = url.trim();
        if (!url.startsWith("http") && !url.startsWith("https")) {
            return;
        }

        long size = getContentLength(url);
        if (size == -1) {
            logger.error(String.format("failed to get image size from url %s, but ignore this error and proceed", url));
        } else if (size < ImageConstant.MINI_IMAGE_SIZE_IN_BYTE) {
            throw new OperationFailureException(operr("the image size get from url %s is %d bytes, " +
                    "it's too small for an image, please check the url again.", url, size));
        } else if (size > self.getAvailableCapacity()) {
            throw new OperationFailureException(operr("the backup storage[uuid:%s, name:%s] has not enough capacity to download the image[%s]." +
                    "Required size:%s, available size:%s", self.getUuid(), self.getName(), url, size, self.getAvailableCapacity()));
        }
    }

    protected void refreshVO() {
        self = dbf.reload(self);
    }

    protected BackupStorageInventory getSelfInventory() {
        return BackupStorageInventory.valueOf(self);
    }

    protected void checkStatus(Message msg) {
        if (!statusChecker.isOperationAllowed(msg.getClass().getName(), self.getStatus().toString())) {
            throw new OperationFailureException(operr("backup storage cannot proceed message[%s] because its status is %s", msg.getClass().getName(), self.getStatus()));
        }
    }

    protected void checkState(Message msg) {
        if (!stateChecker.isOperationAllowed(msg.getClass().getName(), self.getState().toString())) {
            throw new OperationFailureException(operr("backup storage cannot proceed message[%s] because its state is %s", msg.getClass().getName(), self.getState()));
        }
    }

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

    protected void handleLocalMessage(Message msg) throws URISyntaxException {
        if (msg instanceof BackupStorageDeletionMsg) {
            handle((BackupStorageDeletionMsg) msg);
        } else if (msg instanceof ChangeBackupStorageStatusMsg) {
            handle((ChangeBackupStorageStatusMsg) msg);
        } else if (msg instanceof ReturnBackupStorageMsg) {
            handle((ReturnBackupStorageMsg) msg);
        } else if (msg instanceof ConnectBackupStorageMsg) {
            handle((ConnectBackupStorageMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    protected void fireDisconnectedCanonicalEvent(ErrorCode reason) {
        BackupStorageCanonicalEvents.DisconnectedData data = new BackupStorageCanonicalEvents.DisconnectedData();
        data.setBackupStorageUuid(self.getUuid());
        data.setReason(reason);
        evtf.fire(BackupStorageCanonicalEvents.BACKUP_STORAGE_DISCONNECTED, data);
    }

    private void handle(final ConnectBackupStorageMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("connect-backup-storage-%s", self.getUuid());
            }

            @Override
            public void run(final SyncTaskChain chain) {
                boolean fireDisconnecteEvent = self.getStatus() == BackupStorageStatus.Connected;

                final ConnectBackupStorageReply reply = new ConnectBackupStorageReply();
                changeStatus(BackupStorageStatus.Connecting);

                connectHook(msg.isNewAdd(), new Completion(msg, chain) {
                    @Override
                    public void success() {
                        self = dbf.reload(self);
                        changeStatus(BackupStorageStatus.Connected);
                        tracker.track(self.getUuid());
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        if (!msg.isNewAdd()) {
                            tracker.track(self.getUuid());
                            changeStatus(BackupStorageStatus.Disconnected);
                        }

                        if (fireDisconnecteEvent) {
                            fireDisconnectedCanonicalEvent(errorCode);
                        }

                        reply.setError(errorCode);
                        bus.reply(msg, reply);
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

    @Transactional
    private void handle(ReturnBackupStorageMsg msg) {
        self = dbf.getEntityManager().find(BackupStorageVO.class, self.getUuid(), LockModeType.PESSIMISTIC_WRITE);
        long availSize = self.getAvailableCapacity() + msg.getSize();
        if (availSize > self.getTotalCapacity()) {
            availSize = self.getTotalCapacity();
        }

        self.setAvailableCapacity(availSize);
        dbf.getEntityManager().merge(self);
        bus.reply(msg, new ReturnBackupStorageReply());
    }

    protected void changeStatus(final BackupStorageStatus status, final NoErrorCompletion completion) {
        thdf.syncSubmit(new SyncTask<Void>() {
            private final String name = String.format("backupstorage-%s-change-status", self.getUuid());

            @Override
            public String getSyncSignature() {
                return name;
            }

            @Override
            public int getSyncLevel() {
                return 1;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public Void call() {
                if (status == self.getStatus()) {
                    completion.done();
                    return null;
                }

                changeStatus(status);
                logger.debug(String.format("backup storage[uuid:%s, name:%s] change status from %s to %s",
                        self.getUuid(), self.getName(), self.getStatus(), status));
                completion.done();
                return null;
            }
        });
    }

    private void handle(final ChangeBackupStorageStatusMsg msg) {
        changeStatus(BackupStorageStatus.valueOf(msg.getStatus()), new NoErrorCompletion(msg) {
            @Override
            public void done() {
                ChangeBackupStorageStatusReply reply = new ChangeBackupStorageStatusReply();
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(BackupStorageDeletionMsg msg) {
        BackupStorageInventory inv = BackupStorageInventory.valueOf(self);
        extpEmitter.beforeDelete(inv);
        deleteHook();
        extpEmitter.afterDelete(inv);

        BackupStorageDeletionReply reply = new BackupStorageDeletionReply();
        tracker.untrack(self.getUuid());
        bus.reply(msg, reply);
    }

    protected void handleApiMessage(APIMessage msg) {
        try {
            if (msg instanceof APIDeleteBackupStorageMsg) {
                handle((APIDeleteBackupStorageMsg) msg);
            } else if (msg instanceof APIChangeBackupStorageStateMsg) {
                handle((APIChangeBackupStorageStateMsg) msg);
            } else if (msg instanceof APIAttachBackupStorageToZoneMsg) {
                handle((APIAttachBackupStorageToZoneMsg) msg);
            } else if (msg instanceof APIDetachBackupStorageFromZoneMsg) {
                handle((APIDetachBackupStorageFromZoneMsg) msg);
            } else if (msg instanceof APIUpdateBackupStorageMsg) {
                handle((APIUpdateBackupStorageMsg) msg);
            } else {
                bus.dealWithUnknownMessage(msg);
            }
        } catch (Exception e) {
            bus.logExceptionWithMessageDump(msg, e);
            bus.replyErrorByMessageType(msg, e);
        }
    }

    protected BackupStorageVO updateBackupStorage(APIUpdateBackupStorageMsg msg) {
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

    private void handle(APIUpdateBackupStorageMsg msg) {
        BackupStorageVO vo = updateBackupStorage(msg);
        if (vo != null) {
            self = dbf.updateAndRefresh(vo);
        }

        APIUpdateBackupStorageEvent evt = new APIUpdateBackupStorageEvent(msg.getId());
        evt.setInventory(getSelfInventory());
        bus.publish(evt);
    }

    protected void handle(final APIDetachBackupStorageFromZoneMsg msg) {
        final APIDetachBackupStorageFromZoneEvent evt = new APIDetachBackupStorageFromZoneEvent(msg.getId());

        try {
            extpEmitter.preDetach(self, msg.getZoneUuid());
        } catch (BackupStorageException e) {
            evt.setError(err(BackupStorageErrors.DETACH_ERROR, e.getMessage()));
            bus.publish(evt);
            return;
        }

        extpEmitter.beforeDetach(self, msg.getZoneUuid());
        detachHook(new Completion(msg) {
            @Transactional
            private BackupStorageVO updateDb(BackupStorageVO vo, String zoneUuid) {
                dbf.entityForTranscationCallback(TransactionalCallback.Operation.REMOVE, BackupStorageZoneRefVO.class);
                String sql = "delete from BackupStorageZoneRefVO bz where bz.zoneUuid = :zoneUuid and bz.backupStorageUuid = :bsUuid";
                Query q = dbf.getEntityManager().createQuery(sql);
                q.setParameter("zoneUuid", zoneUuid);
                q.setParameter("bsUuid", vo.getUuid());
                q.executeUpdate();
                vo = dbf.getEntityManager().find(BackupStorageVO.class, vo.getUuid());
                return vo;
            }

            @Override
            public void success() {
                self = updateDb(self, msg.getZoneUuid());
                extpEmitter.afterDetach(self, msg.getZoneUuid());

                evt.setInventory(getSelfInventory());
                logger.debug(String.format("successfully detached backup storage[uuid:%s] from zone[uuid:%s]", self.getUuid(), msg.getBackupStorageUuid()));
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(errorCode.getDetails());
                extpEmitter.failToDetach(self, msg.getZoneUuid());
                evt.setError(err(BackupStorageErrors.DETACH_ERROR, errorCode, errorCode.getDetails()));
                bus.publish(evt);
            }
        });
    }

    protected void handle(final APIAttachBackupStorageToZoneMsg msg) {
        final APIAttachBackupStorageToZoneEvent evt = new APIAttachBackupStorageToZoneEvent(msg.getId());
        final BackupStorageVO svo = dbf.findByUuid(msg.getBackupStorageUuid(), BackupStorageVO.class);

        String errStr = extpEmitter.preAttach(svo, msg.getZoneUuid());
        if (errStr != null) {
            evt.setError(err(BackupStorageErrors.ATTACH_ERROR, errStr));
            bus.publish(evt);
            return;
        }

        extpEmitter.beforeAttach(svo, msg.getZoneUuid());
        attachHook(msg.getZoneUuid(), new Completion(msg) {
            @Override
            public void success() {
                BackupStorageZoneRefVO rvo = new BackupStorageZoneRefVO();
                rvo.setBackupStorageUuid(svo.getUuid());
                rvo.setZoneUuid(msg.getZoneUuid());
                dbf.persist(rvo);

                refreshVO();
                extpEmitter.afterAttach(self, msg.getZoneUuid());

                evt.setInventory(getSelfInventory());
                logger.debug(String.format("successfully attached backup storage[uuid:%s, name:%s]", self.getUuid(), self.getName()));
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(errorCode.toString());
                extpEmitter.failToAttach(svo, msg.getZoneUuid());
                evt.setError(err(BackupStorageErrors.ATTACH_ERROR, errorCode, errorCode.getDetails()));
                bus.publish(evt);
            }
        });
    }

    protected void handle(APIChangeBackupStorageStateMsg msg) {
        APIChangeBackupStorageStateEvent evt = new APIChangeBackupStorageStateEvent(msg.getId());

        BackupStorageState currState = self.getState();
        BackupStorageStateEvent event = BackupStorageStateEvent.valueOf(msg.getStateEvent());
        BackupStorageState nextState = AbstractCdpBackupStorage.getNextState(currState, event);

        try {
            extpEmitter.preChange(self, event);
        } catch (BackupStorageException e) {
            evt.setError(err(SysErrors.CHANGE_RESOURCE_STATE_ERROR, e.getMessage()));
            bus.publish(evt);
            return;
        }

        extpEmitter.beforeChange(self, event);
        changeStateHook(event, nextState);
        self.setState(nextState);
        self = dbf.updateAndRefresh(self);
        extpEmitter.afterChange(self, event, currState);
        evt.setInventory(getSelfInventory());
        bus.publish(evt);
    }

    protected void handle(APIDeleteBackupStorageMsg msg) {
        final APIDeleteBackupStorageEvent evt = new APIDeleteBackupStorageEvent(msg.getId());

        final String issuer = BackupStorageVO.class.getSimpleName();
        final List<BackupStorageInventory> ctx = BackupStorageInventory.valueOf(Arrays.asList(self));
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-backup-storage-%s", msg.getUuid()));
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
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                evt.setError(err(SysErrors.DELETE_RESOURCE_ERROR, errCode, errCode.getDetails()));
                bus.publish(evt);
            }
        }).start();
    }

    protected void updateCapacity(Long totalCapacity, Long availableCapacity) {
        if (totalCapacity == null || availableCapacity == null) {
            return;
        }

        SQL.New(BackupStorageVO.class)
                .eq(BackupStorageVO_.uuid, self.getUuid())
                .set(BackupStorageVO_.totalCapacity, totalCapacity)
                .set(BackupStorageVO_.availableCapacity, availableCapacity)
                .update();
    }

    protected boolean changeStatus(BackupStorageStatus status) {
        self = dbf.reload(self);
        if (status == self.getStatus()) {
            return false;
        }

        BackupStorageStatus oldStatus = self.getStatus();

        self.setStatus(status);
        dbf.update(self);

        BackupStorageStatusChangedData d = new BackupStorageStatusChangedData();
        d.setBackupStorageUuid(self.getUuid());
        d.setNewStatus(status.toString());
        d.setOldStatus(oldStatus.toString());
        d.setInventory(BackupStorageInventory.valueOf(self));
        evtf.fire(BackupStorageCanonicalEvents.BACKUP_STORAGE_STATUS_CHANGED, d);

        logger.debug(String.format("change backup storage[uuid:%s] status to %s", self.getUuid(), status));

        return true;
    }
}
