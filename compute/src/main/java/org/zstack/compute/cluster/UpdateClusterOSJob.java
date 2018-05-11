package org.zstack.compute.cluster;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.APIUpdateClusterOSMsg;
import org.zstack.header.cluster.ClusterConstant;
import org.zstack.header.cluster.UpdateClusterOSMsg;
import org.zstack.header.cluster.UpdateClusterOSReply;
import org.zstack.header.core.Completion;
import org.zstack.header.longjob.LongJobFor;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.longjob.LongJob;
import org.zstack.utils.gson.JSONObjectUtil;

/**
 * Created by GuoYi on 3/12/18
 */
@LongJobFor(APIUpdateClusterOSMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class UpdateClusterOSJob implements LongJob {
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;

    @Override
    public void start(LongJobVO job, Completion completion) {
        UpdateClusterOSMsg msg = JSONObjectUtil.toObject(job.getJobData(), UpdateClusterOSMsg.class);
        bus.makeLocalServiceId(msg, ClusterConstant.SERVICE_ID);
        bus.send(msg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                UpdateClusterOSReply rly = reply.castReply();
                if (reply.isSuccess()) {
                    job.setJobResult(JSONObjectUtil.toJsonString(rly.getResults()));
                    dbf.update(job);
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
