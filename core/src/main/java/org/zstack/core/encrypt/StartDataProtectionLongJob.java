package org.zstack.core.encrypt;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.Constants;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.encrypt.APIStartDataProtectionEvent;
import org.zstack.header.core.encrypt.APIStartDataProtectionMsg;
import org.zstack.header.core.encrypt.EncryptionIntegrityVO;
import org.zstack.header.core.encrypt.StartDataProtectionMsg;
import org.zstack.header.image.ImageVO;
import org.zstack.header.longjob.LongJob;
import org.zstack.header.longjob.LongJobFor;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.gson.JSONObjectUtil;

/**
 * @Author: DaoDao
 * @Date: 2021/11/4
 */

@LongJobFor(APIStartDataProtectionMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class StartDataProtectionLongJob implements LongJob {
    @Autowired
    private CloudBus bus;

    @Override
    public void start(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        StartDataProtectionMsg dataProtectionMsg = JSONObjectUtil.toObject(job.getJobData(), StartDataProtectionMsg.class);
        bus.makeLocalServiceId(dataProtectionMsg, EncryptGlobalConfig.SERVICE_ID);
        bus.send(dataProtectionMsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                }
                APIStartDataProtectionEvent event = new APIStartDataProtectionEvent(ThreadContext.get(Constants.THREAD_CONTEXT_API));
                completion.success(event);
            }
        });
    }

    @Override
    public Class getAuditType() {
        return EncryptionIntegrityVO.class;
    }
}
