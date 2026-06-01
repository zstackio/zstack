package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;
import org.zstack.sdk.*;

public class GetLicenseAuthorizationInfoAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    private static final HashMap<String, Parameter> nonAPIParameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public org.zstack.sdk.GetLicenseAuthorizationInfoResult value;

        public Result throwExceptionIfError() {
            if (error != null) {
                throw new ApiException(
                    String.format("error[code: %s, description: %s, details: %s, globalErrorCode: %s]", error.code, error.description, error.details, error.globalErrorCode)    
                );
            }
            
            return this;
        }
    }

    @Param(required = false)
    public java.util.List systemTags;

    @Param(required = false)
    public java.util.List userTags;

    @NonAPIParam
    public boolean isSuppressCredentialCheck = true;

    @Param(required = false)
    public String requestIp;


    private Result makeResult(ApiResult res) {
        Result ret = new Result();
        if (res.error != null) {
            ret.error = res.error;
            return ret;
        }
        
        org.zstack.sdk.GetLicenseAuthorizationInfoResult value = res.getResult(org.zstack.sdk.GetLicenseAuthorizationInfoResult.class);
        ret.value = value == null ? new org.zstack.sdk.GetLicenseAuthorizationInfoResult() : value; 

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
        info.path = "/licenses/authorization-info";
        info.needSession = false;
        info.needPoll = false;
        info.parameterName = "";
        return info;
    }

}
