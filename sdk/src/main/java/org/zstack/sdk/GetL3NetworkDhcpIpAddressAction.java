package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;

public class GetL3NetworkDhcpIpAddressAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public GetL3NetworkDhcpIpAddressResult value;

        public Result throwExceptionIfError() {
            if (error != null) {
                throw new ApiException(
                    String.format("error[code: %s, description: %s, details: %s]", error.code, error.description, error.details)
                );
            }
            
            return this;
        }
    }

    @Param(required = false)
    public java.lang.String l3NetworkUuid;

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
        
        GetL3NetworkDhcpIpAddressResult value = res.getResult(GetL3NetworkDhcpIpAddressResult.class);
        ret.value = value == null ? new GetL3NetworkDhcpIpAddressResult() : value; 

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
        info.path = "/l3-networks/{l3NetworkUuid/dhcp-ip";
        info.needSession = true;
        info.needPoll = false;
        info.parameterName = "";
        return info;
    }

}
