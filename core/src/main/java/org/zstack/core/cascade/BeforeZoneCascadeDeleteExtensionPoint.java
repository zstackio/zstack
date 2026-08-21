package org.zstack.core.cascade;

import org.zstack.header.core.Completion;
import org.zstack.header.zone.ZoneInventory;

public interface BeforeZoneCascadeDeleteExtensionPoint {
    void beforeDelete(ZoneInventory inventory, CascadeAction action, Completion completion);

    void cancel(ZoneInventory inventory, CascadeAction action, Completion completion);
}
