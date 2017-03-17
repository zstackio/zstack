package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;

public class UpdateVmInstanceAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public UpdateVmInstanceResult value;

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

    @Param(required = false, maxLength = 255, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String name;

    @Param(required = false, maxLength = 2048, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String description;

    @Param(required = false, validValues = {"Stopped","Running"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String state;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String defaultL3NetworkUuid;

    @Param(required = false, validValues = {"Linux","Windows","Other","Paravirtualization","WindowsVirtio"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String platform;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {1L,1024L}, noTrim = false)
    public java.lang.Integer cpuNum;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {1L,9223372036854775807L}, noTrim = false)
    public java.lang.Long memorySize;

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
        
        UpdateVmInstanceResult value = res.getResult(UpdateVmInstanceResult.class);
        ret.value = value == null ? new UpdateVmInstanceResult() : value; 

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
        info.path = "/vm-instances/{uuid}/actions";
        info.needSession = true;
        info.needPoll = true;
        info.parameterName = "updateVmInstance";
        return info;
    }

}
