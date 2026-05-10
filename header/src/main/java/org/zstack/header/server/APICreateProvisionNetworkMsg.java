package org.zstack.header.server;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.zone.ZoneVO;

@Action(adminOnly = true, category = PhysicalServerConstant.ACTION_CATEGORY)
@RestRequest(
    path = "/provision-networks",
    method = HttpMethod.POST,
    parameterName = "params",
    responseClass = APICreateProvisionNetworkEvent.class
)
public class APICreateProvisionNetworkMsg extends APICreateMessage {
    @APIParam(maxLength = 255)
    private String name;

    @APIParam(required = false, maxLength = 2048)
    private String description;

    @APIParam(resourceType = ZoneVO.class)
    private String zoneUuid;

    @APIParam(validValues = {"STANDALONE_PXE", "GATEWAY_PXE"})
    private String type;

    @APIParam(required = false)
    private String dhcpInterface;

    @APIParam(required = false)
    private String dhcpRangeStartIp;

    @APIParam(required = false)
    private String dhcpRangeEndIp;

    @APIParam(required = false)
    private String dhcpRangeNetmask;

    @APIParam(required = false)
    private String dhcpRangeGateway;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getZoneUuid() { return zoneUuid; }
    public void setZoneUuid(String zoneUuid) { this.zoneUuid = zoneUuid; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDhcpInterface() { return dhcpInterface; }
    public void setDhcpInterface(String dhcpInterface) { this.dhcpInterface = dhcpInterface; }

    public String getDhcpRangeStartIp() { return dhcpRangeStartIp; }
    public void setDhcpRangeStartIp(String dhcpRangeStartIp) { this.dhcpRangeStartIp = dhcpRangeStartIp; }

    public String getDhcpRangeEndIp() { return dhcpRangeEndIp; }
    public void setDhcpRangeEndIp(String dhcpRangeEndIp) { this.dhcpRangeEndIp = dhcpRangeEndIp; }

    public String getDhcpRangeNetmask() { return dhcpRangeNetmask; }
    public void setDhcpRangeNetmask(String dhcpRangeNetmask) { this.dhcpRangeNetmask = dhcpRangeNetmask; }

    public String getDhcpRangeGateway() { return dhcpRangeGateway; }
    public void setDhcpRangeGateway(String dhcpRangeGateway) { this.dhcpRangeGateway = dhcpRangeGateway; }

    public static APICreateProvisionNetworkMsg __example__() {
        APICreateProvisionNetworkMsg msg = new APICreateProvisionNetworkMsg();
        msg.setName("provision-net-1");
        msg.setZoneUuid(uuid());
        msg.setType("STANDALONE_PXE");
        msg.setDhcpInterface("eth0");
        msg.setDhcpRangeStartIp("192.168.100.10");
        msg.setDhcpRangeEndIp("192.168.100.200");
        msg.setDhcpRangeNetmask("255.255.255.0");
        msg.setDhcpRangeGateway("192.168.100.1");
        return msg;
    }
}
