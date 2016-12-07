package org.zstack.header.storage.snapshot;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

/**
 */
@AutoQuery(replyClass = APIQueryVolumeSnapshotTreeReply.class, inventoryClass = VolumeSnapshotTreeInventory.class)
@Action(category = VolumeSnapshotConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/volume-snapshots/trees",
        optionalPaths = {"/volume-snapshots/trees/{uuid}"},
        responseClass = APIQueryVolumeSnapshotTreeReply.class,
        method = HttpMethod.GET
)
public class APIQueryVolumeSnapshotTreeMsg extends APIQueryMessage {
}
