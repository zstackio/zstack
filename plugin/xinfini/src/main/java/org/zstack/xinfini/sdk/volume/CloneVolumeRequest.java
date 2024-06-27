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
    path = "/bs-volumes/:clone",
    method = HttpMethod.POST,
    responseClass = CreateVolumeSnapshotResponse.class,
    category = XInfiniApiCategory.AFA
)
public class CloneVolumeRequest extends XInfiniRequest {
    @Param
    private String name;

    @Param
    private int bsSnapId;

    @Param
    private String creator = XInfiniConstants.DEFAULT_CREATOR;

    @Param
    private boolean flatten;

    @Param(required = false)
    private String description;

    public int getBsSnapId() {
        return bsSnapId;
    }

    public void setBsSnapId(int bsSnapId) {
        this.bsSnapId = bsSnapId;
    }

    public boolean isFlatten() {
        return flatten;
    }

    public void setFlatten(boolean flatten) {
        this.flatten = flatten;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }

}
