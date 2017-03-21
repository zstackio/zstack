package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;

public class GetTaskProgressAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public GetTaskProgressResult value;

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
    public java.lang.String resourceUuid;

    @Param(required = false, validValues = {"AddImage","LocalStorageMigrateVolume","CreateRootVolumeTemplateFromRootVolume"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String processType;

    @Param(required = false)
    public java.util.List systemTags;

    @Param(required = false)
    public java.util.List userTags;

    @Param(required = true)
    public String sessionId;


    public Result call() {
        ApiResult res = ZSClient.call(this);
        Result ret = new Result();
        if (res.error != null) {
            ret.error = res.error;
            return ret;
        }
        
        GetTaskProgressResult value = res.getResult(GetTaskProgressResult.class);
        ret.value = value == null ? new GetTaskProgressResult() : value;
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
                
                GetTaskProgressResult value = res.getResult(GetTaskProgressResult.class);
                ret.value = value == null ? new GetTaskProgressResult() : value;
                completion.complete(ret);
            }
        });
    }

    Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }

    RestInfo getRestInfo() {
        RestInfo info = new RestInfo();
        info.httpMethod = "GET";
        info.path = "/progress/{processType}/{resourceUuid}";
        info.needSession = true;
        info.needPoll = false;
        info.parameterName = "";
        return info;
    }

}
