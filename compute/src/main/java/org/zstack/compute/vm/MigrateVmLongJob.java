package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.longjob.LongJobFor;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.APIMigrateVmMsg;
import org.zstack.header.vm.MigrateVmInnerMsg;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.longjob.LongJob;
import org.zstack.utils.gson.JSONObjectUtil;


/**
 * Created by on camile 2018/3/7.
 */
@LongJobFor(APIMigrateVmMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class MigrateVmLongJob implements LongJob {
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;

    @Override
    public void start(LongJobVO job, Completion completion) {
        MigrateVmInnerMsg msg = JSONObjectUtil.toObject(job.getJobData(), MigrateVmInnerMsg.class);
        bus.makeLocalServiceId(msg, VmInstanceConstant.SERVICE_ID);
        bus.send(msg, new CloudBusCallBack(completion) {
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
