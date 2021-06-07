package org.zstack.sdk.iam2.api;

import org.zstack.sdk.AbstractAction;
import org.zstack.sdk.ApiException;
import org.zstack.sdk.ApiResult;
import org.zstack.sdk.Completion;
import org.zstack.sdk.ErrorCode;
import org.zstack.sdk.InternalCompletion;
import org.zstack.sdk.NonAPIParam;
import org.zstack.sdk.Param;
import org.zstack.sdk.RestInfo;
import org.zstack.sdk.ZSClient;

import java.util.HashMap;
import java.util.Map;

public class UpdateOrganizationQuotaAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    private static final HashMap<String, Parameter> nonAPIParameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public org.zstack.sdk.iam2.api.UpdateOrganizationQuotaResult value;

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
    public java.lang.String identityUuid;

    @Param(required = true, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String name;

    @Param(required = true, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public long value = 0L;

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
        
        org.zstack.sdk.iam2.api.UpdateOrganizationQuotaResult value = res.getResult(org.zstack.sdk.iam2.api.UpdateOrganizationQuotaResult.class);
        ret.value = value == null ? new org.zstack.sdk.iam2.api.UpdateOrganizationQuotaResult() : value; 

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
        info.httpMethod = "PUT";
        info.path = "/iam2/Organization/quotas/actions";
        info.needSession = true;
        info.needPoll = true;
        info.parameterName = "updateOrganizationQuota";
        return info;
    }

}
