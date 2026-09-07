package org.zstack.network.securitygroup;

import java.util.Collection;
import java.util.Set;

public interface SecurityGroupRuleFilterExtensionPoint {
    Set<String> getInactiveSecurityGroupUuids(Collection<String> securityGroupUuids);
}
