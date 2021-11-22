package org.zstack.storage.ceph.backup;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.agent.AgentConstant;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.Constants;
import org.zstack.header.HasThreadContext;
import org.zstack.header.core.AsyncLatch;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.progress.TaskProgressRange;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.image.*;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.backup.*;
import org.zstack.storage.backup.BackupStorageBase;
import org.zstack.storage.ceph.*;
import org.zstack.storage.ceph.CephMonBase.PingResult;
import org.zstack.storage.ceph.primary.CephPrimaryStorageVO;
import org.zstack.storage.ceph.primary.CephPrimaryStorageVO_;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.zstack.core.Platform.operr;
import static org.zstack.core.progress.ProgressReportService.*;
import static org.zstack.core.progress.ProgressReportService.reportProgress;
import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 7/27/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CephBackupStorageBase extends BackupStorageBase {
    private static final CLogger logger = Utils.getLogger(CephBackupStorageBase.class);

    public CephBackupStorageBase() {
    }

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
    @Autowired
    protected CephBackupStorageMetaDataMaker metaDataMaker;

    public enum PingOperationFailure {
        UnableToCreateFile,
        MonAddrChanged
    }

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
        List<CephPoolCapacity> poolCapacities;
        String type;
        CephBackupStorageMonInventory handleMon;

        public AgentResponse() {
            boolean unitTestOn = CoreGlobalProperty.UNIT_TEST_ON;
            if (unitTestOn && type == null) {
                type = CephConstants.CEPH_MANUFACTURER_OPENSOURCE;
            }
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.success = false;
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

        public List<CephPoolCapacity> getPoolCapacities() {
            return poolCapacities;
        }

        public void setPoolCapacities(List<CephPoolCapacity> poolCapacities) {
            this.poolCapacities = poolCapacities;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
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

    public static class CheckCmd extends AgentCommand {
        List<Pool> pools;

        public List<Pool> getPools() {
            return pools;
        }

        public void setPools(List<Pool> pools) {
            this.pools = pools;
        }
    }

    public static class CheckRsp extends AgentResponse {

    }

    public static class GetDownloadProgressCmd extends AgentCommand {
        private String imageUuid;

        public String getImageUuid() {
            return imageUuid;
        }

        public void setImageUuid(String imageUuid) {
            this.imageUuid = imageUuid;
        }
    }

    public static class GetDownloadProgressRsp extends AgentResponse {
        private boolean completed;
        private int progress;
        private long size;
        private long actualSize;
        private String installPath;
        private String format;
        private long lastOpTime;
        private long downloadSize;

        public long getDownloadSize() {
            return downloadSize;
        }

        public void setDownloadSize(long downloadSize) {
            this.downloadSize = downloadSize;
        }

        public long getLastOpTime() {
            return lastOpTime;
        }

        public void setLastOpTime(long lastOpTime) {
            this.lastOpTime = lastOpTime;
        }

        public boolean isCompleted() {
            return completed;
        }

        public void setCompleted(boolean completed) {
            this.completed = completed;
        }

        public int getProgress() {
            return progress;
        }

        public void setProgress(int progress) {
            this.progress = progress;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public long getActualSize() {
            return actualSize;
        }

        public void setActualSize(long actualSize) {
            this.actualSize = actualSize;
        }

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }
    }

    public static class DownloadCmd extends AgentCommand implements HasThreadContext, Serializable {
        @NoLogging(type = NoLogging.Type.Uri)
        String url;
        String installPath;
        String imageUuid;
        String sendCommandUrl;

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

        public String getSendCommandUrl() {
            return sendCommandUrl;
        }

        public void setSendCommandUrl(String sendCommandUrl) {
            this.sendCommandUrl = sendCommandUrl;
        }
    }

    public static class DownloadRsp extends AgentResponse {
        long size;
        Long actualSize;
        String uploadPath;
        String format;

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

        public String getUploadPath() {
            return uploadPath;
        }

        public void setUploadPath(String uploadPath) {
            this.uploadPath = uploadPath;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
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

    public static class GetLocalFileSizeCmd extends AgentCommand {
        public String path ;
    }

    public static class GetLocalFileSizeRsp extends AgentResponse {
        public long size;
    }

    public static class GetImageSizeCmd extends AgentCommand {
        public String imageUuid;
        public String installPath;
    }

    public static class GetImageSizeRsp extends AgentResponse {
        public Long size;
        public Long actualSize;
    }

    public static class AddImageExportTokenCmd extends AgentCommand {
        public String installPath;
        public String token;
        public long expireTime;
    }

    public static class AddImageExportTokenRsp extends AgentResponse {
        public String md5sum;
    }

    public static class RemoveImageExportTokenCmd extends AgentCommand {
        public String installPath;
    }

    public static class RemoveImageExportTokenRsp extends AgentResponse {}

    public static class GetFactsCmd extends AgentCommand {
        public String monUuid;
    }

    public static class GetFactsRsp extends AgentResponse {
        public String fsid;
        public String monAddr;
    }

    public static class GetImagesMetaDataCmd extends AgentCommand {
        private String poolName;

        public String getPoolName() {
            return poolName;
        }

        public void setPoolName(String poolName) {
            this.poolName = poolName;
        }
    }

    public static class GetImagesMetaDataRsp extends AgentResponse {
        private String imagesMetadata;

        public String getImagesMetadata() {
            return imagesMetadata;
        }

        public void setImagesMetadata(String imagesMetadata) {
            this.imagesMetadata = imagesMetadata;
        }
    }

    public static class CheckImageMetaDataFileExistCmd extends AgentCommand {
        private String poolName;

        public String getPoolName() {
            return poolName;
        }

        public void setPoolName(String poolName) {
            this.poolName = poolName;
        }
    }

    public static class CheckImageMetaDataFileExistRsp extends AgentResponse {
        private String backupStorageMetaFileName;
        private Boolean exist;

        public Boolean getExist() {
            return exist;
        }

        public void setExist(Boolean exist) {
            this.exist = exist;
        }

        public String getBackupStorageMetaFileName() {
            return backupStorageMetaFileName;
        }

        public void setBackupStorageMetaFileName(String backupStorageMetaFileName) {
            this.backupStorageMetaFileName = backupStorageMetaFileName;
        }
    }

    public static class DumpImageInfoToMetaDataFileCmd extends AgentCommand {
        private String poolName;
        private String imageMetaData;
        private boolean dumpAllMetaData;

        public boolean isDumpAllMetaData() {
            return dumpAllMetaData;
        }

        public void setDumpAllMetaData(boolean dumpAllMetaData) {
            this.dumpAllMetaData = dumpAllMetaData;
        }

        public String getPoolName() {
            return poolName;
        }

        public void setPoolName(String poolName) {
            this.poolName = poolName;
        }

        public String getImageMetaData() {
            return imageMetaData;
        }

        public void setImageMetaData(String imageMetaData) {
            this.imageMetaData = imageMetaData;
        }
    }

    public static class DumpImageInfoToMetaDataFileRsp extends AgentResponse {
    }

    public static class DeleteImageInfoFromMetaDataFileCmd extends AgentCommand {
        private String imageUuid;
        private String backupStorageUuid;
        private String poolName;

        public String getPoolName() {
            return poolName;
        }

        public void setPoolName(String poolName) {
            this.poolName = poolName;
        }

        public String getBackupStorageUuid() {
            return backupStorageUuid;
        }

        public void setBackupStorageUuid(String backupStorageUuid) {
            this.backupStorageUuid = backupStorageUuid;
        }

        public String getImageUuid() {
            return imageUuid;
        }

        public void setImageUuid(String imageUuid) {
            this.imageUuid = imageUuid;
        }
    }

    public static class DeleteImageInfoFromMetaDataFileRsp extends AgentResponse {
        private Integer ret;
        private String out;

        public Integer getRet() {
            return ret;
        }

        public void setRet(Integer ret) {
            this.ret = ret;
        }

        public String getOut() {
            return out;
        }

        public void setOut(String out) {
            this.out = out;
        }
    }

    public static class CephToCephMigrateImageCmd extends AgentCommand implements Serializable {
        String imageUuid;
        long imageSize;
        String srcInstallPath;
        String dstInstallPath;
        String dstMonHostname;
        String dstMonSshUsername;
        @NoLogging
        String dstMonSshPassword;
        int dstMonSshPort;

        public String getImageUuid() {
            return imageUuid;
        }

        public void setImageUuid(String imageUuid) {
            this.imageUuid = imageUuid;
        }

        public long getImageSize() {
            return imageSize;
        }

        public void setImageSize(long imageSize) {
            this.imageSize = imageSize;
        }

        public String getSrcInstallPath() {
            return srcInstallPath;
        }

        public void setSrcInstallPath(String srcInstallPath) {
            this.srcInstallPath = srcInstallPath;
        }

        public String getDstInstallPath() {
            return dstInstallPath;
        }

        public void setDstInstallPath(String dstInstallPath) {
            this.dstInstallPath = dstInstallPath;
        }

        public String getDstMonHostname() {
            return dstMonHostname;
        }

        public void setDstMonHostname(String dstMonHostname) {
            this.dstMonHostname = dstMonHostname;
        }

        public String getDstMonSshUsername() {
            return dstMonSshUsername;
        }

        public void setDstMonSshUsername(String dstMonSshUsername) {
            this.dstMonSshUsername = dstMonSshUsername;
        }

        public String getDstMonSshPassword() {
            return dstMonSshPassword;
        }

        public void setDstMonSshPassword(String dstMonSshPassword) {
            this.dstMonSshPassword = dstMonSshPassword;
        }

        public int getDstMonSshPort() {
            return dstMonSshPort;
        }

        public void setDstMonSshPort(int dstMonSshPort) {
            this.dstMonSshPort = dstMonSshPort;
        }
    }

    public static class CancelCommand extends AgentCommand implements org.zstack.header.agent.CancelCommand {
        private String cancellationApiId;

        @Override
        public void setCancellationApiId(String cancellationApiId) {
            this.cancellationApiId = cancellationApiId;
        }
    }

    // common response of storage migration
    public static class StorageMigrationRsp extends AgentResponse {
    }

    public static final String INIT_PATH = "/ceph/backupstorage/init";
    public static final String DOWNLOAD_IMAGE_PATH = "/ceph/backupstorage/image/download";
    public static final String GET_DOWNLOAD_PROGRESS_PATH = "/ceph/backupstorage/image/progress";
    public static final String DELETE_IMAGE_PATH = "/ceph/backupstorage/image/delete";
    public static final String GET_IMAGE_SIZE_PATH = "/ceph/backupstorage/image/getsize";
    public static final String ADD_EXPORT_TOKEN_PATH = "/ceph/backupstorage/image/export/addtoken";
    public static final String REMOVE_EXPORT_TOKEN_PATH = "/ceph/backupstorage/image/export/removetoken";
    public static final String PING_PATH = "/ceph/backupstorage/ping";
    public static final String GET_FACTS = "/ceph/backupstorage/facts";
    public static final String CHECK_IMAGE_METADATA_FILE_EXIST = "/ceph/backupstorage/checkimagemetadatafileexist";
    public static final String DUMP_IMAGE_METADATA_TO_FILE = "/ceph/backupstorage/dumpimagemetadatatofile";
    public static final String GET_IMAGES_METADATA = "/ceph/backupstorage/getimagesmetadata";
    public static final String DELETE_IMAGES_METADATA = "/ceph/backupstorage/deleteimagesmetadata";
    public static final String CHECK_POOL_PATH = "/ceph/backupstorage/checkpool";
    public static final String GET_LOCAL_FILE_SIZE = "/ceph/backupstorage/getlocalfilesize";
    public static final String CEPH_TO_CEPH_MIGRATE_IMAGE_PATH = "/ceph/backupstorage/image/migrate";

    protected String makeImageInstallPath(String imageUuid) {
        return String.format("ceph://%s/%s", getSelf().getPoolName(), imageUuid);
    }

    private <T extends AgentResponse> void httpCall(final String path, final AgentCommand cmd, final Class<T> retClass, final ReturnValueCompletion<T> callback) {
        httpCall(path, cmd, retClass, callback, null, 0);
    }

    private <T extends AgentResponse> void httpCall(final String path, final AgentCommand cmd, final Class<T> retClass, final ReturnValueCompletion<T> callback, TimeUnit unit, long timeout) {
        new HttpCaller<>(path, cmd, retClass, callback, unit, timeout).call();
    }

    protected class HttpCaller<T extends AgentResponse> {
        private Iterator<CephBackupStorageMonBase> it;
        private List<ErrorCode> errorCodes = new ArrayList<ErrorCode>();

        private final String path;
        private final AgentCommand cmd;
        private final Class<T> retClass;
        private final ReturnValueCompletion<T> callback;
        private final TimeUnit unit;
        private final long timeout;

        private String randomFactor = null;
        private boolean tryNext = false;

        HttpCaller(String path, AgentCommand cmd, Class<T> retClass, ReturnValueCompletion<T> callback) {
            this(path, cmd, retClass, callback, null, 0);
        }

        HttpCaller(String path, AgentCommand cmd, Class<T> retClass, ReturnValueCompletion<T> callback, TimeUnit unit, long timeout) {
            this.path = path;
            this.cmd = cmd;
            this.retClass = retClass;
            this.callback = callback;
            this.unit = unit;
            this.timeout = timeout;
        }

        void call() {
            it = prepareMons().iterator();
            prepareCmd();
            doCall();
        }

        HttpCaller<T> specifyOrder(String randomFactor) {
            this.randomFactor = randomFactor;
            return this;
        }

        HttpCaller<T> tryNext() {
            this.tryNext = true;
            return this;
        }

        private void prepareCmd() {
            cmd.uuid = self.getUuid();
            cmd.fsid = getSelf().getFsid();
        }

        private List<CephBackupStorageMonBase> prepareMons() {
            final List<CephBackupStorageMonBase> mons = new ArrayList<CephBackupStorageMonBase>();
            for (CephBackupStorageMonVO monvo : getSelf().getMons()) {
                mons.add(new CephBackupStorageMonBase(monvo));
            }

            if (randomFactor != null) {
                CollectionUtils.shuffleByKeySeed(mons, randomFactor, it -> it.getSelf().getUuid());
            } else {
                Collections.shuffle(mons);
            }

            mons.removeIf(it -> it.getSelf().getStatus() != MonStatus.Connected);
            if (mons.isEmpty()) {
                throw new OperationFailureException(
                        operr("all ceph mons are Disconnected in ceph backup storage[uuid:%s]", self.getUuid())
                );
            }
            return mons;
        }

        private void doCall() {
            if (!it.hasNext()) {
                callback.fail(operr("all mons failed to execute http call[%s], errors are %s",
                        path, JSONObjectUtil.toJsonString(errorCodes)));

                return;
            }

            CephBackupStorageMonBase base = it.next();

            ReturnValueCompletion<T> completion = new ReturnValueCompletion<T>(callback) {
                @Override
                public void success(T ret) {
                    if (!ret.success) {
                        if (tryNext) {
                            doCall();
                        } else {
                            callback.fail(operr("operation error, because:%s", ret.error));
                        }
                        return;
                    }

                    if (!(cmd instanceof InitCmd)) {
                        updateCapacityIfNeeded(ret);
                    }

                    ret.handleMon = CephBackupStorageMonInventory.valueOf(base.getSelf());
                    callback.success(ret);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    String details = String.format("[mon:%s], %s", base.getSelf().getHostname(), errorCode.getDetails());
                    errorCode.setDetails(details);
                    errorCodes.add(errorCode);
                    doCall();
                }
                };

            if (unit == null) {
                base.httpCall(path, cmd, retClass, completion);
            } else {
                base.httpCall(path, cmd, retClass, completion, unit, timeout);
            }
        }
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
            CephCapacity cephCapacity = new CephCapacity(getSelf().getFsid(), rsp);
            new CephCapacityUpdater().update(cephCapacity);
        }
    }

    @Override
    protected void handle(final GetImageSizeOnBackupStorageMsg msg) {
        CephBackupStorageBase.GetImageSizeCmd cmd = new CephBackupStorageBase.GetImageSizeCmd();
        cmd.imageUuid = msg.getImageUuid();
        cmd.installPath = msg.getImageUrl();

        final GetImageSizeOnBackupStorageReply reply = new GetImageSizeOnBackupStorageReply();
        httpCall(GET_IMAGE_SIZE_PATH, cmd, GetImageSizeRsp.class, new ReturnValueCompletion<GetImageSizeRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(GetImageSizeRsp ret) {
                reply.setSize(ret.size);
                bus.reply(msg, reply);
            }
        });
    }

    protected void handle(final GetImageDownloadProgressMsg msg) {
        GetDownloadProgressCmd cmd = new GetDownloadProgressCmd();
        cmd.setImageUuid(msg.getImageUuid());
        cmd.setFsid(getSelf().getFsid());
        cmd.setUuid(self.getUuid());

        GetImageDownloadProgressReply r = new GetImageDownloadProgressReply();

        CephBackupStorageMonVO monvo = Q.New(CephBackupStorageMonVO.class)
                .eq(CephBackupStorageMonVO_.backupStorageUuid, msg.getBackupStorageUuid())
                .eq(CephBackupStorageMonVO_.hostname, msg.getHostname())
                .find();
        if (monvo == null) {
            r.setError(operr(
                    "CephMon[hostname:%s] not found on backup storage[uuid:%s]",
                    msg.getHostname(), msg.getBackupStorageUuid()));
            bus.reply(msg, r);
            return;
        }

        new CephBackupStorageMonBase(monvo).httpCall(GET_DOWNLOAD_PROGRESS_PATH, cmd, GetDownloadProgressRsp.class, new ReturnValueCompletion<GetDownloadProgressRsp>(msg) {
            @Override
            public void success(GetDownloadProgressRsp resp) {
                if (resp.isSuccess()) {
                    r.setCompleted(resp.isCompleted());
                    r.setProgress(resp.getProgress());
                    r.setActualSize(resp.getActualSize());
                    r.setSize(resp.getSize());
                    r.setInstallPath(resp.getInstallPath());
                    r.setFormat(resp.getFormat());
                    r.setDownloadSize(resp.getDownloadSize());
                    r.setLastOpTime(resp.getLastOpTime());
                    r.setSupportSuspend(true);
                } else {
                    r.setError(operr("operation error, because:%s", resp.getError()));
                }
                bus.reply(msg, r);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                r.setError(errorCode);
                bus.reply(msg, r);
            }
        });
    }

    protected void handle(final BakeImageMetadataMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return String.format("bake-image-metadata-for-bs-%s", msg.getBackupStorageUuid());
            }

            @Override
            public String getSyncSignature() {
                return String.format("bake-image-metadata-for-bs-%s", msg.getBackupStorageUuid());
            }

            @Override
            public void run(final SyncTaskChain chain) {
                bakeImageMetadata(msg, chain);
            }

        });
    }

    private void bakeImageMetadata(BakeImageMetadataMsg msg, final SyncTaskChain chain) {
        if (msg.getOperation().equals(CephConstants.AFTER_ADD_BACKUPSTORAGE)) {
            GetImagesMetaDataCmd cmd = new GetImagesMetaDataCmd();
            cmd.setPoolName(msg.getPoolName());
            final BakeImageMetadataReply reply = new BakeImageMetadataReply();

            httpCall(GET_IMAGES_METADATA, cmd, GetImagesMetaDataRsp.class, new ReturnValueCompletion<GetImagesMetaDataRsp>(msg) {
                @Override
                public void fail(ErrorCode err) {
                    reply.setError(err);
                    logger.error(String.format("get images metadata failed: %s", err));
                    bus.reply(msg, reply);
                    chain.next();
                }

                @Override
                public void success(GetImagesMetaDataRsp ret) {
                    logger.error(String.format("get images metadata: %s successfully", ret.getImagesMetadata()));
                    reply.setImagesMetadata(ret.getImagesMetadata());
                    metaDataMaker.restoreImagesBackupStorageMetadataToDatabase(ret.getImagesMetadata(), msg.getBackupStorageUuid());
                    bus.reply(msg, reply);
                    chain.next();
                }
            });
        } else if (msg.getOperation().equals(CephConstants.AFTER_ADD_IMAGE)) {
            final BakeImageMetadataReply reply = new BakeImageMetadataReply();
            CheckImageMetaDataFileExistCmd cmd = new CheckImageMetaDataFileExistCmd();
            cmd.setPoolName(msg.getPoolName());
            httpCall(CHECK_IMAGE_METADATA_FILE_EXIST, cmd, CheckImageMetaDataFileExistRsp.class, new ReturnValueCompletion<CheckImageMetaDataFileExistRsp>(msg) {
                @Override
                public void fail(ErrorCode err) {
                    logger.error(String.format("check images metadata file: %s failed", reply.getBackupStorageMetaFileName()));
                    dumpImagesBackupStorageInfoToMetaDataFile(msg, reply, true, chain);
                }

                @Override
                public void success(CheckImageMetaDataFileExistRsp ret) {
                    dumpImagesBackupStorageInfoToMetaDataFile(msg, reply, false, chain);
                }
            });
        } else if (msg.getOperation().equals(CephConstants.AFTER_EXPUNGE_IMAGE)) {
            final BakeImageMetadataReply reply = new BakeImageMetadataReply();
            CephBackupStorageBase.DeleteImageInfoFromMetaDataFileCmd deleteCmd = new CephBackupStorageBase.DeleteImageInfoFromMetaDataFileCmd();
            deleteCmd.setImageUuid(msg.getImg().getUuid());
            deleteCmd.setBackupStorageUuid(msg.getBackupStorageUuid());
            deleteCmd.setPoolName(msg.getPoolName());
            httpCall(DELETE_IMAGES_METADATA, deleteCmd, DeleteImageInfoFromMetaDataFileRsp.class,
                    new ReturnValueCompletion<DeleteImageInfoFromMetaDataFileRsp>(msg, chain) {
                @Override
                public void success(DeleteImageInfoFromMetaDataFileRsp returnValue) {
                    bus.reply(msg, reply);
                    chain.next();
                }
                @Override
                public void fail(ErrorCode err) {
                    reply.setError(err);
                    logger.error(String.format("delete ceph images metadata failed"));
                    bus.reply(msg, reply);
                    chain.next();
                }
            });
        }
    }

    private void dumpImagesBackupStorageInfoToMetaDataFile(BakeImageMetadataMsg msg, BakeImageMetadataReply reply, boolean allImagesInfo, SyncTaskChain chain ) {
        ImageInventory img = msg.getImg();
        logger.debug("dump ceph images info to meta data file");
        DumpImageInfoToMetaDataFileCmd dumpCmd = new DumpImageInfoToMetaDataFileCmd();
        String metaData;
        if (allImagesInfo) {
            metaData = metaDataMaker.getAllImageInventories(img, null);
        } else {
            metaData = JSONObjectUtil.toJsonString(img);
        }
        dumpCmd.setImageMetaData(metaData);
        dumpCmd.setDumpAllMetaData(allImagesInfo);
        dumpCmd.setPoolName(msg.getPoolName());

        httpCall(DUMP_IMAGE_METADATA_TO_FILE, dumpCmd, DumpImageInfoToMetaDataFileRsp.class, new ReturnValueCompletion<DumpImageInfoToMetaDataFileRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                logger.error("dump ceph images metadata failed");
                bus.reply(msg, reply);
                chain.next();
            }

            @Override
            public void success(DumpImageInfoToMetaDataFileRsp ret) {
                logger.debug("dump ceph images metadata successfully");
                bus.reply(msg, reply);
                chain.next();
            }
        });
    }

    private void handle(CephToCephMigrateImageMsg msg) {
        final CephToCephMigrateImageCmd cmd = new CephToCephMigrateImageCmd();
        cmd.setImageUuid(msg.getImageUuid());
        cmd.setImageSize(msg.getImageSize());
        cmd.setSrcInstallPath(msg.getSrcInstallPath());
        cmd.setDstInstallPath(msg.getDstInstallPath());
        cmd.setDstMonHostname(msg.getDstMonHostname());
        cmd.setDstMonSshUsername(msg.getDstMonSshUsername());
        cmd.setDstMonSshPassword(msg.getDstMonSshPassword());
        cmd.setDstMonSshPort(msg.getDstMonSshPort());

        final CephToCephMigrateImageReply reply = new CephToCephMigrateImageReply();
        httpCall(CEPH_TO_CEPH_MIGRATE_IMAGE_PATH, cmd, StorageMigrationRsp.class, new ReturnValueCompletion<StorageMigrationRsp>(msg) {
            @Override
            public void success(StorageMigrationRsp returnValue) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        }, TimeUnit.MILLISECONDS, msg.getTimeout());
    }

    private void handle(ExportImageFromBackupStorageMsg msg) {
        exportImage(msg);
    }

    @Override
    protected void handle(final DownloadImageMsg msg) {
        final DownloadCmd cmd = new DownloadCmd();
        cmd.url = msg.getImageInventory().getUrl();
        cmd.installPath = makeImageInstallPath(msg.getImageInventory().getUuid());
        cmd.imageUuid = msg.getImageInventory().getUuid();
        cmd.sendCommandUrl = restf.getSendCommandUrl();

        SQL.New(ImageBackupStorageRefVO.class)
                .condAnd(ImageBackupStorageRefVO_.backupStorageUuid, SimpleQuery.Op.EQ, msg.getBackupStorageUuid())
                .condAnd(ImageBackupStorageRefVO_.imageUuid, SimpleQuery.Op.EQ, msg.getImageInventory().getUuid())
                .set(ImageBackupStorageRefVO_.installPath, cmd.installPath)
                .update();

        final DownloadImageReply reply = new DownloadImageReply();
        HttpCaller<DownloadRsp> caller = new HttpCaller<>(DOWNLOAD_IMAGE_PATH, cmd, DownloadRsp.class, new ReturnValueCompletion<DownloadRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(DownloadRsp ret) {
                if (cmd.getUrl().startsWith("upload://")) {
                    reply.setInstallPath(ret.getUploadPath());
                } else {
                    reply.setInstallPath(cmd.installPath);
                }
                reply.setSize(ret.size);

                // current ceph has no way to get the actual size
                // if we cannot get the actual size from HTTP, use the virtual size
                long asize = ret.actualSize == null ? ret.size : ret.actualSize;
                reply.setActualSize(asize);
                reply.setMd5sum("not calculated");
                reply.setFormat(ret.format);
                bus.reply(msg, reply);
            }
        });

        if (cmd.url.startsWith("file:///") || cmd.url.startsWith("/")) {
            caller.tryNext();
        }

        String apiId = ThreadContext.get(Constants.THREAD_CONTEXT_API);
        caller.specifyOrder(apiId).call();
    }

    @Override
    protected void handle(final CancelDownloadImageMsg msg) {
        CancelDownloadImageReply reply = new CancelDownloadImageReply();

        CancelCommand cmd = new CancelCommand();
        cmd.setCancellationApiId(msg.getCancellationApiId());

        new HttpCaller<>(AgentConstant.CANCEL_JOB, cmd, AgentResponse.class, new ReturnValueCompletion<AgentResponse>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(AgentResponse rsp) {
                bus.reply(msg, reply);
            }
        }).specifyOrder(msg.getCancellationApiId()).tryNext().call();
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
            //TODO: GC, the image is still referred, need to cleanup
            bus.reply(msg, reply);
            return;
        }

        DeleteCmd cmd = new DeleteCmd();
        cmd.installPath = msg.getInstallPath();

        httpCall(DELETE_IMAGE_PATH, cmd, DeleteRsp.class, new ReturnValueCompletion<DeleteRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                //TODO GC, do not reply error
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

                // current ceph cannot get actual size
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
    protected void handle(GetLocalFileSizeOnBackupStorageMsg msg) {
        GetLocalFileSizeOnBackupStorageReply reply = new GetLocalFileSizeOnBackupStorageReply();
        GetLocalFileSizeCmd cmd = new GetLocalFileSizeCmd();
        cmd.path = msg.getUrl();
        httpCall(GET_LOCAL_FILE_SIZE, cmd, GetLocalFileSizeRsp.class, new ReturnValueCompletion<GetLocalFileSizeRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(GetLocalFileSizeRsp ret) {
                reply.setSize(ret.size);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(GetImageEncryptedOnBackupStorageMsg msg) {
        GetImageEncryptedOnBackupStorageReply reply = new GetImageEncryptedOnBackupStorageReply();

        String  installPath = Q.New(ImageBackupStorageRefVO.class)
                .select(ImageBackupStorageRefVO_.installPath)
                .eq(ImageBackupStorageRefVO_.backupStorageUuid, self.getUuid())
                .eq(ImageBackupStorageRefVO_.imageUuid, msg.getImageUuid())
                .findValue();

        reply.setEncrypted(installPath);
        bus.reply(msg, reply);
    }

    @Override
    protected void connectHook(final boolean newAdded, final Completion completion) {
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
                        trigger.fail(operr("unable to connect to the ceph backup storage[uuid:%s]. Failed to connect all ceph mons. Errors are %s",
                                        self.getUuid(), JSONObjectUtil.toJsonString(errorCodes)));
                    } else {
                        // reload because mon status changed
                        self = dbf.reload(self);
                        trigger.next();
                    }
                    return;
                }

                final CephBackupStorageMonBase base = it.next();
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
                    String __name__ = "check-mon-integrity";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        final Map<String, String> fsids = new HashMap<String, String>();

                        final List<CephBackupStorageMonBase> mons = CollectionUtils.transformToList(getSelf().getMons(), new Function<CephBackupStorageMonBase, CephBackupStorageMonVO>() {
                            @Override
                            public CephBackupStorageMonBase call(CephBackupStorageMonVO arg) {
                                return arg.getStatus() == MonStatus.Connected ? new CephBackupStorageMonBase(arg) : null;
                            }
                        });

                        DebugUtils.Assert(!mons.isEmpty(), "how can be no connected MON!!! ???");

                        List<ErrorCode> errs = Collections.synchronizedList(new ArrayList<>());
                        new While<>(mons).all((mon, coml) ->{
                            GetFactsCmd cmd = new GetFactsCmd();
                            cmd.uuid = self.getUuid();
                            cmd.monUuid = mon.getSelf().getUuid();
                            mon.httpCall(GET_FACTS, cmd, GetFactsRsp.class, new ReturnValueCompletion<GetFactsRsp>(coml) {
                                @Override
                                public void success(GetFactsRsp rsp) {
                                    if (!rsp.success) {
                                        // one mon cannot get the facts, directly error out
                                        errs.add(operr("operation error, because:%s", rsp.error));
                                        coml.allDone();
                                        return;
                                    }

                                    CephBackupStorageMonVO monVO = mon.getSelf();
                                    fsids.put(monVO.getUuid(), rsp.fsid);
                                    monVO.setMonAddr(rsp.monAddr == null ? monVO.getHostname() : rsp.monAddr);
                                    dbf.update(monVO);
                                    coml.done();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    // one mon cannot get the facts, directly error out
                                    errs.add(errorCode);
                                    coml.allDone();
                                }
                            });
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (!errs.isEmpty()){
                                    trigger.fail(errs.get(0));
                                    return;
                                }

                                Set<String> set = new HashSet<String>();
                                set.addAll(fsids.values());

                                if (set.size() != 1) {
                                    StringBuilder sb = new StringBuilder("the fsid returned by mons are mismatching, it seems the mons belong to different ceph clusters:\n");
                                    for (CephBackupStorageMonBase mon : mons) {
                                        String fsid = fsids.get(mon.getSelf().getUuid());
                                        sb.append(String.format("%s (mon ip) --> %s (fsid)\n", mon.getSelf().getHostname(), fsid));
                                    }

                                    throw new OperationFailureException(operr(sb.toString()));
                                }

                                // check if there is another ceph setup having the same fsid
                                String fsId = set.iterator().next();

                                SimpleQuery<CephBackupStorageVO>  q = dbf.createQuery(CephBackupStorageVO.class);
                                q.add(CephBackupStorageVO_.fsid, Op.EQ, fsId);
                                q.add(CephBackupStorageVO_.uuid, Op.NOT_EQ, self.getUuid());
                                CephBackupStorageVO otherCeph = q.find();
                                if (otherCeph != null) {
                                    throw new OperationFailureException(
                                            operr("there is another CEPH backup storage[name:%s, uuid:%s] with the same" +
                                                            " FSID[%s], you cannot add the same CEPH setup as two different backup storage",
                                                    otherCeph.getName(), otherCeph.getUuid(), fsId)
                                    );
                                }

                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String _name_ = "check-pool";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {

                        Pool p = new Pool();
                        p.name = getSelf().getPoolName();
                        p.predefined = CephSystemTags.PREDEFINED_BACKUP_STORAGE_POOL.hasTag(self.getUuid());

                        if(!newAdded){
                            CheckCmd check = new CheckCmd();
                            check.setPools(list(p));
                            httpCall(CHECK_POOL_PATH, check, CheckRsp.class, new ReturnValueCompletion<CheckRsp>(trigger) {
                                @Override
                                public void fail(ErrorCode err) {
                                    trigger.fail(err);
                                }

                                @Override
                                public void success(CheckRsp ret) {
                                    trigger.next();
                                }
                            });
                        } else {
                            trigger.next();
                        }

                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "init";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {

                        Pool p = new Pool();
                        p.name = getSelf().getPoolName();
                        p.predefined = CephSystemTags.PREDEFINED_BACKUP_STORAGE_POOL.hasTag(self.getUuid());


                        InitCmd cmd = new InitCmd();
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
                                CephCapacity cephCapacity = new CephCapacity(ret.fsid, ret);
                                updater.update(cephCapacity, true);
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "generate-ceph-images-metadata-file";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (!newAdded) {
                            String backupStorageHostName = metaDataMaker.getHostnameFromBackupStorage(CephBackupStorageInventory.valueOf(getSelf()));
                            String backupStorageUuid = getSelf().getUuid();
                            metaDataMaker.dumpImagesBackupStorageInfoToMetaDataFile(null,true, backupStorageHostName, backupStorageUuid);
                        }
                        trigger.next();
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
                                dbf.removeCollection(getSelf().getMons(), CephBackupStorageMonVO.class);
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
        final List<CephBackupStorageMonBase> mons = CollectionUtils.transformToList(getSelf().getMons(), new Function<CephBackupStorageMonBase, CephBackupStorageMonVO>() {
            @Override
            public CephBackupStorageMonBase call(CephBackupStorageMonVO arg) {
                return new CephBackupStorageMonBase(arg);
            }
        });

        final List<ErrorCode> errors = new ArrayList<ErrorCode>();

        class Ping {
            private AtomicBoolean replied = new AtomicBoolean(false);

            @AsyncThread
            private void reconnectMon(final CephBackupStorageMonBase mon, boolean delay) {
                if (!CephGlobalConfig.BACKUP_STORAGE_MON_AUTO_RECONNECT.value(Boolean.class)) {
                    logger.debug(String.format("do not reconnect the ceph backup storage mon[uuid:%s] as the global config[%s] is set to false",
                            self.getUuid(), CephGlobalConfig.BACKUP_STORAGE_MON_AUTO_RECONNECT.getCanonicalName()));
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
                            TimeUnit.SECONDS.sleep(CephGlobalConfig.BACKUP_STORAGE_MON_RECONNECT_DELAY.value(Long.class));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    mon.connect(new Completion(releaseLock) {
                        @Override
                        public void success() {
                            logger.debug(String.format("successfully reconnected the mon[uuid:%s] of the ceph backup" +
                                    " storage[uuid:%s, name:%s]", mon.getSelf().getUuid(), self.getUuid(), self.getName()));
                            releaseLock.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            //TODO
                            logger.warn(String.format("failed to reconnect the mon[uuid:%s] of the ceph backup" +
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

                        ErrorCode err = errf.stringToOperationError(String.format("failed to ping the ceph backup storage[uuid:%s, name:%s]",
                                self.getUuid(), self.getName()), errors);
                        completion.fail(err);
                    }
                });

                for (final CephBackupStorageMonBase mon : mons) {
                    mon.ping(new ReturnValueCompletion<PingResult>(latch) {
                        private void thisMonIsDown(ErrorCode err) {
                            //TODO
                            logger.warn(String.format("cannot ping mon[uuid:%s] of the ceph backup storage[uuid:%s, name:%s], %s",
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

                            } else if (PingOperationFailure.UnableToCreateFile.toString().equals(res.failure)) {
                                // as long as there is one mon saying the ceph not working, the backup storage goes down
                                logger.warn(String.format("the ceph backup storage[uuid:%s, name:%s] is down, as one mon[uuid:%s] reports" +
                                        " an operation failure[%s]", self.getUuid(), self.getName(), mon.getSelf().getUuid(), res.error));
                                backupStorageDown();
                            } else if (!res.success || PingOperationFailure.MonAddrChanged.toString().equals(res.failure)) {
                                // this mon is down(success == false), but the backup storage may still work as other mons may work
                                ErrorCode errorCode = operr("operation error, because:%s", res.error);
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
                for (CephBackupStorageMonBase base : mons) {
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
        if (msg instanceof APIAddMonToCephBackupStorageMsg) {
            handle((APIAddMonToCephBackupStorageMsg) msg);
        } else if (msg instanceof APIUpdateCephBackupStorageMonMsg) {
            handle((APIUpdateCephBackupStorageMonMsg) msg);
        } else if (msg instanceof APIRemoveMonFromCephBackupStorageMsg) {
            handle((APIRemoveMonFromCephBackupStorageMsg) msg);
        } else if (msg instanceof APIExportImageFromBackupStorageMsg) {
            handle((APIExportImageFromBackupStorageMsg) msg);
        } else if (msg instanceof APIDeleteExportedImageFromBackupStorageMsg) {
            handle((APIDeleteExportedImageFromBackupStorageMsg) msg);
        } else {
            super.handleApiMessage(msg);
        }
    }

    @Override
    protected void handleLocalMessage(Message msg) throws URISyntaxException {
        if (msg instanceof BakeImageMetadataMsg) {
            handle((BakeImageMetadataMsg) msg);
        } else if (msg instanceof GetImageDownloadProgressMsg) {
            handle((GetImageDownloadProgressMsg) msg);
        } else if (msg instanceof CephToCephMigrateImageMsg) {
            handle((CephToCephMigrateImageMsg) msg);
        } else if (msg instanceof ExportImageFromBackupStorageMsg) {
            handle((ExportImageFromBackupStorageMsg) msg);
        }
        else {
            super.handleLocalMessage(msg);
        }
    }

    private void handle(APIDeleteExportedImageFromBackupStorageMsg msg) {
        APIDeleteExportedImageFromBackupStorageEvent evt = new APIDeleteExportedImageFromBackupStorageEvent(msg.getId());
        Tuple t = Q.New(ImageBackupStorageRefVO.class).select(ImageBackupStorageRefVO_.installPath, ImageBackupStorageRefVO_.exportUrl)
                .eq(ImageBackupStorageRefVO_.backupStorageUuid, msg.getBackupStorageUuid())
                .eq(ImageBackupStorageRefVO_.imageUuid, msg.getImageUuid())
                .findTuple();
        if (t == null || t.get(1) == null) {
            bus.publish(evt);
            return;
        }

        RemoveImageExportTokenCmd cmd = new RemoveImageExportTokenCmd();
        cmd.installPath = t.get(0, String.class);
        httpCall(REMOVE_EXPORT_TOKEN_PATH, cmd, RemoveImageExportTokenRsp.class, new ReturnValueCompletion<RemoveImageExportTokenRsp>(evt) {
            @Override
            public void success(RemoveImageExportTokenRsp rsp) {
                SQL.New(ImageBackupStorageRefVO.class).set(ImageBackupStorageRefVO_.exportUrl, null)
                        .eq(ImageBackupStorageRefVO_.backupStorageUuid, msg.getBackupStorageUuid())
                        .eq(ImageBackupStorageRefVO_.imageUuid, msg.getImageUuid())
                        .update();

                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void handle(APIExportImageFromBackupStorageMsg msg) {
        APIExportImageFromBackupStorageEvent evt = new APIExportImageFromBackupStorageEvent(msg.getId());
        ExportImageFromBackupStorageMsg emsg = new ExportImageFromBackupStorageMsg();
        bus.makeLocalServiceId(emsg, BackupStorageConstant.SERVICE_ID);
        emsg.setBackupStorageUuid(msg.getBackupStorageUuid());
        emsg.setImageUuid(msg.getImageUuid());
        emsg.setExportFormat(msg.getExportFormat());

        bus.send(emsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    ExportImageFromBackupStorageReply rpl = (ExportImageFromBackupStorageReply) reply;
                    evt.setImageUrl(rpl.getImageUrl());
                    evt.setExportMd5Sum(rpl.getMd5sum());
                } else {
                    evt.setError(reply.getError());
                }
                bus.publish(evt);
            }
        });
    }

    protected void exportImage(ExportImageFromBackupStorageMsg msg) {
        TaskProgressRange parentStage = getTaskStage();

        ExportImageFromBackupStorageReply reply = new ExportImageFromBackupStorageReply();
        Tuple t = Q.New(ImageBackupStorageRefVO.class).select(ImageBackupStorageRefVO_.installPath, ImageBackupStorageRefVO_.exportUrl)
                .eq(ImageBackupStorageRefVO_.backupStorageUuid, msg.getBackupStorageUuid())
                .eq(ImageBackupStorageRefVO_.imageUuid, msg.getImageUuid())
                .findTuple();
        if (t == null) {
            reply.setError(operr("image[uuid: %s] is not on backup storage[uuid:%s, name:%s]",
                    msg.getImageUuid(), self.getUuid(), self.getName()));
            bus.reply(msg, reply);
            return;
        }

        if (t.get(1) != null) {
            reply.setImageUrl(t.get(1, String.class));
            bus.reply(msg, reply);
            return;
        }

        String imageInstallUrl = t.get(0, String.class);
        AddImageExportTokenCmd cmd = new AddImageExportTokenCmd();
        cmd.installPath = imageInstallUrl;
        cmd.token = Platform.getUuidFromBytes(msg.getImageUuid().getBytes());
        httpCall(ADD_EXPORT_TOKEN_PATH, cmd, AddImageExportTokenRsp.class, new ReturnValueCompletion<AddImageExportTokenRsp>(msg) {
            @Override
            public void success(AddImageExportTokenRsp rsp) {
                String url = buildUrl(rsp.handleMon.getHostname(), cmd.token);
                SQL.New(ImageBackupStorageRefVO.class).eq(ImageBackupStorageRefVO_.imageUuid, msg.getImageUuid())
                        .eq(ImageBackupStorageRefVO_.backupStorageUuid, msg.getBackupStorageUuid())
                        .set(ImageBackupStorageRefVO_.exportUrl, url)
                        .set(ImageBackupStorageRefVO_.exportMd5Sum, rsp.md5sum)
                        .update();

                reply.setImageUrl(url);
                reply.setMd5sum(rsp.md5sum);
                reportProgress(parentStage.getEnd().toString());
                bus.reply(msg, reply);
            }

            private String buildUrl(String hostname, String token) {
                String[] splits = imageInstallUrl.split("/");
                String poolName = splits[splits.length - 2];
                String imageName = splits[splits.length - 1];
                return CephAgentUrl.backupStorageUrl(hostname, CephBackupStorageMonBase.EXPORT) +
                        String.format("/%s/%s?token=%s", poolName, imageName, token);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(APIRemoveMonFromCephBackupStorageMsg msg) {
        SimpleQuery<CephBackupStorageMonVO> q = dbf.createQuery(CephBackupStorageMonVO.class);
        q.add(CephBackupStorageMonVO_.hostname, Op.IN, msg.getMonHostnames());
        q.add(CephBackupStorageMonVO_.backupStorageUuid, Op.EQ, self.getUuid());
        List<CephBackupStorageMonVO> vos = q.list();

        if (!vos.isEmpty()) {
            dbf.removeCollection(vos, CephBackupStorageMonVO.class);
        }

        String dstHostName = Q.New(CephBackupStorageMonVO.class).select(CephBackupStorageMonVO_.hostname)
                .eq(CephBackupStorageMonVO_.backupStorageUuid, self.getUuid())
                .orderBy(CephBackupStorageMonVO_.status, SimpleQuery.Od.ASC)
                .limit(1).findValue();

        if (dstHostName != null) {
            vos.forEach(vo -> replaceImageExportUrl(vo.getHostname(), dstHostName));
        }

        APIRemoveMonFromCephBackupStorageEvent evt = new APIRemoveMonFromCephBackupStorageEvent(msg.getId());
        evt.setInventory(CephBackupStorageInventory.valueOf(dbf.reload(getSelf())));
        bus.publish(evt);
    }

    private void replaceImageExportUrl(String srcHostName, String dstHostName) {
        List<ImageBackupStorageRefVO> refs = Q.New(ImageBackupStorageRefVO.class)
                .eq(ImageBackupStorageRefVO_.backupStorageUuid, self.getUuid())
                .like(ImageBackupStorageRefVO_.exportUrl, "http://" + srcHostName + "%")
                .list();

        refs.forEach(it -> it.setExportUrl(it.getExportUrl().replace(srcHostName, dstHostName)));
        dbf.updateCollection(refs);
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
                            monvo.setMonAddr(monvo.getHostname());
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

                        final List<ErrorCode> errorCodes = new ArrayList<ErrorCode>();
                        final AsyncLatch latch = new AsyncLatch(bases.size(), new NoErrorCompletion(trigger) {
                            @Override
                            public void done() {
                                if (!errorCodes.isEmpty()) {
                                    trigger.fail(operr(new ErrorCodeList().causedBy(errorCodes), "unable to connect mons"));
                                } else {
                                    trigger.next();
                                }
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
                        List<CephBackupStorageMonBase> bases = CollectionUtils.transformToList(monVOs, new Function<CephBackupStorageMonBase, CephBackupStorageMonVO>() {
                            @Override
                            public CephBackupStorageMonBase call(CephBackupStorageMonVO arg) {
                                return new CephBackupStorageMonBase(arg);
                            }
                        });

                        final List<ErrorCode> errors = new ArrayList<ErrorCode>();

                        final AsyncLatch latch = new AsyncLatch(bases.size(), new NoErrorCompletion(trigger) {
                            @Override
                            public void done() {
                                // one fail, all fail
                                if (!errors.isEmpty()) {
                                    trigger.fail(operr(new ErrorCodeList().causedBy(errors), "unable to add mon to ceph backup storage"));
                                } else {
                                    trigger.next();
                                }
                            }
                        });

                        for (final CephBackupStorageMonBase base : bases) {
                            GetFactsCmd cmd = new GetFactsCmd();
                            cmd.uuid = self.getUuid();
                            cmd.monUuid = base.getSelf().getUuid();
                            base.httpCall(GET_FACTS, cmd, GetFactsRsp.class, new ReturnValueCompletion<GetFactsRsp>(latch) {
                                @Override
                                public void success(GetFactsRsp rsp) {
                                    if (!rsp.isSuccess()) {
                                        errors.add(operr("operation error, because:%s", rsp.getError()));
                                    } else {
                                        String fsid = rsp.fsid;
                                        if (!getSelf().getFsid().equals(fsid)) {
                                            errors.add(operr("the mon[ip:%s] returns a fsid[%s] different from the current fsid[%s] of the cep cluster," +
                                                            "are you adding a mon not belonging to current cluster mistakenly?", base.getSelf().getHostname(), fsid, getSelf().getFsid()));
                                        }
                                    }

                                    CephBackupStorageMonVO monVO = base.getSelf();
                                    monVO.setMonAddr(rsp.monAddr == null ? monVO.getHostname() : rsp.monAddr);
                                    dbf.update(monVO);

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
                        evt.setInventory(CephBackupStorageInventory.valueOf(dbf.reload(getSelf())));
                        bus.publish(evt);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setError(errCode);
                        bus.publish(evt);
                    }
                });
            }
        }).start();
    }

    private void handle(final APIUpdateCephBackupStorageMonMsg msg) {
        final APIUpdateCephBackupStorageMonEvent evt = new APIUpdateCephBackupStorageMonEvent(msg.getId());
        CephBackupStorageMonVO monvo = dbf.findByUuid(msg.getMonUuid(), CephBackupStorageMonVO.class);
        if (msg.getHostname() != null) {
            monvo.setHostname(msg.getHostname());
        }
        if (msg.getMonPort() != null && msg.getMonPort() > 0 && msg.getMonPort() <= 65535) {
            monvo.setMonPort(msg.getMonPort());
        }
        if (msg.getSshPort() != null && msg.getSshPort() > 0 && msg.getSshPort() <= 65535) {
            monvo.setSshPort(msg.getSshPort());
        }
        if (msg.getSshUsername() != null) {
            monvo.setSshUsername(msg.getSshUsername());
        }
        if (msg.getSshPassword() != null) {
            monvo.setSshPassword(msg.getSshPassword());
        }
        dbf.update(monvo);
        evt.setInventory(CephBackupStorageInventory.valueOf(dbf.reload(getSelf())));
        bus.publish(evt);
    }

    @Override
    public void deleteHook() {
        String fsid = getSelf().getFsid();
        new SQLBatch() {
            @Override
            protected void scripts() {
                if(Q.New(CephPrimaryStorageVO.class).eq(CephPrimaryStorageVO_.fsid, fsid).find() == null){
                    SQL.New(CephCapacityVO.class).eq(CephCapacityVO_.fsid, fsid).delete();
                }

            }
        }.execute();
        dbf.removeCollection(getSelf().getMons(), CephBackupStorageMonVO.class);

    }
}
