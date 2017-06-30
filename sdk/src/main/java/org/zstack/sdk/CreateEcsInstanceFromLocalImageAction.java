package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;

public class CreateEcsInstanceFromLocalImageAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public CreateEcsInstanceFromLocalImageResult value;

        public Result throwExceptionIfError() {
            if (error != null) {
                throw new ApiException(
                    String.format("error[code: %s, description: %s, details: %s]", error.code, error.description, error.details)
                );
            }
            
            return this;
        }
    }

    @Param(required = false, validValues = {"cloud","cloud_efficiency","cloud_ssd","ephemeral_ssd"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String ecsRootVolumeType;

    @Param(required = false, maxLength = 256, minLength = 2, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String description;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {40L,500L}, noTrim = false)
    public java.lang.Long ecsRootVolumeGBSize;

    @Param(required = false, validValues = {"atomic","permissive"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String createMode;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String privateIpAddress;

    @Param(required = false, maxLength = 128, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String ecsInstanceName;

    @Param(required = false, validValues = {"true","false"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String allocatePublicIp;

    @Param(required = false, validRegexValues = "[a-zA-Z0-9]{6}", maxLength = 6, minLength = 6, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String ecsConsolePassword;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String backupStorageUuid;

    @Param(required = true, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String imageUuid;

    @Param(required = true, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String instanceOfferingUuid;

    @Param(required = true, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String ecsVSwitchUuid;

    @Param(required = true, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String ecsSecurityGroupUuid;

    @Param(required = true, validRegexValues = "^[a-zA-Z][\\w\\W]{7,17}$", maxLength = 30, minLength = 8, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String ecsRootPassword;

    @Param(required = true, nonempty = false, nullElements = false, emptyString = true, numberRange = {0L,200L}, noTrim = false)
    public java.lang.Long ecsBandWidth;

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
        
        CreateEcsInstanceFromLocalImageResult value = res.getResult(CreateEcsInstanceFromLocalImageResult.class);
        ret.value = value == null ? new CreateEcsInstanceFromLocalImageResult() : value; 

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
        info.path = "/hybrid/aliyun/ecs";
        info.needSession = true;
        info.needPoll = true;
        info.parameterName = "params";
        return info;
    }

}
