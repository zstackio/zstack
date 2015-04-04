package org.zstack.header.storage.snapshot;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

/**
 */
@AutoQuery(replyClass = APIQueryVolumeSnapshotTreeReply.class, inventoryClass = VolumeSnapshotTreeInventory.class)
public class APIQueryVolumeSnapshotTreeMsg extends APIQueryMessage {
}
