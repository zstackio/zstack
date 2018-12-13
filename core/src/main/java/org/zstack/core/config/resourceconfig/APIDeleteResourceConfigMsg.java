package org.zstack.core.config.resourceconfig;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(path = "/resource-configurations/{category}/{name}/{resourceUuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteResourceConfigEvent.class)
public class APIDeleteResourceConfigMsg extends APIDeleteMessage {
    @APIParam
    private String category;
    @APIParam
    private String name;
    @APIParam
    private String resourceUuid;

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

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }
}
