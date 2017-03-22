package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;

public class CreateStopVmInstanceSchedulerAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public CreateStopVmInstanceSchedulerResult value;

        public Result throwExceptionIfError() {
            if (error != null) {
                throw new ApiException(
                    String.format("error[code: %s, description: %s, details: %s]", error.code, error.description, error.details)
                );
            }
            
            return this;
        }
    }

    @Param(required = true, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String vmUuid;

    @Param(required = true, maxLength = 255, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String schedulerName;

    @Param(required = false, maxLength = 2048, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String schedulerDescription;

    @Param(required = true, validValues = {"simple","cron"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String type;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.Integer interval;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.Integer repeatCount;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.Long startTime;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String cron;

    @Param(required = false)
    public java.lang.String resourceUuid;

    @Param(required = false)
    public java.util.List systemTags;

    @Param(required = false)
    public java.util.List userTags;

    @Param(required = true)
    public String sessionId;

    public long timeout;
    
    public long pollingInterval;


    public Result call() {
        ApiResult res = ZSClient.call(this);
        Result ret = new Result();
        if (res.error != null) {
            ret.error = res.error;
            return ret;
        }
        
        CreateStopVmInstanceSchedulerResult value = res.getResult(CreateStopVmInstanceSchedulerResult.class);
        ret.value = value == null ? new CreateStopVmInstanceSchedulerResult() : value;
        return ret;
    }

    public void call(final Completion<Result> completion) {
        ZSClient.call(this, new InternalCompletion() {
            @Override
            public void complete(ApiResult res) {
                Result ret = new Result();
                if (res.error != null) {
                    ret.error = res.error;
                    completion.complete(ret);
                    return;
                }
                
                CreateStopVmInstanceSchedulerResult value = res.getResult(CreateStopVmInstanceSchedulerResult.class);
                ret.value = value == null ? new CreateStopVmInstanceSchedulerResult() : value;
                completion.complete(ret);
            }
        });
    }

    Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }

    RestInfo getRestInfo() {
        RestInfo info = new RestInfo();
        info.httpMethod = "POST";
        info.path = "/vm-instances/{vmUuid}/schedulers/stopping";
        info.needSession = true;
        info.needPoll = true;
        info.parameterName = "params";
        return info;
    }

}
