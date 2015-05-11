package org.zstack.header.storage.snapshot;

import org.zstack.header.message.APIEvent;

/**
 *@apiResult
 *
 * api event for :ref:`APIRevertVolumeFromSnapshotMsg`
 *
 *@category volume snapshot
 *
 *@since 0.1.0
 *
 *@example
 * {
"org.zstack.header.storage.snapshot.APIRevertVolumeFromSnapshotEvent": {
"success": true
}
}
 */
public class APIRevertVolumeFromSnapshotEvent extends APIEvent {
    public APIRevertVolumeFromSnapshotEvent(String apiId) {
        super(apiId);
    }

    public APIRevertVolumeFromSnapshotEvent() {
        super(null);
    }
}
