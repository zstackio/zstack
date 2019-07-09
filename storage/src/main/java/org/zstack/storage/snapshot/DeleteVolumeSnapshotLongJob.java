package org.zstack.storage.snapshot;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusImpl3;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.longjob.LongJob;
import org.zstack.header.longjob.LongJobFor;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.snapshot.*;
import org.zstack.utils.gson.JSONObjectUtil;

/**
 * Created by kayo on 2018/5/9.
 */
@LongJobFor(APIDeleteVolumeSnapshotMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class DeleteVolumeSnapshotLongJob implements LongJob {
    @Autowired
    protected CloudBus bus;

    @Override
    public void start(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        DeleteVolumeSnapshotMsg msg = new DeleteVolumeSnapshotMsg();
        APIDeleteVolumeSnapshotMsg apiMessage = JSONObjectUtil.toObject(job.getJobData(), APIDeleteVolumeSnapshotMsg.class);
        msg.setTreeUuid(apiMessage.getTreeUuid());
        msg.setVolumeUuid(apiMessage.getSnapshotUuid());
        msg.setSnapshotUuid(apiMessage.getSnapshotUuid());
        msg.setDeletionMode(apiMessage.getDeletionMode());
        bus.makeTargetServiceIdByResourceUuid(msg, VolumeSnapshotConstant.SERVICE_ID, getRoutedMnId(apiMessage));
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success(null);
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void cancel(LongJobVO job, Completion completion) {
        // TODO
        completion.fail(Platform.operr("not supported"));
    }

    private String getRoutedMnId(APIDeleteVolumeSnapshotMsg msg) {
        // ApiMsg has been set service id in their way, follow it.
        return CloudBusImpl3.getManagementNodeUUIDFromServiceID(msg.getServiceId());
    }
}
