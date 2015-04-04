package org.zstack.header.storage.snapshot;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

/**
 */
@AutoQuery(replyClass = APIQueryVolumeSnapshotReply.class, inventoryClass = VolumeSnapshotInventory.class)
public class APIQueryVolumeSnapshotMsg extends APIQueryMessage {
}
