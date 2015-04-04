package org.zstack.network.securitygroup;

import org.zstack.header.core.Completion;
import org.zstack.header.host.HypervisorType;

public interface SecurityGroupHypervisorBackend {
    void applyRules(HostRuleTO hto, Completion complete);

    void cleanUpUnusedRuleOnHost(String hostUuid, Completion completion);
    
    HypervisorType getSecurityGroupBackendHypervisorType();
}
