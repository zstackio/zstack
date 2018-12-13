package org.zstack.core.config.resourceconfig;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(path = "/resource-configurations/{category}/{name}/{resourceUuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateResourceConfigEvent.class)
public class APIUpdateResourceConfigMsg extends APIMessage {
    @APIParam
    private String category;
    @APIParam
    private String name;
    @APIParam
    private String resourceUuid;
    @APIParam
    private String value;

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

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
}
