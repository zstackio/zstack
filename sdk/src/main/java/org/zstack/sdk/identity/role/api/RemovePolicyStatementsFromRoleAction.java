package org.zstack.sdk.identity.role.api;

import java.util.HashMap;
import java.util.Map;
import org.zstack.sdk.*;

public class RemovePolicyStatementsFromRoleAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    private static final HashMap<String, Parameter> nonAPIParameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public org.zstack.sdk.identity.role.api.RemovePolicyStatementsFromRoleResult value;

        public Result throwExceptionIfError() {
            if (error != null) {
                throw new ApiException(
                    String.format("error[code: %s, description: %s, details: %s]", error.code, error.description, error.details)
                );
            }
            
            return this;
        }
    }

    @Param(required = false)
    public java.lang.String uuid;

    @Param(required = false)
    public java.util.List policyStatementUuids;

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
        
        org.zstack.sdk.identity.role.api.RemovePolicyStatementsFromRoleResult value = res.getResult(org.zstack.sdk.identity.role.api.RemovePolicyStatementsFromRoleResult.class);
        ret.value = value == null ? new org.zstack.sdk.identity.role.api.RemovePolicyStatementsFromRoleResult() : value; 

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
        info.httpMethod = "DELETE";
        info.path = "/identities/roles/{uuid}/policy-statements";
        info.needSession = true;
        info.needPoll = true;
        info.parameterName = "";
        return info;
    }

}
