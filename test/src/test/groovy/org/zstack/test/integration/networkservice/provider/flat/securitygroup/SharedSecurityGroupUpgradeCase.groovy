package org.zstack.test.integration.networkservice.provider.flat.securitygroup

import org.zstack.compute.vm.InstantiateVmFromNewCreatedStruct
import org.zstack.compute.vm.VmSystemTags
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.db.SimpleQuery
import org.zstack.core.jsonlabel.JsonLabelVO
import org.zstack.core.jsonlabel.JsonLabelVO_
import org.zstack.header.identity.SharedResourceVO
import org.zstack.header.identity.SharedResourceVO_
import org.zstack.header.vm.VmCreationStrategy
import org.zstack.network.securitygroup.APISetVmNicSecurityGroupMsg.VmNicSecurityGroupRefAO
import org.zstack.network.securitygroup.SecurityGroupRuleVO
import org.zstack.network.securitygroup.SecurityGroupRuleVO_
import org.zstack.network.securitygroup.VmNicSecurityGroupRefVO
import org.zstack.network.securitygroup.VmNicSecurityGroupRefVO_
import org.zstack.networksecuritypolicyschedule.NetworkSecurityPolicyScheduleVO
import org.zstack.networksecuritypolicyschedule.NetworkSecurityPolicyScheduleVO_
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.DeleteVmNicFromSecurityGroupAction
import org.zstack.sdk.SecurityGroupInventory
import org.zstack.sdk.SessionInventory
import org.zstack.sdk.SetVmNicSecurityGroupAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.virtualrouter.VirtualRouterNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Scenario: Use persisted private admin bindings and the old JustCreate JSON format after upgrading.
 * Expected: Existing bindings remain hidden and protected; old Created VMs start with remaining groups after configured groups are deleted or shared.
 * This case verifies legacy data compatibility, not an upgrade between two installed management-node versions.
 */
class SharedSecurityGroupUpgradeCase extends SubCase {
    EnvSpec env
    AccountInventory account
    SessionInventory tenantSession
    SecurityGroupInventory adminGroup, tenantGroup
    String l3Uuid

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
        env.create {}
        l3Uuid = env.inventoryByName("l3").uuid
        account = createAccount {
            name = "legacy-security-group-account"
            password = "password"
        }
        tenantSession = logInByAccount {
            accountName = account.name
            password = "password"
        }
        adminGroup = createSecurityGroup { name = "legacy-admin-group" }
        tenantGroup = createSecurityGroup {
            name = "legacy-tenant-group"
            sessionId = tenantSession.uuid
        }
        testHistoricalBindingsStayHidden()
        testLegacyAdminGroupOnFirstStart()
    }

    @Override
    void clean() {
        env.delete()
    }

    void testHistoricalBindingsStayHidden() {
        // Form the existing binding layouts through API operations.
        List<VmInstanceInventory> vms = (1..3).collect { env.inventoryByName("vm${it}") as VmInstanceInventory }
        vms.each { vm ->
            changeResourceOwner {
                resourceUuid = vm.uuid
                accountUuid = account.uuid
            }
        }
        def attachedVm = attachL3NetworkToVm {
            vmInstanceUuid = vms[2].uuid
            l3NetworkUuid = env.inventoryByName("l3-2").uuid
        }
        addVmNicToSecurityGroup {
            securityGroupUuid = adminGroup.uuid
            vmNicUuids = vms.collect { it.vmNics[0].uuid } + [attachedVm.vmNics.find { it.l3NetworkUuid == env.inventoryByName("l3-2").uuid }.uuid]
        }
        addVmNicToSecurityGroup {
            securityGroupUuid = tenantGroup.uuid
            vmNicUuids = [vms[1].vmNics[0].uuid, vms[2].vmNics[0].uuid]
            sessionId = tenantSession.uuid
        }
        VmInstanceInventory recycled = env.inventoryByName("vm4")
        changeResourceOwner {
            resourceUuid = recycled.uuid
            accountUuid = account.uuid
        }
        addVmNicToSecurityGroup {
            securityGroupUuid = adminGroup.uuid
            vmNicUuids = [recycled.vmNics[0].uuid]
        }
        destroyVmInstance { uuid = recycled.uuid }

        def schedule = createNetworkSecurityPolicySchedule {
            name = "legacy-admin-time-policy"
            resourceType = "SecurityGroup"
            resourceUuid = adminGroup.uuid
            timeType = "UTC"
            repeatType = "Weekly"
            startDate = "2099-01-01"
            endDate = "2099-12-31"
            startTime = "09:00"
            endTime = "18:00"
            weekDays = [1, 2, 3, 4, 5]
        }
        def scheduleBefore = JSONObjectUtil.toJsonString(Q.New(NetworkSecurityPolicyScheduleVO.class).eq(NetworkSecurityPolicyScheduleVO_.uuid, schedule.uuid).find())
        def bindingsBefore = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.securityGroupUuid, adminGroup.uuid).list()
                .collect { [uuid: it.uuid, nic: it.vmNicUuid, priority: it.priority] }.sort { it.uuid }
        def rulesBefore = JSONObjectUtil.toJsonString(Q.New(SecurityGroupRuleVO.class).eq(SecurityGroupRuleVO_.securityGroupUuid, adminGroup.uuid).orderBy(SecurityGroupRuleVO_.uuid, SimpleQuery.Od.ASC).list())
        def hiddenGroups = querySecurityGroup {
            conditions = ["uuid=${adminGroup.uuid}"]
            sessionId = tenantSession.uuid
        }

        assert hiddenGroups.isEmpty() : "legacy private admin group must remain invisible"

        def hiddenBindings = queryVmNicInSecurityGroup {
            conditions = ["securityGroupUuid=${adminGroup.uuid}"]
            sessionId = tenantSession.uuid
        }

        assert hiddenBindings.isEmpty() : "hidden group bindings must not leak through NIC queries"

        String nicUuid = vms[1].vmNics[0].uuid
        def forgedOrder = new SetVmNicSecurityGroupAction(vmNicUuid: nicUuid,
                refs: [new VmNicSecurityGroupRefAO(securityGroupUuid: tenantGroup.uuid, priority: 1), new VmNicSecurityGroupRefAO(securityGroupUuid: adminGroup.uuid, priority: 2)],
                sessionId: tenantSession.uuid).call()

        assert forgedOrder.error != null : "tenant must not sort a hidden group"

        def forgedDetach = new DeleteVmNicFromSecurityGroupAction(securityGroupUuid: adminGroup.uuid, vmNicUuids: [nicUuid], sessionId: tenantSession.uuid).call()

        assert forgedDetach.error != null : "tenant must not detach a hidden group"

        setVmNicSecurityGroup {
            vmNicUuid = nicUuid
            refs = [new VmNicSecurityGroupRefAO(securityGroupUuid: tenantGroup.uuid, priority: 1)]
            sessionId = tenantSession.uuid
        }
        def refs = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, nicUuid).orderBy(VmNicSecurityGroupRefVO_.priority, SimpleQuery.Od.ASC).list()

        assert refs*.securityGroupUuid == [adminGroup.uuid, tenantGroup.uuid] : "omitting a hidden group must preserve it ahead of tenant groups"

        def bindingsAfter = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.securityGroupUuid, adminGroup.uuid).list()
                .collect { [uuid: it.uuid, nic: it.vmNicUuid, priority: it.priority] }.sort { it.uuid }

        assert bindingsAfter == bindingsBefore : "existing binding identities and priorities must remain unchanged"
        assert JSONObjectUtil.toJsonString(Q.New(SecurityGroupRuleVO.class).eq(SecurityGroupRuleVO_.securityGroupUuid, adminGroup.uuid).orderBy(SecurityGroupRuleVO_.uuid, SimpleQuery.Od.ASC).list()) == rulesBefore
        assert !Q.New(SharedResourceVO.class).eq(SharedResourceVO_.resourceUuid, adminGroup.uuid).isExists() : "legacy bindings must not create sharing grants"
        assert JSONObjectUtil.toJsonString(Q.New(NetworkSecurityPolicyScheduleVO.class).eq(NetworkSecurityPolicyScheduleVO_.uuid, schedule.uuid).find()) == scheduleBefore : "existing time policy must remain unchanged"
    }

    void testLegacyAdminGroupOnFirstStart() {
        SecurityGroupInventory deletedGroup = createSecurityGroup { name = "legacy-deleted-admin-group" }
        List<VmInstanceInventory> created = (1..3).collect { int index ->
            String groupUuids = index == 3 ? deletedGroup.uuid : "${adminGroup.uuid},${tenantGroup.uuid},${deletedGroup.uuid}"
            VmInstanceInventory vm = createVmInstance {
                name = "legacy-created-${index}"
                imageUuid = env.inventoryByName("image").uuid
                instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
                l3NetworkUuids = [l3Uuid]
                strategy = VmCreationStrategy.JustCreate.toString()
                systemTags = ["l3::${l3Uuid}::SecurityGroupUuids::${groupUuids}".toString()]
            }
            changeResourceOwner {
                resourceUuid = vm.uuid
                accountUuid = account.uuid
            }
            // The old binary did not persist these fields. Keep all real API-created VM data and only remove the new JSON keys.
            JsonLabelVO label = Q.New(JsonLabelVO.class).eq(JsonLabelVO_.labelKey, InstantiateVmFromNewCreatedStruct.makeLabelKey(vm.uuid)).find()
            Map oldFormat = JSONObjectUtil.toObject(label.labelValue, LinkedHashMap.class)
            oldFormat.remove("requestedSecurityGroupUuids")
            oldFormat.remove("inheritedSecurityGroupUuids")
            label.labelValue = JSONObjectUtil.toJsonString(oldFormat)
            bean(DatabaseFacade.class).update(label)
            return vm
        }
        deleteSecurityGroup { uuid = deletedGroup.uuid }

        VmInstanceInventory first = startVmInstance {
            uuid = created[0].uuid
            sessionId = tenantSession.uuid
        }

        assert first.state == "Running"
        assert Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, first.vmNics[0].uuid)
                .orderBy(VmNicSecurityGroupRefVO_.priority, SimpleQuery.Od.ASC).list()*.securityGroupUuid == [adminGroup.uuid, tenantGroup.uuid]
        assert VmSystemTags.L3_NETWORK_SECURITY_GROUP_UUIDS_REF.getTags(first.uuid).isEmpty() : "first start must consume the group configuration tag"

        def firstStartGroups = querySecurityGroup {
            conditions = ["uuid=${adminGroup.uuid}"]
            sessionId = tenantSession.uuid
        }

        assert firstStartGroups.isEmpty()
        assert !Q.New(SharedResourceVO.class).eq(SharedResourceVO_.resourceUuid, adminGroup.uuid).isExists()

        setSecurityGroupSharing {
            securityGroupUuid = adminGroup.uuid
            mode = "Specified"
            accountUuids = [account.uuid]
        }
        VmInstanceInventory second = startVmInstance {
            uuid = created[1].uuid
            sessionId = tenantSession.uuid
        }

        assert second.state == "Running"
        assert Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, second.vmNics[0].uuid)
                .orderBy(VmNicSecurityGroupRefVO_.priority, SimpleQuery.Od.ASC).list()*.securityGroupUuid == [adminGroup.uuid, tenantGroup.uuid]
        assert VmSystemTags.L3_NETWORK_SECURITY_GROUP_UUIDS_REF.getTags(second.uuid).isEmpty() : "shared first start must consume the group configuration tag"

        def sharedGroups = querySecurityGroup {
            conditions = ["uuid=${adminGroup.uuid}"]
            sessionId = tenantSession.uuid
        }

        assert sharedGroups.size() == 1

        setVmNicSecurityGroup {
            vmNicUuid = second.vmNics[0].uuid
            refs = [new VmNicSecurityGroupRefAO(securityGroupUuid: tenantGroup.uuid, priority: 1), new VmNicSecurityGroupRefAO(securityGroupUuid: adminGroup.uuid, priority: 2)]
            sessionId = tenantSession.uuid
        }

        assert Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, second.vmNics[0].uuid)
                .orderBy(VmNicSecurityGroupRefVO_.priority, SimpleQuery.Od.ASC).list()*.securityGroupUuid == [tenantGroup.uuid, adminGroup.uuid]

        VmInstanceInventory withoutGroups = startVmInstance {
            uuid = created[2].uuid
            sessionId = tenantSession.uuid
        }

        assert withoutGroups.state == "Running" : "deleted groups must not block legacy first start"
        assert !Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmInstanceUuid, withoutGroups.uuid).isExists() : "deleted groups must not create bindings"
        assert VmSystemTags.L3_NETWORK_SECURITY_GROUP_UUIDS_REF.getTags(withoutGroups.uuid).isEmpty() : "empty group configuration must still consume its tag"
    }
}
