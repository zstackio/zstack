package org.zstack.network.service.vip;

import org.zstack.header.message.APIEvent;
/**
 *@apiResult
 * api event for message :ref:`APIDeleteVipMsg`
 *
 *@category vip
 *
 *@since 0.1.0
 *
 *@example
 *
 * {
"org.zstack.network.service.vip.APIDeleteVipEvent": {
"success": true
}
}
 *
 */
public class APIDeleteVipEvent extends APIEvent {
    public APIDeleteVipEvent(String apiId) {
        super(apiId);
    }
    
    public APIDeleteVipEvent() {
        super(null);
    }
}
