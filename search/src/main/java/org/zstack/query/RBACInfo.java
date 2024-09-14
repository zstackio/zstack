package org.zstack.query;

import org.zstack.header.identity.rbac.RBACDescription;
import org.zstack.search.APIRefreshSearchIndexesMsg;

public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "query";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .normalAPIs(APIBatchQueryMsg.class, APIZQLQueryMsg.class, APIRefreshSearchIndexesMsg.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();
    }

    @Override
    public void contributeToRoles() {
        contributeNormalApiToOtherRole();
    }
}
