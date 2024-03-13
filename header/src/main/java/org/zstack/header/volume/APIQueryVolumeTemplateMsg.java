package org.zstack.header.volume;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryVolumeTemplateReply.class, inventoryClass = VolumeTemplateInventory.class)
@Action(category = VolumeConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/volumes/volumeTemplate",
        optionalPaths = {"/volumes/volumeTemplate/{uuid}"},
        responseClass = APIQueryVolumeTemplateReply.class,
        method = HttpMethod.GET
)
public class APIQueryVolumeTemplateMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("uuid=" + uuid());
    }
}
