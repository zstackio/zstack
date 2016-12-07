package org.zstack.network.securitygroup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 *@apiResult
 * api event for :ref:`APIDeleteSecurityGroupRuleMsg`
 *
 *@category security group
 *
 *@since 0.1.0
 *
 *@example
 * {
"org.zstack.network.securitygroup.APIDeleteSecurityGroupRuleEvent": {
"inventory": {
"uuid": "bb0538d800e54497b50710f449056a9f",
"name": "test",
"state": "Enabled",
"createDate": "May 14, 2014 10:36:48 PM",
"lastOpDate": "May 14, 2014 10:36:48 PM",
"internalId": 1,
"rules": [
{
"uuid": "4248355e4a534a2c8b0e9986ab0d00bc",
"securityGroupUuid": "bb0538d800e54497b50710f449056a9f",
"type": "Ingress",
"startPort": 22,
"endPort": 100,
"protocol": "TCP",
"allowedCidr": "0.0.0.0/0",
"createDate": "May 14, 2014 10:36:48 PM",
"lastOpDate": "May 14, 2014 10:36:48 PM"
}
],
"attachedL3NetworkUuids": [
"c345b97c7fa4400687fcecef64949a83"
]
},
"success": true
}
}
 */
@RestResponse(allTo = "inventory")
public class APIDeleteSecurityGroupRuleEvent extends APIEvent {
    /**
     * @desc see :ref:`SecurityGroupInventory`
     */
    private SecurityGroupInventory inventory;
    
    public APIDeleteSecurityGroupRuleEvent(String apiId) {
        super(apiId);
    }
    
    public APIDeleteSecurityGroupRuleEvent() {
        super(null);
    }

    public SecurityGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(SecurityGroupInventory inventory) {
        this.inventory = inventory;
    }
}
