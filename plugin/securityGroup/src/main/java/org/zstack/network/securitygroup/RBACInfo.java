package org.zstack.network.securitygroup;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "security-group";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .targetResources(SecurityGroupVO.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .build();
    }

    @Override
    public void roles() {
        roleBuilder()
                .uuid("4266a67e46cb4e68864899458187941e")
                .permissionBaseOnThis()
                .build();

        roleContributorBuilder()
                .toOtherRole()
                .actions(
                    APIGetCandidateVmNicForSecurityGroupMsg.class,
                    APIQuerySecurityGroupMsg.class,
                    APIQuerySecurityGroupRuleMsg.class,
                    APIQueryVmNicInSecurityGroupMsg.class,
                    APIQueryVmNicSecurityPolicyMsg.class,
                    APIValidateSecurityGroupRuleMsg.class
                )
                .build();

        roleContributorBuilder()
                .roleName("legacy")
                .actions("org.zstack.network.securitygroup.**")
                .build();
    }
}
