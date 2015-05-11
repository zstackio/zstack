package org.zstack.network.securitygroup;

import org.zstack.header.message.APIEvent;
/**
 *@apiResult
 *
 * api event for :ref:`APIDeleteVmNicFromSecurityGroupMsg`
 *
 *@category security group
 *
 *@since 0.1.0
 *
 *@example
 * {
"org.zstack.network.securitygroup.APIDeleteVmNicFromSecurityGroupEvent": {
"success": true
}
}
 */

public class APIDeleteVmNicFromSecurityGroupEvent extends APIEvent {
    public APIDeleteVmNicFromSecurityGroupEvent() {
        super(null);
    }
    
    public APIDeleteVmNicFromSecurityGroupEvent(String apiId) {
        super(apiId);
    }
}
