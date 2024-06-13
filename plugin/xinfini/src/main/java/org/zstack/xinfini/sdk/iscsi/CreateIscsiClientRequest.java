package org.zstack.xinfini.sdk.iscsi;

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
    path = "/iscsi-clients",
    method = HttpMethod.POST,
    responseClass = CreateIscsiClientResponse.class,
    category = XInfiniApiCategory.AFA
)
public class CreateIscsiClientRequest extends XInfiniRequest {

    @Param
    private Integer iscsiClientGroupId;

    @Param
    private String name;

    @Param
    private String code;

    public Integer getIscsiClientGroupId() {
        return iscsiClientGroupId;
    }

    public void setIscsiClientGroupId(Integer iscsiClientGroupId) {
        this.iscsiClientGroupId = iscsiClientGroupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }

}
