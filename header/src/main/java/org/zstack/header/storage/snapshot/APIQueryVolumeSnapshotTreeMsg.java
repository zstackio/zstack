package org.zstack.header.storage.snapshot;

import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

/**
 */
@AutoQuery(replyClass = APIQueryVolumeSnapshotTreeReply.class, inventoryClass = VolumeSnapshotTreeInventory.class)
@Action(category = VolumeSnapshotConstant.ACTION_CATEGORY, names = {"read"})
public class APIQueryVolumeSnapshotTreeMsg extends APIQueryMessage {
}
