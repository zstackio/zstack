package org.zstack.header.server;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@Action(adminOnly = true, category = PhysicalServerConstant.SERVER_POOL_ACTION_CATEGORY)
@RestRequest(path = "/server-pools/{uuid}/actions", method = HttpMethod.PUT, isAction = true, responseClass = APIUpdateServerPoolEvent.class)
public class APIUpdateServerPoolMsg extends APIMessage {
    @APIParam(resourceType = ServerPoolVO.class, checkAccount = true)
    private String uuid;

    @APIParam(required = false, maxLength = 255)
    private String name;

    @APIParam(required = false, maxLength = 2048)
    private String description;

    @APIParam(required = false, maxLength = 2048)
    private String physicalLocation;

    @APIParam(required = false, maxLength = 2048)
    private String networkTopology;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

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

    public String getPhysicalLocation() {
        return physicalLocation;
    }

    public void setPhysicalLocation(String physicalLocation) {
        this.physicalLocation = physicalLocation;
    }

    public String getNetworkTopology() {
        return networkTopology;
    }

    public void setNetworkTopology(String networkTopology) {
        this.networkTopology = networkTopology;
    }

    public static APIUpdateServerPoolMsg __example__() {
        APIUpdateServerPoolMsg msg = new APIUpdateServerPoolMsg();
        msg.setUuid(uuid());
        msg.setName("pool-updated");
        return msg;
    }
}
