package org.zstack.header.vm;

import org.zstack.header.identity.rbac.RBACDescription;
import org.zstack.header.vm.cdrom.VmCdRomVO;
import org.zstack.header.volume.APICreateDataVolumeMsg;

public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "vm";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .targetResources(VmInstanceVO.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        expandedpermissionBuilder()
                .basicApi(APICreateVmInstanceMsg.class)
                .fieldName("dataDiskOfferingUuids")
                .expandTo(APICreateDataVolumeMsg.class)
                .build();
    }

    @Override
    public void contributeToRoles() {
        resourceEnsembleContributorBuilder()
                .resource(VmNicVO.class)
                .resource(VmCdRomVO.class)
                .contributeTo(VmInstanceVO.class)
                .build();
    }

    @Override
    public void roles() {
        roleBuilder()
                .uuid("5f93cf6444ec44cc83209744c8c3d7cc")
                .permissionBaseOnThis()
                .build();

        roleBuilder()
                .uuid("d6b79564f9b641a4b8bb85ea249151c2")
                .name("vm-operation-without-create-permission")
                .permissionBaseOnThis()
                .excludeActions(APICreateVmInstanceMsg.class)
                .build();
    }
}
