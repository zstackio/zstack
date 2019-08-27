package org.zstack.core;

import org.zstack.core.debug.APIDebugSignalMsg;
import org.zstack.core.debug.APIGetDebugSignalMsg;
import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .adminOnlyAPIs(APIDebugSignalMsg.class, APIGetDebugSignalMsg.class)
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
