package org.zstack.test.integration.networkservice.provider.flat.securitygroup

import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmSystemTags
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMSecurityGroupBackend
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO
import org.zstack.network.securitygroup.VmNicSecurityGroupRefVO
import org.zstack.network.securitygroup.SecurityGroupRuleVO
import org.zstack.network.securitygroup.SecurityGroupRuleVO_
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.SecurityGroupInventory
import org.zstack.sdk.SecurityGroupRuleInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.header.apimediator.ApiMessageInterceptionException
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.virtualrouter.VirtualRouterNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

class ChangeSecurityGroupRuleCase extends SubCase {
    EnvSpec env

    L3NetworkInventory l3Net
    VmInstanceInventory vm1, vm2, vm3, vm4
    SecurityGroupInventory sg1, sg2, sg3

    void testChangeRulePriority() {
        List<SecurityGroupRuleAO> ingressRules = new ArrayList<>()
        for (int i = 1; i <= 10; i++) {
            SecurityGroupRuleAO r = new SecurityGroupRuleAO()
            r.type = "Ingress"
            r.ipVersion = 4
            r.dstPortRange = "100" + i
            r.srcIpRange = "172.16." + i + ".0/24"
            r.protocol = "TCP"
            ingressRules.add(r)
        }

        sg1 = addSecurityGroupRule {
            securityGroupUuid = sg1.uuid
            rules = ingressRules
        }

        SecurityGroupRuleInventory rule3 = sg1.rules.find { it.priority == 3 }
        SecurityGroupRuleInventory rule4 = sg1.rules.find { it.priority == 4 }
        SecurityGroupRuleInventory rule5 = sg1.rules.find { it.priority == 5 }
        SecurityGroupRuleInventory rule7 = sg1.rules.find { it.priority == 7 }
        SecurityGroupRuleInventory rule8 = sg1.rules.find { it.priority == 8 }
        SecurityGroupRuleInventory rule9 = sg1.rules.find { it.priority == 9 }
        SecurityGroupRuleInventory rule10 = sg1.rules.find { it.priority == 10 }

        changeSecurityGroupRule {
            uuid = rule3.uuid
            priority = 5
        }

        assert Q.New(SecurityGroupRuleVO).eq(SecurityGroupRuleVO_.uuid, rule3.uuid).find().priority == 5
        assert Q.New(SecurityGroupRuleVO).eq(SecurityGroupRuleVO_.uuid, rule4.uuid).find().priority == 3
        assert Q.New(SecurityGroupRuleVO).eq(SecurityGroupRuleVO_.uuid, rule5.uuid).find().priority == 4

        changeSecurityGroupRule {
            uuid = rule3.uuid
            priority = 2
        }

        assert Q.New(SecurityGroupRuleVO).eq(SecurityGroupRuleVO_.uuid, rule3.uuid).find().priority == 2
        assert Q.New(SecurityGroupRuleVO).eq(SecurityGroupRuleVO_.uuid, rule4.uuid).find().priority == 4
        assert Q.New(SecurityGroupRuleVO).eq(SecurityGroupRuleVO_.uuid, rule5.uuid).find().priority == 5

        changeSecurityGroupRule {
            uuid = rule7.uuid
            priority = 8
        }

        assert Q.New(SecurityGroupRuleVO).eq(SecurityGroupRuleVO_.uuid, rule7.uuid).find().priority == 8
        assert Q.New(SecurityGroupRuleVO).eq(SecurityGroupRuleVO_.uuid, rule8.uuid).find().priority == 7

        changeSecurityGroupRule {
            uuid = rule7.uuid
            priority = 7
        }

        assert Q.New(SecurityGroupRuleVO).eq(SecurityGroupRuleVO_.uuid, rule7.uuid).find().priority == 7
        assert Q.New(SecurityGroupRuleVO).eq(SecurityGroupRuleVO_.uuid, rule8.uuid).find().priority == 8

        changeSecurityGroupRule {
            uuid = rule9.uuid
            priority = -1
        }

        assert Q.New(SecurityGroupRuleVO).eq(SecurityGroupRuleVO_.uuid, rule9.uuid).find().priority == 10
        assert Q.New(SecurityGroupRuleVO).eq(SecurityGroupRuleVO_.uuid, rule10.uuid).find().priority == 9
    }

    void testChangeRuleIpRange() {
        SecurityGroupRuleAO r = new SecurityGroupRuleAO()
        r.type = "Egress"
        r.description = "testChangeRuleIpRange"
        r.ipVersion = 4
        r.dstPortRange = "100-200"
        r.dstIpRange = "1.1.1.0/24"
        r.protocol = "TCP"

        sg2 = addSecurityGroupRule {
            securityGroupUuid = sg2.uuid
            rules = [r]
        }

        SecurityGroupRuleInventory rule = sg2.rules.find { it.description == "testChangeRuleIpRange" }

        changeSecurityGroupRule {
            uuid = rule.uuid
            dstIpRange = "1.1.1.1-1.1.1.10,2.2.2.2,3.3.3.0/24"
        }

        assert Q.New(SecurityGroupRuleVO).eq(SecurityGroupRuleVO_.uuid, rule.uuid).find().dstIpRange == "1.1.1.1-1.1.1.10,2.2.2.2,3.3.3.0/24"
    }

    void testChangeRuleRemoteGroup() {
        SecurityGroupRuleAO r = new SecurityGroupRuleAO()
        r.type = "Egress"
        r.description = "testChangeRuleRemoteGroup"
        r.ipVersion = 4
        r.dstPortRange = "100-200"
        r.dstIpRange = "1.1.1.0/24"
        r.protocol = "TCP"

        sg2 = addSecurityGroupRule {
            securityGroupUuid = sg2.uuid
            rules = [r]
        }

        SecurityGroupRuleInventory rule = sg2.rules.find { it.description == "testChangeRuleRemoteGroup" }

        changeSecurityGroupRule {
            uuid = rule.uuid
            remoteSecurityGroupUuid = sg3.uuid
        }

        assert Q.New(SecurityGroupRuleVO).eq(SecurityGroupRuleVO_.uuid, rule.uuid).find().remoteSecurityGroupUuid == sg3.uuid
        assert Q.New(SecurityGroupRuleVO).eq(SecurityGroupRuleVO_.uuid, rule.uuid).find().dstIpRange == null
    }

    void testChangeDefaultRule() {
        List<SecurityGroupRuleAO> ingressRules = new ArrayList<>()
        for (int i = 1; i <= 5; i++) {
            SecurityGroupRuleAO r = new SecurityGroupRuleAO()
            r.type = "Ingress"
            r.ipVersion = 4
            r.dstPortRange = "100" + i
            r.srcIpRange = "172.17." + i + ".0/24"
            r.protocol = "TCP"
            ingressRules.add(r)
        }

        sg3 = addSecurityGroupRule {
            securityGroupUuid = sg3.uuid
            rules = ingressRules
        }

        SecurityGroupRuleInventory default_rule = sg3.rules.find { it.type == "Ingress" && it.priority == 0 && it.ipVersion == 4 }
        assert default_rule != null

        expect(AssertionError) {
            changeSecurityGroupRule {
                uuid = default_rule.uuid
                priority = 2
            }
        }

        expect(AssertionError) {
            changeSecurityGroupRule {
                uuid = default_rule.uuid
                srcIpRange = "1.1.1.0/24"
            }
        }

        expect(AssertionError) {
            changeSecurityGroupRule {
                uuid = default_rule.uuid
                dstPortRange = "1-100"
            }
        }

        expect(AssertionError) {
            changeSecurityGroupRule {
                uuid = default_rule.uuid
                remoteSecurityGroupUuid = sg1.uuid
            }
        }

        expect(AssertionError) {
            changeSecurityGroupRule {
                uuid = default_rule.uuid
                protocol = "UDP"
            }
        }

        expect(AssertionError) {
            changeSecurityGroupRule {
                uuid = default_rule.uuid
                action = "DROP"
            }
        }

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
            vm4 = env.inventoryByName("vm4") as VmInstanceInventory // vm4 in host3
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

        testChangeRulePriority()
        testChangeRuleIpRange()
        testChangeRuleRemoteGroup()
        testChangeDefaultRule()
    }
}
