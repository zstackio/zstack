package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;

public class CreateAliyunVpcVirtualRouterEntryRemoteAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public CreateAliyunVpcVirtualRouterEntryRemoteResult value;

        public Result throwExceptionIfError() {
            if (error != null) {
                throw new ApiException(
                    String.format("error[code: %s, description: %s, details: %s]", error.code, error.description, error.details)
                );
            }
            
            return this;
        }
    }

    @Param(required = true, nonempty = false, nullElements = false, emptyString = false, noTrim = false)
    public java.lang.String vRouterUuid;

    @Param(required = true, nonempty = false, nullElements = false, emptyString = false, noTrim = false)
    public java.lang.String dstCidrBlock;

    @Param(required = true, nonempty = false, nullElements = false, emptyString = false, noTrim = false)
    public java.lang.String nextHopUuid;

    @Param(required = true, validValues = {"Instance","RouterInterface","VpnGateway"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String nextHopType;

    @Param(required = true, validValues = {"vbr","vrouter"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String vRouterType;

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


    private Result makeResult(ApiResult res) {
        Result ret = new Result();
        if (res.error != null) {
            ret.error = res.error;
            return ret;
        }
        
        CreateAliyunVpcVirtualRouterEntryRemoteResult value = res.getResult(CreateAliyunVpcVirtualRouterEntryRemoteResult.class);
        ret.value = value == null ? new CreateAliyunVpcVirtualRouterEntryRemoteResult() : value; 

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
        info.httpMethod = "POST";
        info.path = "/hybrid/aliyun/route-entry";
        info.needSession = true;
        info.needPoll = true;
        info.parameterName = "params";
        return info;
    }

}
