package org.zstack.xinfini.sdk.iscsi;

import org.springframework.http.HttpMethod;
import org.zstack.xinfini.XInfiniApiCategory;
import org.zstack.xinfini.sdk.XInfiniQueryRequest;
import org.zstack.xinfini.sdk.XInfiniRestRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * @ Author : yh.w
 * @ Date   : Created in 17:36 2024/5/27
 */
@XInfiniRestRequest(
    path = "/iscsi-gateway-client-group-mappings",
    method = HttpMethod.GET,
    responseClass = QueryGatewayClientGroupMappingResponse.class,
    category = XInfiniApiCategory.AFA
)
public class QueryGatewayClientGroupMappingRequest extends XInfiniQueryRequest {
    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }
}
