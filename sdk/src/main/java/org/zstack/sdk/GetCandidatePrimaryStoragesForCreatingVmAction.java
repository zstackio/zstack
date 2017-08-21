package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;

public class GetCandidatePrimaryStoragesForCreatingVmAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public GetCandidatePrimaryStoragesForCreatingVmResult value;

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
    public java.lang.String imageUuid;

    @Param(required = true, nonempty = true, nullElements = false, emptyString = true, noTrim = false)
    public java.util.List l3NetworkUuids;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String rootDiskOfferingUuid;

    @Param(required = false, nonempty = true, nullElements = false, emptyString = true, noTrim = false)
    public java.util.List dataDiskOfferingUuids;

    @Param(required = false)
    public java.lang.String zoneUuid;

    @Param(required = false)
    public java.lang.String clusterUuid;

    @Param(required = false)
    public java.lang.String defaultL3NetworkUuid;

    @Param(required = false)
    public java.util.List systemTags;

    @Param(required = false)
    public java.util.List userTags;

    @Param(required = true)
    public String sessionId;


    private Result makeResult(ApiResult res) {
        Result ret = new Result();
        if (res.error != null) {
            ret.error = res.error;
            return ret;
        }
        
        GetCandidatePrimaryStoragesForCreatingVmResult value = res.getResult(GetCandidatePrimaryStoragesForCreatingVmResult.class);
        ret.value = value == null ? new GetCandidatePrimaryStoragesForCreatingVmResult() : value; 

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
        info.httpMethod = "GET";
        info.path = "/vm-instances/candidate-storages";
        info.needSession = true;
        info.needPoll = false;
        info.parameterName = "";
        return info;
    }

}
