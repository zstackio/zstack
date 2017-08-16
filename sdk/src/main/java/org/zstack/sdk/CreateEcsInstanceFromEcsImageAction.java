package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;

public class CreateEcsInstanceFromEcsImageAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public CreateEcsInstanceFromEcsImageResult value;

        public Result throwExceptionIfError() {
            if (error != null) {
                throw new ApiException(
                    String.format("error[code: %s, description: %s, details: %s]", error.code, error.description, error.details)
                );
            }
            
            return this;
        }
    }

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String ecsRootVolumeType;

    @Param(required = false, maxLength = 1024, minLength = 2, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String description;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {40L,500L}, noTrim = false)
    public java.lang.Long ecsRootVolumeGBSize;

    @Param(required = false, validValues = {"atomic","permissive"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String createMode;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String privateIpAddress;

    @Param(required = false, validValues = {"true","false"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String allocatePublicIp;

    @Param(required = false, validRegexValues = "[A-Za-z0-9]{6}", maxLength = 6, minLength = 6, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String ecsConsolePassword;

    @Param(required = true, maxLength = 128, nonempty = false, nullElements = false, emptyString = false, noTrim = false)
    public java.lang.String name;

    @Param(required = true, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String ecsImageUuid;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String instanceOfferingUuid;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String instanceType;

    @Param(required = true, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String ecsVSwitchUuid;

    @Param(required = true, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String ecsSecurityGroupUuid;

    @Param(required = true, maxLength = 30, minLength = 8, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String ecsRootPassword;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {1L,200L}, noTrim = false)
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
        
        CreateEcsInstanceFromEcsImageResult value = res.getResult(CreateEcsInstanceFromEcsImageResult.class);
        ret.value = value == null ? new CreateEcsInstanceFromEcsImageResult() : value; 

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
