package org.zstack.image;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.core.thread.CancelablePeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.image.*;
import org.zstack.header.longjob.LongJob;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.longjob.LongJobVO_;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.AllocateBackupStorageMsg;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.header.storage.backup.GetImageDownloadProgressMsg;
import org.zstack.header.storage.backup.GetImageDownloadProgressReply;
import org.zstack.longjob.LongJobUtils;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;
import static org.zstack.core.progress.ProgressReportService.reportProgress;
import static org.zstack.header.Constants.THREAD_CONTEXT_API;
import static org.zstack.header.Constants.THREAD_CONTEXT_TASK_NAME;


@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class UploadImageTracker {
    private static final CLogger logger = Utils.getLogger(UploadImageTracker.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private EventFacade evtf;

    private final String apiId = ThreadContext.get(THREAD_CONTEXT_API);
    private final boolean continuable = apiId != null && Q.New(LongJobVO.class).eq(LongJobVO_.apiId, apiId).isExists();

    public static class TrackContext {
        String name;
        String imageUuid;
        String bsUuid;
        String hostname;
    }

    List<TrackContext> ctxs = new ArrayList<>();

    void addTrackTask(ImageVO image, ImageBackupStorageRefVO ref) {
        try {
            addTrackTask(image.getName(), image.getUuid(), ref.getBackupStorageUuid(), new URI(ref.getInstallPath()).getHost());
        } catch (URISyntaxException e) {
            throw new OperationFailureException(operr(e.getMessage()));
        }
    }

    void addTrackTask(String name, String imageUuid, String bsUuid, String hostname) {
        TrackContext ctx = new TrackContext();
        ctx.name = name;
        ctx.imageUuid = imageUuid;
        ctx.bsUuid = bsUuid;
        ctx.hostname = hostname;
        ctxs.add(ctx);
    }

    void runTrackTask() {
        for (TrackContext ctx : ctxs) {
            trackUpload(ctx.name, ctx.imageUuid, ctx.bsUuid, ctx.hostname);
        }
    }

    void trackUpload(ImageVO image, ImageBackupStorageRefVO ref) {
        try {
            trackUpload(image.getName(), image.getUuid(), ref.getBackupStorageUuid(), new URI(ref.getInstallPath()).getHost());
        } catch (URISyntaxException e) {
            throw new OperationFailureException(operr(e.getMessage()));
        }
    }

    void trackUpload(String name, String imageUuid, String bsUuid, String hostname) {
        final int maxNumOfFailure = 3;
        final int maxIdleSecond = 30;

        thdf.submitCancelablePeriodicTask(new CancelablePeriodicTask() {
            private long numError = 0;
            private int numTicks = 0;

            private void markCompletion(final GetImageDownloadProgressReply dr) {
                ImageVO ivo = new SQLBatchWithReturn<ImageVO>() {
                    @Override
                    protected ImageVO scripts() {
                        ImageVO vo = findByUuid(imageUuid, ImageVO.class);
                        if (StringUtils.isNotEmpty(dr.getFormat())) {
                            vo.setFormat(dr.getFormat());
                        }
                        if (vo.getFormat().equals(ImageConstant.ISO_FORMAT_STRING)
                                && ImageConstant.ImageMediaType.RootVolumeTemplate.equals(vo.getMediaType())) {
                            vo.setMediaType(ImageConstant.ImageMediaType.ISO);
                        }
                        if (ImageConstant.QCOW2_FORMAT_STRING.equals(vo.getFormat())
                                && ImageConstant.ImageMediaType.ISO.equals(vo.getMediaType())) {
                            vo.setMediaType(ImageConstant.ImageMediaType.RootVolumeTemplate);
                        }
                        vo.setStatus(ImageStatus.Ready);
                        vo.setSize(dr.getSize());
                        vo.setActualSize(dr.getActualSize());
                        merge(vo);
                        sql(ImageBackupStorageRefVO.class)
                                .eq(ImageBackupStorageRefVO_.backupStorageUuid, bsUuid)
                                .eq(ImageBackupStorageRefVO_.imageUuid, imageUuid)
                                .set(ImageBackupStorageRefVO_.status, ImageStatus.Ready)
                                .set(ImageBackupStorageRefVO_.installPath, dr.getInstallPath())
                                .update();
                        return vo;
                    }
                }.execute();

                logger.debug(String.format("added image [name: %s, uuid: %s]", name, imageUuid));

                final ImageInventory einv = ImageInventory.valueOf(dbf.reload(ivo));
                fireEvent(einv, null);
                CollectionUtils.safeForEach(pluginRgty.getExtensionList(AddImageExtensionPoint.class),
                        ext -> ext.afterAddImage(einv));
            }

            private void markFailure(ErrorCode reason) {
                logger.error(String.format("upload image [name: %s, uuid: %s] failed: %s",
                        name, imageUuid, reason.toString()));

                fireEvent(null, reason);
                if (reason.isError(ImageErrors.UPLOAD_IMAGE_INTERRUPTED) && continuable) {
                    return;
                }

                // Note, the handler of ImageDeletionMsg will deal with storage capacity.
                ImageDeletionMsg msg = new ImageDeletionMsg();
                msg.setImageUuid(imageUuid);
                msg.setBackupStorageUuids(Collections.singletonList(bsUuid));
                msg.setDeletionPolicy(ImageDeletionPolicyManager.ImageDeletionPolicy.Direct.toString());
                msg.setForceDelete(true);
                bus.makeTargetServiceIdByResourceUuid(msg, ImageConstant.SERVICE_ID, imageUuid);
                bus.send(msg);
            }

            private void fireEvent(ImageInventory img, ErrorCode error) {
                ImageCanonicalEvents.ImageTrackData data = new ImageCanonicalEvents.ImageTrackData();
                data.setUuid(imageUuid);
                data.setError(error);
                data.setInventory(img);
                evtf.fire(ImageCanonicalEvents.IMAGE_TRACK_RESULT_PATH, data);
            }

            @Override
            public boolean run() {
                final ImageVO ivo = dbf.findByUuid(imageUuid, ImageVO.class);
                if (ivo == null) {
                    // If image VO not existed, stop tracking.
                    return true;
                }

                numTicks += 1;
                if (ivo.getActualSize() == 0 && numTicks * getInterval() >= maxIdleSecond) {
                    markFailure(operr("upload session expired"));
                    return true;
                }

                final GetImageDownloadProgressMsg dmsg = new GetImageDownloadProgressMsg();
                dmsg.setBackupStorageUuid(bsUuid);
                dmsg.setImageUuid(imageUuid);
                dmsg.setHostname(hostname);
                bus.makeTargetServiceIdByResourceUuid(dmsg, BackupStorageConstant.SERVICE_ID, bsUuid);

                final MessageReply reply = bus.call(dmsg);
                if (reply.isSuccess()) {
                    // reset the error counter
                    numError = 0;

                    final GetImageDownloadProgressReply dr = reply.castReply();

                    if (dr.isCompleted()) {
                        if (!dr.isSuccess()) {
                            markFailure(dr.getError());
                        } else {
                            doReportProgress("adding to image store", 100);
                            markCompletion(dr);
                        }
                        return true;
                    }

                    int timeout = 30;
                    if (System.currentTimeMillis() - dr.getLastOpTime() > TimeUnit.SECONDS.toMillis(timeout)) {
                        markFailure(err(ImageErrors.UPLOAD_IMAGE_INTERRUPTED, dr.getError(),
                                "uploading has been inactive more than %d sec", timeout));
                        return true;
                    }

                    doReportProgress("uploading image", dr.getProgress());
                    if (ivo.getActualSize() == 0 && dr.getActualSize() != 0) {
                        ivo.setActualSize(dr.getActualSize());
                        dbf.updateAndRefresh(ivo);

                        AllocateBackupStorageMsg amsg = new AllocateBackupStorageMsg();
                        amsg.setBackupStorageUuid(bsUuid);
                        amsg.setSize(dr.getActualSize());
                        bus.makeLocalServiceId(amsg, BackupStorageConstant.SERVICE_ID);
                        MessageReply areply = bus.call(amsg);
                        if (!areply.isSuccess()) {
                            markFailure(areply.getError());
                            return true;
                        }
                    }

                    return false;
                }

                numError++;
                if (numError <= maxNumOfFailure) {
                    return false;
                }

                markFailure(reply.getError());
                return true;
            }

            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return 3;
            }

            @Override
            public String getName() {
                return String.format("tracking upload image [name: %s, uuid: %s]", name, imageUuid);
            }
        });
    }



    private void doReportProgress(String taskName, long progress) {
        ThreadContext.put(THREAD_CONTEXT_API, apiId);
        ThreadContext.put(THREAD_CONTEXT_TASK_NAME, taskName);
        reportProgress(String.valueOf(progress));
    }
}
