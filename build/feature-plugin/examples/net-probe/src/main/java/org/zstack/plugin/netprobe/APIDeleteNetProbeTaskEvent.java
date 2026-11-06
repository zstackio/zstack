package org.zstack.plugin.netprobe;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteNetProbeTaskEvent extends APIEvent {
    public APIDeleteNetProbeTaskEvent() {
    }

    public APIDeleteNetProbeTaskEvent(String apiId) {
        super(apiId);
    }
}
