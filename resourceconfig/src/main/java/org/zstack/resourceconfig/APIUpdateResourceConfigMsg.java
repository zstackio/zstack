package org.zstack.resourceconfig;

import org.springframework.http.HttpMethod;
import org.zstack.core.Platform;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vo.ResourceVO;

@RestRequest(path = "/resource-configurations/{category}/{name}/{resourceUuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateResourceConfigEvent.class)
public class APIUpdateResourceConfigMsg extends APIMessage implements ResourceConfigMessage {
    @APIParam
    private String category;
    @APIParam
    private String name;
    @APIParam(checkAccount = true, resourceType = ResourceVO.class, operationTarget = true)
    private String resourceUuid;
    @APIParam
    private String value;

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    @Override
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public static APIUpdateResourceConfigMsg __example__() {
        APIUpdateResourceConfigMsg msg = new APIUpdateResourceConfigMsg();
        msg.category = "host";
        msg.name = "cpu.overProvisioning.ratio";
        msg.resourceUuid = Platform.getUuid();
        msg.value = "10";
        return msg;
    }
}
