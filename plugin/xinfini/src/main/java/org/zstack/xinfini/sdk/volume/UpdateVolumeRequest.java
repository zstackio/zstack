package org.zstack.xinfini.sdk.volume;

import org.springframework.http.HttpMethod;
import org.zstack.externalStorage.sdk.Param;
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
    path = "/bs-volumes/{id}",
    method = "PATCH",
    responseClass = UpdateVolumeResponse.class,
    category = XInfiniApiCategory.AFA
)
public class UpdateVolumeRequest extends XInfiniRequest {
    @Param(required = false, queryable = true)
    private String creator;

    @Param
    private int id;

    @Param(required = false)
    private Long sizeMb;

    @Param(required = false)
    private String name;

    @Param(required = false)
    private XinfiniVolumeQos qos;

    public XinfiniVolumeQos getQos() {
        return qos;
    }

    public void setQos(XinfiniVolumeQos qos) {
        this.qos = qos;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public Long getSizeMb() {
        return sizeMb;
    }

    public void setSizeMb(Long sizeMb) {
        this.sizeMb = sizeMb;
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
