package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;
import org.zstack.sdk.*;

public class UpdatePoliceAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    private static final HashMap<String, Parameter> nonAPIParameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public org.zstack.sdk.UpdatePoliceResult value;

        public Result throwExceptionIfError() {
            if (error != null) {
                throw new ApiException(
                    String.format("error[code: %s, description: %s, details: %s]", error.code, error.description, error.details)
                );
            }
            
            return this;
        }
    }

    @Param(required = true, maxLength = 128, nonempty = false, nullElements = false, emptyString = false, noTrim = false)
    public java.lang.String police_id;

    @Param(required = false, maxLength = 128, nonempty = false, nullElements = false, emptyString = false, noTrim = false)
    public java.lang.String name;

    @Param(required = false, maxLength = 128, nonempty = false, nullElements = false, emptyString = false, noTrim = false)
    public java.lang.String gender;

    @Param(required = false, maxLength = 128, nonempty = false, nullElements = false, emptyString = false, noTrim = false)
    public java.lang.String email;

    @Param(required = false, maxLength = 128, nonempty = false, nullElements = false, emptyString = false, noTrim = false)
    public java.lang.String phone;

    @Param(required = false, maxLength = 128, nonempty = false, nullElements = false, emptyString = false, noTrim = false)
    public java.lang.String address;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {1L,2147483647L}, noTrim = false)
    public java.lang.Integer organization_id;

    @Param(required = false, maxLength = 128, nonempty = false, nullElements = false, emptyString = false, noTrim = false)
    public java.lang.String organization_name;

    @Param(required = false)
    public java.util.List systemTags;

    @Param(required = false)
    public java.util.List userTags;

    @Param(required = false)
    public String sessionId;

    @Param(required = false)
    public String accessKeyId;

    @Param(required = false)
    public String accessKeySecret;

    @Param(required = false)
    public String requestIp;

    @NonAPIParam
    public long timeout = -1;

    @NonAPIParam
    public long pollingInterval = -1;


    private Result makeResult(ApiResult res) {
        Result ret = new Result();
        if (res.error != null) {
            ret.error = res.error;
            return ret;
        }
        
        org.zstack.sdk.UpdatePoliceResult value = res.getResult(org.zstack.sdk.UpdatePoliceResult.class);
        ret.value = value == null ? new org.zstack.sdk.UpdatePoliceResult() : value; 

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
        info.httpMethod = "PUT";
        info.path = "/api/v1/users/{police_id}";
        info.needSession = true;
        info.needPoll = true;
        info.parameterName = "updatePolice";
        return info;
    }

}
