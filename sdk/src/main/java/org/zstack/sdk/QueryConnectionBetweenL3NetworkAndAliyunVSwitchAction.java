package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;

public class QueryConnectionBetweenL3NetworkAndAliyunVSwitchAction extends QueryAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public QueryConnectionBetweenL3NetworkAndAliyunVSwitchResult value;

        public Result throwExceptionIfError() {
            if (error != null) {
                throw new ApiException(
                    String.format("error[code: %s, description: %s, details: %s]", error.code, error.description, error.details)
                );
            }
            
            return this;
        }
    }



    private Result makeResult(ApiResult res) {
        Result ret = new Result();
        if (res.error != null) {
            ret.error = res.error;
            return ret;
        }
        
        QueryConnectionBetweenL3NetworkAndAliyunVSwitchResult value = res.getResult(QueryConnectionBetweenL3NetworkAndAliyunVSwitchResult.class);
        ret.value = value == null ? new QueryConnectionBetweenL3NetworkAndAliyunVSwitchResult() : value; 

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
        info.path = "/hybrid/aliyun/relationships";
        info.needSession = true;
        info.needPoll = false;
        info.parameterName = "";
        return info;
    }

}
