package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;
import org.zstack.sdk.*;

public class QueryExecutionAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    private static final HashMap<String, Parameter> nonAPIParameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public org.zstack.sdk.QueryExecutionResult value;

        public Result throwExceptionIfError() {
            if (error != null) {
                throw new ApiException(
                    String.format("error[code: %s, description: %s, details: %s, globalErrorCode: %s]", error.code, error.description, error.details, error.globalErrorCode)    
                );
            }
            
            return this;
        }
    }

    @Param(required = false, nonempty = true, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String executionUuid;

    @Param(required = false, nonempty = true, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String apiUuid;

    @Param(required = false, nonempty = true, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String messageUuid;

    @Param(required = false, nonempty = true, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String taskRunUuid;

    @Param(required = false, validValues = {"API","SCHEDULED_TASK","MESSAGE"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String triggerType;

    @Param(required = false, nonempty = true, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String triggerName;

    @Param(required = false, validValues = {"RECEIVED","QUEUED","RUNNING","WAITING","SUCCEEDED","FAILED","TIMEOUT","CANCELLED","UNKNOWN","STALE","EXPIRED"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String state;

    @Param(required = false, nonempty = true, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String startedAfter;

    @Param(required = false, nonempty = true, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String startedBefore;

    @Param(required = false, nonempty = true, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String nodeUuid;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {0L,9223372036854775807L}, noTrim = false)
    public java.lang.Long minExecutionDurationMs;

    @Param(required = false, validValues = {"summary","timeline","criticalPath"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String detail = "summary";

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {1L,200L}, noTrim = false)
    public java.lang.Integer limit = 50;

    @Param(required = false, nonempty = true, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String cursor;

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


    private Result makeResult(ApiResult res) {
        Result ret = new Result();
        if (res.error != null) {
            ret.error = res.error;
            return ret;
        }
        
        org.zstack.sdk.QueryExecutionResult value = res.getResult(org.zstack.sdk.QueryExecutionResult.class);
        ret.value = value == null ? new org.zstack.sdk.QueryExecutionResult() : value; 

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
        info.httpMethod = "GET";
        info.path = "/executions";
        info.needSession = true;
        info.needPoll = false;
        info.parameterName = "";
        return info;
    }

}
