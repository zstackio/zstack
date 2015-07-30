package org.zstack.storage.ceph.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.primary.VolumeSnapshotCapability.VolumeSnapshotArrangementType;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.storage.ceph.CephCapacityUpdater;
import org.zstack.storage.ceph.CephGlobalProperty;
import org.zstack.storage.ceph.MonStatus;
import org.zstack.storage.primary.PrimaryStorageBase;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 7/28/2015.
 */
public class CephPrimaryStorageBase extends PrimaryStorageBase {
    @Autowired
    private RESTFacade restf;
    @Autowired
    private ThreadFacade thdf;


    public static class AgentCommand {
        String fsId;
        String uuid;

        public String getFsId() {
            return fsId;
        }

        public void setFsId(String fsId) {
            this.fsId = fsId;
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
        Long availCapacity;

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

        public Long getAvailCapacity() {
            return availCapacity;
        }

        public void setAvailCapacity(Long availCapacity) {
            this.availCapacity = availCapacity;
        }
    }

    public static class InitCmd extends AgentCommand {
        List<String> poolNames;

        public List<String> getPoolNames() {
            return poolNames;
        }

        public void setPoolNames(List<String> poolNames) {
            this.poolNames = poolNames;
        }
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

    public static class CreateEmptyVolumeCmd extends AgentCommand {
        String installPath;
        long size;

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }

    public static class CreateEmptyVolumeRsp extends AgentResponse {
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

    public static class CloneCmd extends AgentCommand {
        String imagePath;
        String installPath;

        public String getImagePath() {
            return imagePath;
        }

        public void setImagePath(String imagePath) {
            this.imagePath = imagePath;
        }

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }
    }

    public static class CloneRsp extends AgentResponse {
    }

    public static class PrepareForCloneCmd extends AgentCommand {
        String imagePath;
        String clonePath;

        public String getImagePath() {
            return imagePath;
        }

        public void setImagePath(String imagePath) {
            this.imagePath = imagePath;
        }

        public String getClonePath() {
            return clonePath;
        }

        public void setClonePath(String clonePath) {
            this.clonePath = clonePath;
        }
    }

    public static class PrepareForCloneRsp extends AgentResponse {
    }

    public static final String INIT_PATH = "/ceph/primarystorage/init";
    public static final String CREATE_VOLUME_PATH = "/ceph/primarystorage/volume/createempty";
    public static final String DELETE_PATH = "/ceph/primarystorage/delete";
    public static final String PREPARE_CLONE_PATH = "/ceph/primarystorage/volume/prepareclone";
    public static final String CLONE_PATH = "/ceph/primarystorage/volume/clone";

    private String getVolumePoolName() {
        return String.format("pri-v-%s", self.getUuid());
    }

    private String getImageCachePoolName() {
        return String.format("pri-c-%s", self.getUuid());
    }

    private String makeVolumeInstallPath(String volUuid) {
        return String.format("%s/%s", getVolumePoolName(), volUuid);
    }

    private String makeCloneInstallPath(String uuid) {
        return String.format("%s/%s", getImageCachePoolName(), uuid);
    }

    public CephPrimaryStorageBase(PrimaryStorageVO self) {
        super(self);
    }

    protected CephPrimaryStorageVO getSelf() {
        return (CephPrimaryStorageVO) self;
    }

    protected CephPrimaryStorageInventory getInventory() {
        return CephPrimaryStorageInventory.valueOf(getSelf());
    }

    private void createEmptyVolume(final InstantiateVolumeMsg msg) {
        final CreateEmptyVolumeCmd cmd = new CreateEmptyVolumeCmd();
        cmd.installPath = makeVolumeInstallPath(msg.getVolume().getUuid());
        cmd.size = msg.getVolume().getSize();

        final InstantiateVolumeReply reply = new InstantiateVolumeReply();

        httpCall(CREATE_VOLUME_PATH, cmd, new JsonAsyncRESTCallback<CreateEmptyVolumeRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(CreateEmptyVolumeRsp ret) {
                VolumeInventory vol = msg.getVolume();
                vol.setInstallPath(cmd.getInstallPath());
                reply.setVolume(vol);
                bus.reply(msg, reply);
            }

            @Override
            public Class<CreateEmptyVolumeRsp> getReturnClass() {
                return CreateEmptyVolumeRsp.class;
            }
        });
    }

    @Override
    protected void handle(final InstantiateVolumeMsg msg) {
        if (msg instanceof InstantiateRootVolumeFromTemplateMsg) {
            createVolumeFromTemplate((InstantiateRootVolumeFromTemplateMsg) msg);
        } else {
            createEmptyVolume(msg);
        }
    }

    private void createVolumeFromTemplate(final InstantiateRootVolumeFromTemplateMsg msg) {
        final ImageInventory img = msg.getTemplateSpec().getInventory();

        final InstantiateVolumeReply reply = new InstantiateVolumeReply();
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-root-volume-%s", msg.getVolume().getUuid()));
        chain.then(new ShareFlow() {
            String cloneInstallPath;
            String volumePath = makeVolumeInstallPath(msg.getVolume().getUuid());

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "prepare-for-clone";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        thdf.chainSubmit(new ChainTask(trigger) {
                            @Override
                            public String getSyncSignature() {
                                return String.format("prepare-clone-for-image-%s", img.getUuid());
                            }

                            @Override
                            public void run(final SyncTaskChain chain) {
                                SimpleQuery<ImageCacheVO> q = dbf.createQuery(ImageCacheVO.class);
                                q.select(ImageCacheVO_.installUrl);
                                q.add(ImageCacheVO_.imageUuid, Op.EQ, img.getUuid());
                                q.add(ImageCacheVO_.primaryStorageUuid, Op.EQ, self.getUuid());
                                cloneInstallPath = q.findValue();

                                if (cloneInstallPath != null) {
                                    trigger.next();
                                    return;
                                }

                                cloneInstallPath = makeCloneInstallPath(img.getUuid());
                                PrepareForCloneCmd cmd = new PrepareForCloneCmd();
                                cmd.imagePath = msg.getTemplateSpec().getSelectedBackupStorage().getInstallPath();
                                cmd.clonePath = cloneInstallPath;
                                httpCall(PREPARE_CLONE_PATH, cmd, new JsonAsyncRESTCallback<PrepareForCloneRsp>(trigger, chain) {
                                    @Override
                                    public void fail(ErrorCode err) {
                                        trigger.fail(err);
                                        chain.next();
                                    }

                                    @Override
                                    public void success(PrepareForCloneRsp ret) {
                                        trigger.next();
                                        chain.next();
                                    }

                                    @Override
                                    public Class<PrepareForCloneRsp> getReturnClass() {
                                        return PrepareForCloneRsp.class;
                                    }
                                });
                            }

                            @Override
                            public String getName() {
                                return String.format("prepare-clone-for-image-%s", img.getUuid());
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "clone-image";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        CloneCmd cmd = new CloneCmd();
                        cmd.imagePath = cloneInstallPath;
                        cmd.installPath = volumePath;

                        httpCall(CLONE_PATH, cmd, new JsonAsyncRESTCallback<CloneRsp>(trigger) {
                            @Override
                            public void fail(ErrorCode err) {
                                trigger.fail(err);
                            }

                            @Override
                            public void success(CloneRsp ret) {
                                trigger.next();
                            }

                            @Override
                            public Class<CloneRsp> getReturnClass() {
                                return CloneRsp.class;
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        VolumeInventory vol = msg.getVolume();
                        vol.setInstallPath(volumePath);
                        reply.setVolume(vol);
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void handle(final DeleteVolumeOnPrimaryStorageMsg msg) {
        DeleteCmd cmd = new DeleteCmd();
        cmd.installPath = msg.getVolume().getInstallPath();

        final DeleteVolumeOnPrimaryStorageReply reply = new DeleteVolumeOnPrimaryStorageReply();

        httpCall(DELETE_PATH, cmd, new JsonAsyncRESTCallback<DeleteRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(DeleteRsp ret) {
                bus.reply(msg, reply);
            }

            @Override
            public Class<DeleteRsp> getReturnClass() {
                return DeleteRsp.class;
            }
        });
    }

    @Override
    protected void handle(CreateTemplateFromVolumeOnPrimaryStorageMsg msg) {
    }

    @Override
    protected void handle(DownloadDataVolumeToPrimaryStorageMsg msg) {
    }

    @Override
    protected void handle(final DeleteBitsOnPrimaryStorageMsg msg) {
        DeleteCmd cmd = new DeleteCmd();
        cmd.installPath = msg.getInstallPath();

        final DeleteBitsOnPrimaryStorageReply reply = new DeleteBitsOnPrimaryStorageReply();

        httpCall(DELETE_PATH, cmd, new JsonAsyncRESTCallback<DeleteRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(DeleteRsp ret) {
                bus.reply(msg, reply);
            }

            @Override
            public Class<DeleteRsp> getReturnClass() {
                return DeleteRsp.class;
            }
        });
    }

    @Override
    protected void handle(DownloadIsoToPrimaryStorageMsg msg) {

    }

    @Override
    protected void handle(DeleteIsoFromPrimaryStorageMsg msg) {

    }

    @Override
    protected void handle(AskVolumeSnapshotCapabilityMsg msg) {
        AskVolumeSnapshotCapabilityReply reply = new AskVolumeSnapshotCapabilityReply();
        VolumeSnapshotCapability cap = new VolumeSnapshotCapability();
        cap.setSupport(true);
        cap.setArrangementType(VolumeSnapshotArrangementType.INDIVIDUAL);
        reply.setCapability(cap);
        bus.reply(msg, reply);
    }

    private <T> void httpCall(final String path, final AgentCommand cmd, final JsonAsyncRESTCallback<T> callback) {
        httpCall(path, cmd, callback, 5, TimeUnit.MINUTES);
    }

    protected String makeHttpPath(String ip, String path) {
        return String.format("http://%s:%s%s", ip, CephGlobalProperty.PRIMARY_STORAGE_AGENT_PORT, path);
    }

    private void updateCapacityIfNeeded(AgentResponse rsp) {
        new CephCapacityUpdater().update(getSelf().getFsid(), rsp.totalCapacity, rsp.availCapacity);
    }

    private <T> void httpCall(final String path, final AgentCommand cmd, final JsonAsyncRESTCallback<T> callback, final long timeout, final TimeUnit timeUnit) {
        cmd.setUuid(self.getUuid());
        cmd.setFsId(getSelf().getFsid());

        final List<CephPrimaryStorageMonBase> mons = new ArrayList<CephPrimaryStorageMonBase>();
        for (CephPrimaryStorageMonVO monvo : getSelf().getMons()) {
            if (monvo.getStatus() == MonStatus.Connected) {
                mons.add(new CephPrimaryStorageMonBase(monvo));
            }
        }

        if (mons.isEmpty()) {
            throw new OperationFailureException(errf.stringToOperationError(
                    String.format("all ceph mons of primary storage[uuid:%s] are not in Connected state", self.getUuid())
            ));
        }

        Collections.shuffle(mons);

        class HttpCaller {
            Iterator<CephPrimaryStorageMonBase> it = mons.iterator();
            List<ErrorCode> errorCodes = new ArrayList<ErrorCode>();

            void call() {
                if (!it.hasNext()) {
                    callback.fail(errf.stringToOperationError(
                            String.format("all mons failed to execute http call[%s], errors are %s", path, JSONObjectUtil.toJsonString(errorCodes))
                    ));

                    return;
                }

                CephPrimaryStorageMonBase base = it.next();

                restf.asyncJsonPost(makeHttpPath(base.getSelf().getHostname(), path), cmd, new JsonAsyncRESTCallback<AgentResponse>() {
                    @Override
                    public void fail(ErrorCode err) {
                        errorCodes.add(err);
                        call();
                    }

                    @Override
                    public void success(AgentResponse ret) {
                        if (!ret.success) {
                            callback.fail(errf.stringToOperationError(ret.error));
                        } else {
                            if (!(cmd instanceof InitCmd)) {
                                updateCapacityIfNeeded(ret);
                            }
                            callback.success((T)ret);
                        }
                    }

                    @Override
                    public Class<AgentResponse> getReturnClass() {
                        return AgentResponse.class;
                    }
                }, timeUnit, timeout);
            }
        }

        new HttpCaller().call();
    }

    @Override
    protected void connectHook(ConnectPrimaryStorageMsg msg, final Completion completion) {
        final List<CephPrimaryStorageMonBase> mons = CollectionUtils.transformToList(getSelf().getMons(), new Function<CephPrimaryStorageMonBase, CephPrimaryStorageMonVO>() {
            @Override
            public CephPrimaryStorageMonBase call(CephPrimaryStorageMonVO arg) {
                return new CephPrimaryStorageMonBase(arg);
            }
        });

        class Connector {
            List<ErrorCode> errorCodes = new ArrayList<ErrorCode>();
            Iterator<CephPrimaryStorageMonBase> it = mons.iterator();

            void connect(final FlowTrigger trigger) {
                if (!it.hasNext()) {
                    if (errorCodes.size() == mons.size()) {
                        trigger.fail(errf.stringToOperationError(
                                String.format("unable to connect to the ceph primary storage[uuid:%s]. Failed to connect all ceph mons. Errors are %s",
                                        self.getUuid(), JSONObjectUtil.toJsonString(errorCodes))
                        ));
                    } else {
                        trigger.next();
                    }
                    return;
                }

                CephPrimaryStorageMonBase base = it.next();
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
        chain.setName(String.format("connect-ceph-primary-storage-%s", self.getUuid()));
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
                        cmd.poolNames = list(getImageCachePoolName(), getVolumePoolName());

                        httpCall(INIT_PATH, cmd, new JsonAsyncRESTCallback<InitRsp>() {
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
                                updater.update(ret.fsid, ret.totalCapacity, ret.availCapacity);
                                trigger.next();
                            }

                            @Override
                            public Class<InitRsp> getReturnClass() {
                                return InitRsp.class;
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
    protected void syncPhysicalCapacity(ReturnValueCompletion<PhysicalCapacityUsage> completion) {
        PrimaryStorageCapacityVO cap = dbf.findByUuid(self.getUuid(), PrimaryStorageCapacityVO.class);
        PhysicalCapacityUsage usage = new PhysicalCapacityUsage();
        usage.availablePhysicalSize = cap.getAvailablePhysicalCapacity();
        usage.totalPhysicalSize =  cap.getTotalPhysicalCapacity();
        completion.success(usage);
    }
}
