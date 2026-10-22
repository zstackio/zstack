package org.zstack.test.integration.networkservice.provider.flat.securitygroup

import org.springframework.util.ReflectionUtils
import org.zstack.core.cascade.CascadeConstant
import org.zstack.core.cascade.CascadeFacade
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.EntityEvent
import org.zstack.core.db.EntityLifeCycleCallback
import org.zstack.core.db.Q
import org.zstack.header.core.Completion
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.OperationFailureException
import org.zstack.header.identity.SharedResourceVO
import org.zstack.header.identity.SharedResourceVO_
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmNicVO
import org.zstack.header.vm.VmNicVO_
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO
import org.zstack.network.securitygroup.APIUpdateSecurityGroupRulePriorityMsg.SecurityGroupRulePriorityAO
import org.zstack.network.securitygroup.RemoveVmNicFromSecurityGroupMsg
import org.zstack.network.securitygroup.RemoveVmNicFromSecurityGroupReply
import org.zstack.network.securitygroup.SecurityGroupManagerImpl
import org.zstack.network.securitygroup.SecurityGroupVO
import org.zstack.network.securitygroup.VmNicSecurityGroupRefVO
import org.zstack.network.securitygroup.VmNicSecurityGroupRefVO_
import org.zstack.sdk.*
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.flat.FlatNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Scenario: Admin shares a group; tenants bind and query it; admin changes or recalls sharing and deletes groups.
 * Expected: Use follows the resource account, shared rules remain read-only, references block recall, and failed settings restore every original authorization field.
 */
class SharedSecurityGroupCase extends SubCase {
    EnvSpec env
    Map<String, AccountInventory> accounts = [:]
    Map<String, SessionInventory> sessions = [:]
    SessionInventory userSession
    UserInventory user
    VmInstanceInventory vmA
    VmInstanceInventory vmB
    SecurityGroupInventory sharedGroup
    SecurityGroupInventory hiddenGroup
    SecurityGroupInventory ownGroup
    String l3Uuid
    String secondL3Uuid
    String unsuitableL3Uuid
    String legacySharingL3Uuid
    EntityEvent sharingWriteEvent
    EntityLifeCycleCallback sharingWriteCallback

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = FlatNetworkServiceEnv.oneHostNoVmEnv()
    }

    @Override
    void test() {
        env.create {}
        testPrepareAccountsAndNics()
        testLegacySharingApisRejectSecurityGroups()
        testSpecifiedShareAndBind()
        testLegacyRevokePreservesSharingAndBindings()
        testSharedGroupStaysReadOnly()
        testReadSharedGroupAndResources()
        testBatchRemoteReferencesRequireAllPermissions()
        testReplaceSpecifiedSharing()
        testRecallWithNicReference()
        testRecallWithRuleReference()
        testAttachSharedSecurityGroup()
        testPublicShareAndReduceScope()
        testDeleteFailureIsReported()
        testDeleteSharedAndOrdinaryGroups()
    }

    @Override
    void clean() {
        if (sharingWriteCallback != null) {
            bean(DatabaseFacade.class).uninstallEntityLifeCycleCallback(SharedResourceVO.class, sharingWriteEvent, sharingWriteCallback)
        }
        SecurityGroupManagerImpl manager = bean(SecurityGroupManagerImpl.class)
        manager.dbf = bean(DatabaseFacade.class)
        manager.casf = bean(CascadeFacade.class)
        env.delete()
    }

    void testPrepareAccountsAndNics() {
        ["A", "B", "C"].each { createAccountAndSession(it) }
        l3Uuid = env.inventoryByName("l3").uuid
        secondL3Uuid = createTestNetwork("second-security-network", "192.168.200", true)
        unsuitableL3Uuid = createTestNetwork("network-without-security-service", "192.168.210", false)
        def provider = queryNetworkServiceProvider { conditions = ["type=SecurityGroup"] }[0]
        attachNetworkServiceToL3Network {
            l3NetworkUuid = l3Uuid
            networkServices = [(provider.uuid): ["SecurityGroup"]]
        }

        vmA = createOwnedVm("vm-account-a", "A", [l3Uuid, secondL3Uuid])
        vmB = createOwnedVm("vm-account-b", "B", [l3Uuid])
        createOwnedVm("vm-on-unsuitable-network", "A", [unsuitableL3Uuid])
        hiddenGroup = createSecurityGroup { name = "admin-private-protection" }
        sharedGroup = createSecurityGroup { name = "admin-shared-access" }
        ownGroup = createSecurityGroup {
            name = "account-a-rules"
            sessionId = sessions.A.uuid
        }
        addVmNicToSecurityGroup {
            securityGroupUuid = hiddenGroup.uuid
            vmNicUuids = [vmA.vmNics[0].uuid]
        }
        sharedGroup = addSecurityGroupRule {
            securityGroupUuid = sharedGroup.uuid
            rules = [new SecurityGroupRuleAO(type: "Ingress", ipVersion: 4, protocol: "TCP", dstPortRange: "22", remoteSecurityGroupUuid: hiddenGroup.uuid, description: "restricted-reference"),
                     new SecurityGroupRuleAO(type: "Ingress", ipVersion: 4, protocol: "TCP", dstPortRange: "80", description: "web-access")]
        }
        ["Specified", "Public"].each { mode ->
            def result = new SetSecurityGroupSharingAction(securityGroupUuid: ownGroup.uuid, mode: mode,
                    accountUuids: mode == "Specified" ? [accounts.B.uuid] : null, sessionId: adminSession()).call()

            assert result.error != null : "tenant-owned group must not become shared: mode=${mode}"
        }

        assert sharingSnapshot(ownGroup.uuid).empty : "rejected sharing must not create authorizations"

        user = createUser {
            name = "security-user-a"
            password = "password"
            sessionId = sessions.A.uuid
        }
        userSession = logInByUser {
            accountName = accounts.A.name
            userName = user.name
            password = "password"
        }
    }

    void testLegacySharingApisRejectSecurityGroups() {
        legacySharingL3Uuid = createTestNetwork("legacy-sharing-network", "192.168.220", false)
        def beforeGroup = sharingSnapshot(sharedGroup.uuid)
        def beforeNetwork = sharingSnapshot(legacySharingL3Uuid)

        [[sharedGroup.uuid], [legacySharingL3Uuid, sharedGroup.uuid]].each { resourceIds ->
            def result = new ShareResourceAction(resourceUuids: resourceIds, accountUuids: [accounts.A.uuid], sessionId: adminSession()).call()

            assert result.error?.globalErrorCode == "ORG_ZSTACK_NETWORK_SECURITYGROUP_10151" : "legacy sharing must reject security groups: actual=${result.error}"
            assert result.error.details.contains("APISetSecurityGroupSharingMsg") : "rejection must identify the security group sharing API: actual=${result.error.details}"
            assert sharingSnapshot(sharedGroup.uuid) == beforeGroup : "rejected legacy sharing must preserve group authorizations"
            assert sharingSnapshot(legacySharingL3Uuid) == beforeNetwork : "mixed-resource rejection must not share the network"
        }

        shareResource {
            resourceUuids = [legacySharingL3Uuid, unsuitableL3Uuid]
            accountUuids = [accounts.A.uuid]
        }

        [legacySharingL3Uuid, unsuitableL3Uuid].each { networkUuid ->
            def networkSharing = sharingSnapshot(networkUuid)
            assert networkSharing*.receiverAccountUuid == [accounts.A.uuid] && networkSharing.every { !it.toPublic } : "legacy batch sharing must grant both networks to the specified account: network=${networkUuid}, actual=${networkSharing}"
        }
    }

    void testSpecifiedShareAndBind() {
        def missingAccounts = new SetSecurityGroupSharingAction(securityGroupUuid: sharedGroup.uuid, mode: "Specified", sessionId: adminSession()).call()

        assert missingAccounts.error?.globalErrorCode == "ORG_ZSTACK_NETWORK_SECURITYGROUP_10139" : "specified sharing must require recipients: actual=${missingAccounts.error}"

        def unexpectedAccounts = new SetSecurityGroupSharingAction(securityGroupUuid: sharedGroup.uuid, mode: "Public", accountUuids: [accounts.A.uuid], sessionId: adminSession()).call()

        assert unexpectedAccounts.error?.globalErrorCode == "ORG_ZSTACK_NETWORK_SECURITYGROUP_10148" : "public sharing must reject specified recipients: actual=${unexpectedAccounts.error}"
        assert sharingSnapshot(sharedGroup.uuid).empty : "invalid sharing parameters must not create authorizations"

        setSecurityGroupSharing {
            securityGroupUuid = sharedGroup.uuid
            mode = "Specified"
            accountUuids = [accounts.A.uuid]
        }
        def recipientGroups = querySecurityGroup {
            conditions = ["uuid=${sharedGroup.uuid}"]
            sessionId = sessions.A.uuid
        }

        assert recipientGroups*.uuid == [sharedGroup.uuid] : "specified recipient must see the group"

        def otherAccountGroups = querySecurityGroup {
            conditions = ["uuid=${sharedGroup.uuid}"]
            sessionId = sessions.B.uuid
        }

        assert otherAccountGroups.empty : "other account must not see the group"

        def forbiddenTransfer = new ChangeResourceOwnerAction(resourceUuid: sharedGroup.uuid, accountUuid: accounts.B.uuid, sessionId: adminSession()).call()

        assert forbiddenTransfer.error != null : "shared group must not be transferred even without bindings"

        def unsharedBinding = new AddVmNicToSecurityGroupAction(securityGroupUuid: sharedGroup.uuid, vmNicUuids: [vmB.vmNics[0].uuid], sessionId: sessions.B.uuid).call()

        assert unsharedBinding.error != null : "unshared account must not bind a known UUID"

        def unprivilegedBinding = new AddVmNicToSecurityGroupAction(securityGroupUuid: sharedGroup.uuid, vmNicUuids: [vmA.vmNics[0].uuid], sessionId: userSession.uuid).call()

        assert unprivilegedBinding.error != null : "subaccount without write permission must not bind a group"

        def readPolicy = createPolicy {
            name = "security-user-read-permissions"
            statements = [new PolicyStatement(name: "read-security-group", effect: PolicyStatementEffect.Allow, actions: ["org.zstack.network.securitygroup.APIGetCandidateVmNicForSecurityGroupMsg"])]
            sessionId = sessions.A.uuid
        }
        attachPolicyToUser {
            userUuid = user.uuid
            policyUuid = readPolicy.uuid
            sessionId = sessions.A.uuid
        }
        def readOnlyCandidate = new GetCandidateVmNicForSecurityGroupAction(securityGroupUuid: sharedGroup.uuid, sessionId: userSession.uuid).call()

        assert readOnlyCandidate.error == null && readOnlyCandidate.value.inventories.empty : "read-only subaccount must not receive NICs it cannot bind: actual=${readOnlyCandidate.error ?: readOnlyCandidate.value.inventories}"

        def policy = createPolicy {
            name = "security-user-permissions"
            statements = [new PolicyStatement(name: "security-group-operations", effect: PolicyStatementEffect.Allow, actions: ["org.zstack.network.securitygroup.*"])]
            sessionId = sessions.A.uuid
        }
        attachPolicyToUser {
            userUuid = user.uuid
            policyUuid = policy.uuid
            sessionId = sessions.A.uuid
        }
        def candidate = new GetCandidateVmNicForSecurityGroupAction(securityGroupUuid: sharedGroup.uuid, sessionId: userSession.uuid).call()

        assert candidate.error == null : "authorized recipient must query candidate NICs: actual=${candidate.error}"
        assert candidate.value.inventories*.uuid.toSet() == vmA.vmNics*.uuid.toSet() : "candidates must contain only writable NICs on applicable networks: actual=${candidate.value.inventories*.uuid}"

        def unsharedCandidates = new GetCandidateVmNicForSecurityGroupAction(securityGroupUuid: sharedGroup.uuid, sessionId: sessions.B.uuid).call()

        assert unsharedCandidates.error != null : "unshared account must not query candidates by UUID"

        setSecurityGroupSharing {
            securityGroupUuid = sharedGroup.uuid
            mode = "Specified"
            accountUuids = [accounts.A.uuid, accounts.B.uuid]
        }
        def beforeCrossAccountBinding = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, vmB.vmNics[0].uuid)
                .select(VmNicSecurityGroupRefVO_.securityGroupUuid).listValues().sort()

        assert !beforeCrossAccountBinding.contains(sharedGroup.uuid) : "cross-account request must target an unbound group"

        def crossAccountBinding = new AddVmNicToSecurityGroupAction(securityGroupUuid: sharedGroup.uuid, vmNicUuids: [vmB.vmNics[0].uuid], sessionId: userSession.uuid).call()

        assert crossAccountBinding.error?.globalErrorCode == "ORG_ZSTACK_IDENTITY_RBAC_10007" : "shared use must not grant write permission on another account's NIC: actual=${crossAccountBinding.error}"
        assert Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, vmB.vmNics[0].uuid)
                .select(VmNicSecurityGroupRefVO_.securityGroupUuid).listValues().sort() == beforeCrossAccountBinding : "rejected cross-account request must preserve every NIC binding"

        setSecurityGroupSharing {
            securityGroupUuid = sharedGroup.uuid
            mode = "Specified"
            accountUuids = [accounts.A.uuid]
        }

        addVmNicToSecurityGroup {
            securityGroupUuid = sharedGroup.uuid
            vmNicUuids = [vmA.vmNics[0].uuid]
            sessionId = userSession.uuid
        }

        assert bound(sharedGroup.uuid, vmA.vmNics[0].uuid) : "authorized subaccount must bind its account's NIC"
        assert !bound(sharedGroup.uuid, vmB.vmNics[0].uuid) : "rejected cross-account request must not create a binding"

        def privateCandidates = new GetCandidateVmNicForSecurityGroupAction(securityGroupUuid: ownGroup.uuid, sessionId: adminSession()).call()

        assert privateCandidates.error == null && !privateCandidates.value.inventories*.uuid.contains(vmB.vmNics[0].uuid) : "admin must not receive NICs whose account cannot use another account's private group"
    }

    void testLegacyRevokePreservesSharingAndBindings() {
        def beforeGroup = sharingSnapshot(sharedGroup.uuid)
        def beforeNetworks = [legacySharingL3Uuid, unsuitableL3Uuid].collectEntries { [(it): sharingSnapshot(it)] }
        def beforeBindings = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, vmA.vmNics[0].uuid)
                .list().collect { [it.uuid, it.vmNicUuid, it.securityGroupUuid, it.priority] }.sort { it[0] }

        assert bound(sharedGroup.uuid, vmA.vmNics[0].uuid) : "legacy recall must target a bound shared group"

        [[sharedGroup.uuid], [legacySharingL3Uuid, unsuitableL3Uuid, sharedGroup.uuid]].each { resourceIds ->
            def result = new RevokeResourceSharingAction(resourceUuids: resourceIds, all: true, sessionId: adminSession()).call()

            assert result.error?.globalErrorCode == "ORG_ZSTACK_NETWORK_SECURITYGROUP_10151" : "legacy recall must reject security groups: actual=${result.error}"
            assert result.error.details.contains("APISetSecurityGroupSharingMsg") : "rejection must identify the security group sharing API: actual=${result.error.details}"
            assert sharingSnapshot(sharedGroup.uuid) == beforeGroup : "rejected legacy recall must preserve every group authorization field"
            assert [legacySharingL3Uuid, unsuitableL3Uuid].collectEntries { [(it): sharingSnapshot(it)] } == beforeNetworks : "mixed-resource rejection must preserve every network authorization"
            assert Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, vmA.vmNics[0].uuid)
                    .list().collect { [it.uuid, it.vmNicUuid, it.securityGroupUuid, it.priority] }.sort { it[0] } == beforeBindings : "rejected legacy recall must preserve NIC bindings and priorities"
        }

        revokeResourceSharing {
            resourceUuids = [legacySharingL3Uuid, unsuitableL3Uuid]
            accountUuids = [accounts.A.uuid]
        }

        assert [legacySharingL3Uuid, unsuitableL3Uuid].every { sharingSnapshot(it).empty } : "legacy batch recall must remove both network authorizations"
        assert sharingSnapshot(sharedGroup.uuid) == beforeGroup : "network recall must preserve security group sharing"
    }

    void testSharedGroupStaysReadOnly() {
        def before = querySecurityGroup { conditions = ["uuid=${sharedGroup.uuid}"] }[0]
        def rules = before.rules.findAll { it.priority > 0 }.sort { it.priority }
        List actions = [
                new UpdateSecurityGroupAction(uuid: sharedGroup.uuid, name: "forbidden-name"),
                new ChangeSecurityGroupStateAction(uuid: sharedGroup.uuid, stateEvent: "disable"),
                new UpdateSecurityGroupRulePriorityAction(securityGroupUuid: sharedGroup.uuid, type: "Ingress", rules: [new SecurityGroupRulePriorityAO(ruleUuid: rules[0].uuid, priority: 2), new SecurityGroupRulePriorityAO(ruleUuid: rules[1].uuid, priority: 1)]),
                new AddSecurityGroupRuleAction(securityGroupUuid: sharedGroup.uuid, rules: [new SecurityGroupRuleAO(type: "Ingress", ipVersion: 4, protocol: "TCP", dstPortRange: "443")]),
                new DeleteSecurityGroupRuleAction(ruleUuids: [rules[0].uuid]),
                new ChangeSecurityGroupRuleStateAction(securityGroupUuid: sharedGroup.uuid, ruleUuids: [rules[0].uuid], state: "Disabled"),
                new ChangeSecurityGroupRuleAction(uuid: rules[0].uuid, description: "forbidden-edit"),
                new SetSecurityGroupSharingAction(securityGroupUuid: sharedGroup.uuid, mode: "Public")
        ]
        actions.each { action ->
            action.sessionId = userSession.uuid
            def result = action.call()
            String expectedCode = action instanceof DeleteSecurityGroupRuleAction || action instanceof ChangeSecurityGroupRuleStateAction || action instanceof ChangeSecurityGroupRuleAction
                    ? "ORG_ZSTACK_NETWORK_SECURITYGROUP_10134" : "ORG_ZSTACK_IDENTITY_RBAC_10007"

            assert result.error?.globalErrorCode == expectedCode : "shared group management must fail permission validation: action=${action.class.simpleName}, expected=${expectedCode}, actual=${result.error}"
        }
        def after = querySecurityGroup { conditions = ["uuid=${sharedGroup.uuid}"] }[0]

        assert after.name == before.name && after.state == before.state : "rejected management must preserve group attributes"
        assert ruleSnapshot(after.rules) == ruleSnapshot(before.rules) : "rejected management must preserve every rule"
    }

    void testReadSharedGroupAndResources() {
        addVmNicToSecurityGroup {
            securityGroupUuid = sharedGroup.uuid
            vmNicUuids = [vmA.vmNics[1].uuid]
            sessionId = sessions.A.uuid
        }
        setSecurityGroupSharing {
            securityGroupUuid = sharedGroup.uuid
            mode = "Specified"
            accountUuids = [accounts.A.uuid, accounts.B.uuid]
        }
        addVmNicToSecurityGroup {
            securityGroupUuid = sharedGroup.uuid
            vmNicUuids = [vmB.vmNics[0].uuid]
            sessionId = sessions.B.uuid
        }
        def adminBindings = new QueryVmNicInSecurityGroupAction(conditions: ["securityGroupUuid=${sharedGroup.uuid}".toString()],
                sessionId: adminSession()).call().throwExceptionIfError().value.inventories

        assert adminBindings*.vmNicUuid.toSet() == (vmA.vmNics*.uuid + vmB.vmNics*.uuid).toSet() : "admin must see the shared group's bindings across accounts"

        def adminHiddenRules = querySecurityGroupRule {
            conditions = ["securityGroupUuid=${hiddenGroup.uuid}"]
            sessionId = adminSession()
        }

        assert adminHiddenRules*.uuid.toSet() == hiddenGroup.rules*.uuid.toSet() : "admin must see private group rules when identity filtering is skipped"

        List queries = [
                [action: new QueryVmInstanceAction(), conditions: ["vmNics.securityGroup.uuid=${sharedGroup.uuid}"], expected: [vmA.uuid]],
                [action: new QueryVmNicAction(), conditions: ["securityGroup.uuid=${sharedGroup.uuid}"], expected: vmA.vmNics*.uuid],
                [action: new QueryVmNicInSecurityGroupAction(), conditions: ["securityGroupUuid=${sharedGroup.uuid}"], expected: vmA.vmNics*.uuid]
        ]
        queries.each { query ->
            def action = query.action
            action.sessionId = sessions.A.uuid
            action.conditions = query.conditions.collect { it.toString() }
            def result = action.call()

            assert result.error == null : "recipient must query its related resources: action=${action.class.simpleName}, actual=${result.error}"

            def ids = action instanceof QueryVmNicInSecurityGroupAction ? result.value.inventories*.vmNicUuid : result.value.inventories*.uuid

            assert ids.toSet() == query.expected.toSet() : "related resources must exclude other accounts: action=${action.class.simpleName}, actual=${ids}"

            action.count = true
            def count = action.call().throwExceptionIfError().value.total

            assert count == query.expected.size() : "count must use the visible resource range: action=${action.class.simpleName}"

            action.count = false
            action.replyWithCount = true
            action.limit = 1
            def page = action.call().throwExceptionIfError().value

            assert page.inventories.size() == 1 && page.total == query.expected.size() : "pagination total must match visible resources: action=${action.class.simpleName}, actual=${page.total}"
        }
        [
                [conditions: [], expectedNics: vmA.vmNics*.uuid],
                [conditions: ["vmNicUuid=${vmA.vmNics[0].uuid}"], expectedNics: [vmA.vmNics[0].uuid]]
        ].each { query ->
            def result = new QueryVmNicInSecurityGroupAction(conditions: query.conditions.collect { it.toString() }, sessionId: sessions.A.uuid, replyWithCount: true).call().throwExceptionIfError().value
            def bindings = result.inventories.collect { [it.vmNicUuid, it.securityGroupUuid] }.toSet()

            assert bindings == query.expectedNics.collect { [it, sharedGroup.uuid] }.toSet() : "binding query must return every visible binding and exclude hidden groups: actual=${bindings}"
            assert result.total == query.expectedNics.size() : "binding total must match the visible NICs: actual=${result.total}"
        }
        def restrictedGroups = querySecurityGroup {
            conditions = ["uuid=${hiddenGroup.uuid}"]
            sessionId = sessions.A.uuid
        }

        assert restrictedGroups.empty : "restricted referenced group must not be readable"

        [
                [name: "owned-and-shared", conditions: [], expected: (sharedGroup.rules + ownGroup.rules)*.uuid],
                [name: "shared", conditions: ["securityGroupUuid=${sharedGroup.uuid}"], expected: sharedGroup.rules*.uuid],
                [name: "hidden", conditions: ["securityGroupUuid=${hiddenGroup.uuid}"], expected: []]
        ].each { query ->
            def action = new QuerySecurityGroupRuleAction(conditions: query.conditions.collect { it.toString() }, sessionId: sessions.A.uuid)
            def rules = action.call().throwExceptionIfError().value.inventories

            assert rules*.uuid.toSet() == query.expected.toSet() : "rule visibility must follow group permissions: query=${query.name}, actual=${rules*.uuid}"

            if (query.name == "shared") {
                assert rules.find { it.remoteSecurityGroupUuid == hiddenGroup.uuid } : "invisible reference must not remove its visible source rule"
            }
            action.count = true
            def count = action.call().throwExceptionIfError().value.total

            assert count == query.expected.size() : "rule count must use the visible group range: query=${query.name}, actual=${count}"

            action.count = false
            action.replyWithCount = true
            action.sortBy = "uuid"
            action.sortDirection = "asc"
            action.limit = 1
            def page = action.call().throwExceptionIfError().value

            assert page.inventories*.uuid == query.expected.sort().take(1) : "rule page must contain only visible rules: query=${query.name}, actual=${page.inventories*.uuid}"
            assert page.total == query.expected.size() : "rule page total must match visible rules: query=${query.name}, actual=${page.total}"
        }
        deleteVmNicFromSecurityGroup {
            securityGroupUuid = sharedGroup.uuid
            vmNicUuids = [vmB.vmNics[0].uuid]
            sessionId = sessions.B.uuid
        }
    }

    void testBatchRemoteReferencesRequireAllPermissions() {
        SecurityGroupInventory remoteGroup = createSecurityGroup { name = "batch-remote-shared-group" }
        setSecurityGroupSharing {
            securityGroupUuid = remoteGroup.uuid
            mode = "Specified"
            accountUuids = [accounts.A.uuid]
        }
        SecurityGroupInventory sourceGroup = createSecurityGroup {
            name = "account-a-batch-reference-rules"
            sessionId = sessions.A.uuid
        }
        sourceGroup = addSecurityGroupRule {
            securityGroupUuid = sourceGroup.uuid
            rules = [new SecurityGroupRuleAO(type: "Ingress", ipVersion: 4, protocol: "TCP", dstPortRange: "80", description: "batch-web-reference"),
                     new SecurityGroupRuleAO(type: "Ingress", ipVersion: 4, protocol: "TCP", dstPortRange: "443", description: "batch-https-reference")]
            remoteSecurityGroupUuids = [sharedGroup.uuid, remoteGroup.uuid]
            sessionId = sessions.A.uuid
        }
        def addedRules = sourceGroup.rules.findAll { it.description in ["batch-web-reference", "batch-https-reference"] }

        assert addedRules.size() == 4 : "batch references must expand both base rules for both shared groups: actual=${addedRules.size()}"
        assert addedRules.collect { [it.remoteSecurityGroupUuid, it.dstPortRange] }.toSet() ==
                [[sharedGroup.uuid, "80"], [sharedGroup.uuid, "443"], [remoteGroup.uuid, "80"], [remoteGroup.uuid, "443"]].toSet() : "batch references must preserve each remote group and port pair"

        def before = ruleSnapshot(sourceGroup.rules)
        def denied = new AddSecurityGroupRuleAction(securityGroupUuid: sourceGroup.uuid,
                rules: [new SecurityGroupRuleAO(type: "Ingress", ipVersion: 4, protocol: "TCP", dstPortRange: "8443", description: "rejected-batch-reference")],
                remoteSecurityGroupUuids: [sharedGroup.uuid, remoteGroup.uuid, hiddenGroup.uuid], sessionId: sessions.A.uuid).call()

        assert denied.error?.globalErrorCode == "ORG_ZSTACK_NETWORK_SECURITYGROUP_10133" : "one unauthorized remote group must reject the complete batch: actual=${denied.error}"

        def after = querySecurityGroupRule {
            conditions = ["securityGroupUuid=${sourceGroup.uuid}"]
            sessionId = sessions.A.uuid
        }

        assert ruleSnapshot(after) == before : "rejected batch must preserve every rule and create no partial references"

        deleteSecurityGroup {
            uuid = sourceGroup.uuid
            sessionId = sessions.A.uuid
        }
        setSecurityGroupSharing {
            securityGroupUuid = remoteGroup.uuid
            mode = "None"
        }
        deleteSecurityGroup { uuid = remoteGroup.uuid }
    }

    void testReplaceSpecifiedSharing() {
        def before = sharingSnapshot(sharedGroup.uuid)
        def accountARecord = before.find { it.receiverAccountUuid == accounts.A.uuid }
        setSecurityGroupSharing {
            securityGroupUuid = sharedGroup.uuid
            mode = "Specified"
            accountUuids = [accounts.A.uuid, accounts.C.uuid]
        }

        assert sharingSnapshot(sharedGroup.uuid).find { it.receiverAccountUuid == accounts.A.uuid } == accountARecord : "unchanged account authorization must retain its ID and fields"
        assert bound(sharedGroup.uuid, vmA.vmNics[0].uuid) : "retained sharing must preserve NIC bindings"

        SecurityGroupInventory rollbackGroup = createSecurityGroup { name = "sharing-write-rollback" }
        DatabaseFacade database = bean(DatabaseFacade.class)
        [
                [event: EntityEvent.PRE_PERSIST, previous: [accounts.A.uuid], target: [accounts.B.uuid, accounts.C.uuid]],
                [event: EntityEvent.PRE_REMOVE, previous: [accounts.A.uuid, accounts.B.uuid], target: [accounts.C.uuid]]
        ].each { scenario ->
            setSecurityGroupSharing {
                securityGroupUuid = rollbackGroup.uuid
                mode = "Specified"
                accountUuids = scenario.previous
            }
            def snapshot = sharingSnapshot(rollbackGroup.uuid)
            AtomicInteger processed = new AtomicInteger()
            AtomicBoolean injected = new AtomicBoolean(false)
            EntityLifeCycleCallback callback = { EntityEvent event, Object entity ->
                SharedResourceVO record = entity as SharedResourceVO
                if (record.resourceUuid == rollbackGroup.uuid && processed.incrementAndGet() == 2) {
                    injected.set(true)
                    throw new OperationFailureException(new ErrorCode("TEST.SHARING.WRITE", "injected authorization batch failure"))
                }
            } as EntityLifeCycleCallback
            sharingWriteEvent = scenario.event
            sharingWriteCallback = callback
            database.installEntityLifeCycleCallback(SharedResourceVO.class, sharingWriteEvent, sharingWriteCallback)
            def result = new SetSecurityGroupSharingAction(securityGroupUuid: rollbackGroup.uuid, mode: "Specified", accountUuids: scenario.target, sessionId: adminSession()).call()
            database.uninstallEntityLifeCycleCallback(SharedResourceVO.class, sharingWriteEvent, sharingWriteCallback)
            sharingWriteCallback = null

            assert injected.get() : "failure must occur on the second authorization in the batch: event=${scenario.event}"
            assert result.error != null : "failed authorization batch must fail the sharing API: event=${scenario.event}"
            assert sharingSnapshot(rollbackGroup.uuid) == snapshot : "failed batch must preserve original IDs and every field: event=${scenario.event}"
        }

        setSecurityGroupSharing {
            securityGroupUuid = rollbackGroup.uuid
            mode = "Specified"
            accountUuids = [accounts.A.uuid, accounts.B.uuid]
        }
        def snapshot = sharingSnapshot(rollbackGroup.uuid)
        AtomicBoolean injected = new AtomicBoolean(false)
        SecurityGroupManagerImpl manager = bean(SecurityGroupManagerImpl.class)
        DatabaseFacade original = manager.dbf
        manager.dbf = Proxy.newProxyInstance(DatabaseFacade.class.classLoader, [DatabaseFacade.class] as Class[], { Object proxy, Method method, Object[] args ->
            def result = ReflectionUtils.invokeMethod(method, original, args)
            if (method.name == "persistCollection" && args?.length == 1 && (args[0] as Collection).any {
                it instanceof SharedResourceVO && it.resourceUuid == rollbackGroup.uuid && it.receiverAccountUuid == accounts.C.uuid
            } && injected.compareAndSet(false, true)) {
                assert Q.New(SharedResourceVO.class).eq(SharedResourceVO_.resourceUuid, rollbackGroup.uuid)
                        .eq(SharedResourceVO_.receiverAccountUuid, accounts.C.uuid).isExists() : "failure must follow the committed new authorization"
                throw new OperationFailureException(new ErrorCode("TEST.SHARING.WRITE", "injected committed authorization failure"))
            }
            return result
        } as InvocationHandler) as DatabaseFacade
        def committedFailure = new SetSecurityGroupSharingAction(securityGroupUuid: rollbackGroup.uuid, mode: "Specified", accountUuids: [accounts.A.uuid, accounts.C.uuid], sessionId: adminSession()).call()
        manager.dbf = original

        assert injected.get() : "write failure must be injected after the new authorization commits"
        assert committedFailure.error?.code == "TEST.SHARING.WRITE" : "sharing must return the original write error: actual=${committedFailure.error}"
        assert sharingSnapshot(rollbackGroup.uuid) == snapshot : "failed setting must remove the committed addition and retain every original field"
        setSecurityGroupSharing {
            securityGroupUuid = rollbackGroup.uuid
            mode = "Specified"
            accountUuids = [accounts.A.uuid, accounts.C.uuid]
        }

        assert sharingSnapshot(rollbackGroup.uuid)*.receiverAccountUuid.toSet() == [accounts.A.uuid, accounts.C.uuid].toSet() : "setting must succeed after clearing the injected failure"
    }

    void testRecallWithNicReference() {
        setSecurityGroupSharing {
            securityGroupUuid = sharedGroup.uuid
            mode = "Specified"
            accountUuids = [accounts.A.uuid, accounts.B.uuid]
        }
        def before = sharingSnapshot(sharedGroup.uuid)
        def denied = new SetSecurityGroupSharingAction(securityGroupUuid: sharedGroup.uuid, mode: "Specified", accountUuids: [accounts.B.uuid, accounts.C.uuid], sessionId: adminSession()).call()

        assert denied.error?.globalErrorCode == "ORG_ZSTACK_NETWORK_SECURITYGROUP_10136" : "using account must block recall: actual=${denied.error}"
        assert sharingSnapshot(sharedGroup.uuid) == before : "reference rejection must preserve all authorizations"
        assert !Q.New(SharedResourceVO.class).eq(SharedResourceVO_.resourceUuid, sharedGroup.uuid)
                .eq(SharedResourceVO_.receiverAccountUuid, accounts.C.uuid).isExists() : "rejected scope change must not authorize the new account"
        setSecurityGroupSharing {
            securityGroupUuid = sharedGroup.uuid
            mode = "Specified"
            accountUuids = [accounts.A.uuid]
        }
        stopVmInstance { uuid = vmA.uuid }
        def stoppedRecall = new SetSecurityGroupSharingAction(securityGroupUuid: sharedGroup.uuid, mode: "None", sessionId: adminSession()).call()

        assert stoppedRecall.error?.globalErrorCode == "ORG_ZSTACK_NETWORK_SECURITYGROUP_10136" : "stopped VM must still block recall"

        deleteVmNicFromSecurityGroup {
            securityGroupUuid = sharedGroup.uuid
            vmNicUuids = vmA.vmNics*.uuid
            sessionId = sessions.A.uuid
        }
        setSecurityGroupSharing {
            securityGroupUuid = sharedGroup.uuid
            mode = "None"
        }

        assert sharingSnapshot(sharedGroup.uuid).empty : "recall must remove sharing after all NICs detach"

        def recalledBinding = new AddVmNicToSecurityGroupAction(securityGroupUuid: sharedGroup.uuid, vmNicUuids: [vmA.vmNics[1].uuid], sessionId: sessions.A.uuid).call()

        assert recalledBinding.error != null : "recalled group must reject later binding"
    }

    void testRecallWithRuleReference() {
        setSecurityGroupSharing {
            securityGroupUuid = sharedGroup.uuid
            mode = "Specified"
            accountUuids = [accounts.A.uuid]
        }
        ownGroup = addSecurityGroupRule {
            securityGroupUuid = ownGroup.uuid
            rules = [new SecurityGroupRuleAO(type: "Ingress", ipVersion: 4, protocol: "TCP", dstPortRange: "443", remoteSecurityGroupUuid: sharedGroup.uuid, description: "shared-reference")]
            sessionId = sessions.A.uuid
        }
        def reference = ownGroup.rules.find { it.description == "shared-reference" }
        def referencedRecall = new SetSecurityGroupSharingAction(securityGroupUuid: sharedGroup.uuid, mode: "None", sessionId: adminSession()).call()

        assert referencedRecall.error?.globalErrorCode == "ORG_ZSTACK_NETWORK_SECURITYGROUP_10149" : "external rule must block recall"

        deleteSecurityGroupRule {
            ruleUuids = [reference.uuid]
            sessionId = sessions.A.uuid
        }
        ownGroup = addSecurityGroupRule {
            securityGroupUuid = ownGroup.uuid
            rules = [new SecurityGroupRuleAO(type: "Ingress", ipVersion: 4, protocol: "TCP", dstPortRange: "443", srcIpRange: "192.168.30.0/24", description: "changed-reference")]
            sessionId = sessions.A.uuid
        }
        def addressRule = ownGroup.rules.find { it.description == "changed-reference" }
        def changed = changeSecurityGroupRule {
            uuid = addressRule.uuid
            remoteSecurityGroupUuid = sharedGroup.uuid
            sessionId = sessions.A.uuid
        }

        assert changed.remoteSecurityGroupUuid == sharedGroup.uuid && !changed.srcIpRange : "owned rule must accept an authorized remote group"

        def denied = new ChangeSecurityGroupRuleAction(uuid: addressRule.uuid, remoteSecurityGroupUuid: hiddenGroup.uuid, sessionId: sessions.A.uuid).call()

        assert denied.error?.globalErrorCode == "ORG_ZSTACK_NETWORK_SECURITYGROUP_10133" : "rule change must reject an unauthorized remote group: actual=${denied.error}"

        def unchanged = querySecurityGroupRule {
            conditions = ["uuid=${addressRule.uuid}"]
            sessionId = sessions.A.uuid
        }[0]

        assert ruleSnapshot([unchanged]) == ruleSnapshot([changed]) : "rejected remote change must preserve the original rule"

        changeSecurityGroupRuleState {
            securityGroupUuid = ownGroup.uuid
            ruleUuids = [addressRule.uuid]
            state = "Disabled"
            sessionId = sessions.A.uuid
        }
        def disabledReferenceRecall = new SetSecurityGroupSharingAction(securityGroupUuid: sharedGroup.uuid, mode: "None", sessionId: adminSession()).call()

        assert disabledReferenceRecall.error?.globalErrorCode == "ORG_ZSTACK_NETWORK_SECURITYGROUP_10149" : "disabled external rule must still block recall"

        deleteSecurityGroupRule {
            ruleUuids = [addressRule.uuid]
            sessionId = sessions.A.uuid
        }
        setSecurityGroupSharing {
            securityGroupUuid = sharedGroup.uuid
            mode = "None"
        }

        assert sharingSnapshot(sharedGroup.uuid).empty : "system self-references must not block recall"
    }

    void testPublicShareAndReduceScope() {
        setSecurityGroupSharing {
            securityGroupUuid = sharedGroup.uuid
            mode = "Public"
        }
        createAccountAndSession("D")
        VmInstanceInventory vmD = createOwnedVm("vm-account-d", "D", [l3Uuid])
        def newAccountGroups = querySecurityGroup {
            conditions = ["uuid=${sharedGroup.uuid}"]
            sessionId = sessions.D.uuid
        }

        assert newAccountGroups*.uuid == [sharedGroup.uuid] : "account created after public sharing must see the group"

        addVmNicToSecurityGroup {
            securityGroupUuid = sharedGroup.uuid
            vmNicUuids = [vmD.vmNics[0].uuid]
            sessionId = sessions.D.uuid
        }
        def before = sharingSnapshot(sharedGroup.uuid)
        def scopeReduction = new SetSecurityGroupSharingAction(securityGroupUuid: sharedGroup.uuid, mode: "Specified", accountUuids: [accounts.A.uuid], sessionId: adminSession()).call()

        assert scopeReduction.error != null : "public reduction must retain every using account"
        assert sharingSnapshot(sharedGroup.uuid) == before : "rejected public reduction must retain the complete public record"

        setSecurityGroupSharing {
            securityGroupUuid = sharedGroup.uuid
            mode = "Specified"
            accountUuids = [accounts.D.uuid]
        }

        assert bound(sharedGroup.uuid, vmD.vmNics[0].uuid) : "public-to-specified change must preserve authorized bindings"

        def excludedAccountGroups = querySecurityGroup {
            conditions = ["uuid=${sharedGroup.uuid}"]
            sessionId = sessions.A.uuid
        }

        assert excludedAccountGroups.empty : "account excluded from specified sharing must lose visibility"

        def completeRecall = new SetSecurityGroupSharingAction(securityGroupUuid: sharedGroup.uuid, mode: "None", sessionId: adminSession()).call()

        assert completeRecall.error != null : "remaining binding must block complete recall"

        deleteVmNicFromSecurityGroup {
            securityGroupUuid = sharedGroup.uuid
            vmNicUuids = [vmD.vmNics[0].uuid]
            sessionId = sessions.D.uuid
        }
        setSecurityGroupSharing {
            securityGroupUuid = sharedGroup.uuid
            mode = "None"
        }
        destroyVmInstance { uuid = vmD.uuid }
    }

    void testAttachSharedSecurityGroup() {
        SecurityGroupInventory group = createSecurityGroup { name = "attach-shared-security-group" }
        String selectedGroup = String.format("l3::%s::SecurityGroupUuids::%s", secondL3Uuid, group.uuid)
        Closure nicIds = {
            Q.New(VmNicVO.class).eq(VmNicVO_.vmInstanceUuid, vmB.uuid).orderByAsc(VmNicVO_.uuid).select(VmNicVO_.uuid).listValues()
        }
        Closure refIds = {
            Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmInstanceUuid, vmB.uuid)
                    .orderByAsc(VmNicSecurityGroupRefVO_.uuid).select(VmNicSecurityGroupRefVO_.uuid).listValues()
        }
        List beforeNics = nicIds()
        List beforeRefs = refIds()
        shareResource {
            resourceUuids = [secondL3Uuid]
            accountUuids = [accounts.B.uuid]
            toPublic = false
        }

        // Stored selections are consumed during attachment even when the request contains no tags.
        def storedTag = createSystemTag {
            resourceUuid = vmB.uuid
            resourceType = VmInstanceVO.simpleName
            tag = selectedGroup
            sessionId = sessions.B.uuid
        }
        def storedDenied = new AttachL3NetworkToVmAction(vmInstanceUuid: vmB.uuid, l3NetworkUuid: secondL3Uuid, sessionId: sessions.B.uuid).call()

        assert storedDenied.error?.globalErrorCode == "ORG_ZSTACK_NETWORK_SECURITYGROUP_10133" : "stored private selection must reject attachment: actual=${storedDenied.error}"
        assert nicIds() == beforeNics && refIds() == beforeRefs : "rejected stored selection must not create a NIC or binding"

        deleteTag {
            uuid = storedTag.uuid
            sessionId = sessions.B.uuid
        }

        def denied = new AttachL3NetworkToVmAction(vmInstanceUuid: vmB.uuid, l3NetworkUuid: secondL3Uuid,
                systemTags: [selectedGroup], sessionId: sessions.B.uuid).call()

        assert denied.error?.globalErrorCode == "ORG_ZSTACK_NETWORK_SECURITYGROUP_10133" : "unshared request selection must reject attachment: actual=${denied.error}"
        assert nicIds() == beforeNics && refIds() == beforeRefs : "rejected request selection must not create a NIC or binding"

        setSecurityGroupSharing {
            securityGroupUuid = group.uuid
            mode = "Specified"
            accountUuids = [accounts.B.uuid]
        }
        VmInstanceInventory attached = attachL3NetworkToVm {
            vmInstanceUuid = vmB.uuid
            l3NetworkUuid = secondL3Uuid
            systemTags = [selectedGroup]
            sessionId = sessions.B.uuid
        }
        def nic = attached.vmNics.find { it.l3NetworkUuid == secondL3Uuid }

        assert nic != null && !beforeNics.contains(nic.uuid) : "shared selection must create a NIC on the requested L3"
        assert bound(group.uuid, nic.uuid) : "the new NIC must bind the selected shared group"

        detachL3NetworkFromVm {
            vmNicUuid = nic.uuid
            sessionId = sessions.B.uuid
        }

        assert nicIds() == beforeNics && refIds() == beforeRefs : "detaching the new NIC must remove its shared group reference"

        setSecurityGroupSharing {
            securityGroupUuid = group.uuid
            mode = "None"
        }

        assert sharingSnapshot(group.uuid).empty : "recall must succeed after the attached NIC is removed"

        def recalled = new AttachL3NetworkToVmAction(vmInstanceUuid: vmB.uuid, l3NetworkUuid: secondL3Uuid,
                systemTags: [selectedGroup], sessionId: sessions.B.uuid).call()

        assert recalled.error?.globalErrorCode == "ORG_ZSTACK_NETWORK_SECURITYGROUP_10133" : "recalled selection must reject a later attachment: actual=${recalled.error}"
        assert nicIds() == beforeNics && refIds() == beforeRefs : "recalled selection must not create a NIC or binding"

        revokeResourceSharing {
            resourceUuids = [secondL3Uuid]
            accountUuids = [accounts.B.uuid]
        }
    }

    void testDeleteFailureIsReported() {
        SecurityGroupInventory group = createSecurityGroup { name = "shared-delete-failures" }
        setSecurityGroupSharing {
            securityGroupUuid = group.uuid
            mode = "Public"
        }
        SecurityGroupManagerImpl manager = bean(SecurityGroupManagerImpl.class)
        CascadeFacade original = manager.casf
        AtomicBoolean intercepted = new AtomicBoolean(false)
        manager.casf = Proxy.newProxyInstance(CascadeFacade.class.classLoader, [CascadeFacade.class] as Class[], { Object proxy, Method method, Object[] args ->
            if (method.name == "asyncCascade" && args.length == 4 && args[0] == CascadeConstant.DELETION_DELETE_CODE && args[1] == SecurityGroupVO.simpleName) {
                intercepted.set(true)
                (args[3] as Completion).fail(new ErrorCode("TEST.CASCADE.DELETE", "injected child cascade failure"))
                return null
            }
            return ReflectionUtils.invokeMethod(method, original, args)
        } as InvocationHandler) as CascadeFacade
        def cascadeFailure = new DeleteSecurityGroupAction(uuid: group.uuid, sessionId: adminSession()).call()
        manager.casf = original

        assert intercepted.get() && cascadeFailure.error?.code == "TEST.CASCADE.DELETE" : "API must return the child cascade failure: actual=${cascadeFailure.error}"
        def survivingGroups = querySecurityGroup { conditions = ["uuid=${group.uuid}"] }

        assert survivingGroups.size() == 1 : "cascade failure must leave the group"

        env.message(RemoveVmNicFromSecurityGroupMsg.class) { RemoveVmNicFromSecurityGroupMsg msg, CloudBus bus ->
            def reply = new RemoveVmNicFromSecurityGroupReply()
            reply.error = new ErrorCode("TEST.DIRECT.DELETE", "injected direct deletion failure")
            bus.reply(msg, reply)
        }
        def directFailure = new DeleteSecurityGroupAction(uuid: group.uuid, sessionId: adminSession()).call()
        env.revokeMessage(RemoveVmNicFromSecurityGroupMsg.class, null)

        assert directFailure.error?.code == "TEST.DIRECT.DELETE" : "API must return direct deletion's completion failure: actual=${directFailure.error}"
        deleteSecurityGroup { uuid = group.uuid }
        def deletedGroups = querySecurityGroup { conditions = ["uuid=${group.uuid}"] }

        assert deletedGroups.empty : "deletion must complete after clearing failures and releasing the SG chain"
    }

    void testDeleteSharedAndOrdinaryGroups() {
        ["Specified", "Public"].each { sharingMode ->
            SecurityGroupInventory group = createSecurityGroup { name = "delete-${sharingMode}" }
            setSecurityGroupSharing {
                securityGroupUuid = group.uuid
                mode = sharingMode
                accountUuids = sharingMode == "Specified" ? [accounts.B.uuid] : null
            }
            addVmNicToSecurityGroup {
                securityGroupUuid = group.uuid
                vmNicUuids = [vmB.vmNics[0].uuid]
                sessionId = sessions.B.uuid
            }
            SecurityGroupInventory source = createSecurityGroup {
                name = "delete-reference-${sharingMode}"
                sessionId = sessions.B.uuid
            }
            source = addSecurityGroupRule {
                securityGroupUuid = source.uuid
                rules = [new SecurityGroupRuleAO(type: "Ingress", ipVersion: 4, protocol: "TCP", dstPortRange: "22", remoteSecurityGroupUuid: group.uuid, description: "delete-blocker")]
                sessionId = sessions.B.uuid
            }
            ["Permissive", "Enforcing"].each { deletionMode ->
                def nicReferencedDelete = new DeleteSecurityGroupAction(uuid: group.uuid, deleteMode: deletionMode, sessionId: adminSession()).call()

                assert nicReferencedDelete.error?.globalErrorCode == "ORG_ZSTACK_NETWORK_SECURITYGROUP_10137" : "shared group with NIC references must reject ${deletionMode} deletion"
            }
            deleteVmNicFromSecurityGroup {
                securityGroupUuid = group.uuid
                vmNicUuids = [vmB.vmNics[0].uuid]
                sessionId = sessions.B.uuid
            }
            ["Permissive", "Enforcing"].each { deletionMode ->
                def ruleReferencedDelete = new DeleteSecurityGroupAction(uuid: group.uuid, deleteMode: deletionMode, sessionId: adminSession()).call()

                assert ruleReferencedDelete.error?.globalErrorCode == "ORG_ZSTACK_NETWORK_SECURITYGROUP_10150" : "shared group with an external rule must reject ${deletionMode} deletion"
            }
            deleteSecurityGroupRule {
                ruleUuids = [source.rules.find { it.description == "delete-blocker" }.uuid]
                sessionId = sessions.B.uuid
            }
            deleteSecurityGroup {
                uuid = group.uuid
                deleteMode = sharingMode == "Specified" ? "Permissive" : "Enforcing"
            }
            def deletedSharedGroups = querySecurityGroup { conditions = ["uuid=${group.uuid}"] }

            assert deletedSharedGroups.empty : "unreferenced shared group must be deletable: mode=${sharingMode}"
        }

        SecurityGroupInventory ordinary = createSecurityGroup {
            name = "ordinary-auto-detach"
            sessionId = sessions.B.uuid
        }
        addVmNicToSecurityGroup {
            securityGroupUuid = ordinary.uuid
            vmNicUuids = [vmB.vmNics[0].uuid]
            sessionId = sessions.B.uuid
        }
        SecurityGroupInventory source = createSecurityGroup {
            name = "ordinary-reference"
            sessionId = sessions.B.uuid
        }
        source = addSecurityGroupRule {
            securityGroupUuid = source.uuid
            rules = [new SecurityGroupRuleAO(type: "Ingress", ipVersion: 4, protocol: "TCP", dstPortRange: "22", remoteSecurityGroupUuid: ordinary.uuid, description: "ordinary-reference")]
            sessionId = sessions.B.uuid
        }
        deleteSecurityGroup {
            uuid = ordinary.uuid
            sessionId = sessions.B.uuid
        }

        assert !bound(ordinary.uuid, vmB.vmNics[0].uuid) : "ordinary deletion must still detach NICs"

        def remainingReferences = querySecurityGroupRule { conditions = ["remoteSecurityGroupUuid=${ordinary.uuid}"] }

        assert remainingReferences.empty : "ordinary deletion must still remove external rule references"

        createAccountAndSession("Cascade")
        SecurityGroupInventory cascadeGroup = createSecurityGroup {
            name = "account-cascade-group"
            sessionId = sessions.Cascade.uuid
        }
        VmInstanceInventory cascadeVm = createOwnedVm("account-cascade-vm", "Cascade", [l3Uuid])
        addVmNicToSecurityGroup {
            securityGroupUuid = cascadeGroup.uuid
            vmNicUuids = [cascadeVm.vmNics[0].uuid]
            sessionId = sessions.Cascade.uuid
        }
        deleteAccount { uuid = accounts.Cascade.uuid }
        def cascadeGroups = querySecurityGroup { conditions = ["uuid=${cascadeGroup.uuid}"] }

        assert cascadeGroups.empty : "account cascade must retain its internal SG deletion path"
    }

    private void createAccountAndSession(String label) {
        accounts[label] = createAccount {
            name = "shared-security-${label.toLowerCase()}"
            password = "password"
        }
        sessions[label] = logInByAccount {
            accountName = accounts[label].name
            password = "password"
        }
    }

    private String createTestNetwork(String networkName, String prefix, boolean securityService) {
        def network = createL3Network {
            name = networkName
            l2NetworkUuid = env.inventoryByName("l2").uuid
            type = "L3BasicNetwork"
        }
        addIpRange {
            name = "${networkName}-ips"
            l3NetworkUuid = network.uuid
            startIp = "${prefix}.10"
            endIp = "${prefix}.100"
            gateway = "${prefix}.1"
            netmask = "255.255.255.0"
        }
        def flat = queryNetworkServiceProvider { conditions = ["type=Flat"] }[0]
        attachNetworkServiceToL3Network {
            l3NetworkUuid = network.uuid
            networkServices = [(flat.uuid): ["DHCP"]]
        }
        if (securityService) {
            def provider = queryNetworkServiceProvider { conditions = ["type=SecurityGroup"] }[0]
            attachNetworkServiceToL3Network {
                l3NetworkUuid = network.uuid
                networkServices = [(provider.uuid): ["SecurityGroup"]]
            }
        }
        return network.uuid
    }

    private VmInstanceInventory createOwnedVm(String vmName, String accountLabel, List<String> networks) {
        VmInstanceInventory vm = createVmInstance {
            name = vmName
            imageUuid = env.inventoryByName("image").uuid
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            l3NetworkUuids = networks
            defaultL3NetworkUuid = networks[0]
        }
        changeResourceOwner {
            resourceUuid = vm.uuid
            accountUuid = accounts[accountLabel].uuid
        }
        return vm
    }

    private boolean bound(String sgUuid, String nicUuid) {
        return Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.securityGroupUuid, sgUuid).eq(VmNicSecurityGroupRefVO_.vmNicUuid, nicUuid).isExists()
    }

    private List<Map> sharingSnapshot(String sgUuid) {
        return Q.New(SharedResourceVO.class).eq(SharedResourceVO_.resourceUuid, sgUuid).list().collect { SharedResourceVO record ->
            [id: record.id, ownerAccountUuid: record.ownerAccountUuid, receiverAccountUuid: record.receiverAccountUuid,
             toPublic: record.toPublic, permission: record.permission, resourceType: record.resourceType, resourceUuid: record.resourceUuid,
             createDate: record.createDate?.toString(), lastOpDate: record.lastOpDate?.toString()]
        }.sort { it.id }
    }

    private List<Map> ruleSnapshot(List rules) {
        return rules.collect { rule ->
            [uuid: rule.uuid, securityGroupUuid: rule.securityGroupUuid, type: rule.type, ipVersion: rule.ipVersion,
             state: rule.state, priority: rule.priority, protocol: rule.protocol, srcIpRange: rule.srcIpRange,
             dstIpRange: rule.dstIpRange, dstPortRange: rule.dstPortRange, remoteSecurityGroupUuid: rule.remoteSecurityGroupUuid,
             action: rule.action, description: rule.description]
        }.sort { it.uuid }
    }

}
