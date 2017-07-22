package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;

public class CreateVpnIkeConfigAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public CreateVpnIkeConfigResult value;

        public Result throwExceptionIfError() {
            if (error != null) {
                throw new ApiException(
                    String.format("error[code: %s, description: %s, details: %s]", error.code, error.description, error.details)
                );
            }
            
            return this;
        }
    }

    @Param(required = true, maxLength = 64, nonempty = false, nullElements = false, emptyString = false, noTrim = false)
    public java.lang.String name;

    @Param(required = true, maxLength = 32, nonempty = false, nullElements = false, emptyString = false, noTrim = false)
    public java.lang.String psk;

    @Param(required = false, validValues = {"disabled","group1","group2","group5","group14","group24"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String pfs = "group1";

    @Param(required = false, validValues = {"ikev1","ikev2"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String version = "ikev1";

    @Param(required = false, validValues = {"main","aggressive"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String mode = "main";

    @Param(required = false, validValues = {"3des","aes-128","aes-192","aes-256","des"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String encAlg = "3des";

    @Param(required = false, validValues = {"md5","sha1"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String authAlg = "sha1";

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {60L,86400L}, noTrim = false)
    public java.lang.Integer lifetime = 86400;

    @Param(required = true, nonempty = false, nullElements = false, emptyString = false, noTrim = false)
    public java.lang.String localIp;

    @Param(required = true, nonempty = false, nullElements = false, emptyString = false, noTrim = false)
    public java.lang.String remoteIp;

    @Param(required = false, maxLength = 1024, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
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
        
        CreateVpnIkeConfigResult value = res.getResult(CreateVpnIkeConfigResult.class);
        ret.value = value == null ? new CreateVpnIkeConfigResult() : value; 

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
        info.path = "/hybrid/vpn-connection/ike";
        info.needSession = true;
        info.needPoll = true;
        info.parameterName = "params";
        return info;
    }

}
