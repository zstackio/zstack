package org.zstack.header.host;

import org.zstack.header.message.APIEvent;

/**
 *@apiResult
 *
 * api event for message :ref:`APIDeleteHostMsg`
 *
 *@since 0.1.0
 *
 *@example
 *
 * {
"org.zstack.header.host.APIDeleteHostEvent": {
"success": true
}
}
 */
public class APIDeleteHostEvent extends APIEvent {
	public APIDeleteHostEvent() {
		super(null);
	}
	
	public APIDeleteHostEvent(String apiId) {
	    super(apiId);
    }

}
