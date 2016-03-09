package org.zstack.header.network.l3;

import org.zstack.header.message.APIEvent;
/**
 *@apiResult
 *
 * api event for :ref:`APIDeleteIpRangeMsg`
 *
 *@category l3Network
 *
 *@since 0.1.0
 *
 *@example
 * {
"org.zstack.header.network.l3.APIDeleteIpRangeEvent": {
"success": true
}
}
 */

public class APIDeleteIpRangeEvent extends APIEvent {
    public APIDeleteIpRangeEvent(String apiId) {
        super(apiId);
    }
    
    public APIDeleteIpRangeEvent() {
        super(null);
    }
}
