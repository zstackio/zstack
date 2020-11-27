package org.zstack.search;

import org.zstack.header.identity.rbac.RBACDescription;
import org.zstack.query.APIBatchQueryMsg;
import org.zstack.query.APIZQLQueryMsg;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .normalAPIs(APIRefreshSearchIndexesMsg.class)
                .build();
    }

    @Override
    public void contributeToRoles() {
        roleContributorBuilder()
                .roleName("other")
                .actions(APIRefreshSearchIndexesMsg.class)
                .build();
    }

    @Override
    public void roles() {

    }

    @Override
    public void globalReadableResources() {

    }
}
