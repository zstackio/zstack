package org.zstack.header.storage.snapshot;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

/**
 */
@AutoQuery(replyClass = APIQueryVolumeSnapshotReply.class, inventoryClass = VolumeSnapshotInventory.class)
@Action(category = VolumeSnapshotConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/volume-snapshots",
        optionalPaths = {"/volume-snapshots/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryVolumeSnapshotReply.class
)
public class APIQueryVolumeSnapshotMsg extends APIQueryMessage {
}
