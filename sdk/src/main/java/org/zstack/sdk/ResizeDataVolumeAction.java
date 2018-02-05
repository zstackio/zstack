package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;
import org.zstack.sdk.*;

public class ResizeDataVolumeAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    private static final HashMap<String, Parameter> nonAPIParameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
<<<<<<< HEAD:sdk/src/main/java/org/zstack/sdk/PrimaryStorageMigrateRootVolumeAction.java
        public org.zstack.sdk.PrimaryStorageMigrateRootVolumeResult value;
=======
        public ResizeDataVolumeResult value;
>>>>>>> upstream/master:sdk/src/main/java/org/zstack/sdk/ResizeDataVolumeAction.java

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

    @Param(required = true, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public long size = 0;

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
        
<<<<<<< HEAD:sdk/src/main/java/org/zstack/sdk/PrimaryStorageMigrateRootVolumeAction.java
        org.zstack.sdk.PrimaryStorageMigrateRootVolumeResult value = res.getResult(org.zstack.sdk.PrimaryStorageMigrateRootVolumeResult.class);
        ret.value = value == null ? new org.zstack.sdk.PrimaryStorageMigrateRootVolumeResult() : value; 
=======
        ResizeDataVolumeResult value = res.getResult(ResizeDataVolumeResult.class);
        ret.value = value == null ? new ResizeDataVolumeResult() : value; 
>>>>>>> upstream/master:sdk/src/main/java/org/zstack/sdk/ResizeDataVolumeAction.java

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

<<<<<<< HEAD:sdk/src/main/java/org/zstack/sdk/PrimaryStorageMigrateRootVolumeAction.java
    protected RestInfo getRestInfo() {
=======
    Map<String, Parameter> getNonAPIParameterMap() {
        return nonAPIParameterMap;
    }

    RestInfo getRestInfo() {
>>>>>>> upstream/master:sdk/src/main/java/org/zstack/sdk/ResizeDataVolumeAction.java
        RestInfo info = new RestInfo();
        info.httpMethod = "PUT";
        info.path = "/volumes/data/resize/{uuid}/actions";
        info.needSession = true;
        info.needPoll = true;
        info.parameterName = "resizeDataVolume";
        return info;
    }

}
