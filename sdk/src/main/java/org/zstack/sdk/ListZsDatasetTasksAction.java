package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;
import org.zstack.sdk.*;

public class ListZsDatasetTasksAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    private static final HashMap<String, Parameter> nonAPIParameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public org.zstack.sdk.ListZsDatasetTasksResult value;

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

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {1L,200L}, noTrim = false)
    public java.lang.Integer limit;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {0L,2147483647L}, noTrim = false)
    public java.lang.Integer offset;

    @Param(required = false, maxLength = 256, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String keyword;

    @Param(required = false, validValues = {"createAt","startTime"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String sortBy;

    @Param(required = false, validValues = {"asc","desc"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String sortDirection;

    @Param(required = false, validValues = {"DOCUMENT_PROCESSING","TEXT_QUESTION_GENERATION","SINGLE_TURN_ANSWER_GENERATION","SINGLE_TURN_QUALITY_EVALUATION","CHUNK_DATA_CLEANING","EVAL_QUESTION_GENERATION","IMAGE_QUESTION_GENERATION","IMAGE_VQA_GENERATION","MULTI_TURN_CONVERSATION_GENERATION"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String taskType;

    @Param(required = false, validValues = {"PROCESSING","COMPLETED","FAILED","INTERRUPTED"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String status;

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
        
        org.zstack.sdk.ListZsDatasetTasksResult value = res.getResult(org.zstack.sdk.ListZsDatasetTasksResult.class);
        ret.value = value == null ? new org.zstack.sdk.ListZsDatasetTasksResult() : value; 

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
        info.path = "/ai/zsdataset/spaces/{spaceUuid}/tasks";
        info.needSession = true;
        info.needPoll = false;
        info.parameterName = "";
        return info;
    }

}
