package org.zstack.xinfini.sdk.iscsi;

import org.springframework.http.HttpMethod;
import org.zstack.externalStorage.sdk.Param;
import org.zstack.xinfini.XInfiniApiCategory;
import org.zstack.xinfini.sdk.XInfiniRequest;
import org.zstack.xinfini.sdk.XInfiniRestRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ Author : yh.w
 * @ Date   : Created in 17:36 2024/5/27
 */
@XInfiniRestRequest(
    path = "/bs-volumes/{id}/:add-client-group-mappings",
    method = HttpMethod.POST,
    responseClass = AddVolumeClientGroupMappingResponse.class,
    category = XInfiniApiCategory.AFA
)
public class AddVolumeClientGroupMappingRequest extends XInfiniRequest {
    @Param
    private int id;

    @Param
    private List<Integer> iscsiClientGroupIds;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<Integer> getIscsiClientGroupIds() {
        return iscsiClientGroupIds;
    }

    public void setIscsiClientGroupIds(List<Integer> iscsiClientGroupIds) {
        this.iscsiClientGroupIds = iscsiClientGroupIds;
    }

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }

}
