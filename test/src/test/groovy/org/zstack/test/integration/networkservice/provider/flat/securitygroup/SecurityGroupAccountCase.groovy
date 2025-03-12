package org.zstack.test.integration.networkservice.provider.flat.securitygroup

import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.SecurityGroupInventory
import org.zstack.sdk.SessionInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.virtualrouter.VirtualRouterNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class SecurityGroupAccountCase extends SubCase {
    EnvSpec env

    L3NetworkInventory l3Net
    VmInstanceInventory vm1, vm2, vm3, vm4
    SecurityGroupInventory sg1, sg2, sg3, sg4
    SessionInventory session1
    AccountInventory user1

    void testDeleteSecurityGroupCase1() {
        addVmNicToSecurityGroup {
            securityGroupUuid = sg1.uuid
            vmNicUuids = [vm1.vmNics[0].uuid]
        }
        addVmNicToSecurityGroup {
            securityGroupUuid = sg2.uuid
            vmNicUuids = [vm1.vmNics[0].uuid]
        }
        addVmNicToSecurityGroup {
            securityGroupUuid = sg3.uuid
            vmNicUuids = [vm1.vmNics[0].uuid]
        }

        addVmNicToSecurityGroup {
            securityGroupUuid = sg4.uuid
            vmNicUuids = [vm4.vmNics[0].uuid]
        }

        SecurityGroupRuleAO r = new SecurityGroupRuleAO()
        r.type = 'Egress'
        r.description = 'sg4-egress-rule-1'
        r.protocol = 'TCP'
        r.dstPortRange = '40-400'
        sg4 = addSecurityGroupRule {
            sessionId = session1.uuid
            securityGroupUuid = sg4.uuid
            remoteSecurityGroupUuids = [sg2.uuid]
            rules = [r]
        }

        r.protocol = 'UDP'
        r.dstPortRange = '50-500'
        r.description = 'sg4-egress-rule-2'
        sg4 = addSecurityGroupRule {
            sessionId = session1.uuid
            securityGroupUuid = sg4.uuid
            rules = [r]
            priority = -1
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

            user1 = createAccount {
                name = "user1"
                password = "password"
            } as AccountInventory

            session1 = logInByAccount {
                accountName = user1.name
                password = "password"
            }
            sg1 = createSecurityGroup {
                sessionId = session1.uuid
                name = "sg-1"
                ipVersion = 4
            } as SecurityGroupInventory

            sg2 = createSecurityGroup {
                sessionId = session1.uuid
                name = "sg-2"
                ipVersion = 6
            } as SecurityGroupInventory

            sg3 = createSecurityGroup {
                sessionId = session1.uuid
                name = "sg-3"
                ipVersion = 4
            } as SecurityGroupInventory

            sg4 = createSecurityGroup {
                sessionId = session1.uuid
                name = "sg-4"
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
            attachSecurityGroupToL3Network {
                securityGroupUuid = sg3.uuid
                l3NetworkUuid = l3Net.uuid
            }
            attachSecurityGroupToL3Network {
                securityGroupUuid = sg4.uuid
                l3NetworkUuid = l3Net.uuid
            }
        }

        testDeleteSecurityGroupCase1()
        deleteAccount {
            uuid = user1.uuid
        }
    }
}
