package org.zstack.storage.fusionstor.backup;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.AsyncLatch;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.APIMessage;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.backup.*;
import org.zstack.storage.backup.BackupStorageBase;
import org.zstack.storage.fusionstor.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;

import javax.persistence.TypedQuery;
import java.util.*;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 7/27/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class FusionstorBackupStorageBase extends BackupStorageBase {

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

    public static class DownloadCmd extends AgentCommand {
        String url;
        String installPath;

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

    public static final String INIT_PATH = "/fusionstor/backupstorage/init";
    public static final String DOWNLOAD_IMAGE_PATH = "/fusionstor/backupstorage/image/download";
    public static final String DELETE_IMAGE_PATH = "/fusionstor/backupstorage/image/delete";
    public static final String PING_PATH = "/fusionstor/backupstorage/ping";

    protected String makeHttpPath(String ip, String path) {
        return String.format("http://%s:%s%s", ip, FusionstorGlobalProperty.BACKUP_STORAGE_AGENT_PORT, path);
    }

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

                restf.asyncJsonPost(makeHttpPath(base.getSelf().getHostname(), path), cmd, new JsonAsyncRESTCallback<T>() {
                    @Override
                    public void fail(ErrorCode err) {
                        errorCodes.add(err);
                        call();
                    }

                    @Override
                    public void success(T ret) {
                        if (!ret.success) {
                            callback.fail(errf.stringToOperationError(ret.error));
                        } else {
                            if (!(cmd instanceof InitCmd)) {
                                updateCapacityIfNeeded(ret);
                            }

                            callback.success(ret);
                        }
                    }

                    @Override
                    public Class<T> getReturnClass() {
                        return retClass;
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
                reply.setMd5sum("not calculated");
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
        String sql = "select count(c) from ImageBackupStorageRefVO img, ImageCacheVO c where img.imageUuid = c.imageUuid and img.backupStorageUuid = :bsUuid and img.installPath = :installPath";
        TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
        q.setParameter("bsUuid", self.getUuid());
        q.setParameter("installPath", installPath);
        return q.getSingleResult() == 0;
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
    protected void handle(SyncImageSizeOnBackupStorageMsg msg) {
        throw new CloudRuntimeException("not implemented yet");
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
                        trigger.next();
                    }
                    return;
                }

                FusionstorBackupStorageMonBase base = it.next();
                base.connect(new Completion(completion) {
                    @Override
                    public void success() {
                        connect(trigger);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        errorCodes.add(errorCode);
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
                            dbf.removeCollection(getSelf().getMons(), FusionstorBackupStorageMonVO.class);
                        }

                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void pingHook(final Completion completion) {
        PingCmd cmd = new PingCmd();

        httpCall(PING_PATH, cmd, PingRsp.class, new ReturnValueCompletion<PingRsp>(completion) {
            @Override
            public void fail(ErrorCode err) {
                completion.fail(err);
            }

            @Override
            public void success(PingRsp ret) {
                if (ret.isSuccess()) {
                    completion.success();
                } else {
                    completion.fail(errf.stringToOperationError(ret.getError()));
                }
            }
        });
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

                        final AsyncLatch latch = new AsyncLatch(bases.size(), new NoErrorCompletion(trigger) {
                            @Override
                            public void done() {
                                trigger.next();
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
                                    // one fails, all fail
                                    trigger.fail(errorCode);
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
