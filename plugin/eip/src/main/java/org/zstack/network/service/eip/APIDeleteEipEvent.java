package org.zstack.network.service.eip;

import org.zstack.header.message.APIEvent;

/**
 *@apiResult
 * api event for :ref:`APIDeleteEipMsg`
 *
 *@category eip
 *
 *@since 0.1.0
 *
 *@example
 * {
"org.zstack.network.service.eip.APIDeleteEipEvent": {
"success": true
}
}
 */
public class APIDeleteEipEvent extends APIEvent {
    public APIDeleteEipEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteEipEvent() {
    }
}
