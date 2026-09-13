package org.zstack.observability;

import org.zstack.header.core.execution.APIQueryExecutionMsg;
import org.zstack.header.identity.rbac.RBACDescription;

/** Access policy for the execution observability query API. */
public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .adminOnlyAPIs(APIQueryExecutionMsg.class)
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
