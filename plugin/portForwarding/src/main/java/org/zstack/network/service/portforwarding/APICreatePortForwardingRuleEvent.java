package org.zstack.network.service.portforwarding;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.rest.RestResponse;

/**
 *@apiResult
 *
 * api event for message :ref:`APICreatePortForwardingRuleMsg`
 *
 *@category port forwarding
 *
 *@since 0.1.0
 *
 *@example
 *
 * {
"org.zstack.network.service.portforwarding.APICreatePortForwardingRuleEvent": {
"inventory": {
"uuid": "5ddaefbaba7d46d889aa3f3a6f50f6c8",
"name": "pfRule1",
"vipUuid": "22647f340e1037d4a2ea499aca42075e",
"vipPortStart": 22,
"vipPortEnd": 100,
"privatePortStart": 22,
"privatePortEnd": 100,
"vmNicUuid": "bd00f2c066c94f07b0dfae2e9e9b567f",
"protocolType": "TCP",
"allowedCidr": "77.10.3.1/24"
},
"success": true
}
}
 */
@RestResponse(allTo = "inventory")
public class APICreatePortForwardingRuleEvent extends APIEvent {
    /**
     * @desc see :ref:`PortForwardingRuleInventory`
     */
    private PortForwardingRuleInventory inventory;
    
    public APICreatePortForwardingRuleEvent(String apiId) {
        super(apiId);
    }
    
    public APICreatePortForwardingRuleEvent() {
        super(null);
    }

    public PortForwardingRuleInventory getInventory() {
        return inventory;
    }

    public void setInventory(PortForwardingRuleInventory inventory) {
        this.inventory = inventory;
    }
}
