package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Autowire;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.APIUpdateVmOSMsg;
import org.zstack.header.vm.UpdateVmOSMsg;
import org.zstack.header.longjob.LongJob;
import org.zstack.header.longjob.LongJobFor;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.utils.gson.JSONObjectUtil;

@LongJobFor(APIUpdateVmOSMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class UpdateVmOSJob implements LongJob {
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;

    @Override
    public void start(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        UpdateVmOSMsg msg = JSONObjectUtil.toObject(job.getJobData(), UpdateVmOSMsg.class);
        bus.makeLocalServiceId(msg, VmInstanceConstant.SERVICE_ID);
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
    public void cancel(LongJobVO job, ReturnValueCompletion<Boolean> completion) {
        completion.fail(Platform.operr("not supported"));
    }
}
