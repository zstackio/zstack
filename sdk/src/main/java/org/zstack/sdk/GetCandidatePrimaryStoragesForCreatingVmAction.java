package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;
import org.zstack.sdk.*;

public class GetCandidatePrimaryStoragesForCreatingVmAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    private static final HashMap<String, Parameter> nonAPIParameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public org.zstack.sdk.GetCandidatePrimaryStoragesForCreatingVmResult value;

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

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {1L,9223372036854775807L}, numberRangeUnit = {"byte", "bytes"}, noTrim = false)
    public java.lang.Long rootDiskSize;

    @Param(required = false, nonempty = true, nullElements = false, emptyString = true, noTrim = false)
    public java.util.List dataDiskOfferingUuids;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {1L,9223372036854775807L}, numberRangeUnit = {"byte", "bytes"}, noTrim = false)
    public java.util.List dataDiskSizes;

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

    @Param(required = false)
    public String sessionId;

    @Param(required = false)
    public String accessKeyId;

    @Param(required = false)
    public String accessKeySecret;

    @Param(required = false)
    public String requestIp;


    private Result makeResult(ApiResult res) {
        Result ret = new Result();
        if (res.error != null) {
            ret.error = res.error;
            return ret;
        }
        
        org.zstack.sdk.GetCandidatePrimaryStoragesForCreatingVmResult value = res.getResult(org.zstack.sdk.GetCandidatePrimaryStoragesForCreatingVmResult.class);
        ret.value = value == null ? new org.zstack.sdk.GetCandidatePrimaryStoragesForCreatingVmResult() : value; 

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

    protected Map<String, Parameter> getNonAPIParameterMap() {
        return nonAPIParameterMap;
    }

    protected RestInfo getRestInfo() {
        RestInfo info = new RestInfo();
        info.httpMethod = "GET";
        info.path = "/vm-instances/candidate-storages";
        info.needSession = true;
        info.needPoll = false;
        info.parameterName = "";
        return info;
    }

}
