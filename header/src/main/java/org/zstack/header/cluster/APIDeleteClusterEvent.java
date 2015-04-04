package org.zstack.header.cluster;

import org.zstack.header.message.APIEvent;
/**
 *@apiResult
 *
 *@since 0.1.0
 *
 *@example
 * {
"org.zstack.header.cluster.APIDeleteClusterEvent": {
"success": true
}
}
 */

public class APIDeleteClusterEvent extends APIEvent {

	public APIDeleteClusterEvent() {
		super(null);
	}
	
	public APIDeleteClusterEvent(String apiId) {
	    super(apiId);
    }

}
