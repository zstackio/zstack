package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;
import org.zstack.sdk.*;

public class OpenZsDatasetTransferAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    private static final HashMap<String, Parameter> nonAPIParameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public org.zstack.sdk.OpenZsDatasetTransferResult value;

        public Result throwExceptionIfError() {
            if (error != null) {
                throw new ApiException(
                    String.format("error[code: %s, description: %s, details: %s, globalErrorCode: %s]", error.code, error.description, error.details, error.globalErrorCode)    
                );
            }
            
            return this;
        }
    }

    @Param(required = false, maxLength = 32, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String projectUuid;

    @Param(required = true, maxLength = 32, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String spaceUuid;

    @Param(required = true, validValues = {"SOURCE","IMAGE","IMAGE_ARCHIVE","IMAGE_PDF"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String purpose;

    @Param(required = true, maxLength = 255, minLength = 1, nonempty = false, nullElements = false, emptyString = true, noTrim = true)
    public java.lang.String filename;

    @Param(required = true, nonempty = false, nullElements = false, emptyString = true, numberRange = {1L,9223372036854775807L}, noTrim = false)
    public java.lang.Long size;

    @Param(required = true, nonempty = false, nullElements = false, emptyString = true, numberRange = {1048576L,67108864L}, noTrim = false)
    public java.lang.Long chunkSize;

    @Param(required = false, validRegexValues = "^[A-Za-z0-9_-]{1,128}$", nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String targetId;

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
        
        org.zstack.sdk.OpenZsDatasetTransferResult value = res.getResult(org.zstack.sdk.OpenZsDatasetTransferResult.class);
        ret.value = value == null ? new org.zstack.sdk.OpenZsDatasetTransferResult() : value; 

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
        info.httpMethod = "POST";
        info.path = "/ai/zsdataset/spaces/{spaceUuid}/transfers";
        info.needSession = true;
        info.needPoll = false;
        info.parameterName = "params";
        return info;
    }

}
