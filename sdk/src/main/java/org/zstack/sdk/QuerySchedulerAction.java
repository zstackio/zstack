package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;

public class QuerySchedulerAction extends QueryAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public QuerySchedulerResult value;

        public Result throwExceptionIfError() {
            if (error != null) {
                throw new ApiException(
                    String.format("error[code: %s, description: %s, details: %s]", error.code, error.description, error.details)
                );
            }
            
            return this;
        }
    }



    public Result call() {
        ApiResult res = ZSClient.call(this);
        Result ret = new Result();
        if (res.error != null) {
            ret.error = res.error;
            return ret;
        }
        
        QuerySchedulerResult value = res.getResult(QuerySchedulerResult.class);
        ret.value = value == null ? new QuerySchedulerResult() : value;
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
                
                QuerySchedulerResult value = res.getResult(QuerySchedulerResult.class);
                ret.value = value == null ? new QuerySchedulerResult() : value;
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
        info.path = "/schedulers";
        info.needSession = true;
        info.needPoll = false;
        info.parameterName = "";
        return info;
    }

}
