package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;

public class CreatePciDeviceOfferingAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public CreatePciDeviceOfferingResult value;

        public Result throwExceptionIfError() {
            if (error != null) {
                throw new ApiException(
                    String.format("error[code: %s, description: %s, details: %s]", error.code, error.description, error.details)
                );
            }
            
            return this;
        }
    }

    @Param(required = true, maxLength = 255, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String name;

    @Param(required = false, maxLength = 2048, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String description;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String type;

    @Param(required = true, maxLength = 4, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String vendorId;

    @Param(required = true, maxLength = 4, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String deviceId;

    @Param(required = false, maxLength = 4, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String subvendorId;

    @Param(required = false, maxLength = 4, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String subdeviceId;

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
        
        CreatePciDeviceOfferingResult value = res.getResult(CreatePciDeviceOfferingResult.class);
        ret.value = value == null ? new CreatePciDeviceOfferingResult() : value; 

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
        info.path = "/pci-device/pci-device-offerings";
        info.needSession = true;
        info.needPoll = true;
        info.parameterName = "params";
        return info;
    }

}
