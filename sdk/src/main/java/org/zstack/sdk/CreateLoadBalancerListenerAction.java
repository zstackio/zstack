package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;

public class CreateLoadBalancerListenerAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public CreateLoadBalancerListenerResult value;

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
    public java.lang.String loadBalancerUuid;

    @Param(required = true, maxLength = 255, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String name;

    @Param(required = false, maxLength = 2048, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String description;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {1L,65535L}, noTrim = false)
    public java.lang.Integer instancePort;

    @Param(required = true, nonempty = false, nullElements = false, emptyString = true, numberRange = {1L,65535L}, noTrim = false)
    public int loadBalancerPort = 0;

    @Param(required = false, validValues = {"tcp","http"}, maxLength = 255, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String protocol;

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
        
        CreateLoadBalancerListenerResult value = res.getResult(CreateLoadBalancerListenerResult.class);
        ret.value = value == null ? new CreateLoadBalancerListenerResult() : value;
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
                
                CreateLoadBalancerListenerResult value = res.getResult(CreateLoadBalancerListenerResult.class);
                ret.value = value == null ? new CreateLoadBalancerListenerResult() : value;
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
        info.path = "/load-balancers/{loadBalancerUuid}/listeners";
        info.needSession = true;
        info.needPoll = true;
        info.parameterName = "params";
        return info;
    }

}
