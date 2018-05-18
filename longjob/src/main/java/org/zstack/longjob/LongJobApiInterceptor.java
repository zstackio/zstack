package org.zstack.longjob;

import org.zstack.core.db.Q;
import org.zstack.header.Component;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.longjob.*;
import org.zstack.header.message.APIMessage;
import org.zstack.portal.apimediator.ApiMessageProcessor;
import org.zstack.portal.apimediator.ApiMessageProcessorImpl;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.*;

import static org.zstack.core.Platform.argerr;

/**
 * Created by GuoYi on 12/6/17.
 */
public class LongJobApiInterceptor implements ApiMessageInterceptor, Component {
    private static final CLogger logger = Utils.getLogger(LongJobApiInterceptor.class);

    /**
     * Key:LongJobName
     */
    private TreeMap<String, Class<APIMessage>> apiMsgOfLongJob = new TreeMap<>();

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
        Class<APIMessage> apiClass = apiMsgOfLongJob.get(msg.getJobName());
        if (null == apiClass) {
            throw new ApiMessageInterceptionException(argerr("%s is not an API", msg.getJobName()));
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

    @Override
    public boolean start() {
        Class<APIMessage> apiClass;
        Set<Class<?>> longJobClasses = BeanUtils.reflections.getTypesAnnotatedWith(LongJobFor.class);
        for (Class it : longJobClasses) {
            LongJobFor at = (LongJobFor) it.getAnnotation(LongJobFor.class);
            try {
                apiClass = (Class<APIMessage>) Class.forName(at.value().getName());
            } catch (ClassNotFoundException | ClassCastException e) {
                //ApiMessage and LongJob are not one by one corresponding ,so we skip it
                e.printStackTrace();
                continue;
            }
            logger.debug(String.format("[LongJob] collect api class [%s]", apiClass.getSimpleName()));
            apiMsgOfLongJob.put(at.value().getSimpleName(), apiClass);
        }
        return true;
    }

    @Override
    public boolean stop() {
        apiMsgOfLongJob.clear();
        return true;
    }
}
