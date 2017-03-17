package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;

public class CreateIPsecConnectionAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public CreateIPsecConnectionResult value;

        public Result throwExceptionIfError() {
            if (error != null) {
                throw new ApiException(
                    String.format("error[code: %s, description: %s, details: %s]", error.code, error.description, error.details)
                );
            }
            
            return this;
        }
    }

    @Param(required = true, maxLength = 255, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String name;

    @Param(required = false, maxLength = 2048, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String description;

    @Param(required = true, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String l3NetworkUuid;

    @Param(required = true, maxLength = 255, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String peerAddress;

    @Param(required = false, validValues = {"psk","certs"}, maxLength = 255, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String authMode = "psk";

    @Param(required = true, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String authKey;

    @Param(required = true, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String vipUuid;

    @Param(required = true, nonempty = true, nullElements = false, emptyString = true, noTrim = false)
    public java.util.List peerCidrs;

    @Param(required = false, validValues = {"md5","sha1","sha256","sha384","sha512"}, maxLength = 32, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String ikeAuthAlgorithm = "sha1";

    @Param(required = false, validValues = {"3des","aes-128","aes-192","aes-256"}, maxLength = 32, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String ikeEncryptionAlgorithm = "aes-128";

    @Param(required = false)
    public int ikeDhGroup = 2;

    @Param(required = false, validValues = {"md5","sha1","sha256","sha384","sha512"}, maxLength = 32, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String policyAuthAlgorithm = "sha1";

    @Param(required = false, validValues = {"3des","aes-128","aes-192","aes-256"}, maxLength = 32, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String policyEncryptionAlgorithm = "aes-128";

    @Param(required = false, validValues = {"dh-group2","dh-group5","dh-group14","dh-group15","dh-group16","dh-group17","dh-group18","dh-group19","dh-group20","dh-group21","dh-group22","dh-group23","dh-group24","dh-group25","dh-group26"}, maxLength = 32, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String pfs;

    @Param(required = false, validValues = {"tunnel","transport"}, maxLength = 32, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String policyMode = "tunnel";

    @Param(required = false, validValues = {"esp","ah","ah-esp"}, maxLength = 32, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String transformProtocol = "esp";

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
        
        CreateIPsecConnectionResult value = res.getResult(CreateIPsecConnectionResult.class);
        ret.value = value == null ? new CreateIPsecConnectionResult() : value; 

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
        info.path = "/ipsec";
        info.needSession = true;
        info.needPoll = true;
        info.parameterName = "params";
        return info;
    }

}
