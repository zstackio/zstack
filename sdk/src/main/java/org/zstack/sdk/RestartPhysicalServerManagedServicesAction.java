package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;

public class RestartPhysicalServerManagedServicesAction extends AbstractAction {
    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();
    private static final HashMap<String, Parameter> nonAPIParameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public RestartPhysicalServerManagedServicesResult value;

        public Result throwExceptionIfError() {
            if (error != null) {
                throw new ApiException(String.format(
                        "error[code: %s, description: %s, details: %s, globalErrorCode: %s]",
                        error.code, error.description, error.details, error.globalErrorCode));
            }
            return this;
        }
    }

    @Param(required = true, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public String serverUuid;

    @Param(required = true, maxLength = 64, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public String roleType;

    @Param(required = true, maxLength = 64, nonempty = true, nullElements = false, emptyString = true, noTrim = false)
    public java.util.List serviceNames;

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
        RestartPhysicalServerManagedServicesResult value =
                res.getResult(RestartPhysicalServerManagedServicesResult.class);
        ret.value = value == null
                ? new RestartPhysicalServerManagedServicesResult() : value;
        return ret;
    }

    public Result call() {
        return makeResult(ZSClient.call(this));
    }

    public void call(final Completion<Result> completion) {
        ZSClient.call(this, new InternalCompletion() {
            @Override
            public void complete(ApiResult res) {
                completion.complete(makeResult(res));
            }
        });
    }

    @Override
    protected Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }

    @Override
    protected Map<String, Parameter> getNonAPIParameterMap() {
        return nonAPIParameterMap;
    }

    @Override
    protected RestInfo getRestInfo() {
        RestInfo info = new RestInfo();
        info.httpMethod = "PUT";
        info.path = "/physical-servers/{serverUuid}/managed-services/actions";
        info.needSession = true;
        info.needPoll = true;
        info.parameterName = "restartPhysicalServerManagedServices";
        return info;
    }
}
