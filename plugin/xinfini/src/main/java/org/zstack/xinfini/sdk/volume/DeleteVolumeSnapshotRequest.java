package org.zstack.xinfini.sdk.volume;

import org.springframework.http.HttpMethod;
import org.zstack.externalStorage.sdk.Param;
import org.zstack.header.xinfini.XInfiniConstants;
import org.zstack.xinfini.XInfiniApiCategory;
import org.zstack.xinfini.sdk.XInfiniRequest;
import org.zstack.xinfini.sdk.XInfiniRestRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * @ Author : yh.w
 * @ Date   : Created in 17:36 2024/5/27
 */
@XInfiniRestRequest(
    path = "/bs-snaps/{id}",
    method = HttpMethod.DELETE,
    responseClass = DeleteVolumeResponse.class,
    category = XInfiniApiCategory.AFA
)
public class DeleteVolumeSnapshotRequest extends XInfiniRequest {
    @Param
    private int id;

    @Param
    private String creator = XInfiniConstants.DEFAULT_CREATOR;

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

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
