package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;

public class UpdateBaremetalHostCfgAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public UpdateBaremetalHostCfgResult value;

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

    @Param(required = false, maxLength = 255, nonempty = false, nullElements = false, emptyString = false, noTrim = false)
    public java.lang.String password;

    @Param(required = false, validValues = {"true","false"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String vnc;

    @Param(required = false, validValues = {"true","false"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String unattended;

    @Param(required = false, nonempty = true, nullElements = false, emptyString = true, noTrim = false)
    public java.util.List nicCfgs;

    @Param(required = false)
    public java.lang.String chassisUuid;

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
        
        UpdateBaremetalHostCfgResult value = res.getResult(UpdateBaremetalHostCfgResult.class);
        ret.value = value == null ? new UpdateBaremetalHostCfgResult() : value; 

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
        info.httpMethod = "PUT";
        info.path = "/baremetal/hostcfg/{uuid}/actions";
        info.needSession = true;
        info.needPoll = true;
        info.parameterName = "updateBaremetalHostCfg";
        return info;
    }

}
