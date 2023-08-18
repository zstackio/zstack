package org.zstack.test.integration.networkservice.provider.flat.securitygroup

import org.springframework.http.HttpEntity
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.compute.vm.VmSystemTags
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMSecurityGroupBackend
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO
import org.zstack.network.securitygroup.APISetVmNicSecurityGroupMsg.VmNicSecurityGroupRefAO
import org.zstack.network.securitygroup.VmNicSecurityGroupRefVO
import org.zstack.network.securitygroup.VmNicSecurityGroupRefVO_
import org.zstack.network.securitygroup.VmNicSecurityPolicyVO
import org.zstack.network.securitygroup.VmNicSecurityPolicyVO_
import org.zstack.network.securitygroup.SecurityGroupGlobalProperty
import org.zstack.network.securitygroup.SecurityGroupRuleVO
import org.zstack.network.securitygroup.SecurityGroupRuleVO_
import org.zstack.network.securitygroup.SecurityGroupVO
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.securitygroup.SecurityGroupRuleType
import org.zstack.network.securitygroup.SecurityGroupUpgradeExtension
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.SecurityGroupInventory
import org.zstack.sdk.SecurityGroupRuleInventory
import org.zstack.sdk.VmNicSecurityGroupRefInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicSecurityPolicyInventory
import org.zstack.header.apimediator.ApiMessageInterceptionException
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.virtualrouter.VirtualRouterNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

class SecurityGroupUpgradeDbCase extends SubCase {
    EnvSpec env

    DatabaseFacade dbf
    SecurityGroupUpgradeExtension upgradeExt
    L3NetworkInventory l3Net
    VmInstanceInventory vm1, vm2, vm3
    SecurityGroupInventory sg1, sg2, sg3

    void testUpgradeSecurityGroupRules() {
        sg1 = createSecurityGroup {
            name = "sg-1"
            ipVersion = 4
        } as SecurityGroupInventory

        sg2 = createSecurityGroup {
            name = "sg-2"
            ipVersion = 6
        } as SecurityGroupInventory
        attachSecurityGroupToL3Network {
            securityGroupUuid = sg1.uuid
            l3NetworkUuid = l3Net.uuid
        }
        attachSecurityGroupToL3Network {
            securityGroupUuid = sg2.uuid
            l3NetworkUuid = l3Net.uuid
        }

        addVmNicToSecurityGroup {
            securityGroupUuid = sg1.uuid
            vmNicUuids = [vm1.vmNics[0].uuid]
        }
        addVmNicToSecurityGroup {
            securityGroupUuid = sg2.uuid
            vmNicUuids = [vm1.vmNics[0].uuid]
        }

        for (int i = 1; i <= 5; i++) {
            SecurityGroupRuleAO r = new SecurityGroupRuleAO()
            r.type = "Ingress"
            r.ipVersion = 4
            r.protocol = "TCP"
            r.startPort = i
            r.endPort = i

            addSecurityGroupRule {
                securityGroupUuid = sg1.uuid
                rules = [r]
            }
        }

        for (int i = 1; i <= 5; i++) {
            SecurityGroupRuleAO r = new SecurityGroupRuleAO()
            r.type = "Egress"
            r.ipVersion = 4
            r.startPort = i
            r.endPort = i
            r.protocol = "UDP"

            addSecurityGroupRule {
                securityGroupUuid = sg1.uuid
                rules = [r]
            }
        }

        SQL.New(SecurityGroupRuleVO.class)
                .eq(SecurityGroupRuleVO_.securityGroupUuid, sg1.uuid)
                .set(SecurityGroupRuleVO_.priority, SecurityGroupConstant.LOWEST_RULE_PRIORITY)
                .set(SecurityGroupRuleVO_.dstPortRange, null)
                .set(SecurityGroupRuleVO_.description, null)
                .update()
        SQL.New(SecurityGroupRuleVO.class)
                .eq(SecurityGroupRuleVO_.securityGroupUuid, sg2.uuid)
                .set(SecurityGroupRuleVO_.priority, SecurityGroupConstant.LOWEST_RULE_PRIORITY)
                .set(SecurityGroupRuleVO_.dstPortRange, null)
                .set(SecurityGroupRuleVO_.description, null)
                .update()
        SQL.New(VmNicSecurityPolicyVO.class)
                .eq(VmNicSecurityPolicyVO_.vmNicUuid, vm1.vmNics[0].uuid)
                .delete()
        SQL.New(VmNicSecurityGroupRefVO.class)
                .eq(VmNicSecurityGroupRefVO_.vmNicUuid, vm1.vmNics[0].uuid)
                .set(VmNicSecurityGroupRefVO_.priority, SecurityGroupConstant.LOWEST_RULE_PRIORITY)
                .update()
        
        upgradeExt.start()

        List<SecurityGroupRuleVO> ingressRules = Q.New(SecurityGroupRuleVO.class)
                .eq(SecurityGroupRuleVO_.securityGroupUuid, sg1.uuid)
                .eq(SecurityGroupRuleVO_.type, SecurityGroupRuleType.Ingress).list()
        List<SecurityGroupRuleVO> egressRules = Q.New(SecurityGroupRuleVO.class)
                .eq(SecurityGroupRuleVO_.securityGroupUuid, sg1.uuid)
                .eq(SecurityGroupRuleVO_.type, SecurityGroupRuleType.Egress).list()

        assert ingressRules.find {it.priority == 0 && it.ipVersion == 4 && it.description == "default rule"}
        assert ingressRules.find {it.priority == 0 && it.ipVersion == 6 && it.description == "default rule"}
        assert ingressRules.find {it.priority == 1}
        assert ingressRules.find {it.priority == 2}
        assert ingressRules.find {it.priority == 3}
        assert ingressRules.find {it.priority == 4}
        assert ingressRules.find {it.priority == 5}

        assert egressRules.find {it.priority == 0 && it.ipVersion == 4 && it.description == "default rule"}
        assert egressRules.find {it.priority == 0 && it.ipVersion == 6 && it.description == "default rule"}
        assert egressRules.find {it.priority == 1}
        assert egressRules.find {it.priority == 2}
        assert egressRules.find {it.priority == 3}
        assert egressRules.find {it.priority == 4}
        assert egressRules.find {it.priority == 5}

        List<VmNicSecurityGroupRefVO> refs = Q.New(VmNicSecurityGroupRefVO.class)
                .eq(VmNicSecurityGroupRefVO_.vmNicUuid, vm1.vmNics[0].uuid).list()
        assert refs.size() == 2
        assert refs.find {it.priority == 1}
        assert refs.find {it.priority == 2}
        assert Q.New(VmNicSecurityPolicyVO.class).eq(VmNicSecurityPolicyVO_.vmNicUuid, vm1.vmNics[0].uuid).isExists()

        deleteSecurityGroup {
            uuid = sg1.uuid
        }
        deleteSecurityGroup {
            uuid = sg2.uuid
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
        dbf = bean(DatabaseFacade.class)
        upgradeExt = bean(SecurityGroupUpgradeExtension.class)
        SecurityGroupGlobalProperty.UPGRADE_SECURITY_GROUP = true

        env.create {
            l3Net = env.inventoryByName("l3") as L3NetworkInventory
            vm1 = env.inventoryByName("vm1") as VmInstanceInventory // vm1 in host1
            vm2 = env.inventoryByName("vm2") as VmInstanceInventory // vm2 in host2
            vm3 = env.inventoryByName("vm3") as VmInstanceInventory // vm3 in host3

            sg3 = createSecurityGroup {
                name = "sg-3"
                ipVersion = 4
            } as SecurityGroupInventory
        }

        testUpgradeSecurityGroupRules()

        SecurityGroupGlobalProperty.UPGRADE_SECURITY_GROUP = false
    }
}
