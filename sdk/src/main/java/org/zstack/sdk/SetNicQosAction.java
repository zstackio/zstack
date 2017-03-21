package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;

public class SetNicQosAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public SetNicQosResult value;

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
    public java.lang.String uuid;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {8192L,9223372036854775807L}, noTrim = false)
    public java.lang.Long outboundBandwidth;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {8192L,9223372036854775807L}, noTrim = false)
    public java.lang.Long inboundBandwidth;

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
        
        SetNicQosResult value = res.getResult(SetNicQosResult.class);
        ret.value = value == null ? new SetNicQosResult() : value;
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
                
                SetNicQosResult value = res.getResult(SetNicQosResult.class);
                ret.value = value == null ? new SetNicQosResult() : value;
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
        info.path = "/vm-instances/{uuid}/qos";
        info.needSession = true;
        info.needPoll = true;
        info.parameterName = "params";
        return info;
    }

}
