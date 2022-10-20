package org.zstack.core;

import org.zstack.core.debug.APIDebugSignalMsg;
import org.zstack.core.debug.APIGetDebugSignalMsg;
import org.zstack.header.core.APIGetChainTaskMsg;
import org.zstack.header.identity.rbac.RBACDescription;
import org.zstack.core.debug.APICleanQueueMsg;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .adminOnlyAPIs(APIDebugSignalMsg.class, APIGetDebugSignalMsg.class, APICleanQueueMsg.class,
                        APIGetChainTaskMsg.class)
                .build();
    }

    @Override
    public void contributeToRoles() {

    }

    @Override
    public void roles() {

    }

    @Override
    public void globalReadableResources() {

    }
}
