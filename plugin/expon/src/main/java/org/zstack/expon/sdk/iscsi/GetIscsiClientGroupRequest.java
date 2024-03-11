package org.zstack.expon.sdk.iscsi;

import org.springframework.http.HttpMethod;
import org.zstack.expon.sdk.ExponQueryRequest;
import org.zstack.expon.sdk.ExponRestRequest;

import java.util.HashMap;
import java.util.Map;

@ExponRestRequest(
        path = "/block/iscsi/clients/{id}",
        method = HttpMethod.GET,
        responseClass = GetIscsiClientGroupResponse.class,
        sync = false
)
public class GetIscsiClientGroupRequest extends ExponQueryRequest {
    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    private String id;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }
}
