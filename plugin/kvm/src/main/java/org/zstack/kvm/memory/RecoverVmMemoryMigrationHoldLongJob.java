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

@UseApiTimeout(APIRecoverVmMemoryMigrationHoldMsg.class)
@LongJobFor(APIRecoverVmMemoryMigrationHoldMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class RecoverVmMemoryMigrationHoldLongJob implements LongJob {
    @Autowired
    protected CloudBus bus;

    @Override
    public void start(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        APIRecoverVmMemoryMigrationHoldMsg request = JSONObjectUtil.toObject(job.getJobData(),
                APIRecoverVmMemoryMigrationHoldMsg.class);
        MemoryVmMigrationHoldRecoveryMsg msg = new MemoryVmMigrationHoldRecoveryMsg();
        msg.setVmUuid(request.getUuid());
        msg.setConfirmedHostUuid(request.getConfirmedHostUuid());
        bus.makeLocalServiceId(msg, MemoryOptimizationManager.SERVICE_ID);
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
