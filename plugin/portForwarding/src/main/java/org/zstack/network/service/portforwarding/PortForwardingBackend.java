package org.zstack.network.service.portforwarding;

import org.zstack.header.core.Completion;
import org.zstack.header.network.service.NetworkServiceProviderType;

import java.util.List;

public interface PortForwardingBackend {
    NetworkServiceProviderType getProviderType();
    
    void applyPortForwardingRule(PortForwardingStruct struct, Completion completion);
    
    void revokePortForwardingRule(PortForwardingStruct struct, Completion completion);

    void removeVirtualRouterPortForwardingRuleRefVO(List<String> ruleUuids, String vrUUid);
    void addVirtualRouterPortForwardingRuleRefVO(List<String> ruleUuids, String vrUUid);

    List<String> getAllPfUuidsOfRouter(String vrUUid);
}
