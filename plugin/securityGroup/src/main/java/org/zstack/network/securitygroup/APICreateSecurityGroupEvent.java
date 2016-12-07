package org.zstack.network.securitygroup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 *@apiResult
 *
 * api event for :ref:`APICreateSecurityGroupMsg`
 *
 *@category security group
 *
 *@since 0.1.0
 *
 *@example
 * {
"org.zstack.network.securitygroup.APICreateSecurityGroupEvent": {
"inventory": {
"uuid": "3904b4837f0c4f539063777ed463b648",
"name": "test",
"state": "Enabled",
"createDate": "May 14, 2014 9:38:24 PM",
"lastOpDate": "May 14, 2014 9:38:24 PM",
"internalId": 1,
"rules": [],
"attachedL3NetworkUuids": []
},
"success": true
}
}
 */
@RestResponse(allTo = "inventory")
public class APICreateSecurityGroupEvent extends APIEvent {
    /**
     * @desc see :ref:`SecurityGroupInventory`
     */
    private SecurityGroupInventory inventory;

    public APICreateSecurityGroupEvent(String apiId) {
        super(apiId);
    }
    
    public APICreateSecurityGroupEvent() {
    }
    
    public SecurityGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(SecurityGroupInventory inventory) {
        this.inventory = inventory;
    }
}
