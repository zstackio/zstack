package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;

public class GetMetricDataAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public GetMetricDataResult value;

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
    public java.lang.String namespace;

    @Param(required = true, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String metricName;

    @Param(required = true, nonempty = false, nullElements = false, emptyString = true, numberRange = {0L,9223372036854775807L}, noTrim = false)
    public java.lang.Long startTime;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {0L,9223372036854775807L}, noTrim = false)
    public java.lang.Long endTime;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {0L,2147483647L}, noTrim = false)
    public java.lang.Integer period;

    @Param(required = false)
    public java.util.List labels;

    @Param(required = false)
    public java.util.List systemTags;

    @Param(required = false)
    public java.util.List userTags;

    @Param(required = true)
    public String sessionId;


    private Result makeResult(ApiResult res) {
        Result ret = new Result();
        if (res.error != null) {
            ret.error = res.error;
            return ret;
        }
        
        GetMetricDataResult value = res.getResult(GetMetricDataResult.class);
        ret.value = value == null ? new GetMetricDataResult() : value; 

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

    Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }

    RestInfo getRestInfo() {
        RestInfo info = new RestInfo();
        info.httpMethod = "GET";
        info.path = "/zwatch/metrics/{namespace}/{metricName}";
        info.needSession = true;
        info.needPoll = false;
        info.parameterName = "";
        return info;
    }

}
