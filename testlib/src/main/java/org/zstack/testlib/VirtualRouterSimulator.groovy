package org.zstack.testlib

import org.springframework.http.HttpEntity
import org.zstack.appliancevm.ApplianceVmCommands
import org.zstack.appliancevm.ApplianceVmConstant
import org.zstack.appliancevm.ApplianceVmKvmCommands
import org.zstack.network.service.virtualrouter.VirtualRouterCommands
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerBackend
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by xing5 on 2017/2/20.
 */
class VirtualRouterSimulator {
    static {
        Deployer.simulator(ApplianceVmConstant.INIT_PATH) {
            return new ApplianceVmCommands.InitRsp()
        }

        Deployer.simulator(ApplianceVmConstant.REFRESH_FIREWALL_PATH) {
            return new ApplianceVmCommands.RefreshFirewallRsp()
        }

        Deployer.simulator(ApplianceVmKvmCommands.PrepareBootstrapInfoCmd.PATH) {
            return new ApplianceVmKvmCommands.PrepareBootstrapInfoRsp()
        }

        Deployer.simulator(ApplianceVmConstant.ECHO_PATH) {
            return [:]
        }

        Deployer.simulator(VirtualRouterConstant.VR_INIT) {
            return new VirtualRouterCommands.InitRsp()
        }

        Deployer.simulator(VirtualRouterConstant.VR_ADD_DHCP_PATH) {
            return new VirtualRouterCommands.AddDhcpEntryRsp()
        }

        Deployer.simulator(VirtualRouterConstant.VR_REVOKE_PORT_FORWARDING) {
            return new VirtualRouterCommands.RevokePortForwardingRuleRsp()
        }

        Deployer.simulator(VirtualRouterConstant.VR_CREATE_EIP) {
            return new VirtualRouterCommands.CreateEipRsp()
        }

        Deployer.simulator(VirtualRouterConstant.VR_REMOVE_EIP) {
            return new VirtualRouterCommands.RemoveEipRsp()
        }

        Deployer.simulator(VirtualRouterConstant.VR_SYNC_EIP) {
            return new VirtualRouterCommands.SyncEipRsp()
        }

        Deployer.simulator(VirtualRouterConstant.VR_CREATE_VIP) {
            return new VirtualRouterCommands.CreateVipRsp()
        }

        Deployer.simulator(VirtualRouterConstant.VR_REMOVE_VIP) {
            return new VirtualRouterCommands.RemoveVipRsp()
        }

        Deployer.simulator(VirtualRouterConstant.VR_SYNC_PORT_FORWARDING) {
            return new VirtualRouterCommands.SyncPortForwardingRuleRsp()
        }

        Deployer.simulator(VirtualRouterConstant.VR_CREATE_PORT_FORWARDING) {
            return new VirtualRouterCommands.CreatePortForwardingRuleRsp()
        }

        Deployer.simulator(VirtualRouterConstant.VR_ECHO_PATH) {
            return [:]
        }

        Deployer.simulator(VirtualRouterConstant.VR_PING) { HttpEntity<String> e ->
            VirtualRouterCommands.PingCmd cmd = JSONObjectUtil.toObject(e.body, VirtualRouterCommands.PingCmd.class)
            VirtualRouterCommands.PingRsp rsp = new VirtualRouterCommands.PingRsp()
            rsp.uuid = cmd.uuid
            return rsp
        }

        Deployer.simulator(VirtualRouterConstant.VR_SYNC_SNAT_PATH) {
            return new VirtualRouterCommands.SyncSNATRsp()
        }

        Deployer.simulator(VirtualRouterLoadBalancerBackend.REFRESH_LB_PATH) {
            return new VirtualRouterLoadBalancerBackend.RefreshLbRsp()
        }

        Deployer.simulator(VirtualRouterLoadBalancerBackend.DELETE_LB_PATH) {
            return new VirtualRouterLoadBalancerBackend.DeleteLbRsp()
        }

        Deployer.simulator(VirtualRouterConstant.VR_SET_SNAT_PATH) {
            return new VirtualRouterCommands.SetSNATRsp()
        }

        Deployer.simulator(VirtualRouterConstant.VR_REMOVE_DNS_PATH) {
            return new VirtualRouterCommands.RemoveDnsRsp()
        }

        Deployer.simulator(VirtualRouterConstant.VR_SET_DNS_PATH) {
            return new VirtualRouterCommands.SetDnsRsp()
        }

        Deployer.simulator(VirtualRouterConstant.VR_CONFIGURE_NIC_PATH) {
            return new VirtualRouterCommands.ConfigureNicRsp()
        }

        Deployer.simulator(VirtualRouterConstant.VR_REMOVE_DHCP_PATH) {
            return new VirtualRouterCommands.RemoveDhcpEntryRsp()
        }
    }
}
