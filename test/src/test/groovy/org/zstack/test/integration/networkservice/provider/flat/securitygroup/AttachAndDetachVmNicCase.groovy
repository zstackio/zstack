package org.zstack.test.integration.networkservice.provider.flat.securitygroup

import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmSystemTags
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMSecurityGroupBackend
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO
import org.zstack.network.securitygroup.VmNicSecurityGroupRefVO
import org.zstack.network.securitygroup.VmNicSecurityGroupRefVO_
import org.zstack.network.securitygroup.VmNicSecurityPolicyVO
import org.zstack.network.securitygroup.VmNicSecurityPolicyVO_
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

class AttachAndDetachVmNicCase extends SubCase {
    EnvSpec env

    L3NetworkInventory l3Net
    VmInstanceInventory vm1, vm2, vm3, vm4
    SecurityGroupInventory sg1, sg2, sg3, sg4

    void testAttachVmNicToSecurityGroup() {
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

        VmNicSecurityPolicyVO policy = Q.New(VmNicSecurityPolicyVO.class).eq(VmNicSecurityPolicyVO_.vmNicUuid, vm1.vmNics[0].uuid).find()
        assert policy != null
        assert policy.ingressPolicy == "DENY"
        assert policy.egressPolicy == "ALLOW"

        List<VmNicSecurityGroupRefVO> refs = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, vm1.vmNics[0].uuid).list()
        
        assert refs.size() == 3
        refs.stream().forEach({
            if (it.securityGroupUuid.equals(sg1.uuid)) {
                assert it.priority == 1
            }
            if (it.securityGroupUuid.equals(sg2.uuid)) {
                assert it.priority == 2
            }
            if (it.securityGroupUuid.equals(sg3.uuid)) {
                assert it.priority == 3
            }
        })

        deleteVmNicFromSecurityGroup {
            securityGroupUuid = sg2.uuid
            vmNicUuids = [vm1.vmNics[0].uuid]
        }

        refs = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, vm1.vmNics[0].uuid).list()
        assert refs.size() == 2
        refs.stream().forEach({
            if (it.securityGroupUuid.equals(sg1.uuid)) {
                assert it.priority == 1
            }
            if (it.securityGroupUuid.equals(sg3.uuid)) {
                assert it.priority == 2
            }
        })

        addVmNicToSecurityGroup {
            securityGroupUuid = sg2.uuid
            vmNicUuids = [vm1.vmNics[0].uuid]
        }

        refs = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, vm1.vmNics[0].uuid).list()
        assert refs.size() == 3
        refs.stream().forEach({
            if (it.securityGroupUuid.equals(sg1.uuid)) {
                assert it.priority == 1
            }
            if (it.securityGroupUuid.equals(sg3.uuid)) {
                assert it.priority == 2
            }
            if (it.securityGroupUuid.equals(sg2.uuid)) {
                assert it.priority == 3
            }
        })
    }

    void testDetachVmNicFromSecurityGroup() {
        addVmNicToSecurityGroup {
            securityGroupUuid = sg1.uuid
            vmNicUuids = [vm2.vmNics[0].uuid]
        }

        VmNicSecurityPolicyVO policy = Q.New(VmNicSecurityPolicyVO.class).eq(VmNicSecurityPolicyVO_.vmNicUuid, vm2.vmNics[0].uuid).find()
        assert policy != null
        assert policy.ingressPolicy == "DENY"
        assert policy.egressPolicy == "ALLOW"

        List<VmNicSecurityGroupRefVO> refs = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, vm2.vmNics[0].uuid).list()
        assert refs.find {it.securityGroupUuid == sg1.uuid && it.priority == 1}

        addVmNicToSecurityGroup {
            securityGroupUuid = sg2.uuid
            vmNicUuids = [vm2.vmNics[0].uuid]
        }
        addVmNicToSecurityGroup {
            securityGroupUuid = sg3.uuid
            vmNicUuids = [vm2.vmNics[0].uuid]
        }

        refs = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, vm2.vmNics[0].uuid).list()
        assert refs.find {it.securityGroupUuid == sg1.uuid && it.priority == 1}
        assert refs.find {it.securityGroupUuid == sg2.uuid && it.priority == 2}
        assert refs.find {it.securityGroupUuid == sg3.uuid && it.priority == 3}

        deleteVmNicFromSecurityGroup {
            securityGroupUuid = sg1.uuid
            vmNicUuids = [vm2.vmNics[0].uuid]
        }

        refs = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, vm2.vmNics[0].uuid).list()
        assert refs.find {it.securityGroupUuid == sg2.uuid && it.priority == 1}
        assert refs.find {it.securityGroupUuid == sg3.uuid && it.priority == 2}

        addVmNicToSecurityGroup {
            securityGroupUuid = sg1.uuid
            vmNicUuids = [vm2.vmNics[0].uuid]
        }

        refs = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, vm2.vmNics[0].uuid).list()
        assert refs.find {it.securityGroupUuid == sg2.uuid && it.priority == 1}
        assert refs.find {it.securityGroupUuid == sg3.uuid && it.priority == 2}
        assert refs.find {it.securityGroupUuid == sg1.uuid && it.priority == 3}

        deleteVmNicFromSecurityGroup {
            securityGroupUuid = sg2.uuid
            vmNicUuids = [vm2.vmNics[0].uuid]
        }
        deleteVmNicFromSecurityGroup {
            securityGroupUuid = sg3.uuid
            vmNicUuids = [vm2.vmNics[0].uuid]
        }

        refs = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, vm2.vmNics[0].uuid).list()
        assert refs.find {it.securityGroupUuid == sg1.uuid && it.priority == 1}
    }

    void testDeleteL3FromSecurityGroup() {
        attachSecurityGroupToL3Network {
            securityGroupUuid = sg4.uuid
            l3NetworkUuid = l3Net.uuid
        }
        addVmNicToSecurityGroup {
            securityGroupUuid = sg4.uuid
            vmNicUuids = [vm1.vmNics[0].uuid, vm2.vmNics[0].uuid, vm3.vmNics[0].uuid]
        }
        List<VmNicSecurityGroupRefVO> refs = Q.New(VmNicSecurityGroupRefVO.class).in(VmNicSecurityGroupRefVO_.vmNicUuid, [vm1.vmNics[0].uuid, vm2.vmNics[0].uuid, vm3.vmNics[0].uuid]).list()
        assert refs.find {it.securityGroupUuid == sg4.uuid && it.vmNicUuid == vm1.vmNics[0].uuid}
        assert refs.find {it.securityGroupUuid == sg4.uuid && it.vmNicUuid == vm2.vmNics[0].uuid}
        assert refs.find {it.securityGroupUuid == sg4.uuid && it.vmNicUuid == vm3.vmNics[0].uuid}

        detachSecurityGroupFromL3Network {
            securityGroupUuid = sg4.uuid
            l3NetworkUuid = l3Net.uuid
        }

        refs = Q.New(VmNicSecurityGroupRefVO.class).in(VmNicSecurityGroupRefVO_.vmNicUuid, [vm1.vmNics[0].uuid, vm2.vmNics[0].uuid, vm3.vmNics[0].uuid]).list()
        assert refs.find {it.securityGroupUuid == sg4.uuid && it.vmNicUuid == vm1.vmNics[0].uuid} == null
        assert refs.find {it.securityGroupUuid == sg4.uuid && it.vmNicUuid == vm2.vmNics[0].uuid} == null
        assert refs.find {it.securityGroupUuid == sg4.uuid && it.vmNicUuid == vm3.vmNics[0].uuid} == null
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
            sg4 = createSecurityGroup {
                name = "sg-4"
                ipVersion = 4
            } as SecurityGroupInventory
        }

        testAttachVmNicToSecurityGroup()
        testDetachVmNicFromSecurityGroup()
        testDeleteL3FromSecurityGroup()
    }
}
