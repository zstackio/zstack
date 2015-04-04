package org.zstack.header.storage.backup;

import org.zstack.header.message.APIEvent;
/**
 *@apiResult
 * api event for message :ref:`APIDeleteBackupStorageMsg`
 *
 *@since 0.1.0
 *
 *@example
 * {
"org.zstack.header.storage.backup.APIDeleteBackupStorageEvent": {
"success": true
}
}
 */
public class APIDeleteBackupStorageEvent extends APIEvent {

	public APIDeleteBackupStorageEvent(String apiId) {
	    super(apiId);
    }
	
	public APIDeleteBackupStorageEvent() {
		super(null);
	}

}
