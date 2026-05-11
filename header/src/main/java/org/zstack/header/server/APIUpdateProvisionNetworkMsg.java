package org.zstack.header.server;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@Action(adminOnly = true, category = PhysicalServerConstant.ACTION_CATEGORY)
@RestRequest(path = "/provision-networks/{uuid}/actions", method = HttpMethod.PUT, isAction = true, responseClass = APIUpdateProvisionNetworkEvent.class)
public class APIUpdateProvisionNetworkMsg extends APIMessage {
    @APIParam(resourceType = PhysicalServerProvisionNetworkVO.class, checkAccount = true)
    private String uuid;

    @APIParam(required = false, maxLength = 255)
    private String name;

    @APIParam(required = false, maxLength = 2048)
    private String description;

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

    public String getDhcpInterface() {
        return dhcpInterface;
    }

    public void setDhcpInterface(String dhcpInterface) {
        this.dhcpInterface = dhcpInterface;
    }

    public String getDhcpRangeStartIp() {
        return dhcpRangeStartIp;
    }

    public void setDhcpRangeStartIp(String dhcpRangeStartIp) {
        this.dhcpRangeStartIp = dhcpRangeStartIp;
    }

    public String getDhcpRangeEndIp() {
        return dhcpRangeEndIp;
    }

    public void setDhcpRangeEndIp(String dhcpRangeEndIp) {
        this.dhcpRangeEndIp = dhcpRangeEndIp;
    }

    public String getDhcpRangeNetmask() {
        return dhcpRangeNetmask;
    }

    public void setDhcpRangeNetmask(String dhcpRangeNetmask) {
        this.dhcpRangeNetmask = dhcpRangeNetmask;
    }

    public String getDhcpRangeGateway() {
        return dhcpRangeGateway;
    }

    public void setDhcpRangeGateway(String dhcpRangeGateway) {
        this.dhcpRangeGateway = dhcpRangeGateway;
    }

    public static APIUpdateProvisionNetworkMsg __example__() {
        APIUpdateProvisionNetworkMsg msg = new APIUpdateProvisionNetworkMsg();
        msg.setUuid(uuid());
        msg.setName("provision-net-updated");
        msg.setDhcpInterface("bond0");
        return msg;
    }
}
