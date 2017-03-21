package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;

public class GetPortForwardingAttachableVmNicsAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public GetPortForwardingAttachableVmNicsResult value;

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
    public java.lang.String ruleUuid;

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
        
        GetPortForwardingAttachableVmNicsResult value = res.getResult(GetPortForwardingAttachableVmNicsResult.class);
        ret.value = value == null ? new GetPortForwardingAttachableVmNicsResult() : value;
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
                
                GetPortForwardingAttachableVmNicsResult value = res.getResult(GetPortForwardingAttachableVmNicsResult.class);
                ret.value = value == null ? new GetPortForwardingAttachableVmNicsResult() : value;
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
        info.path = "/port-forwarding/{ruleUuid}/vm-instances/candidate-nics";
        info.needSession = true;
        info.needPoll = false;
        info.parameterName = "";
        return info;
    }

}
