package org.zstack.test.integration.network.l3network.ipv6

import org.springframework.http.HttpEntity
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMSecurityGroupBackend
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg
import org.zstack.network.securitygroup.RuleTO
import org.zstack.network.securitygroup.SecurityGroupRuleProtocolType
import org.zstack.network.securitygroup.SecurityGroupRuleTO
import org.zstack.network.securitygroup.SecurityGroupRuleType
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.sdk.*
import org.zstack.test.integration.network.l3network.Env
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.network.IPv6Constants

import java.util.stream.Collectors

import static java.util.Arrays.asList

/**
 * Created by shixin on 2018/09/26.
 */
class IPv6SecurityGroupCase extends SubCase {
    EnvSpec env

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
        env = Env.Ipv6FlatL3Network()
    }

    @Override
    void test() {
        env.create {
            testSecurityGroupValidator()
            testApplySecurityGroup()
            testSecurityGroupApplyNetworkServices()
        }
    }

    void testSecurityGroupValidator() {
        L3NetworkInventory l3_statefull = env.inventoryByName("l3-Statefull-DHCP")
        L3NetworkInventory l3 = env.inventoryByName("l3")

        SecurityGroupInventory sg4 = createSecurityGroup {
            name = "SecurityGroup4"
            ipVersion = 4
        }

        SecurityGroupInventory sg6 = createSecurityGroup {
            name = "SecurityGroup6"
            ipVersion = 6
        }

        APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO rule4 = new APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO()
        rule4.allowedCidr = "192.168.0.1/24"
        rule4.type = SecurityGroupRuleType.Ingress.toString()
        rule4.protocol = SecurityGroupRuleProtocolType.TCP.toString()
        rule4.startPort = 100
        rule4.endPort = 200

        APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO rule6 = new APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO()
        rule6.allowedCidr = "2002::/64"
        rule6.type = SecurityGroupRuleType.Ingress.toString()
        rule6.protocol = SecurityGroupRuleProtocolType.TCP.toString()
        rule6.startPort = 100
        rule6.endPort = 200
        rule6.ipVersion = 6

        expect(AssertionError.class) {
            addSecurityGroupRule {
                delegate.securityGroupUuid = sg4.uuid
                delegate.rules = [rule6]
            }
        }

        expect(AssertionError.class) {
            addSecurityGroupRule {
                delegate.securityGroupUuid = sg6.uuid
                delegate.rules = [rule4]
            }
        }

        addSecurityGroupRule {
            delegate.securityGroupUuid = sg4.uuid
            delegate.rules = [rule4]
        }

        addSecurityGroupRule {
            delegate.securityGroupUuid = sg6.uuid
            delegate.rules = [rule6]
        }

        expect(AssertionError.class) {
            attachSecurityGroupToL3Network {
                securityGroupUuid = sg4.uuid
                l3NetworkUuid = l3_statefull.uuid
            }
        }

        expect(AssertionError.class) {
            attachSecurityGroupToL3Network {
                securityGroupUuid = sg6.uuid
                l3NetworkUuid = l3.uuid
            }
        }

        attachSecurityGroupToL3Network {
            securityGroupUuid = sg4.uuid
            l3NetworkUuid = l3.uuid
        }

        attachSecurityGroupToL3Network {
            securityGroupUuid = sg6.uuid
            l3NetworkUuid = l3_statefull.uuid
        }
    }

    void testApplySecurityGroup() {
        L3NetworkInventory l3_statefull = env.inventoryByName("l3-Statefull-DHCP")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")

        VmInstanceInventory vm = createVmInstance {
            name = "vm-eip"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = asList(l3_statefull.uuid)
        }
        VmNicInventory nic = vm.getVmNics()[0]
        attachL3NetworkToVmNic {
            vmNicUuid = nic.uuid
            l3NetworkUuid = l3.uuid
        }

        SecurityGroupInventory sg4 = querySecurityGroup {
            conditions=["name=SecurityGroup4"]
        }[0]
        SecurityGroupInventory sg6 = querySecurityGroup {
            conditions=["name=SecurityGroup6"]
        }[0]

        addVmNicToSecurityGroup {
            securityGroupUuid = sg4.uuid
            vmNicUuids = [nic.uuid]
        }
        addVmNicToSecurityGroup {
            securityGroupUuid = sg6.uuid
            vmNicUuids = [nic.uuid]
        }
    }

    void testSecurityGroupApplyNetworkServices() {
        HostInventory host = env.inventoryByName("kvm")

        VmInstanceInventory vm = queryVmInstance {
            conditions = ["name=vm-eip"]
        }[0]
        VmNicInventory nic = vm.getVmNics()[0]

        KVMAgentCommands.ApplySecurityGroupRuleCmd cmd = null
        env.afterSimulator(KVMSecurityGroupBackend.SECURITY_GROUP_APPLY_RULE_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.ApplySecurityGroupRuleCmd.class)
            return rsp
        }

        rebootVmInstance {
            uuid = vm.uuid
        }
        retryInSecs {
            assert cmd.ruleTOs.size() == 1
            SecurityGroupRuleTO rule = cmd.ruleTOs.get(0)
            assert rule.vmNicUuid == nic.uuid
            assert rule.rules.size() == 2

            assert rule.securityGroupBaseRules.size() == 4
            rule.rules = rule.rules.stream().sorted { u1, u2 -> u1.ipVersion - u2.ipVersion }.collect(Collectors.toList());
            RuleTO rule1 = rule.rules.get(0)
            RuleTO rule2 = rule.rules.get(1)
            assert rule1.ipVersion == IPv6Constants.IPv4
            assert rule1.allowedCidr == "192.168.0.1/24"
            assert rule2.ipVersion == IPv6Constants.IPv6
            assert rule2.allowedCidr == "2002::/64"
        }

        KVMAgentCommands.RefreshAllRulesOnHostCmd rcmd = null
        env.afterSimulator(KVMSecurityGroupBackend.SECURITY_GROUP_REFRESH_RULE_ON_HOST_PATH) { rsp, HttpEntity<String> e ->
            rcmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.RefreshAllRulesOnHostCmd.class)
            return rsp
        }
        reconnectHost {
            uuid = host.uuid
        }
        retryInSecs {
            assert rcmd.ruleTOs.size() == 1
            SecurityGroupRuleTO rule = rcmd.ruleTOs.get(0)
            assert rule.vmNicUuid == nic.uuid

            assert rule.rules.size() == 2
            assert rule.securityGroupBaseRules.size() == 4
            rule.rules = rule.rules.stream().sorted { u1, u2 -> u1.ipVersion - u2.ipVersion }.collect(Collectors.toList());
            RuleTO rule1 = rule.rules.get(0)
            RuleTO rule2 = rule.rules.get(1)
            assert rule1.ipVersion == IPv6Constants.IPv4
            assert rule1.allowedCidr == "192.168.0.1/24"
            assert rule2.ipVersion == IPv6Constants.IPv6
            assert rule2.allowedCidr == "2002::/64"
        }

        SecurityGroupInventory sg4 = querySecurityGroup {
            conditions = ["name=SecurityGroup4"]
        }[0]
        SecurityGroupInventory sg6 = querySecurityGroup {
            conditions = ["name=SecurityGroup6"]
        }[0]

        cmd = null
        deleteVmNicFromSecurityGroup {
            securityGroupUuid = sg4.uuid
            vmNicUuids = [nic.uuid]
        }

        rebootVmInstance {
            uuid = vm.uuid
        }
        retryInSecs {
            assert cmd.ruleTOs.size() == 1
            SecurityGroupRuleTO rule = cmd.ruleTOs.get(0)
            assert rule.vmNicUuid == nic.uuid

            assert rule.rules.size() == 1
            assert rule.securityGroupBaseRules.size() == 2
            RuleTO rule1 = rule.rules.get(0)
            assert rule1.ipVersion == IPv6Constants.IPv6
            assert rule1.allowedCidr == "2002::/64"
        }

        rcmd = null
        reconnectHost {
            uuid = host.uuid
        }
        retryInSecs {
            assert rcmd.ruleTOs.size() == 1
            SecurityGroupRuleTO rule = rcmd.ruleTOs.get(0)
            assert rule.vmNicUuid == nic.uuid

                assert rule.rules.size() == 1
            assert rule.securityGroupBaseRules.size() == 2
            RuleTO rule1 = rule.rules.get(0)
            assert rule1.ipVersion == IPv6Constants.IPv6
            assert rule1.allowedCidr == "2002::/64"
        }
    }

}

