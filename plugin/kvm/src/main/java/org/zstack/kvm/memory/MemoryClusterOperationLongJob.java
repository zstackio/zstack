package org.zstack.kvm.memory;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.longjob.LongJob;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.MessageReply;
import org.zstack.longjob.LongJobUtils;
import org.zstack.utils.gson.JSONObjectUtil;

/** Runs a cluster pause, resume or drain; the job result maps hostUuid to its outcome. */
public abstract class MemoryClusterOperationLongJob implements LongJob {
    @Autowired
    protected CloudBus bus;

    private final Class<? extends APIMemoryClusterOperationMsg> api;
    private final String action;

    protected MemoryClusterOperationLongJob(Class<? extends APIMemoryClusterOperationMsg> api, String action) {
        this.api = api;
        this.action = action;
    }

    @Override
    public void start(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        APIMemoryClusterOperationMsg request = JSONObjectUtil.toObject(job.getJobData(), api);
        MemoryClusterOperationMsg msg = new MemoryClusterOperationMsg();
        msg.setClusterUuid(request.getUuid());
        msg.setHostUuid(request.getHostUuid());
        msg.setAction(action);
        msg.setAccountUuid(job.getAccountUuid());
        msg.setUserUuid(request.getSession() == null ? null : request.getSession().getUserUuid());
        bus.makeLocalServiceId(msg, MemoryOptimizationManager.SERVICE_ID);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }
                MemoryClusterOperationReply result = reply.castReply();
                LongJobUtils.setJobResult(job.getUuid(), JSONObjectUtil.toJsonString(result.getResults()));
                completion.success(null);
            }
        });
    }
}
