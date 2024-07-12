package org.zstack.xinfini.sdk.iscsi;

import org.springframework.http.HttpMethod;
import org.zstack.externalStorage.sdk.Param;
import org.zstack.header.xinfini.XInfiniConstants;
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
    path = "/iscsi-client-groups",
    method = HttpMethod.POST,
    responseClass = CreateIscsiClientGroupResponse.class,
    category = XInfiniApiCategory.AFA
)
public class CreateIscsiClientGroupRequest extends XInfiniRequest {

    @Param
    private List<Integer> iscsiGatewayIds;

    @Param
    private List<String> iscsiClientCodes;

    @Param
    private String name;

    @Param
    private String creator = XInfiniConstants.DEFAULT_CREATOR;

    public List<Integer> getIscsiGatewayIds() {
        return iscsiGatewayIds;
    }

    public void setIscsiGatewayIds(List<Integer> iscsiGatewayIds) {
        this.iscsiGatewayIds = iscsiGatewayIds;
    }

    public List<String> getIscsiClientCodes() {
        return iscsiClientCodes;
    }

    public void setIscsiClientCodes(List<String> iscsiClientCodes) {
        this.iscsiClientCodes = iscsiClientCodes;
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
