package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;

public class DestroyVmInstanceAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public DestroyVmInstanceResult value;

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

    @Param(required = false)
    public java.lang.String deleteMode = "Permissive";

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
        
        DestroyVmInstanceResult value = res.getResult(DestroyVmInstanceResult.class);
        ret.value = value == null ? new DestroyVmInstanceResult() : value;
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
                
                DestroyVmInstanceResult value = res.getResult(DestroyVmInstanceResult.class);
                ret.value = value == null ? new DestroyVmInstanceResult() : value;
                completion.complete(ret);
            }
        });
    }

    Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }

    RestInfo getRestInfo() {
        RestInfo info = new RestInfo();
        info.httpMethod = "DELETE";
        info.path = "/vm-instances/{uuid}";
        info.needSession = true;
        info.needPoll = true;
        info.parameterName = "null";
        return info;
    }

}
