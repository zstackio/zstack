package org.zstack.sdk.iam2.container;

import java.util.HashMap;
import java.util.Map;
import org.zstack.sdk.*;

public class GetIAM2ProjectContainerImagesAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    private static final HashMap<String, Parameter> nonAPIParameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public org.zstack.sdk.iam2.container.GetIAM2ProjectContainerImagesResult value;

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
    public java.lang.String projectId;

    @Param(required = true, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String repositoryId;

    @Param(required = false)
    public java.lang.Integer limit = 1000;

    @Param(required = false)
    public java.lang.Integer start = 0;

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
        
        org.zstack.sdk.iam2.container.GetIAM2ProjectContainerImagesResult value = res.getResult(org.zstack.sdk.iam2.container.GetIAM2ProjectContainerImagesResult.class);
        ret.value = value == null ? new org.zstack.sdk.iam2.container.GetIAM2ProjectContainerImagesResult() : value; 

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
        info.path = "/iam2/project/{projectId}/repository/{repositoryId}/image";
        info.needSession = true;
        info.needPoll = false;
        info.parameterName = "";
        return info;
    }

}
