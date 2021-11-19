package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;
import org.zstack.sdk.*;

public class UpdateInfoSecSecretResourcePoolAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    private static final HashMap<String, Parameter> nonAPIParameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public org.zstack.sdk.UpdateSecretResourcePoolResult value;

        public Result throwExceptionIfError() {
            if (error != null) {
                throw new ApiException(
                    String.format("error[code: %s, description: %s, details: %s]", error.code, error.description, error.details)
                );
            }
            
            return this;
        }
    }

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {6000L,180000L}, noTrim = false)
    public java.lang.Long connectTimeOut;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.Boolean autoCheck;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {6000L,180000L}, noTrim = false)
    public java.lang.Long interval;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {1L,4L}, noTrim = false)
    public java.lang.Integer connectionMode;

    @Param(required = false, maxLength = 30, minLength = 4, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String activatedToken;

    @Param(required = false, maxLength = 30, minLength = 4, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String protectToken;

    @Param(required = false, maxLength = 30, minLength = 4, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String hmacToken;

    @Param(required = true, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String uuid;

    @Param(required = false, maxLength = 255, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String name;

    @Param(required = false, maxLength = 2048, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String description;

    @Param(required = false, maxLength = 255, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String model;

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
        
        org.zstack.sdk.UpdateSecretResourcePoolResult value = res.getResult(org.zstack.sdk.UpdateSecretResourcePoolResult.class);
        ret.value = value == null ? new org.zstack.sdk.UpdateSecretResourcePoolResult() : value; 

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
        info.path = "/secret-resource-pools/infoSec/{uuid}/actions";
        info.needSession = true;
        info.needPoll = true;
        info.parameterName = "updateInfoSecSecretResourcePool";
        return info;
    }

}
