package org.zstack.network.securitygroup;

import org.zstack.header.message.APIEvent;
/**
 *@apiResult
 *
 * api event for :ref:`APIDeleteSecurityGroupMsg`
 *
 *@category security group
 *
 *@since 0.1.0
 *
 *@example
 * {
"org.zstack.network.securitygroup.APIDeleteSecurityGroupEvent": {
"success": true
}
}
 */
public class APIDeleteSecurityGroupEvent extends APIEvent {
    public APIDeleteSecurityGroupEvent() {
        super(null);
    }
    
    public APIDeleteSecurityGroupEvent(String apiId) {
        super(apiId);
    }
}
