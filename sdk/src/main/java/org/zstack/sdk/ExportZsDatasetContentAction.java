package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;
import org.zstack.sdk.*;

public class ExportZsDatasetContentAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    private static final HashMap<String, Parameter> nonAPIParameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public org.zstack.sdk.ExportZsDatasetContentResult value;

        public Result throwExceptionIfError() {
            if (error != null) {
                throw new ApiException(
                    String.format("error[code: %s, description: %s, details: %s, globalErrorCode: %s]", error.code, error.description, error.details, error.globalErrorCode)    
                );
            }
            
            return this;
        }
    }

    @Param(required = true, validValues = {"QUESTION","SINGLE_TURN","MULTI_TURN","EVAL_QUESTION","IMAGE_VQA"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String contentType;

    @Param(required = true, validValues = {"JSON","JSONL","CSV","TXT","ZIP"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String format;

    @Param(required = false, validValues = {"RAW","ALPACA","SHAREGPT"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String style;

    @Param(required = false, nonempty = true, nullElements = false, emptyString = false, noTrim = false)
    public java.util.List selectedIds;

    @Param(required = false, maxLength = 32, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String projectUuid;

    @Param(required = true, maxLength = 32, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String spaceUuid;

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
        
        org.zstack.sdk.ExportZsDatasetContentResult value = res.getResult(org.zstack.sdk.ExportZsDatasetContentResult.class);
        ret.value = value == null ? new org.zstack.sdk.ExportZsDatasetContentResult() : value; 

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
        info.path = "/ai/zsdataset/spaces/{spaceUuid}/exports";
        info.needSession = true;
        info.needPoll = true;
        info.parameterName = "params";
        return info;
    }

}
