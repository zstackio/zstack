package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;

/**
 *@apiResult
 *
 * api event for message :ref:`APIDestroyVmInstanceMsg`
 *
 *@since 0.1.0
 *
 *@example
 *
 * {
"org.zstack.header.vm.APIDestroyVmInstanceEvent": {
"success": true
}
}
 *
 */
public class APIDestroyVmInstanceEvent extends APIEvent {

    public APIDestroyVmInstanceEvent(String apiId) {
        super(apiId);
    }
    
    public APIDestroyVmInstanceEvent() {
        super(null);
    }
}
