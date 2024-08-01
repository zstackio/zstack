package org.zstack.core.debug;

import org.zstack.header.core.APIGetChainTaskMsg;
import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "core-debug";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .adminOnlyAPIs(
                        APIDebugSignalMsg.class,
                        APIGetDebugSignalMsg.class,
                        APICleanQueueMsg.class,
                        APIGetChainTaskMsg.class
                )
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();
    }
}
