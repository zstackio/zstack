package org.zstack.header.server;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.zone.ZoneVO;

@Action(adminOnly = true, category = PhysicalServerConstant.SERVER_POOL_ACTION_CATEGORY)
@RestRequest(path = "/server-pools", method = HttpMethod.POST, parameterName = "params", responseClass = APICreateServerPoolEvent.class)
public class APICreateServerPoolMsg extends APICreateMessage {
    @APIParam(maxLength = 255)
    private String name;

    @APIParam(resourceType = ZoneVO.class)
    private String zoneUuid;

    @APIParam(required = false, maxLength = 2048)
    private String description;

    @APIParam(required = false, maxLength = 2048)
    private String physicalLocation;

    @APIParam(required = false, maxLength = 2048)
    private String networkTopology;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
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

    public static APICreateServerPoolMsg __example__() {
        APICreateServerPoolMsg msg = new APICreateServerPoolMsg();
        msg.setName("pool-rack-A1");
        msg.setZoneUuid(uuid());
        msg.setPhysicalLocation("Beijing-DC1-RackA1");
        return msg;
    }
}
