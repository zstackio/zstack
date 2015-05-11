package org.zstack.header.network.l2;

import org.zstack.header.message.APIEvent;
/**
 *@apiResult
 * api event for message :ref:`APIDeleteL2NetworkMsg`
 *
 *@category l2Network
 *
 *@since 0.1.0
 *
 *@example
 * {
"org.zstack.header.network.l2.APIDeleteL2NetworkEvent": {
"success": true
}
}
 */
public class APIDeleteL2NetworkEvent extends APIEvent {
    public APIDeleteL2NetworkEvent(String apiId) {
        super(apiId);
    }
    
    public APIDeleteL2NetworkEvent() {
        super(null);
    }
}
