package org.zstack.network.l2;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.BeforeZoneCascadeDeleteExtensionPoint;
import org.zstack.header.core.Completion;
import org.zstack.header.zone.ZoneInventory;

public class L2NetworkZoneCascadeDeleteExtension implements BeforeZoneCascadeDeleteExtensionPoint {
    @Autowired
    private L2NetworkCascadeExtension l2NetworkCascadeExtension;

    @Override
    public void beforeDelete(ZoneInventory inventory, CascadeAction action, Completion completion) {
        l2NetworkCascadeExtension.prepareConfirmedDelete(action, completion);
    }

    @Override
    public void cancel(ZoneInventory inventory, CascadeAction action, Completion completion) {
        l2NetworkCascadeExtension.cancelConfirmedDelete(action, completion);
    }
}
