package org.zstack.network.service.virtualrouter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by frank on 8/10/2015.
 */
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

    public List<String> getAllRoles(String vrUuid) {
        List<String> roles = new ArrayList<String>();
        if (VirtualRouterSystemTags.VR_DHCP_ROLE.hasTag(vrUuid)) {
            roles.add(VirtualRouterSystemTags.VR_DHCP_ROLE.getTagFormat());
        }
        if (VirtualRouterSystemTags.VR_DNS_ROLE.hasTag(vrUuid)) {
            roles.add(VirtualRouterSystemTags.VR_DNS_ROLE.getTagFormat());
        }
        if (VirtualRouterSystemTags.VR_SNAT_ROLE.hasTag(vrUuid)) {
            roles.add(VirtualRouterSystemTags.VR_SNAT_ROLE.getTagFormat());
        }
        if (VirtualRouterSystemTags.VR_EIP_ROLE.hasTag(vrUuid)) {
            roles.add(VirtualRouterSystemTags.VR_EIP_ROLE.getTagFormat());
        }
        if (VirtualRouterSystemTags.VR_PORT_FORWARDING_ROLE.hasTag(vrUuid)) {
            roles.add(VirtualRouterSystemTags.VR_PORT_FORWARDING_ROLE.getTagFormat());
        }
        if (VirtualRouterSystemTags.VR_LB_ROLE.hasTag(vrUuid)) {
            roles.add(VirtualRouterSystemTags.VR_LB_ROLE.getTagFormat());
        }
        return roles;
    }
}
