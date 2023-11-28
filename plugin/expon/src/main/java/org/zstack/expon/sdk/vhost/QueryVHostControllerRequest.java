package org.zstack.expon.sdk.vhost;

import org.springframework.http.HttpMethod;
import org.zstack.expon.sdk.ExponQuery;
import org.zstack.expon.sdk.ExponQueryRequest;
import org.zstack.expon.sdk.ExponRestRequest;
import org.zstack.expon.sdk.volume.VolumeModule;
import org.zstack.expon.sdk.volume.QueryVolumeResponse;

import java.util.HashMap;
import java.util.Map;

@ExponRestRequest(
        path = "/block/vhost",
        method = HttpMethod.GET,
        responseClass = QueryVolumeResponse.class,
        sync = true
)
@ExponQuery(inventoryClass = VolumeModule.class, replyClass = QueryVolumeResponse.class)
public class QueryVHostControllerRequest extends ExponQueryRequest {
    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }
}
