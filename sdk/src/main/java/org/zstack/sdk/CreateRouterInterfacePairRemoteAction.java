package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;

public class CreateRouterInterfacePairRemoteAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public DeleteConnectionAccessPointLocalResult value;

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
    public java.lang.String dataCenterUuid;

    @Param(required = true, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String accessPointUuid;

    @Param(required = true, validValues = {"Small.1","Small.2","Small.5","Middle.1","Middle.2","Middle.5","Large.1","Large.2"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String Spec;

    @Param(required = true, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String vRouterUuid;

    @Param(required = true, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String vBorderRouterUuid;

    @Param(required = false, maxLength = 128, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String aDescription;

    @Param(required = false, maxLength = 64, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String aName;

    @Param(required = false, maxLength = 128, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String bDescription;

    @Param(required = false, maxLength = 64, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String bName;

    @Param(required = true, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String ownerName;

    @Param(required = false)
    public java.lang.String resourceUuid;

    @Param(required = false)
    public java.util.List systemTags;

    @Param(required = false)
    public java.util.List userTags;

    @Param(required = true)
    public String sessionId;

    public long timeout;
    
    public long pollingInterval;


    private Result makeResult(ApiResult res) {
        Result ret = new Result();
        if (res.error != null) {
            ret.error = res.error;
            return ret;
        }
        
        DeleteConnectionAccessPointLocalResult value = res.getResult(DeleteConnectionAccessPointLocalResult.class);
        ret.value = value == null ? new DeleteConnectionAccessPointLocalResult() : value; 

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
        info.httpMethod = "POST";
        info.path = "/hybrid/aliyun/router-interface";
        info.needSession = true;
        info.needPoll = true;
        info.parameterName = "params";
        return info;
    }

}
