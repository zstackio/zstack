package org.zstack.query;

import org.zstack.header.identity.rbac.RBACDescription;
import org.zstack.search.APIRefreshSearchIndexesMsg;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .name("query")
                .normalAPIs(APIBatchQueryMsg.class, APIZQLQueryMsg.class, APIRefreshSearchIndexesMsg.class)
                .build();
    }

    @Override
    public void contributeToRoles() {
        roleContributorBuilder()
                .roleName("other")
                .actions(APIBatchQueryMsg.class, APIZQLQueryMsg.class, APIRefreshSearchIndexesMsg.class)
                .build();
    }

    @Override
    public void roles() {

    }

    @Override
    public void globalReadableResources() {

    }
}
