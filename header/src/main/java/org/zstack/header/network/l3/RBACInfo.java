package org.zstack.header.network.l3;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "l3";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .targetResources(L3NetworkVO.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();
    }

    @Override
    public void roles() {
        roleBuilder()
                .name("networks")
                .uuid("884b0fcc99b04120807e64466fd63336")
                .permissionBaseOnThis()
                .build();
    }

    @Override
    public void globalReadableResources() {
        globalReadableResourceBuilder()
                .resources(UsedIpVO.class)
                .resources(IpRangeVO.class)
                .resources(AddressPoolVO.class)
                .build();
    }
}
