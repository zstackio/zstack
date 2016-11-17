package org.zstack.storage.fusionstor.backup;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.*;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.image.APIAddImageMsg;
import org.zstack.header.image.ImageBackupStorageRefInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.APIMessage;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.backup.*;
import org.zstack.storage.backup.BackupStorageBase;
import org.zstack.storage.fusionstor.*;
import org.zstack.storage.fusionstor.FusionstorMonBase.PingResult;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 7/27/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class FusionstorBackupStorageBase extends BackupStorageBase {
    private static final CLogger logger = Utils.getLogger(FusionstorBackupStorageBase.class);

    class ReconnectMonLock {
        AtomicBoolean hold = new AtomicBoolean(false);

        boolean lock() {
            return hold.compareAndSet(false, true);
        }

        void unlock() {
            hold.set(false);
        }
    }

    ReconnectMonLock reconnectMonLock = new ReconnectMonLock();

    @Autowired
    protected RESTFacade restf;

    public static class AgentCommand {
        String fsid;
        String uuid;

        public String getFsid() {
            return fsid;
        }

        public void setFsid(String fsid) {
            this.fsid = fsid;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }
    }

    public static class AgentResponse {
        String error;
        boolean success = true;
        Long totalCapacity;
        Long availableCapacity;

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public Long getTotalCapacity() {
            return totalCapacity;
        }

        public void setTotalCapacity(Long totalCapacity) {
            this.totalCapacity = totalCapacity;
        }

        public Long getAvailableCapacity() {
            return availableCapacity;
        }

        public void setAvailableCapacity(Long availableCapacity) {
            this.availableCapacity = availableCapacity;
        }
    }

    public static class Pool {
        String name;
        boolean predefined;
    }

    public static class InitCmd extends AgentCommand {
        List<Pool> pools;
    }

    public static class InitRsp extends AgentResponse {
        String fsid;

        public String getFsid() {
            return fsid;
        }

        public void setFsid(String fsid) {
            this.fsid = fsid;
        }
    }

    @ApiTimeout(apiClasses = {APIAddImageMsg.class})
    public static class DownloadCmd extends AgentCommand {
        String url;
        String installPath;
        String imageUuid;

        public String getImageUuid() {
            return imageUuid;
        }

        public void setImageUuid(String imageUuid) {
            this.imageUuid = imageUuid;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }
    }

    public static class DownloadRsp extends AgentResponse {
        long size;
        Long actualSize;
        String format;

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }

        public Long getActualSize() {
            return actualSize;
        }

        public void setActualSize(Long actualSize) {
            this.actualSize = actualSize;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }

    public static class DeleteCmd extends AgentCommand {
        String installPath;

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }
    }

    public static class DeleteRsp extends AgentResponse {
    }

    public static class PingCmd extends AgentCommand {
    }

    public static class PingRsp extends AgentResponse {

    }

    public static class GetImageSizeCmd extends AgentCommand {
        public String imageUuid;
        public String installPath;
    }

    public static class GetImageSizeRsp extends AgentResponse {
        public Long size;
        public Long actualSize;
    }

    public static class GetFactsCmd extends AgentCommand {
        public String monUuid;
    }

    public static class GetFactsRsp extends AgentResponse {
        public String fsid;
    }

    public static final String INIT_PATH = "/fusionstor/backupstorage/init";
    public static final String DOWNLOAD_IMAGE_PATH = "/fusionstor/backupstorage/image/download";
    public static final String DELETE_IMAGE_PATH = "/fusionstor/backupstorage/image/delete";
    public static final String GET_IMAGE_SIZE_PATH = "/fusionstor/backupstorage/image/getsize";
    public static final String PING_PATH = "/fusionstor/backupstorage/ping";
    public static final String GET_FACTS = "/fusionstor/backupstorage/facts";

    protected String makeImageInstallPath(String imageUuid) {
        return String.format("fusionstor://%s/%s", getSelf().getPoolName(), imageUuid);
    }

    private <T extends AgentResponse> void httpCall(final String path, final AgentCommand cmd, final Class<T> retClass, final ReturnValueCompletion<T> callback) {
        cmd.setFsid(getSelf().getFsid());
        cmd.setUuid(self.getUuid());

        final List<FusionstorBackupStorageMonBase> mons = new ArrayList<FusionstorBackupStorageMonBase>();
        for (FusionstorBackupStorageMonVO monvo : getSelf().getMons()) {
            if (monvo.getStatus() == MonStatus.Connected) {
                mons.add(new FusionstorBackupStorageMonBase(monvo));
            }
        }

        if (mons.isEmpty()) {
            throw new OperationFailureException(errf.stringToOperationError(
                    String.format("all fusionstor mons are Disconnected in fusionstor backup storage[uuid:%s]", self.getUuid())
            ));
        }

        Collections.shuffle(mons);

        class HttpCaller {
            Iterator<FusionstorBackupStorageMonBase> it = mons.iterator();
            List<ErrorCode> errorCodes = new ArrayList<ErrorCode>();

            void call() {
                if (!it.hasNext()) {
                    callback.fail(errf.stringToOperationError(
                            String.format("all mons failed to execute http call[%s], errors are %s", path, JSONObjectUtil.toJsonString(errorCodes))
                    ));

                    return;
                }

                FusionstorBackupStorageMonBase base = it.next();
                base.httpCall(path, cmd, retClass, new ReturnValueCompletion<T>() {
                    @Override
                    public void success(T ret) {
                        if (!ret.success) {
                            // not an IO error but an operation error, return it
                            callback.fail(errf.stringToOperationError(ret.error));
                        } else {
                            if (!(cmd instanceof InitCmd)) {
                                updateCapacityIfNeeded(ret);
                            }

                            callback.success(ret);
                        }
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        errorCodes.add(errorCode);
                        call();
                    }
                });
            }
        }

        new HttpCaller().call();
    }

    public FusionstorBackupStorageBase(BackupStorageVO self) {
        super(self);
    }

    protected FusionstorBackupStorageVO getSelf() {
        return (FusionstorBackupStorageVO) self;
    }

    protected FusionstorBackupStorageInventory getInventory() {
        return FusionstorBackupStorageInventory.valueOf(getSelf());
    }

    private void updateCapacityIfNeeded(AgentResponse rsp) {
        if (rsp.getTotalCapacity() != null && rsp.getAvailableCapacity() != null) {
            new FusionstorCapacityUpdater().update(getSelf().getFsid(), rsp.totalCapacity, rsp.availableCapacity);
        }
    }

    @Override
    protected void handle(final DownloadImageMsg msg) {
        final DownloadCmd cmd = new DownloadCmd();
        cmd.url = msg.getImageInventory().getUrl();
        cmd.installPath = makeImageInstallPath(msg.getImageInventory().getUuid());
        cmd.imageUuid = msg.getImageInventory().getUuid();

        final DownloadImageReply reply = new DownloadImageReply();
        httpCall(DOWNLOAD_IMAGE_PATH, cmd, DownloadRsp.class, new ReturnValueCompletion<DownloadRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(DownloadRsp ret) {
                reply.setInstallPath(cmd.installPath);
                reply.setSize(ret.size);

                // current fusionstor has no way to get the actual size
                // if we cannot get the actual size from HTTP, use the virtual size
                long asize = ret.actualSize == null ? ret.size : ret.actualSize;
                reply.setActualSize(asize);
                reply.setMd5sum("not calculated");
                if ( msg.getFormat().equals("iso") && ret.format.equals("raw")) {
                    reply.setFormat("iso");
                } else {
                    reply.setFormat(ret.format);
                }
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final DownloadVolumeMsg msg) {
        final DownloadCmd cmd = new DownloadCmd();
        cmd.url = msg.getUrl();
        cmd.installPath = makeImageInstallPath(msg.getVolume().getUuid());

        final DownloadVolumeReply reply = new DownloadVolumeReply();
        httpCall(DOWNLOAD_IMAGE_PATH, cmd, DownloadRsp.class, new ReturnValueCompletion<DownloadRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(DownloadRsp ret) {
                reply.setInstallPath(cmd.installPath);
                reply.setSize(ret.size);
                reply.setMd5sum("not calculated");
                bus.reply(msg, reply);
            }
        });
    }

    @Transactional(readOnly = true)
    private boolean canDelete(String installPath) {
        String sql = "select count(c)" +
                " from ImageBackupStorageRefVO img, ImageCacheVO c" +
                " where img.imageUuid = c.imageUuid" +
                " and img.backupStorageUuid = :bsUuid" +
                " and img.installPath = :installPath";
        TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
        q.setParameter("bsUuid", self.getUuid());
        q.setParameter("installPath", installPath);
        return q.getSingleResult() == 0;
    }

    @Override
    protected void handle(final GetImageSizeOnBackupStorageMsg msg){
        //TODO
        throw new CloudRuntimeException(String.format("not implemented"));
    }

    @Override
    protected void handle(final DeleteBitsOnBackupStorageMsg msg) {
        final DeleteBitsOnBackupStorageReply reply = new DeleteBitsOnBackupStorageReply();
        if (!canDelete(msg.getInstallPath())) {
            //TODO: the image is still referred, need to cleanup
            bus.reply(msg, reply);
            return;
        }

        DeleteCmd cmd = new DeleteCmd();
        cmd.installPath = msg.getInstallPath();

        httpCall(DELETE_IMAGE_PATH, cmd, DeleteRsp.class, new ReturnValueCompletion<DeleteRsp>() {
            @Override
            public void fail(ErrorCode err) {
                //TODO
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(DeleteRsp ret) {
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(BackupStorageAskInstallPathMsg msg) {
        BackupStorageAskInstallPathReply reply = new BackupStorageAskInstallPathReply();
        reply.setInstallPath(makeImageInstallPath(msg.getImageUuid()));
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(final SyncImageSizeOnBackupStorageMsg msg) {
        GetImageSizeCmd cmd = new GetImageSizeCmd();
        cmd.imageUuid = msg.getImage().getUuid();

        ImageBackupStorageRefInventory ref = CollectionUtils.find(msg.getImage().getBackupStorageRefs(), new Function<ImageBackupStorageRefInventory, ImageBackupStorageRefInventory>() {
            @Override
            public ImageBackupStorageRefInventory call(ImageBackupStorageRefInventory arg) {
                return self.getUuid().equals(arg.getBackupStorageUuid()) ? arg : null;
            }
        });

        if (ref == null) {
            throw new CloudRuntimeException(String.format("cannot find ImageBackupStorageRefInventory of image[uuid:%s] for" +
                    " the backup storage[uuid:%s]", msg.getImage().getUuid(), self.getUuid()));
        }

        final SyncImageSizeOnBackupStorageReply reply = new SyncImageSizeOnBackupStorageReply();
        cmd.installPath = ref.getInstallPath();
        httpCall(GET_IMAGE_SIZE_PATH, cmd, GetImageSizeRsp.class, new ReturnValueCompletion<GetImageSizeRsp>(msg) {
            @Override
            public void success(GetImageSizeRsp rsp) {
                reply.setSize(rsp.size);

                // current fusionstor cannot get actual size
                long asize = rsp.actualSize == null ? msg.getImage().getActualSize() : rsp.actualSize;
                reply.setActualSize(asize);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void connectHook(final boolean newAdded, final Completion completion) {
        final List<FusionstorBackupStorageMonBase> mons = CollectionUtils.transformToList(getSelf().getMons(), new Function<FusionstorBackupStorageMonBase, FusionstorBackupStorageMonVO>() {
            @Override
            public FusionstorBackupStorageMonBase call(FusionstorBackupStorageMonVO arg) {
                return new FusionstorBackupStorageMonBase(arg);
            }
        });

        class Connector {
            List<ErrorCode> errorCodes = new ArrayList<ErrorCode>();
            Iterator<FusionstorBackupStorageMonBase> it = mons.iterator();

            void connect(final FlowTrigger trigger) {
                if (!it.hasNext()) {
                    if (errorCodes.size() == mons.size()) {
                        trigger.fail(errf.stringToOperationError(
                                String.format("unable to connect to the fusionstor backup storage[uuid:%s]. Failed to connect all fusionstor mons. Errors are %s",
                                        self.getUuid(), JSONObjectUtil.toJsonString(errorCodes))
                        ));
                    } else {
                        // reload because mon status changed
                        self = dbf.reload(self);
                        trigger.next();
                    }
                    return;
                }

                final FusionstorBackupStorageMonBase base = it.next();
                base.connect(new Completion(completion) {
                    @Override
                    public void success() {
                        connect(trigger);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        errorCodes.add(errorCode);

                        if (newAdded) {
                            // the mon fails to connect, remove it
                            dbf.remove(base.getSelf());
                        }

                        connect(trigger);
                    }
                });
            }
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("connect-fusionstor-backup-storage-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "connect-monitor";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new Connector().connect(trigger);
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "check-mon-integrity";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        final Map<String, String> fsids = new HashMap<String, String>();

                        final List<FusionstorBackupStorageMonBase> mons = CollectionUtils.transformToList(getSelf().getMons(), new Function<FusionstorBackupStorageMonBase, FusionstorBackupStorageMonVO>() {
                            @Override
                            public FusionstorBackupStorageMonBase call(FusionstorBackupStorageMonVO arg) {
                                return arg.getStatus() == MonStatus.Connected ? new FusionstorBackupStorageMonBase(arg) : null;
                            }
                        });

                        DebugUtils.Assert(!mons.isEmpty(), "how can be no connected MON!!! ???");

                        final AsyncLatch latch = new AsyncLatch(mons.size(), new NoErrorCompletion(trigger) {
                            @Override
                            public void done() {
                                Set<String> set = new HashSet<String>();
                                set.addAll(fsids.values());

                                if (set.size() != 1) {
                                    StringBuilder sb =  new StringBuilder("the fsid returned by mons are mismatching, it seems the mons belong to different fusionstor clusters:\n");
                                    for (FusionstorBackupStorageMonBase mon : mons) {
                                        String fsid = fsids.get(mon.getSelf().getUuid());
                                        sb.append(String.format("%s (mon ip) --> %s (fsid)\n", mon.getSelf().getHostname(), fsid));
                                    }

                                    throw new OperationFailureException(errf.stringToOperationError(sb.toString()));
                                }

                                // check if there is another fusion setup having the same fsid
                                String fsId = set.iterator().next();

                                SimpleQuery<FusionstorBackupStorageVO>  q = dbf.createQuery(FusionstorBackupStorageVO.class);
                                q.add(FusionstorBackupStorageVO_.fsid, Op.EQ, fsId);
                                q.add(FusionstorBackupStorageVO_.uuid, Op.NOT_EQ, self.getUuid());
                                FusionstorBackupStorageVO otherfusion = q.find();
                                if (otherfusion != null) {
                                    throw new OperationFailureException(errf.stringToOperationError(
                                            String.format("there is another Fusionstor backup storage[name:%s, uuid:%s] with the same" +
                                                            " FSID[%s], you cannot add the same Fusionstor setup as two different backup storage",
                                                    otherfusion.getName(), otherfusion.getUuid(), fsId)
                                    ));
                                }

                                trigger.next();
                            }
                        });

                        for (final FusionstorBackupStorageMonBase mon : mons) {
                            GetFactsCmd cmd = new GetFactsCmd();
                            cmd.uuid = self.getUuid();
                            cmd.monUuid = mon.getSelf().getUuid();
                            mon.httpCall(GET_FACTS, cmd, GetFactsRsp.class, new ReturnValueCompletion<GetFactsRsp>(latch) {
                                @Override
                                public void success(GetFactsRsp rsp) {
                                    if (!rsp.success) {
                                        // one mon cannot get the facts, directly error out
                                        trigger.fail(errf.stringToOperationError(rsp.error));
                                        return;
                                    }

                                    fsids.put(mon.getSelf().getUuid(), rsp.fsid);
                                    latch.ack();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    // one mon cannot get the facts, directly error out
                                    trigger.fail(errorCode);
                                }
                            });
                        }

                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "init";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        InitCmd cmd = new InitCmd();
                        Pool p = new Pool();
                        p.name = getSelf().getPoolName();
                        p.predefined = FusionstorSystemTags.PREDEFINED_BACKUP_STORAGE_POOL.hasTag(self.getUuid());
                        cmd.pools = list(p);

                        httpCall(INIT_PATH, cmd, InitRsp.class, new ReturnValueCompletion<InitRsp>(trigger) {
                            @Override
                            public void fail(ErrorCode err) {
                                trigger.fail(err);
                            }

                            @Override
                            public void success(InitRsp ret) {
                                if (getSelf().getFsid() == null) {
                                    getSelf().setFsid(ret.fsid);
                                    self = dbf.updateAndRefresh(self);
                                }

                                FusionstorCapacityUpdater updater = new FusionstorCapacityUpdater();
                                updater.update(ret.fsid, ret.totalCapacity, ret.availableCapacity, true);
                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        if (newAdded) {
                            self = dbf.reload(self);
                            if (!getSelf().getMons().isEmpty()) {
                                dbf.removeCollection(getSelf().getMons(), FusionstorBackupStorageMonVO.class);
                            }
                        }

                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void pingHook(final Completion completion) {
        final List<FusionstorBackupStorageMonBase> mons = CollectionUtils.transformToList(getSelf().getMons(), new Function<FusionstorBackupStorageMonBase, FusionstorBackupStorageMonVO>() {
            @Override
            public FusionstorBackupStorageMonBase call(FusionstorBackupStorageMonVO arg) {
                return new FusionstorBackupStorageMonBase(arg);
            }
        });

        final List<ErrorCode> errors = new ArrayList<ErrorCode>();

        class Ping {
            private AtomicBoolean replied = new AtomicBoolean(false);

            @AsyncThread
            private void reconnectMon(final FusionstorBackupStorageMonBase mon, boolean delay) {
                if (!FusionstorGlobalConfig.BACKUP_STORAGE_MON_AUTO_RECONNECT.value(Boolean.class)) {
                    logger.debug(String.format("do not reconnect the fusionstor backup storage mon[uuid:%s] as the global config[%s] is set to false",
                            self.getUuid(), FusionstorGlobalConfig.BACKUP_STORAGE_MON_AUTO_RECONNECT.getCanonicalName()));
                    return;
                }

                // there has been a reconnect in process
                if (!reconnectMonLock.lock()) {
                    return;
                }

                final NoErrorCompletion releaseLock = new NoErrorCompletion() {
                    @Override
                    public void done() {
                        reconnectMonLock.unlock();
                    }
                };

                try {
                    if (delay) {
                        try {
                            TimeUnit.SECONDS.sleep(FusionstorGlobalConfig.BACKUP_STORAGE_MON_RECONNECT_DELAY.value(Long.class));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    mon.connect(new Completion(releaseLock) {
                        @Override
                        public void success() {
                            logger.debug(String.format("successfully reconnected the mon[uuid:%s] of the fusionstor backup" +
                                    " storage[uuid:%s, name:%s]", mon.getSelf().getUuid(), self.getUuid(), self.getName()));
                            releaseLock.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            //TODO
                            logger.warn(String.format("failed to reconnect the mon[uuid:%s] of the fusionstor backup" +
                                    " storage[uuid:%s, name:%s], %s", mon.getSelf().getUuid(), self.getUuid(), self.getName(), errorCode));
                            releaseLock.done();
                        }
                    });
                } catch (Throwable t) {
                    releaseLock.done();
                    logger.warn(t.getMessage(), t);
                }
            }

            void ping() {
                // this is only called when all mons are disconnected
                final AsyncLatch latch = new AsyncLatch(mons.size(), new NoErrorCompletion() {
                    @Override
                    public void done() {
                        if (!replied.compareAndSet(false, true)) {
                            return;
                        }

                        ErrorCode err =  errf.stringToOperationError(String.format("failed to ping the fusionstor backup storage[uuid:%s, name:%s]",
                                self.getUuid(), self.getName()), errors);
                        completion.fail(err);
                    }
                });

                for (final FusionstorBackupStorageMonBase mon : mons) {
                    mon.ping(new ReturnValueCompletion<PingResult>(latch) {
                        private void thisMonIsDown(ErrorCode err) {
                            //TODO
                            logger.warn(String.format("cannot ping mon[uuid:%s] of the fusionstor backup storage[uuid:%s, name:%s], %s",
                                    mon.getSelf().getUuid(), self.getUuid(), self.getName(), err));
                            errors.add(err);
                            mon.changeStatus(MonStatus.Disconnected);
                            reconnectMon(mon, true);
                            latch.ack();
                        }

                        @Override
                        public void success(PingResult res) {
                            if (res.success) {
                                // as long as there is one mon working, the backup storage works
                                pingSuccess();

                                if (mon.getSelf().getStatus() == MonStatus.Disconnected) {
                                    reconnectMon(mon, false);
                                }

                            } else if (res.operationFailure) {
                                // as long as there is one mon saying the fusionstor not working, the backup storage goes down
                                logger.warn(String.format("the fusionstor backup storage[uuid:%s, name:%s] is down, as one mon[uuid:%s] reports" +
                                        " an operation failure[%s]", self.getUuid(), self.getName(), mon.getSelf().getUuid(), res.error));
                                backupStorageDown();
                            } else  {
                                // this mon is down(success == false, operationFailure == false), but the backup storage may still work as other mons may work
                                ErrorCode errorCode = errf.stringToOperationError(res.error);
                                thisMonIsDown(errorCode);
                            }
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            thisMonIsDown(errorCode);
                        }
                    });
                }
            }

            // this is called once a mon return an operation failure
            private void backupStorageDown() {
                if (!replied.compareAndSet(false, true)) {
                    return;
                }

                // set all mons to be disconnected
                for (FusionstorBackupStorageMonBase base : mons) {
                    base.changeStatus(MonStatus.Disconnected);
                }

                ErrorCode err = errf.stringToOperationError(String.format("failed to ping the backup primary storage[uuid:%s, name:%s]",
                        self.getUuid(), self.getName()), errors);
                completion.fail(err);
            }

            private void pingSuccess() {
                if (!replied.compareAndSet(false, true)) {
                    return;
                }

                completion.success();
            }
        }

        new Ping().ping();
    }

    @Override
    public List<ImageInventory> scanImages() {
        return null;
    }

    @Override
    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIAddMonToFusionstorBackupStorageMsg) {
            handle((APIAddMonToFusionstorBackupStorageMsg) msg);
        } else if (msg instanceof APIRemoveMonFromFusionstorBackupStorageMsg) {
            handle((APIRemoveMonFromFusionstorBackupStorageMsg) msg);
        } else {
            super.handleApiMessage(msg);
        }
    }

    private void handle(APIRemoveMonFromFusionstorBackupStorageMsg msg) {
        SimpleQuery<FusionstorBackupStorageMonVO> q = dbf.createQuery(FusionstorBackupStorageMonVO.class);
        q.add(FusionstorBackupStorageMonVO_.hostname, Op.IN, msg.getMonHostnames());
        q.add(FusionstorBackupStorageMonVO_.backupStorageUuid, Op.EQ, self.getUuid());
        List<FusionstorBackupStorageMonVO> vos = q.list();

        if (!vos.isEmpty()) {
            dbf.removeCollection(vos, FusionstorBackupStorageMonVO.class);
        }

        APIRemoveMonFromFusionstorBackupStorageEvent evt = new APIRemoveMonFromFusionstorBackupStorageEvent(msg.getId());
        evt.setInventory(FusionstorBackupStorageInventory.valueOf(dbf.reload(getSelf())));
        bus.publish(evt);
    }

    private void handle(final APIAddMonToFusionstorBackupStorageMsg msg) {
        final APIAddMonToFusionstorBackupStorageEvent evt = new APIAddMonToFusionstorBackupStorageEvent(msg.getId());

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("add-mon-fusionstor-backup-storage-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            List<FusionstorBackupStorageMonVO> monVOs = new ArrayList<FusionstorBackupStorageMonVO>();

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "create-mon-in-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        for (String url : msg.getMonUrls()) {
                            FusionstorBackupStorageMonVO monvo = new FusionstorBackupStorageMonVO();
                            MonUri uri = new MonUri(url);
                            monvo.setUuid(Platform.getUuid());
                            monvo.setStatus(MonStatus.Connecting);
                            monvo.setHostname(uri.getHostname());
                            monvo.setMonPort(uri.getMonPort());
                            monvo.setSshPort(uri.getSshPort());
                            monvo.setSshUsername(uri.getSshUsername());
                            monvo.setSshPassword(uri.getSshPassword());
                            monvo.setBackupStorageUuid(self.getUuid());
                            monVOs.add(monvo);
                        }

                        dbf.persistCollection(monVOs);
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        dbf.removeCollection(monVOs, FusionstorBackupStorageMonVO.class);
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "connect-mons";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        List<FusionstorBackupStorageMonBase> bases = CollectionUtils.transformToList(monVOs, new Function<FusionstorBackupStorageMonBase, FusionstorBackupStorageMonVO>() {
                            @Override
                            public FusionstorBackupStorageMonBase call(FusionstorBackupStorageMonVO arg) {
                                return new FusionstorBackupStorageMonBase(arg);
                            }
                        });

                        final List<ErrorCode> errorCodes = new ArrayList<ErrorCode>();
                        final AsyncLatch latch = new AsyncLatch(bases.size(), new NoErrorCompletion(trigger) {
                            @Override
                            public void done() {
                                if (!errorCodes.isEmpty()) {
                                    trigger.fail(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR, "unable to connect mons", errorCodes));
                                } else {
                                    trigger.next();
                                }
                            }
                        });

                        for (FusionstorBackupStorageMonBase base : bases) {
                            base.connect(new Completion(trigger) {
                                @Override
                                public void success() {
                                    latch.ack();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    errorCodes.add(errorCode);
                                    latch.ack();
                                }
                            });
                        }
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "check-mon-integrity";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        List<FusionstorBackupStorageMonBase> bases = CollectionUtils.transformToList(monVOs, new Function<FusionstorBackupStorageMonBase, FusionstorBackupStorageMonVO>() {
                            @Override
                            public FusionstorBackupStorageMonBase call(FusionstorBackupStorageMonVO arg) {
                                return new FusionstorBackupStorageMonBase(arg);
                            }
                        });

                        final List<ErrorCode> errors = new ArrayList<ErrorCode>();

                        final AsyncLatch latch = new AsyncLatch(bases.size(), new NoErrorCompletion(trigger) {
                            @Override
                            public void done() {
                                // one fail, all fail
                                if (!errors.isEmpty()) {
                                    trigger.fail(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR, "unable to add mon to fusionstor backup storage", errors));
                                } else {
                                    trigger.next();
                                }
                            }
                        });

                        for (final FusionstorBackupStorageMonBase base : bases) {
                            GetFactsCmd cmd = new GetFactsCmd();
                            cmd.uuid = self.getUuid();
                            cmd.monUuid = base.getSelf().getUuid();
                            base.httpCall(GET_FACTS, cmd, GetFactsRsp.class, new ReturnValueCompletion<GetFactsRsp>(latch) {
                                @Override
                                public void success(GetFactsRsp rsp) {
                                    if (!rsp.isSuccess()) {
                                        errors.add(errf.stringToOperationError(rsp.getError()));
                                    } else {
                                        String fsid = rsp.fsid;
                                        if (!getSelf().getFsid().equals(fsid)) {
                                            errors.add(errf.stringToOperationError(
                                                    String.format("the mon[ip:%s] returns a fsid[%s] different from the current fsid[%s] of the cep cluster," +
                                                            "are you adding a mon not belonging to current cluster mistakenly?", base.getSelf().getHostname(), fsid, getSelf().getFsid())
                                            ));
                                        }
                                    }

                                    latch.ack();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    errors.add(errorCode);
                                    latch.ack();
                                }
                            });
                        }
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        evt.setInventory(FusionstorBackupStorageInventory.valueOf(dbf.reload(getSelf())));
                        bus.publish(evt);
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

    @Override
    public void deleteHook() {
        dbf.removeCollection(getSelf().getMons(), FusionstorBackupStorageMonVO.class);
    }
}
