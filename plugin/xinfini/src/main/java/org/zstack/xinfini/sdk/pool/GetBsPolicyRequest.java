package org.zstack.xinfini.sdk.pool;

import org.springframework.http.HttpMethod;
import org.zstack.externalStorage.sdk.Param;
import org.zstack.xinfini.XInfiniApiCategory;
import org.zstack.xinfini.sdk.XInfiniRequest;
import org.zstack.xinfini.sdk.XInfiniRestRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:49 2024/5/29
 */
@XInfiniRestRequest(
        path = "/bs-policies/{id}",
        method = HttpMethod.GET,
        responseClass = GetBsPolicyResponse.class,
        category = XInfiniApiCategory.AFA
)
public class GetBsPolicyRequest extends XInfiniRequest {
    @Param
    private int id;

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}