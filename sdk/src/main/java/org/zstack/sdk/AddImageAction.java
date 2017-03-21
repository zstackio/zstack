package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;

public class AddImageAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public AddImageResult value;

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

    @Param(required = true, maxLength = 1024, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String url;

    @Param(required = false, validValues = {"RootVolumeTemplate","ISO","DataVolumeTemplate"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String mediaType;

    @Param(required = false, maxLength = 255, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String guestOsType;

    @Param(required = false)
    public boolean system = false;

    @Param(required = true, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String format;

    @Param(required = false, validValues = {"Linux","Windows","Other","Paravirtualization","WindowsVirtio"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String platform;

    @Param(required = true, nonempty = true, nullElements = false, emptyString = true, noTrim = false)
    public java.util.List backupStorageUuids;

    @Param(required = false)
    public java.lang.String type;

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


    public Result call() {
        ApiResult res = ZSClient.call(this);
        Result ret = new Result();
        if (res.error != null) {
            ret.error = res.error;
            return ret;
        }
        
        AddImageResult value = res.getResult(AddImageResult.class);
        ret.value = value == null ? new AddImageResult() : value;
        return ret;
    }

    public void call(final Completion<Result> completion) {
        ZSClient.call(this, new InternalCompletion() {
            @Override
            public void complete(ApiResult res) {
                Result ret = new Result();
                if (res.error != null) {
                    ret.error = res.error;
                    completion.complete(ret);
                    return;
                }
                
                AddImageResult value = res.getResult(AddImageResult.class);
                ret.value = value == null ? new AddImageResult() : value;
                completion.complete(ret);
            }
        });
    }

    Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }

    RestInfo getRestInfo() {
        RestInfo info = new RestInfo();
        info.httpMethod = "POST";
        info.path = "/images";
        info.needSession = true;
        info.needPoll = true;
        info.parameterName = "params";
        return info;
    }

}
