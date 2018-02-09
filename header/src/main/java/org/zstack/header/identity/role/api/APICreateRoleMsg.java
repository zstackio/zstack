package org.zstack.header.identity.role.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(path = "/identities/roles", method = HttpMethod.POST, responseClass = APICreateRoleEvent.class)
public class APICreateRoleMsg extends APICreateMessage {
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(maxLength = 2048)
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
