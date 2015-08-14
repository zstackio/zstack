package org.zstack.storage.ceph.backup;

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
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.APIMessage;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.backup.*;
import org.zstack.storage.backup.BackupStorageBase;
import org.zstack.storage.ceph.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;

import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 7/27/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CephBackupStorageBase extends BackupStorageBase {

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

    public static final String INIT_PATH = "/ceph/backupstorage/init";
    public static final String DOWNLOAD_IMAGE_PATH = "/ceph/backupstorage/image/download";
    public static final String DELETE_IMAGE_PATH = "/ceph/backupstorage/image/delete";
    public static final String PING_PATH = "/ceph/backupstorage/ping";

    protected String makeHttpPath(String ip, String path) {
        return String.format("http://%s:%s%s", ip, CephGlobalProperty.BACKUP_STORAGE_AGENT_PORT, path);
    }

    protected String makeImageInstallPath(String imageUuid) {
        return String.format("ceph://%s/%s", getSelf().getPoolName(), imageUuid);
    }

    private <T extends AgentResponse> void httpCall(final String path, final AgentCommand cmd, Class<T> retClass, final ReturnValueCompletion<T> callback) {
        httpCall(path, cmd, retClass, callback, 5, TimeUnit.MINUTES);
    }

    private <T extends AgentResponse> void httpCall(final String path, final AgentCommand cmd, final Class<T> retClass, final ReturnValueCompletion<T> callback, final long timeout, final TimeUnit timeUnit) {
        cmd.setFsid(getSelf().getFsid());
        cmd.setUuid(self.getUuid());

        final List<CephBackupStorageMonBase> mons = new ArrayList<CephBackupStorageMonBase>();
        for (CephBackupStorageMonVO monvo : getSelf().getMons()) {
            if (monvo.getStatus() == MonStatus.Connected) {
                mons.add(new CephBackupStorageMonBase(monvo));
            }
        }

        if (mons.isEmpty()) {
            throw new OperationFailureException(errf.stringToOperationError(
                    String.format("all ceph mons are Disconnected in ceph backup storage[uuid:%s]", self.getUuid())
            ));
        }

        Collections.shuffle(mons);

        class HttpCaller {
            Iterator<CephBackupStorageMonBase> it = mons.iterator();
            List<ErrorCode> errorCodes = new ArrayList<ErrorCode>();

            void call() {
                if (!it.hasNext()) {
                    callback.fail(errf.stringToOperationError(
                            String.format("all mons failed to execute http call[%s], errors are %s", path, JSONObjectUtil.toJsonString(errorCodes))
                    ));

                    return;
                }

                CephBackupStorageMonBase base = it.next();

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
                }, timeUnit, timeout);
            }
        }

        new HttpCaller().call();
    }

    public CephBackupStorageBase(BackupStorageVO self) {
        super(self);
    }

    protected CephBackupStorageVO getSelf() {
        return (CephBackupStorageVO) self;
    }

    protected CephBackupStorageInventory getInventory() {
        return CephBackupStorageInventory.valueOf(getSelf());
    }

    private void updateCapacityIfNeeded(AgentResponse rsp) {
        if (rsp.getTotalCapacity() != null && rsp.getAvailableCapacity() != null) {
            new CephCapacityUpdater().update(getSelf().getFsid(), rsp.totalCapacity, rsp.availableCapacity);
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
        }, CephGlobalConfig.BACKUP_STORAGE_DOWNLOAD_IMAGE_TIMEOUT.value(Long.class), TimeUnit.SECONDS);
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
        }, CephGlobalConfig.BACKUP_STORAGE_DOWNLOAD_IMAGE_TIMEOUT.value(Long.class), TimeUnit.SECONDS);
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
    protected void handle(final PingBackupStorageMsg msg) {
        PingCmd cmd = new PingCmd();

        final PingBackupStorageReply reply = new PingBackupStorageReply();
        httpCall(PING_PATH, cmd, PingRsp.class, new ReturnValueCompletion<PingRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setAvailable(false);
                bus.reply(msg, reply);
            }

            @Override
            public void success(PingRsp ret) {
                reply.setAvailable(true);
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
    protected void connectHook(final Completion completion) {
        final List<CephBackupStorageMonBase> mons = CollectionUtils.transformToList(getSelf().getMons(), new Function<CephBackupStorageMonBase, CephBackupStorageMonVO>() {
            @Override
            public CephBackupStorageMonBase call(CephBackupStorageMonVO arg) {
                return new CephBackupStorageMonBase(arg);
            }
        });

        class Connector {
            List<ErrorCode> errorCodes = new ArrayList<ErrorCode>();
            Iterator<CephBackupStorageMonBase> it = mons.iterator();

            void connect(final FlowTrigger trigger) {
                if (!it.hasNext()) {
                    if (errorCodes.size() == mons.size()) {
                        trigger.fail(errf.stringToOperationError(
                                String.format("unable to connect to the ceph backup storage[uuid:%s]. Failed to connect all ceph mons. Errors are %s",
                                        self.getUuid(), JSONObjectUtil.toJsonString(errorCodes))
                        ));
                    } else {
                        trigger.next();
                    }
                    return;
                }

                CephBackupStorageMonBase base = it.next();
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
        chain.setName(String.format("connect-ceph-backup-storage-%s", self.getUuid()));
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
                        p.predefined = CephSystemTags.PREDEFINED_BACKUP_STORAGE_POOL.hasTag(self.getUuid());
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

                                CephCapacityUpdater updater = new CephCapacityUpdater();
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
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    public List<ImageInventory> scanImages() {
        return null;
    }

    @Override
    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIAddMonToCephBackupStorageMsg) {
            handle((APIAddMonToCephBackupStorageMsg) msg);
        } else if (msg instanceof APIRemoveMonFromCephBackupStorageMsg) {
            handle((APIRemoveMonFromCephBackupStorageMsg) msg);
        } else {
            super.handleApiMessage(msg);
        }
    }

    private void handle(APIRemoveMonFromCephBackupStorageMsg msg) {
        SimpleQuery<CephBackupStorageMonVO> q = dbf.createQuery(CephBackupStorageMonVO.class);
        q.add(CephBackupStorageMonVO_.hostname, Op.IN, msg.getMonHostnames());
        q.add(CephBackupStorageMonVO_.backupStorageUuid, Op.EQ, self.getUuid());
        List<CephBackupStorageMonVO> vos = q.list();

        if (!vos.isEmpty()) {
            dbf.removeCollection(vos, CephBackupStorageMonVO.class);
        }

        APIRemoveMonFromCephBackupStorageEvent evt = new APIRemoveMonFromCephBackupStorageEvent(msg.getId());
        evt.setInventory(CephBackupStorageInventory.valueOf(dbf.reload(getSelf())));
        bus.publish(evt);
    }

    private void handle(final APIAddMonToCephBackupStorageMsg msg) {
        final APIAddMonToCephBackupStorageEvent evt = new APIAddMonToCephBackupStorageEvent(msg.getId());

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("add-mon-ceph-backup-storage-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            List<CephBackupStorageMonVO> monVOs = new ArrayList<CephBackupStorageMonVO>();

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "create-mon-in-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        for (String url : msg.getMonUrls()) {
                            CephBackupStorageMonVO monvo = new CephBackupStorageMonVO();
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
                    public void rollback(FlowTrigger trigger, Map data) {
                        dbf.removeCollection(monVOs, CephBackupStorageMonVO.class);
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "connect-mons";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        List<CephBackupStorageMonBase> bases = CollectionUtils.transformToList(monVOs, new Function<CephBackupStorageMonBase, CephBackupStorageMonVO>() {
                            @Override
                            public CephBackupStorageMonBase call(CephBackupStorageMonVO arg) {
                                return new CephBackupStorageMonBase(arg);
                            }
                        });

                        final AsyncLatch latch = new AsyncLatch(bases.size(), new NoErrorCompletion(trigger) {
                            @Override
                            public void done() {
                                trigger.next();
                            }
                        });

                        for (CephBackupStorageMonBase base : bases) {
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
                        evt.setInventory(CephBackupStorageInventory.valueOf(dbf.reload(getSelf())));
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
        dbf.removeCollection(getSelf().getMons(), CephBackupStorageMonVO.class);
    }
}
