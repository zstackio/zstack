package org.zstack.header.resourceattribute;

import org.zstack.header.identity.rbac.RBACDescription;
import org.zstack.header.resourceattribute.entity.ResourceAttributeKeyVO;
import org.zstack.header.rest.SDKPackage;

@SDKPackage(packageName="org.zstack.sdk.attribute")
public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "attribute";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .zsvAdvancedAvailable()
                .build();
    }

    @Override
    public void contributeToRoles() {
        contributeNormalApiToOtherRole();
    }

    @Override
    public void globalReadableResources() {
        globalReadableResourceBuilder()
                .resources(ResourceAttributeKeyVO.class)
                .build();
    }
}
