package org.zstack.sdk.keyprovider.api;

import java.util.HashMap;
import java.util.Map;
import org.zstack.sdk.*;

public class GetKeyProviderRelatedVmsAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    private static final HashMap<String, Parameter> nonAPIParameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public org.zstack.sdk.keyprovider.api.GetKeyProviderRelatedVmsResult value;

        public Result throwExceptionIfError() {
            if (error != null) {
                throw new ApiException(
                    String.format("error[code: %s, description: %s, details: %s, globalErrorCode: %s]", error.code, error.description, error.details, error.globalErrorCode)    
                );
            }
            
            return this;
        }
    }

    @Param(required = true, nonempty = false, nullElements = false, emptyString = false, noTrim = false)
    public java.lang.String providerUuid;

    @Param(required = false, nonempty = true, nullElements = false, emptyString = false, noTrim = false)
    public java.util.List vmInstanceUuids;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String name;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = false, noTrim = false)
    public java.lang.String state;

    @Param(required = false, validValues = {"CurrentVtpm","Snapshot","LocalBackup"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String referenceType;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {0L,2147483647L}, noTrim = false)
    public int start = 0;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {0L,1000L}, noTrim = false)
    public int limit = 100;

    @Param(required = false, validValues = {"vmInstanceUuid","name","state","lastOpDate","backupCount","snapshotCount"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String sortBy = "lastOpDate";

    @Param(required = false, validValues = {"asc","desc"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String sortDirection = "desc";

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
        
        org.zstack.sdk.keyprovider.api.GetKeyProviderRelatedVmsResult value = res.getResult(org.zstack.sdk.keyprovider.api.GetKeyProviderRelatedVmsResult.class);
        ret.value = value == null ? new org.zstack.sdk.keyprovider.api.GetKeyProviderRelatedVmsResult() : value; 

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
        info.path = "/key-providers/{providerUuid}/related-vms";
        info.needSession = true;
        info.needPoll = false;
        info.parameterName = "";
        return info;
    }

}
