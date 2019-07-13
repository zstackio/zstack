package org.zstack.compute.vm;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.Constants;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.longjob.LongJobFor;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.*;
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

    protected String auditResourceUuid;

    @Override
    public void start(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        MigrateVmInnerMsg msg = JSONObjectUtil.toObject(job.getJobData(), MigrateVmInnerMsg.class);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, msg.getVmUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    MigrateVmInnerReply r = reply.castReply();
                    APIMigrateVmEvent evt = new APIMigrateVmEvent(ThreadContext.get(Constants.THREAD_CONTEXT_API));

                    auditResourceUuid = r.getInventory().getUuid();
                    evt.setInventory(r.getInventory());
                    completion.success(evt);
                } else {
                    auditResourceUuid = msg.getVmInstanceUuid();
                    completion.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void cancel(LongJobVO job, ReturnValueCompletion<Boolean> completion) {
        // TODO
        completion.fail(Platform.operr("not supported"));
    }

    @Override
    public Class getAuditType() {
        return VmInstanceVO.class;
    }

    @Override
    public String getAuditResourceUuid() {
        return auditResourceUuid;
    }
}
