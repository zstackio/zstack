package org.zstack.test.integration.networkservice.provider.flat.securitygroup

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmSystemTags
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMSecurityGroupBackend
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO
import org.zstack.network.securitygroup.APISetVmNicSecurityGroupMsg.VmNicSecurityGroupRefAO
import org.zstack.network.securitygroup.VmNicSecurityGroupRefVO
import org.zstack.network.securitygroup.VmNicSecurityGroupRefVO_
import org.zstack.network.securitygroup.VmNicSecurityPolicyVO
import org.zstack.network.securitygroup.SecurityGroupMembersTO
import org.zstack.sdk.AddVmNicToSecurityGroupAction
import org.zstack.sdk.SetVmNicSecurityGroupAction
import org.zstack.sdk.DeleteVmNicFromSecurityGroupAction
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.SessionInventory
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

class SetVmNicSecurityGroupCase extends SubCase {
    EnvSpec env

    L3NetworkInventory l3Net
    VmInstanceInventory vm1, vm2, vm3, vm4
    SecurityGroupInventory sg1, sg2, sg3
    AccountInventory accountA, accountB
    SessionInventory sessionA
    SecurityGroupInventory tenantGroup, hiddenGroupOne, hiddenGroupTwo
    String tenantNicUuid

    void testVmNicAttachSecurityGroup() {
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

        VmNicSecurityGroupRefAO ref1 = new VmNicSecurityGroupRefAO()
        ref1.securityGroupUuid = sg1.uuid
        ref1.priority = 1

        VmNicSecurityGroupRefAO ref2 = new VmNicSecurityGroupRefAO()
        ref2.securityGroupUuid = sg2.uuid
        ref2.priority = 2

        List<VmNicSecurityGroupRefInventory> refInvs = setVmNicSecurityGroup {
            vmNicUuid = vm1.vmNics[0].uuid
            refs = [ref1, ref2]
        }

        String vm1_nic_uuid = vm1.vmNics[0].uuid

        VmNicSecurityPolicyInventory policy = queryVmNicSecurityPolicy {
            conditions = ["vmNicUuid=${vm1_nic_uuid}"]
        }[0]

        assert policy.ingressPolicy == "DENY"
        assert policy.egressPolicy == "ALLOW"
        assert refInvs.size() == 2
        for (VmNicSecurityGroupRefInventory inv : refInvs) {
            if (inv.securityGroupUuid == sg1.uuid) {
                assert inv.priority == 1
            }
            if (inv.securityGroupUuid == sg2.uuid) {
                assert inv.priority == 2
            }
        }

        VmNicSecurityGroupRefAO ref3 = new VmNicSecurityGroupRefAO()
        ref3.securityGroupUuid = sg3.uuid
        ref3.priority = 1

        ref2.priority = 3
        ref1.priority = 2

        refInvs = setVmNicSecurityGroup {
            vmNicUuid = vm1_nic_uuid
            refs = [ref3, ref2, ref1]
        }

        assert refInvs.size() == 3
        for (VmNicSecurityGroupRefInventory inv : refInvs) {
            if (inv.securityGroupUuid == sg1.uuid) {
                assert inv.priority == 2
            }
            if (inv.securityGroupUuid == sg2.uuid) {
                assert inv.priority == 3
            }
            if (inv.securityGroupUuid == sg3.uuid) {
                assert inv.priority == 1
            }
        }

        refInvs = setVmNicSecurityGroup {
            vmNicUuid = vm1_nic_uuid
            refs = []
        }

        assert refInvs.size() == 0
    }

    void testUpdateGroupMembers() {
        String vm1_nic_uuid = vm1.vmNics[0].uuid

        VmNicSecurityGroupRefAO ref1 = new VmNicSecurityGroupRefAO()
        ref1.securityGroupUuid = sg1.uuid
        ref1.priority = 1

        VmNicSecurityGroupRefAO ref2 = new VmNicSecurityGroupRefAO()
        ref2.securityGroupUuid = sg2.uuid
        ref2.priority = 2

        VmNicSecurityGroupRefAO ref3 = new VmNicSecurityGroupRefAO()
        ref3.securityGroupUuid = sg3.uuid
        ref3.priority = 3

        KVMAgentCommands.UpdateGroupMemberCmd ucmd = null
        env.simulator(KVMSecurityGroupBackend.SECURITY_GROUP_UPDATE_GROUP_MEMBER ){ HttpEntity<String> e ->
            ucmd = JSONObjectUtil.toObject(e.getBody(), KVMAgentCommands.UpdateGroupMemberCmd.class)
            return new KVMAgentCommands.ApplySecurityGroupRuleResponse()
        }

        setVmNicSecurityGroup {
            vmNicUuid = vm1_nic_uuid
            refs = [ref3, ref2, ref1]
        }

        retryInSecs {
            assert ucmd != null
        }
    }

    void testSetVmNicSecurityGroupDirect() {
        deleteSecurityGroup {
            uuid = sg1.uuid
        }
        deleteSecurityGroup {
            uuid = sg2.uuid
        }
        deleteSecurityGroup {
            uuid = sg3.uuid
        }

        sg1 = createSecurityGroup {
            name = "sg-1"
            ipVersion = 4
        }

        sg2 = createSecurityGroup {
            name = "sg-2"
            ipVersion = 6
        }

        sg3 = createSecurityGroup {
            name = "sg-3"
            ipVersion = 4
        }

        String vm1_nic_uuid = vm1.vmNics[0].uuid

        VmNicSecurityGroupRefAO ref1 = new VmNicSecurityGroupRefAO()
        ref1.securityGroupUuid = sg1.uuid
        ref1.priority = 1

        VmNicSecurityGroupRefAO ref2 = new VmNicSecurityGroupRefAO()
        ref2.securityGroupUuid = sg2.uuid
        ref2.priority = 2

        VmNicSecurityGroupRefAO ref3 = new VmNicSecurityGroupRefAO()
        ref3.securityGroupUuid = sg3.uuid
        ref3.priority = 3

        KVMAgentCommands.ApplySecurityGroupRuleCmd cmd = null
        env.afterSimulator(KVMSecurityGroupBackend.SECURITY_GROUP_APPLY_RULE_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.ApplySecurityGroupRuleCmd.class)

            return rsp
        }

        setVmNicSecurityGroup {
            vmNicUuid = vm1_nic_uuid
            refs = [ref3, ref2, ref1]
        }

        List<VmNicSecurityGroupRefVO> refAOs = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, vm1_nic_uuid).list()
        assert refAOs.size() == 3
        assert refAOs.find { it.securityGroupUuid == sg1.uuid }.priority == 1
        assert refAOs.find { it.securityGroupUuid == sg2.uuid }.priority == 2
        assert refAOs.find { it.securityGroupUuid == sg3.uuid }.priority == 3

        retryInSecs {
            assert cmd != null
            assert cmd.vmNicTOs.get(0).vmNicUuid == vm1_nic_uuid
            assert cmd.vmNicTOs.get(0).securityGroupRefs.get(sg1.uuid) == 1
            assert cmd.vmNicTOs.get(0).securityGroupRefs.get(sg2.uuid) == 2
            assert cmd.vmNicTOs.get(0).securityGroupRefs.get(sg3.uuid) == 3
        }

        cmd = null

        ref2.priority = 1
        ref3.priority = 2
        setVmNicSecurityGroup {
            vmNicUuid = vm1_nic_uuid
            refs = [ref2, ref3]
        }

        refAOs = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, vm1_nic_uuid).list()
        assert refAOs.size() == 2
        assert refAOs.find { it.securityGroupUuid == sg2.uuid }.priority == 1
        assert refAOs.find { it.securityGroupUuid == sg3.uuid }.priority == 2

        retryInSecs {
            assert cmd != null
            assert cmd.vmNicTOs.get(0).vmNicUuid == vm1_nic_uuid
            assert cmd.vmNicTOs.get(0).securityGroupRefs.get(sg2.uuid) == 1
            assert cmd.vmNicTOs.get(0).securityGroupRefs.get(sg3.uuid) == 2
        }

        cmd = null
        setVmNicSecurityGroup {
            vmNicUuid = vm1_nic_uuid
            refs = []
        }

        refAOs = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, vm1_nic_uuid).list()
        assert refAOs.size() == 0

        retryInSecs {
            assert cmd != null
            assert cmd.vmNicTOs.get(0).vmNicUuid == vm1_nic_uuid
            assert cmd.vmNicTOs.get(0).securityGroupRefs.size() == 0
        }
    }

    void testAdminManageHiddenBindings() {
        accountA = createAccount {
            name = "binding-account-a"
            password = "password"
        }
        accountB = createAccount {
            name = "binding-account-b"
            password = "password"
        }
        sessionA = logInByAccount {
            accountName = accountA.name
            password = "password"
        }
        def sessionB = logInByAccount {
            accountName = accountB.name
            password = "password"
        }
        changeResourceOwner {
            resourceUuid = vm1.uuid
            accountUuid = accountA.uuid
        }
        tenantNicUuid = vm1.vmNics[0].uuid
        tenantGroup = createSecurityGroup {
            name = "tenant-binding-group"
            sessionId = sessionA.uuid
        }
        def foreignGroup = createSecurityGroup {
            name = "foreign-private-group"
            sessionId = sessionB.uuid
        }
        hiddenGroupOne = createSecurityGroup { name = "admin-hidden-one" }
        hiddenGroupTwo = createSecurityGroup { name = "admin-hidden-two" }

        addVmNicToSecurityGroup {
            securityGroupUuid = tenantGroup.uuid
            vmNicUuids = [tenantNicUuid]
            sessionId = sessionA.uuid
        }
        addVmNicToSecurityGroup {
            securityGroupUuid = hiddenGroupOne.uuid
            vmNicUuids = [tenantNicUuid]
        }

        assert bindingOrder() == [hiddenGroupOne.uuid, tenantGroup.uuid] : "private admin group must precede the tenant group"

        setVmNicSecurityGroup {
            vmNicUuid = tenantNicUuid
            refs = refsFor([tenantGroup.uuid, hiddenGroupOne.uuid])
        }
        deleteVmNicFromSecurityGroup {
            securityGroupUuid = hiddenGroupOne.uuid
            vmNicUuids = [tenantNicUuid]
        }

        assert bindingOrder() == [tenantGroup.uuid] : "admin must be able to detach its private group"

        def before = bindingOrder()
        def foreignAdd = new AddVmNicToSecurityGroupAction(securityGroupUuid: foreignGroup.uuid, vmNicUuids: [tenantNicUuid], sessionId: adminSession()).call()

        assert foreignAdd.error?.globalErrorCode == "ORG_ZSTACK_NETWORK_SECURITYGROUP_10133" : "admin must not bypass use permission for another account's group: ${foreignAdd.error}"

        def foreignSet = new SetVmNicSecurityGroupAction(vmNicUuid: tenantNicUuid, refs: refsFor([tenantGroup.uuid, foreignGroup.uuid]), sessionId: adminSession()).call()

        assert foreignSet.error?.globalErrorCode == "ORG_ZSTACK_NETWORK_SECURITYGROUP_10133" : "Set must reject another account's private group: ${foreignSet.error}"
        assert bindingOrder() == before : "rejected Add and Set must preserve the original binding"

        def forged = new AddVmNicToSecurityGroupAction(securityGroupUuid: hiddenGroupOne.uuid, vmNicUuids: [tenantNicUuid], sessionId: sessionA.uuid).call()

        assert forged.error != null : "tenant must not bind a private admin group by UUID"
        assert bindingOrder() == before : "rejected tenant binding must preserve existing groups"
    }

    void testShareHiddenGroupsThenReorder() {
        addVmNicToSecurityGroup {
            securityGroupUuid = hiddenGroupTwo.uuid
            vmNicUuids = [tenantNicUuid]
        }
        addVmNicToSecurityGroup {
            securityGroupUuid = hiddenGroupOne.uuid
            vmNicUuids = [tenantNicUuid]
        }

        assert bindingOrder() == [hiddenGroupOne.uuid, hiddenGroupTwo.uuid, tenantGroup.uuid]

        setSecurityGroupSharing {
            securityGroupUuid = hiddenGroupOne.uuid
            mode = "Specified"
            accountUuids = [accountA.uuid]
        }

        assert bindingOrder() == [hiddenGroupOne.uuid, hiddenGroupTwo.uuid, tenantGroup.uuid] : "sharing must not reorder existing bindings"

        setVmNicSecurityGroup {
            vmNicUuid = tenantNicUuid
            refs = refsFor([tenantGroup.uuid, hiddenGroupOne.uuid])
            sessionId = sessionA.uuid
        }

        assert bindingOrder() == [hiddenGroupTwo.uuid, tenantGroup.uuid, hiddenGroupOne.uuid] : "tenant sorting must move the remaining hidden group to the front"

        setVmNicSecurityGroup {
            vmNicUuid = tenantNicUuid
            refs = refsFor([tenantGroup.uuid])
            sessionId = sessionA.uuid
        }

        assert bindingOrder() == [hiddenGroupTwo.uuid, tenantGroup.uuid] : "omitting the shared group must detach it and preserve the hidden group"

        def explicitHidden = new SetVmNicSecurityGroupAction(vmNicUuid: tenantNicUuid, refs: refsFor([tenantGroup.uuid, hiddenGroupTwo.uuid]), sessionId: sessionA.uuid).call()

        assert explicitHidden.error?.globalErrorCode == "ORG_ZSTACK_NETWORK_SECURITYGROUP_10133" : "tenant must not submit a hidden group for sorting: ${explicitHidden.error}"

        def detachHidden = new DeleteVmNicFromSecurityGroupAction(securityGroupUuid: hiddenGroupTwo.uuid, vmNicUuids: [tenantNicUuid], sessionId: sessionA.uuid).call()

        assert detachHidden.error != null : "tenant must not detach a hidden group"
        assert bindingOrder() == [hiddenGroupTwo.uuid, tenantGroup.uuid]

        setSecurityGroupSharing {
            securityGroupUuid = hiddenGroupOne.uuid
            mode = "None"
        }
    }

    private List<VmNicSecurityGroupRefAO> refsFor(List<String> uuids) {
        return uuids.withIndex().collect { String uuid, int index -> new VmNicSecurityGroupRefAO(securityGroupUuid: uuid, priority: index + 1) }
    }

    private List<String> bindingOrder() {
        return Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, tenantNicUuid)
                .orderBy(VmNicSecurityGroupRefVO_.priority, org.zstack.core.db.SimpleQuery.Od.ASC).list().collect { it.securityGroupUuid }
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

        testVmNicAttachSecurityGroup()
        testUpdateGroupMembers()
        testSetVmNicSecurityGroupDirect()
        testAdminManageHiddenBindings()
        testShareHiddenGroupsThenReorder()
    }
}
