package org.zstack.header.cluster;

import org.zstack.header.message.APIEvent;

/**
 * @apiResult
 * @example {
 * "org.zstack.header.cluster.APIDeleteClusterEvent": {
 * "success": true
 * }
 * }
 * @since 0.1.0
 */

public class APIDeleteClusterEvent extends APIEvent {

    public APIDeleteClusterEvent() {
        super(null);
    }

    public APIDeleteClusterEvent(String apiId) {
        super(apiId);
    }

}
