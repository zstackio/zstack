package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;

public class CreateEcsSecurityGroupRuleRemoteAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public CreateEcsSecurityGroupRuleRemoteResult value;

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
    public java.lang.String groupUuid;

    @Param(required = true, validValues = {"ingress","egress"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String direction;

    @Param(required = true, validValues = {"tcp","udp","icmp","gre","all"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String protocol;

    @Param(required = true, nonempty = false, nullElements = false, emptyString = false, noTrim = false)
    public java.lang.String portRange;

    @Param(required = true, nonempty = false, nullElements = false, emptyString = false, noTrim = false)
    public java.lang.String cidr;

    @Param(required = false, validValues = {"accept","drop"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String policy = "accept";

    @Param(required = false, validValues = {"intranet","internet"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String nictype = "intranet";

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {1L,100L}, noTrim = false)
    public java.lang.Integer priority = 1;

    @Param(required = false, maxLength = 256, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String description;

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
        
        CreateEcsSecurityGroupRuleRemoteResult value = res.getResult(CreateEcsSecurityGroupRuleRemoteResult.class);
        ret.value = value == null ? new CreateEcsSecurityGroupRuleRemoteResult() : value; 

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
        info.path = "/hybrid/aliyun/security-group-rule";
        info.needSession = true;
        info.needPoll = true;
        info.parameterName = "params";
        return info;
    }

}
