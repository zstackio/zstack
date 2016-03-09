package org.zstack.header.storage.snapshot;

import org.zstack.header.message.APIEvent;

/**
 *@apiResult
 * api event for message :ref:`APIDeleteVolumeSnapshotMsg`
 *
 *@category volume snapshot
 *
 *@since 0.1.0
 *
 *@example
 * {
"org.zstack.header.storage.snapshot.APIDeleteVolumeSnapshotEvent": {
"success": true
}
}
 */
public class APIDeleteVolumeSnapshotEvent extends APIEvent {
    public APIDeleteVolumeSnapshotEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteVolumeSnapshotEvent() {
        super(null);
    }
}
