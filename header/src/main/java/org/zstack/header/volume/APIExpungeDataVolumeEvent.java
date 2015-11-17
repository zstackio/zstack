package org.zstack.header.volume;

import org.zstack.header.message.APIEvent;

/**
 * Created by frank on 11/16/2015.
 */
public class APIExpungeDataVolumeEvent extends APIEvent {
    public APIExpungeDataVolumeEvent() {
    }

    public APIExpungeDataVolumeEvent(String apiId) {
        super(apiId);
    }
}
