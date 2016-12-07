package org.zstack.header.volume;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

@AutoQuery(replyClass = APIQueryVolumeReply.class, inventoryClass = VolumeInventory.class)
@Action(category = VolumeConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/volumes",
        optionalPaths = {"/volumes/{uuid}"},
        responseClass = APIQueryVolumeReply.class,
        method = HttpMethod.GET
)
public class APIQueryVolumeMsg extends APIQueryMessage {

}
