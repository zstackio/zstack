package org.zstack.sdk.zwatch.api;

import java.util.HashMap;
import java.util.Map;
import org.zstack.sdk.*;

public class UpdateAlarmDataAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    private static final HashMap<String, Parameter> nonAPIParameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public org.zstack.sdk.zwatch.api.UpdateAlarmDataResult value;

        public Result throwExceptionIfError() {
            if (error != null) {
                throw new ApiException(
                    String.format("error[code: %s, description: %s, details: %s]", error.code, error.description, error.details)
                );
            }
            
            return this;
        }
    }

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String dataUuid;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.Long dataStartTime;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.Long dataEndTime;

    @Param(required = true, validValues = {"OnlyOne","InRange","All"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String updateMode;

    @Param(required = false, validValues = {"Read","Unread"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String readStatus;

    @Param(required = false)
    public java.util.List systemTags;

    @Param(required = false)
    public java.util.List userTags;

    @Param(required = false)
    public String sessionId;

    @Param(required = false)
    public String accessKeyId;

    @Param(required = false)
    public String accessKeySecret;

    @Param(required = false)
    public String requestIp;

    @NonAPIParam
    public long timeout = -1;

    @NonAPIParam
    public long pollingInterval = -1;


    private Result makeResult(ApiResult res) {
        Result ret = new Result();
        if (res.error != null) {
            ret.error = res.error;
            return ret;
        }
        
        org.zstack.sdk.zwatch.api.UpdateAlarmDataResult value = res.getResult(org.zstack.sdk.zwatch.api.UpdateAlarmDataResult.class);
        ret.value = value == null ? new org.zstack.sdk.zwatch.api.UpdateAlarmDataResult() : value; 

        return ret;
    }

    public Result call() {
        ApiResult res = ZSClient.call(this);
        return makeResult(res);
    }

    public void call(final Completion<Result> completion) {
        ZSClient.call(this, new InternalCompletion() {
            @Override
            public void complete(ApiResult res) {
                completion.complete(makeResult(res));
            }
        });
    }

    protected Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }

    protected Map<String, Parameter> getNonAPIParameterMap() {
        return nonAPIParameterMap;
    }

    protected RestInfo getRestInfo() {
        RestInfo info = new RestInfo();
        info.httpMethod = "PUT";
        info.path = "/zwatch/alarm-histories/actions";
        info.needSession = true;
        info.needPoll = true;
        info.parameterName = "updateAlarmData";
        return info;
    }

}
