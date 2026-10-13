package org.zstack.directory;

import org.zstack.header.identity.rbac.RBACDescription;

/**
 * @author shenjin
 * @date 2022/12/7 11:27
 */
public class RBAInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .adminOnlyAPIs("org.zstack.directory.**")
                .normalAPIs(APIQueryDirectoryMsg.class)
                .build();
    }

    @Override
    public void contributeToRoles() {
        roleContributorBuilder()
                .roleName("other")
                .actions(APIQueryDirectoryMsg.class)
                .build();
    }

    @Override
    public void roles() {

    }

    @Override
    public void globalReadableResources() {

    }
}
