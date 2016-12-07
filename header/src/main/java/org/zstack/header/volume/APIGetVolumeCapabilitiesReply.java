package org.zstack.header.volume;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.Map;

/**
 * Created by xing5 on 2016/5/19.
 */
@RestResponse(allTo = "capabilities")
public class APIGetVolumeCapabilitiesReply extends APIReply {
    private Map<String, Object> capabilities;

    public Map<String, Object> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(Map<String, Object> capabilities) {
        this.capabilities = capabilities;
    }
}
