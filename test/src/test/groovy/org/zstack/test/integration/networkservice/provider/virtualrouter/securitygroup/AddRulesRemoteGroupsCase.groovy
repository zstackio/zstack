package org.zstack.test.integration.networkservice.provider.virtualrouter.securitygroup

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.Constants
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMSecurityGroupBackend
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg
import org.zstack.network.securitygroup.SecurityGroupMembersTO
import org.zstack.network.securitygroup.SecurityGroupRuleProtocolType
import org.zstack.network.securitygroup.SecurityGroupRuleTO
import org.zstack.network.securitygroup.SecurityGroupRuleType
import org.zstack.network.securitygroup.VmNicSecurityGroupRefVO
import org.zstack.network.securitygroup.VmNicSecurityGroupRefVO_
import org.zstack.sdk.AddSecurityGroupRuleAction
import org.zstack.sdk.HostInventory
import org.zstack.sdk.SecurityGroupInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.virtualrouter.VirtualRouterNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by MaJin on 2017-06-19.
 */

// failure probability is 0.005% : duplicate rules
class AddRulesRemoteGroupsCase extends SubCase{
    EnvSpec env
    VmInstanceInventory vm1, vm2, vm3, vm4
    HostInventory host1, host2, host3
    SecurityGroupInventory sg1, sg2, sg3
    HashMap<String, List<String>> sgVmIp = new HashMap<>()

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
            vm1 = env.inventoryByName("vm1") as VmInstanceInventory // vm1 in host1
            vm2 = env.inventoryByName("vm2") as VmInstanceInventory // vm2 in host2
            vm3 = env.inventoryByName("vm3") as VmInstanceInventory // vm3 in host3
            vm4 = env.inventoryByName("vm4") as VmInstanceInventory // vm4 in host3
            host1 = env.inventoryByName("kvm1") as HostInventory
            host2 = env.inventoryByName("kvm2") as HostInventory
            host3 = env.inventoryByName("kvm3") as HostInventory
            testCreateSecurityGroup()
            testAddRule()
                /*
                 sg1's vm in host1, ipset in host1, host2, host3
                 sg2's vm in host2, ipset in host1, host2, host3
                 sg3's vm in host3, ipset in host2, host3
                 */

            // after action, sg3's ipset is in host2 and host3, should update its ipset member on host2 and host3
            testRemoveVmNicFromSecurityGroup([vm3.vmNics.get(0).uuid], sg3.uuid, [host2.uuid, host3.uuid])
            sgVmIp.get(sg3.uuid).remove(vm3.vmNics.get(0).ip)

            // after action, sg3's ipset is in host2 and host3, should update its ipset member on host2 and host3
            testAddVmNicToSecurityGroup([vm3.vmNics.get(0).uuid], sg3.uuid, [host2.uuid, host3.uuid])
            sgVmIp.get(sg3.uuid).add(vm3.vmNics.get(0).ip)

            // after action, sg1's ipset is in host2 and host3, should update its ipset member on host2 and host3
            testRemoveVmNicFromSecurityGroup([vm1.vmNics.get(0).uuid], sg1.uuid, [host2.uuid, host3.uuid])
            sgVmIp.get(sg1.uuid).remove(vm1.vmNics.get(0).ip)

            // after action, sg1's ipset is in host1, host2 and host3, should update its ipset member on host1, host2 and host3
            testAddVmNicToSecurityGroup([vm1.vmNics.get(0).uuid], sg1.uuid, [host1.uuid, host2.uuid, host3.uuid])
            sgVmIp.get(sg1.uuid).add(vm1.vmNics.get(0).ip)

            addRule(sg1.uuid, 1, [sg3.uuid], 4) //[sg3.uuid] is remoteSecurityGroup 4 is existed rule count on sg1

            // after action, sg3's ipset is in host1, host2 and host3, should update its ipset member on host1, host2 and host3
            testRemoveVmNicFromSecurityGroup([vm4.vmNics.get(0).uuid], sg3.uuid, [host1.uuid, host2.uuid, host3.uuid])
            sgVmIp.get(sg1.uuid).remove(vm4.vmNics.get(0).ip)

            //sg3's ipset is in host1, host2 and host3, should delete its ipset on host1, host2 and host3
            testDeleteSecurityGroup(sg3.uuid, [host1.uuid, host2.uuid, host3.uuid])

            testAddDuplicateRule(sg1.uuid, sg2.uuid)
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    private static String returnRandomCidr(){
        Random random = new Random()
        StringBuffer sb = new StringBuffer()
        for (int i = 0; i < 4; i++) {
            int n = random.nextInt(256)
            sb.append(n.toString())
            sb.append(".")
        }
        sb.deleteCharAt(sb.length() - 1)

        int cidr = random.nextInt(33)
        sb.append("/")
        sb.append(cidr.toString())
        return new String(sb)
    }

    private static APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO returnRandomRule(){
        Random random = new Random()
        APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO rule = new APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO()
        rule.allowedCidr = returnRandomCidr()
        rule.type = random.nextBoolean() ? SecurityGroupRuleType.Ingress.toString() : SecurityGroupRuleType.Egress.toString()
        rule.protocol = random.nextBoolean() ? SecurityGroupRuleProtocolType.TCP.toString() : SecurityGroupRuleProtocolType.UDP.toString()
        rule.startPort = random.nextInt(65536)
        rule.endPort = random.nextInt(65536 - rule.startPort) + rule.startPort
        return rule
    }

    private SecurityGroupInventory createSecurityGroup(VmInstanceInventory ... vms){
        // initial
        List<String> nicUuids = new ArrayList<>()
        List<String> vmIps = new ArrayList<>()
        for(VmInstanceInventory vm : vms){
            nicUuids.add(vm.getVmNics().get(0).getUuid())
            vmIps.add(vm.getVmNics().get(0).getIp())
        }

        // action
        SecurityGroupInventory sg = createSecurityGroup{
            name = "sg"
        } as SecurityGroupInventory

        attachSecurityGroupToL3Network {
            securityGroupUuid = sg.uuid
            l3NetworkUuid = vms[0].getVmNics().get(0).l3NetworkUuid
        }

        KVMAgentCommands.ApplySecurityGroupRuleCmd cmd
        env.simulator(KVMSecurityGroupBackend.SECURITY_GROUP_APPLY_RULE_PATH ){ HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.getBody(), KVMAgentCommands.ApplySecurityGroupRuleCmd.class)
            return new KVMAgentCommands.ApplySecurityGroupRuleResponse()
        }

        addVmNicToSecurityGroup {
            securityGroupUuid = sg.uuid
            vmNicUuids = nicUuids
        }
        sgVmIp.put(sg.uuid, vmIps)

        // validate
        retryInSecs{
            assert cmd != null
        }
        boolean call = false
        for (SecurityGroupRuleTO rule : cmd.ruleTOs){
            assert rule.getRules().isEmpty()
            assert rule.getSecurityGroupBaseRules().size() == 2
            assert rule.getSecurityGroupBaseRules().get(0).remoteGroupUuid == sg.uuid
            assert rule.getSecurityGroupBaseRules().get(0).remoteGroupVmIps.containsAll(vmIps)
            call = true
        }
        assert call

        return sg
    }

    void testAddDuplicateRule(String sgUuid, String remoteGroupUuid){
        APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO rule1 = returnRandomRule()

        addSecurityGroupRule {
            rules = [rule1]
            securityGroupUuid = sgUuid
        }

        AddSecurityGroupRuleAction a1 = new AddSecurityGroupRuleAction()
        a1.rules = [rule1]
        a1.securityGroupUuid = sgUuid
        a1.sessionId = currentEnvSpec.session.uuid

        assert a1.call().error != null

        addSecurityGroupRule {
            rules = [rule1]
            securityGroupUuid = sgUuid
            remoteSecurityGroupUuids = [remoteGroupUuid]
        }

        APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO rule2 = returnRandomRule()

        addSecurityGroupRule {
            rules = [rule2]
            securityGroupUuid = sgUuid
            remoteSecurityGroupUuids = [remoteGroupUuid]
        }

        AddSecurityGroupRuleAction a2 = new AddSecurityGroupRuleAction()
        a2.rules = [rule2]
        a2.securityGroupUuid = sgUuid
        a2.remoteSecurityGroupUuids = [remoteGroupUuid]
        a2.sessionId = currentEnvSpec.session.uuid

        assert a2.call().error != null

        addSecurityGroupRule {
            rules = [rule2]
            securityGroupUuid = sgUuid
        }
    }

    private void addRule(String sgUuid, int ruleCounts, int existedRuleCount){
        addRule(sgUuid, ruleCounts, null, existedRuleCount)
    }

    private void addRule(String sgUuid, int ruleCounts, List<String> remoteSgUuids, int existedRuleCount){
        // initial
        List<APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO> sgRules = new ArrayList<>()
        for (int i = 0; i < ruleCounts; i++) {
            sgRules.add(returnRandomRule())
        }

        // action
        KVMAgentCommands.ApplySecurityGroupRuleCmd cmd

        env.simulator(KVMSecurityGroupBackend.SECURITY_GROUP_APPLY_RULE_PATH ){ HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.getBody(), KVMAgentCommands.ApplySecurityGroupRuleCmd.class)
            return new KVMAgentCommands.ApplySecurityGroupRuleResponse()
        }

        addSecurityGroupRule {
            rules = sgRules
            securityGroupUuid = sgUuid
            remoteSecurityGroupUuids = remoteSgUuids
        }

        // validate
        retryInSecs{
            assert cmd != null
        }
        for (SecurityGroupRuleTO rule : cmd.ruleTOs){
            if(remoteSgUuids != null && !remoteSgUuids.isEmpty()){
                assert rule.getSecurityGroupBaseRules().size() == remoteSgUuids.size() * ruleCounts + existedRuleCount
                rule.getSecurityGroupBaseRules().forEach{ it ->
                    assert it.remoteGroupVmIps.size() == sgVmIp.get(it.remoteGroupUuid).size()
                    assert it.remoteGroupVmIps.containsAll(sgVmIp.get(it.remoteGroupUuid))
                }
            }else {
                assert rule.rules.size() == ruleCounts + existedRuleCount
                rule.rules.forEach{ it ->
                    assert it.remoteGroupUuid == null
                    assert it.remoteGroupVmIps == null
                }
            }
        }
    }

    private static List<String> getVmIpsInSecurityGroup(String sgUuid){
        return SQL.New("select nic.ip" +
                " from VmNicVO nic, VmNicSecurityGroupRefVO ref" +
                " where ref.vmNicUuid = nic.uuid" +
                " and ref.securityGroupUuid = :sgUuid" +
                " and nic.ip is not null", String).param("sgUuid", sgUuid).list()
    }

    void testCreateSecurityGroup(){
        sg1 = createSecurityGroup(vm1)  //sg1'vm in host1
        sg2 = createSecurityGroup(vm2)  //sg2'vm in host2
        sg3 = createSecurityGroup(vm3, vm4) // sg3'vm in host3
    }

    void testAddRule(){ // 2 is default rule count
        addRule(sg1.uuid, 4, 0)
        addRule(sg2.uuid, 2, 0)
        addRule(sg3.uuid, 3, 0)
        addRule(sg1.uuid, 2, [sg2.uuid], 2)
        addRule(sg2.uuid, 3, [sg1.uuid, sg3.uuid], 2)
        addRule(sg3.uuid, 1, [sg2.uuid, sg1.uuid], 2)
    }

    void testRemoveVmNicFromSecurityGroup(List<String> nicUuids, String sgUuid, List<String> expectHostUuids){
        boolean hasHost = expectHostUuids != null && !expectHostUuids.isEmpty()

        KVMAgentCommands.ApplySecurityGroupRuleCmd acmd
        KVMAgentCommands.UpdateGroupMemberCmd ucmd
        List<String> actuallyHostUuids = Collections.synchronizedList(new ArrayList<String>())

        env.simulator(KVMSecurityGroupBackend.SECURITY_GROUP_APPLY_RULE_PATH ){ HttpEntity<String> e ->
            acmd = JSONObjectUtil.toObject(e.getBody(), KVMAgentCommands.ApplySecurityGroupRuleCmd.class)
            return new KVMAgentCommands.ApplySecurityGroupRuleResponse()
        }

        env.simulator(KVMSecurityGroupBackend.SECURITY_GROUP_UPDATE_GROUP_MEMBER ){ HttpEntity<String> e ->
            String huuid = e.getHeaders().getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)
            actuallyHostUuids.add(huuid)
            ucmd = JSONObjectUtil.toObject(e.getBody(), KVMAgentCommands.UpdateGroupMemberCmd.class)
            return new KVMAgentCommands.ApplySecurityGroupRuleResponse()
        }

        deleteVmNicFromSecurityGroup {
            vmNicUuids = nicUuids
            securityGroupUuid = sgUuid
        }

        retryInSecs{
            assert acmd != null
            assert ucmd != null || !hasHost
            assert actuallyHostUuids.size() == expectHostUuids.size()
        }

        //assert acmd.ruleTOs.size() == nicUuids.size()

        if(!hasHost){
            return
        }
        List<String> expectTransportVmIps = getVmIpsInSecurityGroup(sgUuid)
        List<String> actuallyTransportVmIps = ucmd.updateGroupTOs.get(0).securityGroupVmIps
        int IpsCount = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.securityGroupUuid, sgUuid).count()

        assert actuallyHostUuids.containsAll(expectHostUuids)
        assert ucmd.updateGroupTOs.size() == 1
        assert ucmd.updateGroupTOs.get(0).securityGroupUuid == sgUuid
        assert ucmd.updateGroupTOs.get(0).actionCode == SecurityGroupMembersTO.ACTION_CODE_UPDATE_GROUP_MEMBER
        assert actuallyTransportVmIps.size() == expectTransportVmIps.size()
        assert actuallyTransportVmIps.size() == IpsCount
        expectTransportVmIps.removeAll(actuallyTransportVmIps)
        assert expectTransportVmIps.isEmpty()

    }

    void testAddVmNicToSecurityGroup(List<String> nicUuids, String sgUuid, List<String> expectHostUuids){
        boolean hasHost = expectHostUuids != null && !expectHostUuids.isEmpty()

        KVMAgentCommands.ApplySecurityGroupRuleCmd acmd
        KVMAgentCommands.UpdateGroupMemberCmd ucmd
        List<String> actuallyHostUuids = Collections.synchronizedList(new ArrayList<String>())

        env.simulator(KVMSecurityGroupBackend.SECURITY_GROUP_APPLY_RULE_PATH ){ HttpEntity<String> e ->
            acmd = JSONObjectUtil.toObject(e.getBody(), KVMAgentCommands.ApplySecurityGroupRuleCmd.class)
            return new KVMAgentCommands.ApplySecurityGroupRuleResponse()
        }

        env.simulator(KVMSecurityGroupBackend.SECURITY_GROUP_UPDATE_GROUP_MEMBER ){ HttpEntity<String> e ->
            String huuid = e.getHeaders().getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)
            actuallyHostUuids.add(huuid)
            ucmd = JSONObjectUtil.toObject(e.getBody(), KVMAgentCommands.UpdateGroupMemberCmd.class)
            return new KVMAgentCommands.ApplySecurityGroupRuleResponse()
        }

        addVmNicToSecurityGroup {
            vmNicUuids = nicUuids
            securityGroupUuid = sgUuid
        }

        retryInSecs{
            assert acmd != null
            assert ucmd != null || !hasHost
            assert actuallyHostUuids.size() == expectHostUuids.size()
        }
        //assert acmd.ruleTOs.size() == nicUuids.size()

        if(!hasHost){
            return
        }
        List<String> expectTransportVmIps = getVmIpsInSecurityGroup(sgUuid)
        List<String> actuallyTransportVmIps = ucmd.updateGroupTOs.get(0).securityGroupVmIps
        int IpsCount = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.securityGroupUuid, sgUuid).count()

        assert actuallyHostUuids.containsAll(expectHostUuids)
        assert ucmd.updateGroupTOs.size() == 1
        assert ucmd.updateGroupTOs.get(0).securityGroupUuid == sgUuid
        assert ucmd.updateGroupTOs.get(0).actionCode == SecurityGroupMembersTO.ACTION_CODE_UPDATE_GROUP_MEMBER
        assert actuallyTransportVmIps.size() == expectTransportVmIps.size()
        assert actuallyTransportVmIps.size() == IpsCount
        expectTransportVmIps.removeAll(actuallyTransportVmIps)
        assert expectTransportVmIps.isEmpty()
    }

    void testDeleteSecurityGroup(String sgUuid, List<String> expectHostUuids){
        boolean hasVmNic = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.securityGroupUuid, sgUuid).isExists()
        boolean hasHost = expectHostUuids != null && !expectHostUuids.isEmpty()

        KVMAgentCommands.ApplySecurityGroupRuleCmd acmd
        KVMAgentCommands.UpdateGroupMemberCmd ucmd
        List<String> actuallyHostUuids = Collections.synchronizedList(new ArrayList<String>())

        env.simulator(KVMSecurityGroupBackend.SECURITY_GROUP_APPLY_RULE_PATH ){ HttpEntity<String> e ->
            acmd = JSONObjectUtil.toObject(e.getBody(), KVMAgentCommands.ApplySecurityGroupRuleCmd.class)
            return new KVMAgentCommands.ApplySecurityGroupRuleResponse()
        }

        env.simulator(KVMSecurityGroupBackend.SECURITY_GROUP_UPDATE_GROUP_MEMBER ){ HttpEntity<String> e ->
            String huuid = e.getHeaders().getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)
            actuallyHostUuids.add(huuid)
            ucmd = JSONObjectUtil.toObject(e.getBody(), KVMAgentCommands.UpdateGroupMemberCmd.class)
            return new KVMAgentCommands.ApplySecurityGroupRuleResponse()
        }

        deleteSecurityGroup {
            uuid = sgUuid
        }

        retryInSecs{
            assert acmd != null || !hasVmNic
            assert ucmd != null || !hasHost
            assert actuallyHostUuids.size() == expectHostUuids.size()
        }

        if(hasHost){
            actuallyHostUuids.removeAll(expectHostUuids)
            assert actuallyHostUuids.isEmpty()
            assert ucmd.updateGroupTOs.get(0).actionCode == SecurityGroupMembersTO.ACTION_CODE_DELETE_GROUP
            assert ucmd.updateGroupTOs.get(0).securityGroupUuid == sgUuid
        }
    }
}
