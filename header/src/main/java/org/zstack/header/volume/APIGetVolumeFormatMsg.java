package org.zstack.header.volume;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 */
@Action(category = VolumeConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/volumes/formats",
        method = HttpMethod.GET,
        responseClass = APIGetVolumeFormatReply.class
)
public class APIGetVolumeFormatMsg extends APISyncCallMessage {
}
