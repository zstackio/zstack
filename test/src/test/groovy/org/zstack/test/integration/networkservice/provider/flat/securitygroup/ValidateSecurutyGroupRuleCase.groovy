package org.zstack.test.integration.networkservice.provider.flat.securitygroup

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmSystemTags
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMSecurityGroupBackend
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO
import org.zstack.network.securitygroup.APISetVmNicSecurityGroupMsg.VmNicSecurityGroupRefAO
import org.zstack.network.securitygroup.VmNicSecurityGroupRefVO
import org.zstack.network.securitygroup.VmNicSecurityPolicyVO
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.SecurityGroupInventory
import org.zstack.sdk.SecurityGroupRuleInventory
import org.zstack.sdk.VmNicSecurityGroupRefInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.ValidateSecurutyGroupRuleResult
import org.zstack.sdk.VmNicSecurityPolicyInventory
import org.zstack.header.apimediator.ApiMessageInterceptionException
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.virtualrouter.VirtualRouterNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

class ValidateSecurutyGroupRuleCase extends SubCase {
    EnvSpec env

    L3NetworkInventory l3Net
    VmInstanceInventory vm1, vm2, vm3, vm4
    SecurityGroupInventory sg1, sg2, sg3

    void testRuleAvailability() {
        ValidateSecurutyGroupRuleResult reply = validateSecurutyGroupRule {
            securityGroupUuid = sg1.uuid
            type = "Ingress"
            protocol = "TCP"
            srcIpRange = "1.1.1.1-1.1.1.10,2.2.2.0/24"
            dstPortRange = "1-100"
            allowedCidr = "1.1.1.0/24"
        }

        assert reply.available == false

        reply = validateSecurutyGroupRule {
            securityGroupUuid = sg1.uuid
            type = "Ingress"
            protocol = "TCP"
            srcIpRange = "1.1.1.1-1.1.1.10,2.2.2.0/24"
            dstIpRange = "1.1.1.1-1.1.1.10,2.2.2.0/24"
            dstPortRange = "1-100"
        }

        assert reply.available == false

        reply = validateSecurutyGroupRule {
            securityGroupUuid = sg1.uuid
            type = "Egress"
            protocol = "TCP"
            srcIpRange = "1.1.1.1-1.1.1.10,2.2.2.0/24"
            dstPortRange = "1-100"
        }

        assert reply.available == false

        reply = validateSecurutyGroupRule {
            securityGroupUuid = sg1.uuid
            type = "Ingress"
            protocol = "ALL"
            srcIpRange = "1.1.1.1-1.1.1.10,2.2.2.0/24"
            remoteSecurityGroupUuid = sg2.uuid
        }

        assert reply.available == false

        SecurityGroupRuleAO rule1 = new SecurityGroupRuleAO()
        rule1.allowedCidr = "192.168.1.0/24"
        rule1.type = "Ingress"
        rule1.protocol = "TCP"
        rule1.startPort = 200
        rule1.endPort = 300
        sg1 = addSecurityGroupRule {
            securityGroupUuid = sg1.uuid
            rules = [rule1]
        }

        reply = validateSecurutyGroupRule {
            securityGroupUuid = sg1.uuid
            allowedCidr = "192.168.1.0/24"
            type = "Ingress"
            protocol = "TCP"
            startPort = 200
            endPort = 300
        }

        assert reply.available == false
    }


    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = VirtualRouterNetworkServiceEnv.fourVmThreeHostNoEipForSecurityGroupEnv()
    }

    @Override
    void test() {
        env.create {
            l3Net = env.inventoryByName("l3") as L3NetworkInventory
            vm1 = env.inventoryByName("vm1") as VmInstanceInventory // vm1 in host1
            vm2 = env.inventoryByName("vm2") as VmInstanceInventory // vm2 in host2
            vm3 = env.inventoryByName("vm3") as VmInstanceInventory // vm3 in host3
            sg1 = createSecurityGroup {
                name = "sg-1"
                ipVersion = 4
            } as SecurityGroupInventory

            sg2 = createSecurityGroup {
                name = "sg-2"
                ipVersion = 6
            } as SecurityGroupInventory

            sg3 = createSecurityGroup {
                name = "sg-3"
                ipVersion = 4
            } as SecurityGroupInventory
        }

        testRuleAvailability()
    }
}
