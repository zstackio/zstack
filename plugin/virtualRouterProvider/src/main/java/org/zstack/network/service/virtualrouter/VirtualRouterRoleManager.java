package org.zstack.network.service.virtualrouter;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Created by frank on 8/10/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VirtualRouterRoleManager {
    public void makeDhcpRole(String vrUuid) {
        if (!VirtualRouterSystemTags.VR_DHCP_ROLE.hasTag(vrUuid)) {
            VirtualRouterSystemTags.VR_DHCP_ROLE.createInherentTag(vrUuid);
        }
    }

    public void makeDnsRole(String vrUuid) {
        if (!VirtualRouterSystemTags.VR_DNS_ROLE.hasTag(vrUuid)) {
            VirtualRouterSystemTags.VR_DNS_ROLE.createInherentTag(vrUuid);
        }
    }

    public void makeSnatRole(String vrUuid) {
        if (!VirtualRouterSystemTags.VR_SNAT_ROLE.hasTag(vrUuid)) {
            VirtualRouterSystemTags.VR_SNAT_ROLE.createInherentTag(vrUuid);
        }
    }

    public void makeEipRole(String vrUuid) {
        if (!VirtualRouterSystemTags.VR_EIP_ROLE.hasTag(vrUuid)) {
            VirtualRouterSystemTags.VR_EIP_ROLE.createInherentTag(vrUuid);
        }
    }

    public void makePortForwardingRole(String vrUuid) {
        if (!VirtualRouterSystemTags.VR_PORT_FORWARDING_ROLE.hasTag(vrUuid)) {
            VirtualRouterSystemTags.VR_PORT_FORWARDING_ROLE.createInherentTag(vrUuid);
        }
    }

    public void makeLoadBalancerRole(String vrUuid) {
        if (!VirtualRouterSystemTags.VR_LB_ROLE.hasTag(vrUuid)) {
            VirtualRouterSystemTags.VR_LB_ROLE.createInherentTag(vrUuid);
        }
    }
}
