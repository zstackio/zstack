package org.zstack.header.storage.primary;

import org.zstack.header.message.APIEvent;

/**
 *@apiResult
 * api event for message :ref:`APIDeletePrimaryStorageMsg`
 *
 *@since 0.1.0
 *
 *@example
 * {
"org.zstack.header.storage.primary.APIDeletePrimaryStorageEvent": {
"success": true
}
}
 *
 */
public class APIDeletePrimaryStorageEvent extends APIEvent {

	public APIDeletePrimaryStorageEvent(String apiId) {
	    super(apiId);
    }
	
	public APIDeletePrimaryStorageEvent() {
		super(null);
	}

}
