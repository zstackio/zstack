package org.zstack.header.volume;

import org.zstack.header.message.APIEvent;

/**
 * @apiResult api event for message :ref:`APIDeleteDataVolumeMsg`
 * @category volume
 * @example {
 * "org.zstack.header.volume.APIDeleteDataVolumeEvent": {
 * "success": true
 * }
 * }
 * @since 0.1.0
 */
public class APIDeleteDataVolumeEvent extends APIEvent {
    public APIDeleteDataVolumeEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteDataVolumeEvent() {
        super(null);
    }
}
