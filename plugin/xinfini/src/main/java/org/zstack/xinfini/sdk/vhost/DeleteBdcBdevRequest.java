package org.zstack.xinfini.sdk.vhost;

import org.springframework.http.HttpMethod;
import org.zstack.externalStorage.sdk.Param;
import org.zstack.header.xinfini.XInfiniConstants;
import org.zstack.xinfini.XInfiniApiCategory;
import org.zstack.xinfini.sdk.XInfiniRequest;
import org.zstack.xinfini.sdk.XInfiniRestRequest;
import org.zstack.xinfini.sdk.volume.DeleteVolumeResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * @ Author : yh.w
 * @ Date   : Created in 17:36 2024/5/27
 */
@XInfiniRestRequest(
    path = "/bdc-bdevs/{id}",
    method = HttpMethod.DELETE,
    responseClass = DeleteVolumeResponse.class,
    category = XInfiniApiCategory.AFA
)
public class DeleteBdcBdevRequest extends XInfiniRequest {
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
