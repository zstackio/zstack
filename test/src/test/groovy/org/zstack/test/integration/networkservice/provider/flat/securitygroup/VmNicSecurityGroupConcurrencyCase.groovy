package org.zstack.test.integration.networkservice.provider.flat.securitygroup

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmSystemTags
import org.zstack.kvm.KVMAgentCommands
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.core.db.SimpleQuery
import org.zstack.kvm.KVMSecurityGroupBackend
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO
import org.zstack.network.securitygroup.APISetVmNicSecurityGroupMsg.VmNicSecurityGroupRefAO
import org.zstack.network.securitygroup.VmNicSecurityGroupRefVO
import org.zstack.network.securitygroup.VmNicSecurityGroupRefVO_
import org.zstack.network.securitygroup.VmNicSecurityPolicyVO
import org.zstack.network.securitygroup.SecurityGroupMembersTO
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.SecurityGroupInventory
import org.zstack.sdk.SecurityGroupRuleInventory
import org.zstack.sdk.VmNicSecurityGroupRefInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.VmNicSecurityPolicyInventory
import org.zstack.sdk.AddVmNicToSecurityGroupAction
import org.zstack.sdk.DeleteVmNicFromSecurityGroupAction
import org.zstack.sdk.SetVmNicSecurityGroupAction
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.virtualrouter.VirtualRouterNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

import org.zstack.core.db.DatabaseFacade
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.identity.SharedResourceVO
import org.zstack.header.identity.SharedResourceVO_
import org.zstack.header.errorcode.OperationFailureException
import org.zstack.identity.AccountManager
import org.zstack.network.securitygroup.SetSecurityGroupSharingMsg
import org.zstack.network.securitygroup.APIDeleteSecurityGroupMsg
import org.zstack.header.vm.AddVmNicToSecurityGroupMsg
import org.zstack.network.securitygroup.SecurityGroupManagerImpl
import org.zstack.network.securitygroup.SecurityGroupPermissionChecker
import org.zstack.network.securitygroup.SecurityGroupVmInstanceExtension
import org.zstack.network.securitygroup.SecurityGroupRuleVO
import org.zstack.network.securitygroup.SecurityGroupRuleVO_
import org.zstack.network.securitygroup.SecurityGroupVO
import org.zstack.network.securitygroup.SecurityGroupVO_
import org.zstack.networksecuritypolicyschedule.NetworkSecurityPolicyScheduleVO
import org.zstack.networksecuritypolicyschedule.NetworkSecurityPolicyScheduleVO_
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.SessionInventory
import org.zstack.sdk.SetSecurityGroupSharingAction
import org.zstack.sdk.DeleteSecurityGroupAction
import org.zstack.sdk.AddSecurityGroupRuleAction
import org.zstack.sdk.ChangeResourceOwnerAction

import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

import static org.mockito.Mockito.*
import static org.mockito.AdditionalAnswers.delegatesTo

import java.util.concurrent.atomic.AtomicInteger
class VmNicSecurityGroupConcurrencyCase extends SubCase {
    EnvSpec env

    L3NetworkInventory l3Net
    InstanceOfferingInventory offer
    ImageInventory image
    List<VmInstanceInventory> vmInvs = new ArrayList<>();
    List<SecurityGroupInventory> sgInvs = new ArrayList<>();
    // VmInstanceInventory vm1, vm2, vm3, vm4
    SecurityGroupInventory sg1, sg2, sg3
    AccountInventory accountA, accountB, accountC, accountD
    SessionInventory sessionA
    SecurityGroupInventory tenantGroup
    VmInstanceInventory tenantVm
    String tenantNicUuid

    void buildVmInvenceory() {
        for (int i = 1; i <= 10; i++) {
            VmInstanceInventory vm = createVmInstance {
                name = "vm-$i"
                imageUuid = image.uuid
                l3NetworkUuids = [l3Net.uuid]
                instanceOfferingUuid  = offer.uuid
            } as VmInstanceInventory

            vmInvs.add(vm)
        }
    }

    void buildSecurityGroup() {
        for (int i = 1; i <= 10; i++) {
            SecurityGroupInventory sg = createSecurityGroup {
                name = "sg-$i"
                ipVersion = 4
            } as SecurityGroupInventory

            attachSecurityGroupToL3Network {
                securityGroupUuid = sg.uuid
                l3NetworkUuid = l3Net.uuid
            }

            sgInvs.add(sg)
        }
    }

    void testAddVmNicToSecurityGroup1() {
        def threads = []
        for (int i = 0; i < 10; i++) {
            String nicUuid = vmInvs[i].vmNics[0].uuid

            def thread = Thread.start {
                addVmNicToSecurityGroup {
                    securityGroupUuid = sg1.uuid
                    vmNicUuids = [nicUuid]
                }
            }
            threads.add(thread)
        }

        threads.each { it.join() }

        List<VmNicSecurityGroupRefVO> refs = Q.New(VmNicSecurityGroupRefVO).in(VmNicSecurityGroupRefVO_.securityGroupUuid, [sg1.uuid]).list()
        assert refs.size() == 10
        vmInvs.each { vm ->
            assert refs.find {it.vmNicUuid == vm.vmNics[0].uuid && it.priority == 1} != null
        }
    }

    void testAddVmNicToSecurityGroup2() {
        def threads = []
        for (int i = 0; i < 10; i++) {
            String nicUuid = vmInvs[i].vmNics[0].uuid

            def thread = Thread.start {
                addVmNicToSecurityGroup {
                    securityGroupUuid = sg2.uuid
                    vmNicUuids = [nicUuid]
                }
            }
            threads.add(thread)
        }

        threads.each { it.join() }

        List<VmNicSecurityGroupRefVO> refs = Q.New(VmNicSecurityGroupRefVO).in(VmNicSecurityGroupRefVO_.securityGroupUuid, [sg1.uuid, sg2.uuid]).list()
        assert refs.size() == 20
        vmInvs.each { vm ->
            assert refs.find {it.vmNicUuid == vm.vmNics[0].uuid && it.securityGroupUuid == sg1.uuid && it.priority == 1} != null
            assert refs.find {it.vmNicUuid == vm.vmNics[0].uuid && it.securityGroupUuid == sg2.uuid && it.priority == 2} != null
        }
    }

    void testAddVmNicToSecurityGroup3() {
        def threads = []
        for (int i = 0; i < 10; i++) {
            String nicUuid = vmInvs[i].vmNics[0].uuid

            def thread = Thread.start {
                addVmNicToSecurityGroup {
                    securityGroupUuid = sg3.uuid
                    vmNicUuids = [nicUuid]
                }
            }
            threads.add(thread)
        }

        threads.each { it.join() }

        List<VmNicSecurityGroupRefVO> refs = Q.New(VmNicSecurityGroupRefVO).in(VmNicSecurityGroupRefVO_.securityGroupUuid, [sg1.uuid, sg2.uuid, sg3.uuid]).list()
        assert refs.size() == 30
        vmInvs.each { vm ->
            assert refs.find {it.vmNicUuid == vm.vmNics[0].uuid && it.securityGroupUuid == sg1.uuid && it.priority == 1} != null
            assert refs.find {it.vmNicUuid == vm.vmNics[0].uuid && it.securityGroupUuid == sg2.uuid && it.priority == 2} != null
            assert refs.find {it.vmNicUuid == vm.vmNics[0].uuid && it.securityGroupUuid == sg3.uuid && it.priority == 3} != null
        }
    }

    void testRemoveVmNicFromSecurityGroup1() {
        def threads = []
        for (int i = 0; i < 10; i++) {
            String nicUuid = vmInvs[i].vmNics[0].uuid

            def thread = Thread.start {
                deleteVmNicFromSecurityGroup {
                    securityGroupUuid = sg1.uuid
                    vmNicUuids = [nicUuid]
                }
            }
            threads.add(thread)
        }

        threads.each { it.join() }

        List<VmNicSecurityGroupRefVO> refs = Q.New(VmNicSecurityGroupRefVO).in(VmNicSecurityGroupRefVO_.securityGroupUuid, [sg1.uuid, sg2.uuid, sg3.uuid]).list()
        assert refs.size() == 20
        vmInvs.each { vm ->
            assert refs.find {it.vmNicUuid == vm.vmNics[0].uuid && it.securityGroupUuid == sg2.uuid && it.priority == 1} != null
            assert refs.find {it.vmNicUuid == vm.vmNics[0].uuid && it.securityGroupUuid == sg3.uuid && it.priority == 2} != null
        }
    }

    List<VmNicSecurityGroupRefAO> buildSecurityGroupRefAO(int count) {
        List<VmNicSecurityGroupRefAO> refAOs = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            String sgUuid = sgInvs.get(i - 1).uuid
            VmNicSecurityGroupRefAO ao = new VmNicSecurityGroupRefAO()
            ao.securityGroupUuid = sgUuid
            ao.priority = i
            refAOs.add(ao)
        }

        return refAOs
    }

    void testSetVmNicSecurityGroup() {
        SQL.New(VmNicSecurityPolicyVO).delete()
        def errors = []
        def threads = []
        for (int i = 1; i <= 10; i++) {
            List<VmNicSecurityGroupRefAO> refAOs = buildSecurityGroupRefAO(i)
            String nicUuid = vmInvs[0].vmNics[0].uuid
            def thread = Thread.start {

                SetVmNicSecurityGroupAction action = new SetVmNicSecurityGroupAction ()
                action.vmNicUuid = nicUuid
                action.refs = refAOs
                action.sessionId = adminSession()

                SetVmNicSecurityGroupAction.Result result = action.call()
                if (result.error != null) {
                    errors.add(result.error)
                }
            }
            threads.add(thread)
        }

        threads.each { it.join() }

        assert errors.size() == 0
    }

    void testVmNicDetachSecurityGroup() {
        def errors = []
        def threads = []
        for (int i = 1; i <= 10; i++) {
            String sgUuid = sgInvs.get(i - 1).uuid
            String nicUuid = vmInvs[0].vmNics[0].uuid
            def thread = Thread.start {
                DeleteVmNicFromSecurityGroupAction action = new DeleteVmNicFromSecurityGroupAction()
                action.securityGroupUuid = sgUuid
                action.vmNicUuids = [nicUuid]
                action.sessionId = adminSession()

                DeleteVmNicFromSecurityGroupAction.Result result = action.call()
                if (result.error != null) {
                    errors.add(result.error)
                }
            }

            threads.add(thread)
        }

        threads.each { it.join() }

        assert errors.size() == 0
        List<VmNicSecurityGroupRefVO> refVOs = Q.New(VmNicSecurityGroupRefVO).eq(VmNicSecurityGroupRefVO_.vmNicUuid, vmInvs[0].vmNics[0].uuid).list()
        assert refVOs.size() == 0
    }

    void testVmNicAttachSecurityGroup() {
        def errors = []
        def threads = []
        for (int i = 1; i <= 10; i++) {
            String sgUuid = sgInvs.get(i - 1).uuid
            String nicUuid = vmInvs[0].vmNics[0].uuid
            def thread = Thread.start {
                AddVmNicToSecurityGroupAction action = new AddVmNicToSecurityGroupAction()
                action.securityGroupUuid = sgUuid
                action.vmNicUuids = [nicUuid]
                action.sessionId = adminSession()

                AddVmNicToSecurityGroupAction.Result result = action.call()
                if (result.error != null) {
                    errors.add(result.error)
                }
            }

            threads.add(thread)
        }

        threads.each { it.join() }

        assert errors.size() == 0
        List<VmNicSecurityGroupRefVO> refVOs = Q.New(VmNicSecurityGroupRefVO).eq(VmNicSecurityGroupRefVO_.vmNicUuid, vmInvs[0].vmNics[0].uuid).orderBy(VmNicSecurityGroupRefVO_.priority, SimpleQuery.Od.ASC).list()
        assert refVOs.size() == 10
        refVOs.each { ref ->
            assert ref.priority == refVOs.indexOf(ref) + 1
        }
    }

    void prepareSharedBindingScenario() {
        accountA = createAccount {
            name = "concurrent-account-a"
            password = "password"
        }
        accountB = createAccount {
            name = "concurrent-account-b"
            password = "password"
        }
        accountC = createAccount {
            name = "concurrent-account-c"
            password = "password"
        }
        accountD = createAccount {
            name = "concurrent-account-d"
            password = "password"
        }
        sessionA = logInByAccount {
            accountName = accountA.name
            password = "password"
        }
        tenantVm = createVmInstance {
            name = "shared-group-concurrency-vm"
            imageUuid = image.uuid
            instanceOfferingUuid = offer.uuid
            l3NetworkUuids = [l3Net.uuid]
        }
        changeResourceOwner {
            resourceUuid = tenantVm.uuid
            accountUuid = accountA.uuid
        }
        tenantNicUuid = tenantVm.vmNics[0].uuid
        tenantGroup = createSecurityGroup {
            name = "concurrency-tenant-group"
            sessionId = sessionA.uuid
        }
        addVmNicToSecurityGroup {
            securityGroupUuid = tenantGroup.uuid
            vmNicUuids = [tenantNicUuid]
            sessionId = sessionA.uuid
        }
    }

    void testRecallWhileBinding() {
        SecurityGroupInventory group = sharedGroup("add-before-recall")
        CountDownLatch addEntered = new CountDownLatch(1), releaseAdd = new CountDownLatch(1)
        Closure removeNotifier = notifyWhenReceivedMessage(AddVmNicToSecurityGroupMsg.class) { msg ->
            if (msg.securityGroupUuid == group.uuid) {
                addEntered.countDown()

                assert releaseAdd.await(30, TimeUnit.SECONDS)
            }
        }
        FutureTask add, recall
        CountDownLatch recallDelivered = new CountDownLatch(1)
        Closure removeRecallNotifier = observeMessageDelivery(SetSecurityGroupSharingMsg.class, group.uuid, recallDelivered)
        try {
            add = async { new AddVmNicToSecurityGroupAction(securityGroupUuid: group.uuid, vmNicUuids: [tenantNicUuid], sessionId: sessionA.uuid).call() }

            assert addEntered.await(30, TimeUnit.SECONDS)

            recall = async { new SetSecurityGroupSharingAction(securityGroupUuid: group.uuid, mode: "None", sessionId: adminSession()).call() }

            assert recallDelivered.await(30, TimeUnit.SECONDS)
            assert !recall.isDone() : "recall must wait while Add holds the SG queue"
        } finally {
            releaseAdd.countDown()
        }
        def firstAddResult = add.get(30, TimeUnit.SECONDS)

        assert firstAddResult.error == null

        def blockedRecallResult = recall.get(30, TimeUnit.SECONDS)
        removeNotifier()
        removeRecallNotifier()

        assert blockedRecallResult.error?.globalErrorCode == "ORG_ZSTACK_NETWORK_SECURITYGROUP_10136"

        deleteVmNicFromSecurityGroup {
            securityGroupUuid = group.uuid
            vmNicUuids = [tenantNicUuid]
            sessionId = sessionA.uuid
        }
        setSecurityGroupSharing {
            securityGroupUuid = group.uuid
            mode = "None"
        }

        group = sharedGroup("recall-before-add")
        String groupUuid = group.uuid
        def checker = spy(bean(SecurityGroupPermissionChecker.class))
        CountDownLatch recallEntered = new CountDownLatch(1), releaseRecall = new CountDownLatch(1), internalAdd = new CountDownLatch(1)
        doAnswer { invocation ->
            recallEntered.countDown()

            assert releaseRecall.await(30, TimeUnit.SECONDS)

            return invocation.callRealMethod()
        }.when(checker).checkSharingReferences(eq(groupUuid), eq(false), anyList())
        removeNotifier = notifyWhenReceivedMessage(AddVmNicToSecurityGroupMsg.class) { msg -> if (msg.securityGroupUuid == groupUuid) internalAdd.countDown() }
        CountDownLatch addDelivered = new CountDownLatch(1)
        Closure removeAddNotifier = observeMessageDelivery(org.zstack.network.securitygroup.APIAddVmNicToSecurityGroupMsg.class, groupUuid, addDelivered)
        bean(SecurityGroupManagerImpl.class).permissionChecker = checker
        try {
            recall = async { new SetSecurityGroupSharingAction(securityGroupUuid: groupUuid, mode: "None", sessionId: adminSession()).call() }

            assert recallEntered.await(30, TimeUnit.SECONDS)

            add = async { new AddVmNicToSecurityGroupAction(securityGroupUuid: groupUuid, vmNicUuids: [tenantNicUuid], sessionId: sessionA.uuid).call() }

            assert addDelivered.await(30, TimeUnit.SECONDS)
            assert !internalAdd.await(1, TimeUnit.SECONDS) : "Add must not reach the Ref message while Sharing holds the SG queue"
        } finally {
            releaseRecall.countDown()
        }
        def recalledBeforeAddResult = recall.get(30, TimeUnit.SECONDS)

        assert recalledBeforeAddResult.error == null

        def rejectedAddResult = add.get(30, TimeUnit.SECONDS)
        removeNotifier()
        removeAddNotifier()

        assert rejectedAddResult.error?.globalErrorCode == "ORG_ZSTACK_NETWORK_SECURITYGROUP_10133"

        bean(SecurityGroupManagerImpl.class).permissionChecker = bean(SecurityGroupPermissionChecker.class)

        assert !bound(groupUuid)

        group = sharedGroup("recall-before-set-write")
        groupUuid = group.uuid
        checker = spy(bean(SecurityGroupPermissionChecker.class))
        CountDownLatch setEntered = new CountDownLatch(1), releaseSet = new CountDownLatch(1)
        doAnswer { invocation ->
            setEntered.countDown()

            assert releaseSet.await(30, TimeUnit.SECONDS)

            return invocation.callRealMethod()
        }.when(checker).checkBindingUse(any(org.zstack.header.identity.SessionInventory.class), eq(accountA.uuid), anyList())
        def before = nicSnapshot()
        bean(SecurityGroupManagerImpl.class).permissionChecker = checker
        FutureTask set
        try {
            set = async { new SetVmNicSecurityGroupAction(vmNicUuid: tenantNicUuid, refs: [new VmNicSecurityGroupRefAO(securityGroupUuid: tenantGroup.uuid, priority: 1), new VmNicSecurityGroupRefAO(securityGroupUuid: groupUuid, priority: 2)], sessionId: sessionA.uuid).call() }

            assert setEntered.await(30, TimeUnit.SECONDS)

            def recallResult = new SetSecurityGroupSharingAction(securityGroupUuid: groupUuid, mode: "None", sessionId: adminSession()).call()

            assert recallResult.error == null
        } finally {
            releaseSet.countDown()
        }
        def setResult = set.get(30, TimeUnit.SECONDS)

        assert setResult.error?.globalErrorCode == "ORG_ZSTACK_NETWORK_SECURITYGROUP_10133"

        bean(SecurityGroupManagerImpl.class).permissionChecker = bean(SecurityGroupPermissionChecker.class)

        assert nicSnapshot() == before : "rejected Set must preserve all original bindings and priorities"
    }

    void testSharingRollbackBeforeNextRequest() {
        SecurityGroupInventory group = sharedGroup("sharing-rollback-queue", [accountA.uuid, accountB.uuid])
        def original = sharingSnapshot(group.uuid)
        DatabaseFacade realDbf = bean(DatabaseFacade.class)
        DatabaseFacade database = mock(DatabaseFacade.class, delegatesTo(realDbf))
        def checker = spy(bean(SecurityGroupPermissionChecker.class))
        AtomicReference<SharedResourceVO> addedRecord = new AtomicReference<>()
        AtomicReference<List> secondInitial = new AtomicReference<>()
        CountDownLatch updateWritten = new CountDownLatch(1), failUpdate = new CountDownLatch(1)
        CountDownLatch rollbackPaused = new CountDownLatch(1), finishRollback = new CountDownLatch(1), secondEntered = new CountDownLatch(1)
        doAnswer { invocation ->
            Collection records = invocation.arguments[0] as Collection
            realDbf.persistCollection(records)
            SharedResourceVO record = records.find { it.resourceUuid == group.uuid && it.receiverAccountUuid == accountC.uuid } as SharedResourceVO
            if (record != null) {
                addedRecord.set(record)

                assert Q.New(SharedResourceVO.class).eq(SharedResourceVO_.id, record.id).isExists() : "pause must follow a committed new authorization"

                updateWritten.countDown()

                assert failUpdate.await(30, TimeUnit.SECONDS)

                throw new OperationFailureException(new ErrorCode("TEST.SHARING.WRITE", "sharing write failed on purpose"))
            }
            return null
        }.when(database).persistCollection(anyCollection())
        doAnswer { invocation ->
            if ((invocation.arguments[0] as Collection).contains(addedRecord.get()?.id)) {
                rollbackPaused.countDown()

                assert finishRollback.await(30, TimeUnit.SECONDS)
            }
            realDbf.removeByPrimaryKeys(invocation.arguments[0], invocation.arguments[1])
            return null
        }.when(database).removeByPrimaryKeys(anyCollection(), eq(SharedResourceVO.class))
        doAnswer { invocation ->
            if ((invocation.arguments[2] as List).contains(accountD.uuid)) {
                secondInitial.set(sharingSnapshot(group.uuid))
                secondEntered.countDown()
            }
            return invocation.callRealMethod()
        }.when(checker).checkSharingReferences(eq(group.uuid), anyBoolean(), anyList())
        CountDownLatch secondDelivered = new CountDownLatch(1)
        Closure removeSecondNotifier = {}
        bean(SecurityGroupManagerImpl.class).dbf = database
        bean(SecurityGroupManagerImpl.class).permissionChecker = checker
        FutureTask first, second
        try {
            first = async { new SetSecurityGroupSharingAction(securityGroupUuid: group.uuid, mode: "Specified", accountUuids: [accountA.uuid, accountC.uuid], sessionId: adminSession()).call() }

            assert updateWritten.await(30, TimeUnit.SECONDS)

            failUpdate.countDown()

            assert rollbackPaused.await(30, TimeUnit.SECONDS)

            removeSecondNotifier = observeMessageDelivery(SetSecurityGroupSharingMsg.class, group.uuid, secondDelivered)
            second = async { new SetSecurityGroupSharingAction(securityGroupUuid: group.uuid, mode: "Specified", accountUuids: [accountA.uuid, accountD.uuid], sessionId: adminSession()).call() }

            assert secondDelivered.await(30, TimeUnit.SECONDS)
            assert !secondEntered.await(1, TimeUnit.SECONDS) : "next Sharing request must wait for complete rollback"
            assert !second.isDone()
        } finally {
            failUpdate.countDown()
            finishRollback.countDown()
        }
        def firstResult = first.get(30, TimeUnit.SECONDS)

        assert firstResult.error?.code == "TEST.SHARING.WRITE" : "injected write failure must reach the API"

        def secondResult = second.get(30, TimeUnit.SECONDS)
        removeSecondNotifier()

        assert secondResult.error == null

        bean(SecurityGroupManagerImpl.class).permissionChecker = bean(SecurityGroupPermissionChecker.class)
        bean(SecurityGroupManagerImpl.class).dbf = bean(DatabaseFacade.class)

        assert secondInitial.get() == original : "next request must read original grant IDs, permissions, owners and timestamps"
        assert sharingSnapshot(group.uuid)*.receiverAccountUuid.toSet() == [accountA.uuid, accountD.uuid].toSet()
        assert !sharingSnapshot(group.uuid).any { it.receiverAccountUuid == accountC.uuid }
    }

    void testRecallWhileAddingRule() {
        SecurityGroupInventory group = sharedGroup("recall-before-rule-write")
        def checker = spy(bean(SecurityGroupPermissionChecker.class))
        CountDownLatch ruleEntered = new CountDownLatch(1), releaseRule = new CountDownLatch(1)
        doAnswer { invocation ->
            if ((invocation.arguments[1] as List).contains(group.uuid)) {
                ruleEntered.countDown()

                assert releaseRule.await(30, TimeUnit.SECONDS)
            }
            return invocation.callRealMethod()
        }.when(checker).checkUse(eq(accountA.uuid), anyList())
        def before = JSONObjectUtil.toJsonString(Q.New(SecurityGroupRuleVO.class).eq(SecurityGroupRuleVO_.securityGroupUuid, tenantGroup.uuid).orderBy(SecurityGroupRuleVO_.uuid, SimpleQuery.Od.ASC).list())
        bean(SecurityGroupManagerImpl.class).permissionChecker = checker
        FutureTask addRule
        try {
            addRule = async { new AddSecurityGroupRuleAction(securityGroupUuid: tenantGroup.uuid,
                    rules: [new SecurityGroupRuleAO(type: "Ingress", ipVersion: 4, protocol: "ALL", remoteSecurityGroupUuid: group.uuid)], sessionId: sessionA.uuid).call() }

            assert ruleEntered.await(30, TimeUnit.SECONDS)

            def recallResult = new SetSecurityGroupSharingAction(securityGroupUuid: group.uuid, mode: "None", sessionId: adminSession()).call()

            assert recallResult.error == null
        } finally {
            releaseRule.countDown()
        }
        def addRuleResult = addRule.get(30, TimeUnit.SECONDS)

        assert addRuleResult.error?.globalErrorCode == "ORG_ZSTACK_NETWORK_SECURITYGROUP_10133"

        bean(SecurityGroupManagerImpl.class).permissionChecker = bean(SecurityGroupPermissionChecker.class)

        assert JSONObjectUtil.toJsonString(Q.New(SecurityGroupRuleVO.class).eq(SecurityGroupRuleVO_.securityGroupUuid, tenantGroup.uuid).orderBy(SecurityGroupRuleVO_.uuid, SimpleQuery.Od.ASC).list()) == before : "rejected reference must preserve source rules and their priorities"
    }

    void testRecallWhileTransferringVm() {
        SecurityGroupInventory group = sharedGroup("recall-before-transfer", [accountA.uuid, accountB.uuid])
        deleteVmNicFromSecurityGroup {
            securityGroupUuid = tenantGroup.uuid
            vmNicUuids = [tenantNicUuid]
            sessionId = sessionA.uuid
        }
        addVmNicToSecurityGroup {
            securityGroupUuid = group.uuid
            vmNicUuids = [tenantNicUuid]
            sessionId = sessionA.uuid
        }
        shareResource {
            resourceUuids = [l3Net.uuid, image.uuid, offer.uuid]
            accountUuids = [accountB.uuid]
            toPublic = false
        }
        def before = nicSnapshot()
        def checker = spy(bean(SecurityGroupPermissionChecker.class))
        CountDownLatch transferEntered = new CountDownLatch(1), releaseTransfer = new CountDownLatch(1)
        doAnswer { invocation ->
            transferEntered.countDown()

            assert releaseTransfer.await(30, TimeUnit.SECONDS)

            return invocation.callRealMethod()
        }.when(checker).checkVmOwnerChange(eq(tenantVm.uuid), eq(accountB.uuid))
        bean(SecurityGroupVmInstanceExtension.class).permissionChecker = checker
        FutureTask transfer
        try {
            transfer = async { new ChangeResourceOwnerAction(resourceUuid: tenantVm.uuid, accountUuid: accountB.uuid, sessionId: adminSession()).call() }

            assert transferEntered.await(30, TimeUnit.SECONDS)

            def recallResult = new SetSecurityGroupSharingAction(securityGroupUuid: group.uuid, mode: "Specified", accountUuids: [accountA.uuid], sessionId: adminSession()).call()

            assert recallResult.error == null
        } finally {
            releaseTransfer.countDown()
        }
        def transferResult = transfer.get(30, TimeUnit.SECONDS)

        assert transferResult.error?.globalErrorCode == "ORG_ZSTACK_NETWORK_SECURITYGROUP_10138"

        bean(SecurityGroupVmInstanceExtension.class).permissionChecker = bean(SecurityGroupPermissionChecker.class)

        assert nicSnapshot() == before
        assert bean(AccountManager.class).getOwnerAccountUuidOfResource(tenantVm.uuid) == accountA.uuid : "failed transfer must preserve the VM owner"

        def restoreSharing = new SetSecurityGroupSharingAction(securityGroupUuid: group.uuid, mode: "Specified", accountUuids: [accountA.uuid, accountB.uuid], sessionId: adminSession()).call()

        assert restoreSharing.error == null

        changeResourceOwner {
            resourceUuid = tenantVm.uuid
            accountUuid = accountB.uuid
        }

        assert nicSnapshot() == before : "successful owner change must preserve bindings and priorities"

        def usedAccountRecall = new SetSecurityGroupSharingAction(securityGroupUuid: group.uuid, mode: "Specified", accountUuids: [accountA.uuid], sessionId: adminSession()).call()

        assert usedAccountRecall.error?.globalErrorCode == "ORG_ZSTACK_NETWORK_SECURITYGROUP_10136"

        changeResourceOwner {
            resourceUuid = tenantVm.uuid
            accountUuid = accountA.uuid
        }
        deleteVmNicFromSecurityGroup {
            securityGroupUuid = group.uuid
            vmNicUuids = [tenantNicUuid]
            sessionId = sessionA.uuid
        }
        addVmNicToSecurityGroup {
            securityGroupUuid = tenantGroup.uuid
            vmNicUuids = [tenantNicUuid]
            sessionId = sessionA.uuid
        }
    }

    void testDeleteAfterConcurrentBinding() {
        ["Permissive", "Enforcing"].each { String deletionMode ->
            SecurityGroupInventory group = sharedGroup("delete-after-bind-${deletionMode}")
            def schedule = createNetworkSecurityPolicySchedule {
                name = "delete-guard-${deletionMode}"
                resourceType = "SecurityGroup"
                resourceUuid = group.uuid
                timeType = "UTC"
                repeatType = "Weekly"
                startDate = "2099-01-01"
                endDate = "2099-12-31"
                startTime = "09:00"
                endTime = "18:00"
                weekDays = [1, 2, 3, 4, 5]
            }
            CountDownLatch deleteDelivered = new CountDownLatch(1), releaseDelete = new CountDownLatch(1)
            Closure removeNotifier = notifyWhenReceivedMessage(APIDeleteSecurityGroupMsg.class) { msg ->
                if (msg.uuid == group.uuid && msg.serviceId.contains("securityGroup")) {
                    deleteDelivered.countDown()

                    assert releaseDelete.await(30, TimeUnit.SECONDS)
                }
            }
            FutureTask deletion
            try {
                deletion = async { new DeleteSecurityGroupAction(uuid: group.uuid, deleteMode: deletionMode, sessionId: adminSession()).call() }

                assert deleteDelivered.await(30, TimeUnit.SECONDS)

                addVmNicToSecurityGroup {
                    securityGroupUuid = group.uuid
                    vmNicUuids = [tenantNicUuid]
                    sessionId = sessionA.uuid
                }
            } finally {
                releaseDelete.countDown()
            }
            def deletionResult = deletion.get(30, TimeUnit.SECONDS)
            removeNotifier()

            assert deletionResult.error?.globalErrorCode == "ORG_ZSTACK_NETWORK_SECURITYGROUP_10137"
            assert bound(group.uuid)
            assert Q.New(SecurityGroupVO.class).eq(SecurityGroupVO_.uuid, group.uuid).isExists()
            assert Q.New(NetworkSecurityPolicyScheduleVO.class).eq(NetworkSecurityPolicyScheduleVO_.uuid, schedule.uuid).isExists() : "rejected delete must not remove the time policy"
            assert Q.New(SecurityGroupRuleVO.class).eq(SecurityGroupRuleVO_.securityGroupUuid, group.uuid).count() == group.rules.size()

            deleteVmNicFromSecurityGroup {
                securityGroupUuid = group.uuid
                vmNicUuids = [tenantNicUuid]
                sessionId = sessionA.uuid
            }
            deleteSecurityGroup {
                uuid = group.uuid
                deleteMode = deletionMode
            }

            group = sharedGroup("queued-delete-${deletionMode}")
            String queuedGroupUuid = group.uuid
            CountDownLatch addEntered = new CountDownLatch(1), releaseAdd = new CountDownLatch(1), queuedDelete = new CountDownLatch(1)
            Closure removeAddNotifier = notifyWhenReceivedMessage(AddVmNicToSecurityGroupMsg.class) { msg ->
                if (msg.securityGroupUuid == queuedGroupUuid) {
                    addEntered.countDown()

                    assert releaseAdd.await(30, TimeUnit.SECONDS)
                }
            }
            Closure removeDeleteNotifier = observeMessageDelivery(APIDeleteSecurityGroupMsg.class, queuedGroupUuid, queuedDelete)
            FutureTask add
            try {
                add = async { new AddVmNicToSecurityGroupAction(securityGroupUuid: queuedGroupUuid, vmNicUuids: [tenantNicUuid], sessionId: sessionA.uuid).call() }

                assert addEntered.await(30, TimeUnit.SECONDS)

                deletion = async { new DeleteSecurityGroupAction(uuid: queuedGroupUuid, deleteMode: deletionMode, sessionId: adminSession()).call() }

                assert queuedDelete.await(30, TimeUnit.SECONDS)
                assert !deletion.isDone() : "delete must wait for Add's SG queue"
            } finally {
                releaseAdd.countDown()
            }
            def addResult = add.get(30, TimeUnit.SECONDS)

            assert addResult.error == null

            def queuedDeletionResult = deletion.get(30, TimeUnit.SECONDS)
            removeAddNotifier()
            removeDeleteNotifier()

            assert queuedDeletionResult.error?.globalErrorCode == "ORG_ZSTACK_NETWORK_SECURITYGROUP_10137"
            assert bound(queuedGroupUuid)

            deleteVmNicFromSecurityGroup {
                securityGroupUuid = queuedGroupUuid
                vmNicUuids = [tenantNicUuid]
                sessionId = sessionA.uuid
            }
            deleteSecurityGroup {
                uuid = queuedGroupUuid
                deleteMode = deletionMode
            }
        }

        SecurityGroupInventory group = sharedGroup("delete-before-add")
        def checker = spy(bean(SecurityGroupPermissionChecker.class))
        CountDownLatch deleteEntered = new CountDownLatch(1), releaseDelete = new CountDownLatch(1), internalAdd = new CountDownLatch(1)
        doAnswer { invocation ->
            deleteEntered.countDown()

            assert releaseDelete.await(30, TimeUnit.SECONDS)

            return invocation.callRealMethod()
        }
                .when(checker).checkDelete(eq(group.uuid))
        Closure removeNotifier = notifyWhenReceivedMessage(AddVmNicToSecurityGroupMsg.class) { msg -> if (msg.securityGroupUuid == group.uuid) internalAdd.countDown() }
        bean(SecurityGroupManagerImpl.class).permissionChecker = checker
        FutureTask deleteBeforeAdd, addAfterDelete
        try {
            deleteBeforeAdd = async { new DeleteSecurityGroupAction(uuid: group.uuid, sessionId: adminSession()).call() }

            assert deleteEntered.await(30, TimeUnit.SECONDS)

            addAfterDelete = async { new AddVmNicToSecurityGroupAction(securityGroupUuid: group.uuid, vmNicUuids: [tenantNicUuid], sessionId: sessionA.uuid).call() }

            assert !internalAdd.await(1, TimeUnit.SECONDS)
        } finally {
            releaseDelete.countDown()
        }
        def deleteBeforeAddResult = deleteBeforeAdd.get(30, TimeUnit.SECONDS)

        assert deleteBeforeAddResult.error == null

        def addAfterDeleteResult = addAfterDelete.get(30, TimeUnit.SECONDS)
        removeNotifier()

        assert addAfterDeleteResult.error?.globalErrorCode == "ORG_ZSTACK_NETWORK_SECURITYGROUP_10140"

        bean(SecurityGroupManagerImpl.class).permissionChecker = bean(SecurityGroupPermissionChecker.class)

        assert !bound(group.uuid) : "queued Add must not create a binding for a deleted group"
    }

    void testDeleteWhileChangingSharing() {
        SecurityGroupInventory group = createSecurityGroup { name = "share-before-delete" }
        addVmNicToSecurityGroup {
            securityGroupUuid = group.uuid
            vmNicUuids = [tenantNicUuid]
        }
        def checker = spy(bean(SecurityGroupPermissionChecker.class))
        CountDownLatch sharingEntered = new CountDownLatch(1), releaseSharing = new CountDownLatch(1)
        doAnswer { invocation ->
            sharingEntered.countDown()

            assert releaseSharing.await(30, TimeUnit.SECONDS)

            return invocation.callRealMethod()
        }
                .when(checker).checkSharingReferences(eq(group.uuid), eq(false), anyList())
        CountDownLatch deletionDelivered = new CountDownLatch(1)
        Closure removeDeleteNotifier = observeMessageDelivery(APIDeleteSecurityGroupMsg.class, group.uuid, deletionDelivered)
        bean(SecurityGroupManagerImpl.class).permissionChecker = checker
        FutureTask sharing, deletion
        try {
            sharing = async { new SetSecurityGroupSharingAction(securityGroupUuid: group.uuid, mode: "Specified", accountUuids: [accountA.uuid], sessionId: adminSession()).call() }

            assert sharingEntered.await(30, TimeUnit.SECONDS)

            deletion = async { new DeleteSecurityGroupAction(uuid: group.uuid, sessionId: adminSession()).call() }

            assert deletionDelivered.await(30, TimeUnit.SECONDS)
            assert !deletion.isDone()
        } finally {
            releaseSharing.countDown()
        }
        def sharingResult = sharing.get(30, TimeUnit.SECONDS)

        assert sharingResult.error == null

        def deletionResult = deletion.get(30, TimeUnit.SECONDS)
        removeDeleteNotifier()

        assert deletionResult.error?.globalErrorCode == "ORG_ZSTACK_NETWORK_SECURITYGROUP_10137"

        bean(SecurityGroupManagerImpl.class).permissionChecker = bean(SecurityGroupPermissionChecker.class)

        assert bound(group.uuid)

        deleteVmNicFromSecurityGroup {
            securityGroupUuid = group.uuid
            vmNicUuids = [tenantNicUuid]
            sessionId = sessionA.uuid
        }
        deleteSecurityGroup { uuid = group.uuid }

        group = sharedGroup("delete-before-sharing")
        String groupUuid = group.uuid
        checker = spy(bean(SecurityGroupPermissionChecker.class))
        CountDownLatch deleteEntered = new CountDownLatch(1), releaseDelete = new CountDownLatch(1)
        doAnswer { invocation ->
            deleteEntered.countDown()

            assert releaseDelete.await(30, TimeUnit.SECONDS)

            return invocation.callRealMethod()
        }
                .when(checker).checkDelete(eq(groupUuid))
        CountDownLatch sharingDelivered = new CountDownLatch(1)
        Closure removeSharingNotifier = observeMessageDelivery(SetSecurityGroupSharingMsg.class, groupUuid, sharingDelivered)
        bean(SecurityGroupManagerImpl.class).permissionChecker = checker
        try {
            deletion = async { new DeleteSecurityGroupAction(uuid: groupUuid, sessionId: adminSession()).call() }

            assert deleteEntered.await(30, TimeUnit.SECONDS)

            sharing = async { new SetSecurityGroupSharingAction(securityGroupUuid: groupUuid, mode: "Specified", accountUuids: [accountA.uuid, accountB.uuid], sessionId: adminSession()).call() }

            assert sharingDelivered.await(30, TimeUnit.SECONDS)
            assert !sharing.isDone()
        } finally {
            releaseDelete.countDown()
        }
        def deletedBeforeSharingResult = deletion.get(30, TimeUnit.SECONDS)

        assert deletedBeforeSharingResult.error == null

        def queuedSharingResult = sharing.get(30, TimeUnit.SECONDS)
        removeSharingNotifier()

        assert queuedSharingResult.error?.globalErrorCode == "ORG_ZSTACK_NETWORK_SECURITYGROUP_10140"

        bean(SecurityGroupManagerImpl.class).permissionChecker = bean(SecurityGroupPermissionChecker.class)

        assert sharingSnapshot(groupUuid).isEmpty() : "Sharing queued behind deletion must not leave grants"
    }

    private SecurityGroupInventory sharedGroup(String name, List<String> accounts = null) {
        SecurityGroupInventory group = createSecurityGroup { delegate.name = name }
        setSecurityGroupSharing {
            securityGroupUuid = group.uuid
            mode = "Specified"
            accountUuids = accounts ?: [accountA.uuid]
        }
        return group
    }

    private List nicSnapshot() {
        return Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, tenantNicUuid)
                .orderBy(VmNicSecurityGroupRefVO_.priority, SimpleQuery.Od.ASC).list().collect { [uuid: it.uuid, group: it.securityGroupUuid, priority: it.priority] }
    }

    private List sharingSnapshot(String uuid) {
        return Q.New(SharedResourceVO.class).eq(SharedResourceVO_.resourceUuid, uuid).orderBy(SharedResourceVO_.id, SimpleQuery.Od.ASC).list().collect {
            [id: it.id, ownerAccountUuid: it.ownerAccountUuid, receiverAccountUuid: it.receiverAccountUuid, toPublic: it.toPublic,
             permission: it.permission, resourceType: it.resourceType, resourceUuid: it.resourceUuid, createDate: it.createDate, lastOpDate: it.lastOpDate]
        }
    }

    private boolean bound(String uuid) {
        return Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, tenantNicUuid).eq(VmNicSecurityGroupRefVO_.securityGroupUuid, uuid).isExists()
    }

    private Closure observeMessageDelivery(Class type, String groupUuid, CountDownLatch delivered) {
        return notifyWhenReceivedMessage(type) { msg ->
            if (msg.securityGroupUuid == groupUuid && msg.serviceId.contains("securityGroup")) {
                delivered.countDown()
            }
        }
    }

    private FutureTask async(Closure operation) {
        FutureTask future = new FutureTask(operation as Callable)
        Thread.start { future.run() }
        return future
    }

    @Override
    void clean() {
        bean(SecurityGroupManagerImpl.class).dbf = bean(DatabaseFacade.class)
        bean(SecurityGroupManagerImpl.class).permissionChecker = bean(SecurityGroupPermissionChecker.class)
        bean(SecurityGroupVmInstanceExtension.class).permissionChecker = bean(SecurityGroupPermissionChecker.class)
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
            offer = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
            image = env.inventoryByName("image") as ImageInventory
            l3Net = env.inventoryByName("l3") as L3NetworkInventory
            buildVmInvenceory()
            buildSecurityGroup()
            sg1 = sgInvs[0]
            sg2 = sgInvs[1]
            sg3 = sgInvs[2]
        }

        testAddVmNicToSecurityGroup1()
        testAddVmNicToSecurityGroup2()
        testAddVmNicToSecurityGroup3()
        testRemoveVmNicFromSecurityGroup1()
        testSetVmNicSecurityGroup()
        testVmNicDetachSecurityGroup()
        testVmNicAttachSecurityGroup()
        prepareSharedBindingScenario()
        testRecallWhileBinding()
        testSharingRollbackBeforeNextRequest()
        testRecallWhileAddingRule()
        testRecallWhileTransferringVm()
        testDeleteAfterConcurrentBinding()
        testDeleteWhileChangingSharing()
    }
}
