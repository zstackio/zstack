package org.zstack.xinfini.sdk.vhost;

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
    path = "/bdc-bdevs",
    method = HttpMethod.POST,
    responseClass = CreateBdcBdevResponse.class,
    category = XInfiniApiCategory.AFA
)
public class CreateBdcBdevRequest extends XInfiniRequest {
    @Param
    private int bdcId;

    @Param
    private int bsVolumeId;

    @Param
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getBsVolumeId() {
        return bsVolumeId;
    }

    public void setBsVolumeId(int bsVolumeId) {
        this.bsVolumeId = bsVolumeId;
    }

    public int getBdcId() {
        return bdcId;
    }

    public void setBdcId(int bdcId) {
        this.bdcId = bdcId;
    }

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }

}
