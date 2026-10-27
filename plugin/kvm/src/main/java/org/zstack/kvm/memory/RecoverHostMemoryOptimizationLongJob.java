package org.zstack.kvm.memory;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.longjob.LongJob;
import org.zstack.header.longjob.LongJobFor;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.longjob.UseApiTimeout;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.MessageReply;
import org.zstack.longjob.LongJobUtils;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.Collections;

@UseApiTimeout(APIRecoverHostMemoryOptimizationMsg.class)
@LongJobFor(APIRecoverHostMemoryOptimizationMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class RecoverHostMemoryOptimizationLongJob implements LongJob {
    @Autowired
    protected CloudBus bus;

    @Override
    public void start(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        APIRecoverHostMemoryOptimizationMsg request = JSONObjectUtil.toObject(job.getJobData(),
                APIRecoverHostMemoryOptimizationMsg.class);
        MemoryHostOperationMsg msg = new MemoryHostOperationMsg();
        msg.setHostUuid(request.getUuid());
        msg.setAction(MemoryUncertainRecoveryRules.ACTION);
        msg.setExpectedControlOperationUuid(request.getExpectedControlOperationUuid());
        msg.setRecovery(request.toRecovery());
        msg.setJobUuid(job.getUuid());
        msg.setAccountUuid(job.getAccountUuid());
        msg.setUserUuid(request.getSession() == null ? null : request.getSession().getUserUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, MemoryOptimizationManager.SERVICE_ID, request.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }
                LongJobUtils.setJobResult(job.getUuid(),
                        JSONObjectUtil.toJsonString(Collections.singletonMap(request.getUuid(), "success")));
                completion.success(null);
            }
        });
    }
}
