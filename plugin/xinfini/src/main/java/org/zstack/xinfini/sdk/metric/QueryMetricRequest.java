package org.zstack.xinfini.sdk.metric;

import org.springframework.http.HttpMethod;
import org.zstack.xinfini.XInfiniApiCategory;
import org.zstack.xinfini.sdk.XInfiniQueryRequest;
import org.zstack.xinfini.sdk.XInfiniRestRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:49 2024/5/29
 */
@XInfiniRestRequest(
        path = "/samples/query",
        method = HttpMethod.GET,
        responseClass = QueryMetricResponse.class,
        category = XInfiniApiCategory.SDDC
)
public class QueryMetricRequest extends XInfiniQueryRequest {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }
}
