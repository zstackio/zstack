package org.zstack.longjob;

import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.longjob.*;
import org.zstack.header.message.APIMessage;
import org.zstack.portal.apimediator.ApiMessageProcessor;
import org.zstack.portal.apimediator.ApiMessageProcessorImpl;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.argerr;

/**
 * Created by GuoYi on 12/6/17.
 */
public class LongJobApiInterceptor implements ApiMessageInterceptor {
    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APISubmitLongJobMsg) {
            validate((APISubmitLongJobMsg) msg);
        } else if (msg instanceof APICancelLongJobMsg) {
            validate((APICancelLongJobMsg) msg);
        } else if (msg instanceof APIDeleteLongJobMsg) {
            validate((APIDeleteLongJobMsg) msg);
        }

        return msg;
    }

    private void validate(APISubmitLongJobMsg msg) {
        Class<APIMessage> apiClass = null;
        List<Class> longJobClasses = BeanUtils.scanClass("org.zstack", LongJobFor.class);
        for (Class it : longJobClasses) {
            LongJobFor at = (LongJobFor) it.getAnnotation(LongJobFor.class);
            if (at.value().getSimpleName().equals(msg.getJobName())) {
                try {
                    apiClass = (Class<APIMessage>) Class.forName(at.value().getName());
                } catch (ClassNotFoundException e) {
                    throw new OperationFailureException(argerr("%s is not an API", msg.getJobName()));
                }
            }
        }
        if (apiClass == null) {
            throw new OperationFailureException(argerr("%s does not have corresponding longjob", msg.getJobName()));
        }

        // validate msg.jobData
        Map<String, Object> config = new HashMap<>();
        List<String> serviceConfigFolders = new ArrayList<>();
        serviceConfigFolders.add("serviceConfig");
        config.put("serviceConfigFolders", serviceConfigFolders);
        ApiMessageProcessor processor = new ApiMessageProcessorImpl(config);
        APIMessage jobMsg = JSONObjectUtil.toObject(msg.getJobData(), apiClass);
        jobMsg.setSession(msg.getSession());
        jobMsg = processor.process(jobMsg);                     // may throw ApiMessageInterceptionException
        msg.setJobData(JSONObjectUtil.toJsonString(jobMsg));    // msg may be changed during validation
    }

    private void validate(APICancelLongJobMsg msg) {
        LongJobState state = Q.New(LongJobVO.class)
                .select(LongJobVO_.state)
                .eq(LongJobVO_.uuid, msg.getUuid())
                .findValue();

        if (state == LongJobState.Succeeded) {
            throw new ApiMessageInterceptionException(argerr("cannot cancel longjob that is succeeded"));
        }
        if (state == LongJobState.Canceled) {
            throw new ApiMessageInterceptionException(argerr("cannot cancel longjob that is already canceled"));
        }
        if (state == LongJobState.Failed) {
            throw new ApiMessageInterceptionException(argerr("cannot cancel longjob that is failed"));
        }
    }

    private void validate(APIDeleteLongJobMsg msg) {
        LongJobState state = Q.New(LongJobVO.class)
                .select(LongJobVO_.state)
                .eq(LongJobVO_.uuid, msg.getUuid())
                .findValue();

        if (state != LongJobState.Succeeded && state != LongJobState.Canceled && state != LongJobState.Failed) {
            throw new ApiMessageInterceptionException(argerr("delete longjob only when it's succeeded, canceled, or failed"));
        }
    }
}
