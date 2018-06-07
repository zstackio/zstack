package org.zstack.storage.snapshot;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.Completion;
import org.zstack.header.longjob.LongJob;
import org.zstack.header.longjob.LongJobFor;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.snapshot.APIDeleteVolumeSnapshotMsg;
import org.zstack.header.storage.snapshot.DeleteVolumeSnapshotMsg;
import org.zstack.header.storage.snapshot.VolumeSnapshotConstant;
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
    public void start(LongJobVO job, Completion completion) {
        DeleteVolumeSnapshotMsg msg = new DeleteVolumeSnapshotMsg();
        APIDeleteVolumeSnapshotMsg apiMessage = JSONObjectUtil.toObject(job.getJobData(), APIDeleteVolumeSnapshotMsg.class);
        msg.setApiMessage(apiMessage);
        msg.setTreeUuid(apiMessage.getTreeUuid());
        msg.setVolumeUuid(apiMessage.getSnapshotUuid());
        msg.setSnapshotUuid(apiMessage.getSnapshotUuid());
        bus.makeLocalServiceId(msg, VolumeSnapshotConstant.SERVICE_ID);
        bus.send(msg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
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
}
