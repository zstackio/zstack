package org.zstack.network.securitygroup;

import java.util.List;

public interface SecurityGroupManager {
    VmNicSecurityGroupTo getVmNicSecurityGroupRules(List<String> sgUuids);
}
