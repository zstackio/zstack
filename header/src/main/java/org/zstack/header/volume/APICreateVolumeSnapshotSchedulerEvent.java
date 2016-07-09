package org.zstack.header.volume;

import org.zstack.header.message.APIEvent;

/**
 * Created by root on 7/12/16.
 */
public class APICreateVolumeSnapshotSchedulerEvent extends APIEvent {
    public APICreateVolumeSnapshotSchedulerEvent(String apiId) {
        super(apiId);
    }
    public APICreateVolumeSnapshotSchedulerEvent() {
        super(null);
    }
}
