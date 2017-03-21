package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;

public class PrometheusQueryVmMonitoringDataAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public PrometheusQueryVmMonitoringDataResult value;

        public Result throwExceptionIfError() {
            if (error != null) {
                throw new ApiException(
                    String.format("error[code: %s, description: %s, details: %s]", error.code, error.description, error.details)
                );
            }
            
            return this;
        }
    }

    @Param(required = true, nonempty = true, nullElements = false, emptyString = true, noTrim = false)
    public java.util.List vmUuids;

    @Param(required = false)
    public boolean instant = false;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {0L,9223372036854775807L}, noTrim = false)
    public java.lang.Long startTime;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {0L,9223372036854775807L}, noTrim = false)
    public java.lang.Long endTime;

    @Param(required = false)
    public java.lang.String step;

    @Param(required = true, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String expression;

    @Param(required = false)
    public java.lang.String relativeTime;

    @Param(required = false)
    public java.util.List systemTags;

    @Param(required = false)
    public java.util.List userTags;

    @Param(required = true)
    public String sessionId;


    public Result call() {
        ApiResult res = ZSClient.call(this);
        Result ret = new Result();
        if (res.error != null) {
            ret.error = res.error;
            return ret;
        }
        
        PrometheusQueryVmMonitoringDataResult value = res.getResult(PrometheusQueryVmMonitoringDataResult.class);
        ret.value = value == null ? new PrometheusQueryVmMonitoringDataResult() : value;
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
                
                PrometheusQueryVmMonitoringDataResult value = res.getResult(PrometheusQueryVmMonitoringDataResult.class);
                ret.value = value == null ? new PrometheusQueryVmMonitoringDataResult() : value;
                completion.complete(ret);
            }
        });
    }

    Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }

    RestInfo getRestInfo() {
        RestInfo info = new RestInfo();
        info.httpMethod = "GET";
        info.path = "/prometheus/vm-instances";
        info.needSession = true;
        info.needPoll = false;
        info.parameterName = "params";
        return info;
    }

}
