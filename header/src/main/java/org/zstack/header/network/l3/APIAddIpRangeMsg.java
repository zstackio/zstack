package org.zstack.header.network.l3;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * @api add a ip range to l3Network
 * @category l3Network
 * @cli
 * @httpMsg {
 * "org.zstack.header.network.l3.APIAddIpRangeMsg": {
 * "endIp": "192.168.0.90",
 * "description": "Test",
 * "startIp": "192.168.0.10",
 * "l3NetworkUuid": "22c3277f6b7540c8995bee842cf112d4",
 * "netmask": "255.255.255.0",
 * "session": {
 * "uuid": "8ef12a3c3b0e4664bab77ac5053a7df7"
 * },
 * "gateway": "192.168.0.1",
 * "name": "public ip range"
 * }
 * }
 * @msg {
 * "org.zstack.header.network.l3.APIAddIpRangeMsg": {
 * "endIp": "192.168.0.90",
 * "description": "Test",
 * "startIp": "192.168.0.10",
 * "l3NetworkUuid": "22c3277f6b7540c8995bee842cf112d4",
 * "netmask": "255.255.255.0",
 * "session": {
 * "uuid": "8ef12a3c3b0e4664bab77ac5053a7df7"
 * },
 * "gateway": "192.168.0.1",
 * "name": "public ip range",
 * "timeout": 1800000,
 * "id": "cffeac3ad46d4fffa4262ecc8aaaa699",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APIAddIpRangeEvent`
 * @since 0.1.0
 */
@Action(category = L3NetworkConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/l3-networks/{l3NetworkUuid}/ip-ranges",
        method = HttpMethod.POST,
        responseClass = APIAddIpRangeEvent.class,
        parameterName = "params"
)
public class APIAddIpRangeMsg extends APICreateMessage implements L3NetworkMessage {
    /**
     * @desc l3Network uuid
     */
    @APIParam(resourceType = L3NetworkVO.class, checkAccount = true, operationTarget = true)
    private String l3NetworkUuid;
    /**
     * @desc max length of 255 characters
     */
    @APIParam(maxLength = 255)
    private String name;
    /**
     * @desc max length of 2048 characters
     */
    @APIParam(required = false, maxLength = 2048)
    private String description;
    /**
     * @desc start IPv4 address
     */
    @APIParam
    private String startIp;
    /**
     * @desc end IPv4 address
     */
    @APIParam
    private String endIp;
    /**
     * @desc IPv4 network netmask
     */
    @APIParam
    private String netmask;
    /**
     * @desc IPv4 gateway
     */
    @APIParam
    private String gateway;

    @Override
    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
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

    public String getStartIp() {
        return startIp;
    }

    public void setStartIp(String startIp) {
        this.startIp = startIp;
    }

    public String getEndIp() {
        return endIp;
    }

    public void setEndIp(String endIP) {
        this.endIp = endIP;
    }

    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }
}
