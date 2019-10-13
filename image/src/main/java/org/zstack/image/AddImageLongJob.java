package org.zstack.image;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.Constants;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.image.*;
import org.zstack.header.longjob.*;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.MessageReply;
import org.zstack.longjob.LongJobUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.List;

import static org.zstack.longjob.LongJobUtils.cancelErr;
import static org.zstack.longjob.LongJobUtils.jobCanceled;


/**
 * Created by on camile 2018/2/2.
 */
@LongJobFor(APIAddImageMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AddImageLongJob implements LongJob {
    private static final CLogger logger = Utils.getLogger(AddImageLongJob.class);

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;

    protected String auditResourceUuid;

    @Override
    public void start(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        AddImageMsg msg = JSONObjectUtil.toObject(job.getJobData(), AddImageMsg.class);
        if (msg.getResourceUuid() == null) {
            msg.setResourceUuid(Platform.getUuid());
            job.setJobData(JSONObjectUtil.toJsonString(msg));
            dbf.update(job);
        }

        bus.makeTargetServiceIdByResourceUuid(msg, ImageConstant.SERVICE_ID, msg.getResourceUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    handleSuccess(reply);
                } else {
                    auditResourceUuid = msg.getResourceUuid();
                    completion.fail(reply.getError());
                }
            }

            private void handleSuccess(MessageReply reply) {
                AddImageReply r = reply.castReply();
                APIAddImageEvent evt = new APIAddImageEvent(job.getApiId());

                auditResourceUuid = r.getInventory().getUuid();
                evt.setInventory(r.getInventory());
                if (jobCanceled(job.getUuid())) {
                    cleanImage(msg, completion, cancelErr(job.getUuid()));
                } else {
                    completion.success(evt);
                }
            }
        });
    }

    @Override
    public void resume(LongJobVO job) {
        AddImageMsg msg = JSONObjectUtil.toObject(job.getJobData(), AddImageMsg.class);
        ImageDeletionMsg dmsg = buildDeletionMsg(msg);
        bus.send(dmsg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("delete image [%s] failed after management node restarted", msg.getResourceUuid()));
                }

                LongJobUtils.changeState(job.getUuid(), LongJobStateEvent.fail,
                        vo -> vo.setJobResult("Failed because management node restarted."));
            }
        });
    }

    private void cleanImage(AddImageMsg msg, ReturnValueCompletion<APIEvent> completion, ErrorCode err) {
        ImageDeletionMsg dmsg = buildDeletionMsg(msg);
        bus.send(dmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                completion.fail(err);
            }
        });
    }

    @Override
    public void cancel(LongJobVO job, ReturnValueCompletion<Boolean> completion) {
        CancelAddImageMsg msg = new CancelAddImageMsg();
        AddImageMsg amsg = JSONObjectUtil.toObject(job.getJobData(), AddImageMsg.class);
        msg.setMsg(amsg);
        msg.setImageUuid(amsg.getResourceUuid());
        msg.setCancellationApiId(job.getApiId());
        bus.makeTargetServiceIdByResourceUuid(msg, ImageConstant.SERVICE_ID, msg.getImageUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success(false);
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public Class getAuditType() {
        return ImageVO.class;
    }

    @Override
    public String getAuditResourceUuid() {
        return auditResourceUuid;
    }

    private ImageDeletionMsg buildDeletionMsg(AddImageMsg msg) {
        ImageDeletionMsg dmsg = new ImageDeletionMsg();
        dmsg.setImageUuid(msg.getResourceUuid());
        dmsg.setForceDelete(true);
        dmsg.setBackupStorageUuids(msg.getBackupStorageUuids());
        dmsg.setDeletionPolicy(ImageDeletionPolicyManager.ImageDeletionPolicy.Direct.toString());
        bus.makeTargetServiceIdByResourceUuid(dmsg, ImageConstant.SERVICE_ID, dmsg.getImageUuid());
        return dmsg;
    }
}
