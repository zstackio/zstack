package org.zstack.header.volume;

import java.util.List;

/**
 * Created by MaJin on 2019/5/30.
 */
public interface VolumeAttachedJudger {
    Boolean isAttached(VolumeInventory inv);

    List<String> getAttachedVmUuids(VolumeInventory inv);
}
