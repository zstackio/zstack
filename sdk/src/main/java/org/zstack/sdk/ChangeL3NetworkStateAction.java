package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;

public class ChangeL3NetworkStateAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public ChangeL3NetworkStateResult value;

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

    @Param(required = true, validValues = {"enable","disable"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String stateEvent;

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
        
        ChangeL3NetworkStateResult value = res.getResult(ChangeL3NetworkStateResult.class);
        ret.value = value == null ? new ChangeL3NetworkStateResult() : value;
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
                
                ChangeL3NetworkStateResult value = res.getResult(ChangeL3NetworkStateResult.class);
                ret.value = value == null ? new ChangeL3NetworkStateResult() : value;
                completion.complete(ret);
            }
        });
    }

    Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }

    RestInfo getRestInfo() {
        RestInfo info = new RestInfo();
        info.httpMethod = "PUT";
        info.path = "/l3-networks/{uuid}/actions";
        info.needSession = true;
        info.needPoll = true;
        info.parameterName = "changeL3NetworkState";
        return info;
    }

}
