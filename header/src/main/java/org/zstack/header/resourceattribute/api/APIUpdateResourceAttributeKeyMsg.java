package org.zstack.header.resourceattribute.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.resourceattribute.ResourceAttributeMessage;
import org.zstack.header.resourceattribute.entity.ResourceAttributeKeyVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/resource-attributes/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateResourceAttributeKeyEvent.class
)
public class APIUpdateResourceAttributeKeyMsg extends APIMessage implements ResourceAttributeMessage {
    @APIParam(required = true, resourceType = ResourceAttributeKeyVO.class)
    private String uuid;

    @APIParam(required = false)
    private String description;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getKeyUuid() {
        return getUuid();
    }

    public static APIUpdateResourceAttributeKeyMsg __example__() {
        APIUpdateResourceAttributeKeyMsg msg = new APIUpdateResourceAttributeKeyMsg();
        msg.setUuid(uuid(ResourceAttributeKeyVO.class));
        msg.setDescription("test");
        return msg;
    }
}
