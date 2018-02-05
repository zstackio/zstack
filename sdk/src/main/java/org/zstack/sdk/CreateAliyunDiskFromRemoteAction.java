package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;
import org.zstack.sdk.*;

public class CreateAliyunDiskFromRemoteAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    private static final HashMap<String, Parameter> nonAPIParameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public org.zstack.sdk.CreateAliyunDiskFromRemoteResult value;

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
    public java.lang.String identityUuid;

    @Param(required = true, validRegexValues = "^[\\u4e00-\\u9fa5a-zA-Z][\\u4e00-\\u9fa5_a-zA-Z0-9.-]+$", maxLength = 128, minLength = 2, nonempty = false, nullElements = false, emptyString = false, noTrim = false)
    public java.lang.String name;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {20L,32768L}, noTrim = false)
    public java.lang.Integer sizeWithGB;

    @Param(required = false, maxLength = 1024, minLength = 2, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String description;

    @Param(required = false, validValues = {"cloud_efficiency","cloud_ssd"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String diskCategory;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String snapshotUuid;

    @Param(required = false)
    public java.lang.String resourceUuid;

    @Param(required = false)
    public java.util.List systemTags;

    @Param(required = false)
    public java.util.List userTags;

    @Param(required = true)
    public String sessionId;

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
        
        org.zstack.sdk.CreateAliyunDiskFromRemoteResult value = res.getResult(org.zstack.sdk.CreateAliyunDiskFromRemoteResult.class);
        ret.value = value == null ? new org.zstack.sdk.CreateAliyunDiskFromRemoteResult() : value; 

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

<<<<<<< HEAD
    protected RestInfo getRestInfo() {
=======
    Map<String, Parameter> getNonAPIParameterMap() {
        return nonAPIParameterMap;
    }

    RestInfo getRestInfo() {
>>>>>>> upstream/master
        RestInfo info = new RestInfo();
        info.httpMethod = "POST";
        info.path = "/hybrid/aliyun/disk";
        info.needSession = true;
        info.needPoll = true;
        info.parameterName = "params";
        return info;
    }

}
