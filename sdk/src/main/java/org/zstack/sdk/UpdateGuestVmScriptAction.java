package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;
import org.zstack.sdk.*;

public class UpdateGuestVmScriptAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    private static final HashMap<String, Parameter> nonAPIParameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public org.zstack.sdk.UpdateGuestVmScriptResult value;

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

    @Param(required = false, validRegexValues = "^(?! )[\\u4E00-\\u9FFFa-zA-Z0-9_\\-\\.():+\\s]*(?<! )$", maxLength = 128, minLength = 1, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String name;

    @Param(required = false, maxLength = 256, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String description;

    @Param(required = false, maxLength = 4096, minLength = 1, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String scriptContent;

    @Param(required = false, maxLength = 5120, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String renderParams;

    @Param(required = false, validValues = {"Windows","Linux"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String platform;

    @Param(required = false, validValues = {"Shell","Python","Perl","Bat","Powershell"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String scriptType;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {0L,86400L}, noTrim = false)
    public java.lang.Integer scriptTimeout;

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
        
        org.zstack.sdk.UpdateGuestVmScriptResult value = res.getResult(org.zstack.sdk.UpdateGuestVmScriptResult.class);
        ret.value = value == null ? new org.zstack.sdk.UpdateGuestVmScriptResult() : value; 

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
        info.path = "/scripts/{uuid}/actions";
        info.needSession = true;
        info.needPoll = true;
        info.parameterName = "updateGuestVmScript";
        return info;
    }

}
