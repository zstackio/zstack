package org.zstack.storage.ceph.backup;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.backup.*;
import org.zstack.storage.backup.BackupStorageBase;
import org.zstack.storage.ceph.MonStatus;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by frank on 7/27/2015.
 */
public class CephBackupStorageBase extends BackupStorageBase {

    @Autowired
    protected RESTFacade restf;

    public static class AgentCommand {
    }

    public static class AgentResponse {
        String error;
        boolean success;
        long totalCapacity;
        long availCapacity;

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

    public static final int AGENT_PORT = 7761;
    public static final String DOWNLOAD_IMAGE_PATH = "/image/download";
    public static final String DELETE_IMAGE_PATH = "/image/delete";
    public static final String PING_PATH = "/ping";

    protected String makeHttpPath(String ip, String path) {
        return String.format("http://%s:%s%s", ip, AGENT_PORT, path);
    }

    protected String makeImageInstallPath(String imageUuid) {
        return String.format("%s/%s", self.getUuid(), imageUuid);
    }

    private <T> void httpCall(final String path, final AgentCommand cmd, final JsonAsyncRESTCallback<T> callback) {
        httpCall(path, cmd, callback, 5, TimeUnit.MINUTES);
    }

    private <T> void httpCall(final String path, final AgentCommand cmd, final JsonAsyncRESTCallback<T> callback, final long timeout, final TimeUnit timeUnit) {
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

                restf.asyncJsonPost(makeHttpPath(base.getHostname(), DOWNLOAD_IMAGE_PATH), cmd, new JsonAsyncRESTCallback<AgentResponse>() {
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

    public CephBackupStorageBase(BackupStorageVO self) {
        super(self);
    }

    protected CephBackupStorageVO getSelf() {
        return (CephBackupStorageVO) self;
    }

    protected CephBackupStorageInventory getInventory() {
        return CephBackupStorageInventory.valueOf(getSelf());
    }

    @Override
    protected void handle(final DownloadImageMsg msg) {
        final DownloadCmd cmd = new DownloadCmd();
        cmd.url = msg.getImageInventory().getUrl();
        cmd.installPath = makeImageInstallPath(msg.getImageInventory().getUuid());

        final DownloadImageReply reply = new DownloadImageReply();
        httpCall(DOWNLOAD_IMAGE_PATH, cmd, new JsonAsyncRESTCallback<DownloadRsp>(msg) {
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

            @Override
            public Class<DownloadRsp> getReturnClass() {
                return null;
            }
        }, CephBackupStorageGlobalConfig.DOWNLOAD_IMAGE_TIMEOUT.value(Long.class), TimeUnit.SECONDS);
    }

    @Override
    protected void handle(final DownloadVolumeMsg msg) {
        final DownloadCmd cmd = new DownloadCmd();
        cmd.url = msg.getUrl();
        cmd.installPath = makeImageInstallPath(msg.getVolume().getUuid());

        final DownloadVolumeReply reply = new DownloadVolumeReply();
        httpCall(DOWNLOAD_IMAGE_PATH, cmd, new JsonAsyncRESTCallback<DownloadRsp>(msg) {
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

            @Override
            public Class<DownloadRsp> getReturnClass() {
                return null;
            }
        }, CephBackupStorageGlobalConfig.DOWNLOAD_IMAGE_TIMEOUT.value(Long.class), TimeUnit.SECONDS);
    }

    @Override
    protected void handle(final DeleteBitsOnBackupStorageMsg msg) {
        DeleteCmd cmd = new DeleteCmd();
        cmd.installPath = msg.getInstallPath();

        final DeleteBitsOnBackupStorageReply reply = new DeleteBitsOnBackupStorageReply();
        httpCall(DELETE_IMAGE_PATH, cmd, new JsonAsyncRESTCallback<DeleteRsp>() {
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

            @Override
            public Class<DeleteRsp> getReturnClass() {
                return DeleteRsp.class;
            }
        });
    }

    @Override
    protected void handle(final PingBackupStorageMsg msg) {
        PingCmd cmd = new PingCmd();

        final PingBackupStorageReply reply = new PingBackupStorageReply();
        httpCall(PING_PATH, cmd, new JsonAsyncRESTCallback<PingRsp>() {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(PingRsp ret) {
                bus.reply(msg, reply);
            }

            @Override
            public Class<PingRsp> getReturnClass() {
                return PingRsp.class;
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

            void connect() {
                if (!it.hasNext()) {
                    if (errorCodes.size() == mons.size()) {
                        completion.fail(errf.stringToOperationError(
                                String.format("unable to connect to the ceph backup storage[uuid:%s]. Failed to connect all ceph mons. Errors are %s",
                                        self.getUuid(), JSONObjectUtil.toJsonString(errorCodes))
                        ));
                    } else {
                        completion.success();
                    }
                    return;
                }

                CephBackupStorageMonBase base = it.next();
                base.connect(new Completion(completion) {
                    @Override
                    public void success() {
                        connect();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        errorCodes.add(errorCode);
                        connect();
                    }
                });
            }
        }

        new Connector().connect();
    }

    @Override
    public List<ImageInventory> scanImages() {
        return null;
    }
}
