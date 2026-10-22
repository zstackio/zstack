package org.zstack.network.securitygroup;

import org.zstack.header.identity.rbac.RBACDescription;
import org.zstack.header.message.APIMessage;
import org.zstack.header.vm.VmNicVO;

import java.util.Arrays;
import java.util.List;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        List<Class> bindingAPIs = Arrays.asList(APIAddVmNicToSecurityGroupMsg.class, APIDeleteVmNicFromSecurityGroupMsg.class, APISetVmNicSecurityGroupMsg.class);
        Class[] managementAPIs = APIMessage.apiMessageClasses.stream()
                .filter(api -> api.getPackage().getName().equals(SecurityGroupConstant.class.getPackage().getName()))
                .filter(api -> !bindingAPIs.contains(api)).toArray(Class[]::new);
        permissionBuilder()
                .name("security-group")
                .normalAPIs(managementAPIs)
                .targetResources(SecurityGroupVO.class)
                .build();
        permissionBuilder()
                .name("security-group-nic-binding")
                .normalAPIs(bindingAPIs.toArray(new Class[0]))
                .targetResources(VmNicVO.class)
                .build();
    }

    @Override
    public void contributeToRoles() {

    }

    @Override
    public void roles() {
        roleBuilder()
                .name("security-group")
                .uuid("4266a67e46cb4e68864899458187941e")
                .permissionsByName("security-group", "security-group-nic-binding")
                .build();
    }

    @Override
    public void globalReadableResources() {

    }
}
