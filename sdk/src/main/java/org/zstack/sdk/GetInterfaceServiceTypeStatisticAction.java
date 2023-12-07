package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;
import org.zstack.sdk.*;

public class GetInterfaceServiceTypeStatisticAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    private static final HashMap<String, Parameter> nonAPIParameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public org.zstack.sdk.GetInterfaceServiceTypeStatisticResult value;

        public Result throwExceptionIfError() {
            if (error != null) {
                throw new ApiException(
                    String.format("error[code: %s, description: %s, details: %s]", error.code, error.description, error.details)
                );
            }
            
            return this;
        }
    }

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String interfaceUuid;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.Integer vlanId;

    @Param(required = false, validValues = {"All","Interface","Bonding"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String interfaceType = "All";

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.util.List serviceType;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String zoneUuid;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String clusterUuid;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String hostUuid;

    @Param(required = false, validValues = {"InterfaceName","VlanId","HostIp","HostName","ClusterName","CreateDate"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String sortBy = "CreateDate";

    @Param(required = false, validValues = {"asc","desc"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String sortDirection = "asc";

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {0L,2147483647L}, noTrim = false)
    public java.lang.Integer start = 0;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {0L,2147483647L}, noTrim = false)
    public java.lang.Integer limit = 20;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public boolean replyWithCount = false;

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
        
        org.zstack.sdk.GetInterfaceServiceTypeStatisticResult value = res.getResult(org.zstack.sdk.GetInterfaceServiceTypeStatisticResult.class);
        ret.value = value == null ? new org.zstack.sdk.GetInterfaceServiceTypeStatisticResult() : value; 

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
        info.path = "/hosts/hosts-network-interfaces/service-type-statistic";
        info.needSession = true;
        info.needPoll = false;
        info.parameterName = "";
        return info;
    }

}
