package org.zstack.test.integration.networkservice.provider.flat.eip

import org.springframework.http.HttpEntity
import org.zstack.core.cloudbus.CloudBus
import org.springframework.web.util.UriComponentsBuilder
import org.zstack.core.componentloader.PluginRegistry
import org.zstack.core.workflow.FlowChainBuilder
import org.zstack.header.core.workflow.Flow
import org.zstack.header.core.workflow.FlowErrorHandler
import org.zstack.header.core.workflow.FlowTrigger
import org.zstack.header.core.workflow.NoRollbackFlow
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.host.CheckVmStateOnHypervisorMsg
import org.zstack.header.host.CheckVmStateOnHypervisorReply
import org.zstack.header.host.HostConstant
import org.zstack.header.host.PingHostMsg
import org.zstack.header.network.service.AfterApplyFlatEipExtensionPoint
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.rest.RESTConstant
import org.zstack.header.rest.RESTFacade
import org.zstack.header.vm.VmAbnormalLifeCycleExtensionPoint
import org.zstack.header.vm.VmAbnormalLifeCycleStruct
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMAgentCommands
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatEipBackend
import org.zstack.network.service.flat.FlatNetwordProviderGlobalConfig
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.network.service.vip.StopVipMsg
import org.zstack.network.service.vip.StopVipReply
import org.zstack.sdk.EipInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HttpError
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import static org.zstack.core.Platform.operr

/**
 * Created by weiwang on 04/12/2017
 */
class StartFlatNetworkVmWithEipCase extends SubCase {

    EnvSpec env
    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image"
                    url = "http://zstack.org/download/test.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }

                    l3Network {
                        name = "pubL3"

                        ip {
                            startIp = "11.168.100.10"
                            endIp = "11.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.100.1"
                        }
                    }
                }

                attachBackupStorage("sftp")

                eip {
                    name = "eip"
                    useVip("pubL3")
                }
                eip {
                    name = "eip-1"
                    useVip("pubL3")
                }
                eip {
                    name = "eip-2"
                    useVip("pubL3")
                }
            }

            vm {
                name = "vm"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
            }
            vm {
                name = "vm-1"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
                useHost("kvm")
            }
            vm {
                name = "vm-2"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
                useHost("kvm")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testStartVmWithEip()
            testRecoverVmWithEip()
            testRecoverVmWithEipWithError()
            testMigrateVmWithEipZSTAC86874()
            testFailedMigrationEipUsesActualVmLocationZSTAC86874()
            testAbnormalLifecycleMovesAndRollsBackEipsZSTAC86874()
            testPingHostSyncsFlatEipAfterRollbackFailureZSTAC91420()
        }
    }

    void testStartVmWithEip(){
        def eip = env.inventoryByName("eip") as EipInventory
        def vm = env.inventoryByName("vm") as VmInstanceInventory

        attachEip {
            eipUuid = eip.uuid
            vmNicUuid = vm.vmNics[0].uuid
        }

        stopVmInstance {
            uuid = vm.uuid
        }

        startVmInstance {
            uuid = vm.uuid
        }
    }

    void testRecoverVmWithEip(){
        def eip = env.inventoryByName("eip-1") as EipInventory
        def vm = env.inventoryByName("vm-1") as VmInstanceInventory

        attachEip {
            eipUuid = eip.uuid
            vmNicUuid = vm.vmNics[0].uuid
        }

        stopVmInstance {
            uuid = vm.uuid
        }

        destroyVmInstance {
            uuid = vm.uuid
        }

        EipInventory eip1 = queryEip { conditions=["name=eip-1"] }[0]
        assert eip1.vmNicUuid == null
        assert eip1.guestIp == null

        recoverVmInstance {
            uuid = vm.uuid
        }

        startVmInstance {
            uuid = vm.uuid
        }
    }

    void testRecoverVmWithEipWithError(){
        def eip = env.inventoryByName("eip-1") as EipInventory
        def vm = env.inventoryByName("vm-1") as VmInstanceInventory

        attachEip {
            eipUuid = eip.uuid
            vmNicUuid = vm.vmNics[0].uuid
        }

        env.message(StopVipMsg.class) { StopVipMsg msg, CloudBus bus ->
            def reply = new StopVipReply()
            reply.setError(operr("on purpose"))
            bus.reply(msg, reply)
        }

        destroyVmInstance {
            uuid = vm.uuid
        }

        EipInventory eip1 = queryEip { conditions=["name=eip-1"] }[0]
        assert eip1.vmNicUuid == null
        assert eip1.guestIp == null

        recoverVmInstance {
            uuid = vm.uuid
        }

        startVmInstance {
            uuid = vm.uuid
        }
    }

    void testMigrateVmWithEipZSTAC86874() {
        def eip = env.inventoryByName("eip-1") as EipInventory
        def vm = env.inventoryByName("vm-1") as VmInstanceInventory
        def host1 = env.inventoryByName("kvm") as HostInventory
        def host2 = env.inventoryByName("kvm2") as HostInventory

        attachEip {
            eipUuid = eip.uuid
            vmNicUuid = vm.vmNics[0].uuid
        }

        List<String> operations = Collections.synchronizedList([])
        env.afterSimulator(FlatEipBackend.BATCH_PREPARE_EIP_PATH) { rsp ->
            operations.add("prepare")
            return rsp
        }

        env.afterSimulator(FlatEipBackend.BATCH_ENABLE_EIP_PATH) { rsp ->
            operations.add("enable")
            return rsp
        }

        env.afterSimulator(FlatEipBackend.BATCH_DELETE_EIP_PATH) { rsp ->
            operations.add("delete")
            throw new HttpError(403, "on purpose")
        }

        migrateVm {
            vmInstanceUuid = vm.uuid
            hostUuid = host2.uuid
        }

        retryInSecs {
            assert operations == ["prepare", "enable", "delete"]
        }

        operations.clear()
        env.afterSimulator(FlatEipBackend.BATCH_DELETE_EIP_PATH) { rsp ->
            operations.add("delete")
            return rsp
        }

        migrateVm {
            vmInstanceUuid = vm.uuid
            hostUuid = host1.uuid
        }

        retryInSecs {
            assert operations == ["prepare", "enable", "delete"]
        }
    }

    void testFailedMigrationEipUsesActualVmLocationZSTAC86874() {
        def vm = env.inventoryByName("vm-1") as VmInstanceInventory
        def unresolvedVm = env.inventoryByName("vm-2") as VmInstanceInventory
        def unresolvedEip = env.inventoryByName("eip-2") as EipInventory
        def host1 = env.inventoryByName("kvm") as HostInventory
        def host2 = env.inventoryByName("kvm2") as HostInventory

        attachEip {
            eipUuid = unresolvedEip.uuid
            vmNicUuid = unresolvedVm.vmNics[0].uuid
        }

        List<String> operations = Collections.synchronizedList([])
        env.afterSimulator(FlatEipBackend.BATCH_PREPARE_EIP_PATH) { rsp ->
            operations.add("prepare")
            return rsp
        }
        env.afterSimulator(FlatEipBackend.BATCH_ENABLE_EIP_PATH) { rsp ->
            operations.add("enable")
            return rsp
        }
        env.afterSimulator(FlatEipBackend.BATCH_DELETE_EIP_PATH) { rsp ->
            operations.add("delete")
            return rsp
        }
        env.simulator(KVMConstant.KVM_MIGRATE_VM_PATH) {
            def rsp = new KVMAgentCommands.MigrateVmResponse()
            rsp.setError("migrate failed on purpose")
            return rsp
        }
        env.message(CheckVmStateOnHypervisorMsg.class) { CheckVmStateOnHypervisorMsg msg, CloudBus bus ->
            def reply = new CheckVmStateOnHypervisorReply()
            reply.states = [(vm.uuid): msg.hostUuid == host1.uuid ?
                    VmInstanceState.Running.toString() : VmInstanceState.Stopped.toString()]
            reply.success = true
            bus.reply(msg, reply)
        }

        expect(AssertionError.class) {
            migrateVm {
                vmInstanceUuid = vm.uuid
                hostUuid = host2.uuid
            }
        }

        retryInSecs {
            assert operations == ["prepare", "delete", "enable"]
        }

        operations.clear()
        env.message(CheckVmStateOnHypervisorMsg.class) { CheckVmStateOnHypervisorMsg msg, CloudBus bus ->
            def reply = new CheckVmStateOnHypervisorReply()
            reply.states = [(vm.uuid): msg.hostUuid == host2.uuid ?
                    VmInstanceState.Running.toString() : VmInstanceState.Stopped.toString()]
            reply.success = true
            bus.reply(msg, reply)
        }

        expect(AssertionError.class) {
            migrateVm {
                vmInstanceUuid = vm.uuid
                hostUuid = host2.uuid
            }
        }

        retryInSecs {
            assert operations == ["prepare", "enable", "delete"]
        }

        operations.clear()
        env.message(CheckVmStateOnHypervisorMsg.class) { CheckVmStateOnHypervisorMsg msg, CloudBus bus ->
            def reply = new CheckVmStateOnHypervisorReply()
            reply.states = [(unresolvedVm.uuid): VmInstanceState.Stopped.toString()]
            reply.success = true
            bus.reply(msg, reply)
        }

        expect(AssertionError.class) {
            migrateVm {
                vmInstanceUuid = unresolvedVm.uuid
                hostUuid = host2.uuid
            }
        }

        retryInSecs {
            assert operations == ["prepare"]
        }
        env.cleanSimulatorAndMessageHandlers()
    }

    void testAbnormalLifecycleMovesAndRollsBackEipsZSTAC86874() {
        def vm = queryVmInstance { conditions = ["name=vm-1"] }[0]
        if (vm.state == "Stopped") {
            vm = startVmInstance { uuid = vm.uuid }
        }
        assert vm.state == "Running"
        assert vm.hostUuid != null
        def host1 = env.inventoryByName("kvm") as HostInventory
        def host2 = env.inventoryByName("kvm2") as HostInventory
        List<VmAbnormalLifeCycleExtensionPoint> extensions = bean(PluginRegistry.class)
                .getExtensionList(VmAbnormalLifeCycleExtensionPoint.class)
        def failAfterNetwork = new VmAbnormalLifeCycleExtensionPoint() {
            @Override
            Flow createVmAbnormalLifeCycleHandlingFlow(VmAbnormalLifeCycleStruct struct) {
                return new NoRollbackFlow() {
                    @Override
                    void run(FlowTrigger trigger, Map data) {
                        if (struct.vmInstance.uuid == vm.uuid) {
                            trigger.fail(operr("rollback on purpose"))
                        } else {
                            trigger.next()
                        }
                    }
                }
            }
        }

        vm = queryVmInstance { conditions = ["uuid=${vm.uuid}"] }[0]
        String source = vm.hostUuid
        String destination = source == host1.uuid ? host2.uuid : host1.uuid
        List<String> operations = Collections.synchronizedList([])
        env.afterSimulator(FlatEipBackend.BATCH_APPLY_EIP_PATH) { rsp ->
            operations.add("apply")
            return rsp
        }
        env.afterSimulator(FlatEipBackend.BATCH_DELETE_EIP_PATH) { rsp ->
            operations.add("delete")
            return rsp
        }

        // Inject a failure after the real network extension to exercise its rollback.
        extensions.add(failAfterNetwork)
        try {
            reportVmState(vm.uuid, destination, "Running")
            retryInSecs { assert operations == ["apply", "delete", "apply", "delete"] }
            assert queryVmInstance { conditions = ["uuid=${vm.uuid}"] }[0].hostUuid == source
        } finally {
            extensions.remove(failAfterNetwork)
        }

        operations.clear()
        boolean failOriginalDelete = true
        env.afterSimulator(FlatEipBackend.BATCH_DELETE_EIP_PATH) { rsp ->
            operations.add("delete")
            if (failOriginalDelete) {
                failOriginalDelete = false
                rsp.success = false
                rsp.error = "delete original EIP on purpose"
            }
            return rsp
        }
        reportVmState(vm.uuid, destination, "Running")
        retryInSecs { assert operations == ["apply", "delete", "delete"] }
        assert queryVmInstance { conditions = ["uuid=${vm.uuid}"] }[0].hostUuid == source

        operations.clear()
        env.afterSimulator(FlatEipBackend.BATCH_DELETE_EIP_PATH) { rsp ->
            operations.add("delete")
            return rsp
        }
        reportVmState(vm.uuid, destination, "Running")
        retryInSecs {
            def updated = queryVmInstance { conditions = ["uuid=${vm.uuid}"] }[0]
            assert updated.hostUuid == destination
            assert updated.state == "Running"
            assert operations == ["apply", "delete"]
        }
        env.cleanSimulatorAndMessageHandlers()
    }

    private void reportVmState(String vmUuid, String hostUuid, String state) {
        RESTFacade rest = bean(RESTFacade.class)
        String url = UriComponentsBuilder.fromHttpUrl(rest.baseUrl)
                .path(RESTConstant.COMMAND_CHANNEL_PATH).build().toUriString()
        def cmd = new KVMAgentCommands.ReportVmStateCmd(vmUuid: vmUuid, hostUuid: hostUuid, vmState: state)
        rest.syncJsonPost(url, JSONObjectUtil.toJsonString(cmd),
                [(RESTConstant.COMMAND_PATH): KVMConstant.KVM_REPORT_VM_STATE], String.class)
    }

    private void sendPingHost(String targetHostUuid) {
        CloudBus bus = bean(CloudBus.class)
        PingHostMsg pmsg = new PingHostMsg()
        pmsg.hostUuid = targetHostUuid
        bus.makeTargetServiceIdByResourceUuid(pmsg, HostConstant.SERVICE_ID, targetHostUuid)
        assert bus.call(pmsg).success
    }

    void testPingHostSyncsFlatEipAfterRollbackFailureZSTAC91420() {
        def eip = queryEip { conditions = ["name=eip"] }[0] as EipInventory
        def eip1 = queryEip { conditions = ["name=eip-1"] }[0] as EipInventory
        def eip2 = queryEip { conditions = ["name=eip-2"] }[0] as EipInventory
        def vm = queryVmInstance { conditions = ["name=vm"] }[0] as VmInstanceInventory
        def vm1 = queryVmInstance { conditions = ["name=vm-1"] }[0] as VmInstanceInventory
        def vm2 = queryVmInstance { conditions = ["name=vm-2"] }[0] as VmInstanceInventory
        def host1 = env.inventoryByName("kvm") as HostInventory
        def host2 = env.inventoryByName("kvm2") as HostInventory

        if (vm.state == VmInstanceState.Running.toString()) {
            stopVmInstance {
                uuid = vm.uuid
            }
        }
        if (vm2.state == VmInstanceState.Running.toString()) {
            stopVmInstance {
                uuid = vm2.uuid
            }
        }
        if (vm1.state == VmInstanceState.Running.toString()) {
            stopVmInstance {
                uuid = vm1.uuid
            }
        }
        startVmInstance {
            uuid = vm1.uuid
            hostUuid = host1.uuid
        }

        updateGlobalConfig {
            category = FlatNetwordProviderGlobalConfig.CATRGORY
            name = FlatNetwordProviderGlobalConfig.EIP_SYNC_ON_HOST_PING_COUNT.name
            value = "0"
        }

        def pluginRgty = bean(org.zstack.core.componentloader.PluginRegistry.class)
        FlatEipBackend flatEipBackend = bean(FlatEipBackend.class)
        List<Tuple2<List<String>, String>> appliedVipEvents = Collections.synchronizedList([])
        AfterApplyFlatEipExtensionPoint dynamicExt = new AfterApplyFlatEipExtensionPoint() {
            @Override
            void AfterApplyFlatEip(List<String> vipUuids, String hUuid) {
                appliedVipEvents.add(new Tuple2<>(new ArrayList<>(vipUuids), hUuid))
            }
        }
        pluginRgty.defineDynamicExtension(AfterApplyFlatEipExtensionPoint.class, dynamicExt)

        try {
            List<FlatEipBackend.SyncEipCmd> host1SyncCmds = Collections.synchronizedList([])
            List<FlatEipBackend.SyncEipCmd> host2SyncCmds = Collections.synchronizedList([])
            int host1SyncAttempt = 0
            env.simulator(FlatEipBackend.SYNC_EIP_PATH) { HttpEntity<String> entity ->
                String hUuid = entity.getHeaders().getFirst(org.zstack.header.Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)
                FlatEipBackend.SyncEipCmd body = json(entity.getBody(), FlatEipBackend.SyncEipCmd.class)
                def rsp = new FlatEipBackend.SyncEipRsp()
                if (hUuid == host1.uuid) {
                    host1SyncCmds.add(body)
                    host1SyncAttempt++
                    if (host1SyncAttempt == 1) {
                        rsp.success = false
                        rsp.error = "partial sync error on purpose"
                        rsp.appliedVipUuids = [eip1.vipUuid]
                    }
                } else if (hUuid == host2.uuid) {
                    host2SyncCmds.add(body)
                }
                return rsp
            }

            env.simulator(KVMConstant.KVM_START_VM_PATH) {
                def rsp = new KVMAgentCommands.StartVmResponse()
                rsp.setError("start vm timeout on purpose")
                return rsp
            }
            env.afterSimulator(FlatEipBackend.BATCH_DELETE_EIP_PATH) { rsp ->
                rsp.success = false
                rsp.error = "delete eip failed during rollback without GC on purpose"
                return rsp
            }

            expect(AssertionError.class) {
                startVmInstance {
                    uuid = vm.uuid
                    hostUuid = host1.uuid
                }
            }

            assert queryGCJob {
                conditions = [
                        "context~=%${eip.uuid}%".toString(),
                        "runnerClass=org.zstack.network.service.flat.FlatEipGC",
                ]
            }.isEmpty()

            VmInstanceVO stoppedVo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
            assert stoppedVo.state == VmInstanceState.Stopped
            assert stoppedVo.hostUuid == null
            EipInventory reloadedEip = queryEip { conditions = ["uuid=${eip.uuid}"] }[0]
            assert reloadedEip.vmNicUuid == vm.vmNics[0].uuid

            // Even with periodic count = 0, dirty host1 still syncs immediately on ping;
            // 1st dirty-triggered sync fails partially: notifies extension AND re-marks host1 dirty
            appliedVipEvents.clear()
            sendPingHost(host1.uuid)
            retryInSecs {
                assert host1SyncCmds.size() == 1
                assert appliedVipEvents.size() == 1
                assert !flatEipBackend.isHostEipSyncInProgress(host1.uuid)
            }
            assert appliedVipEvents[0].first == [eip1.vipUuid]
            assert appliedVipEvents[0].second == host1.uuid
            List<String> syncedExpectedUuids = (host1SyncCmds[0].eips ?: []).collect { it.eipUuid }
            assert syncedExpectedUuids == [eip1.uuid]
            assert !syncedExpectedUuids.contains(eip.uuid)
            assert !syncedExpectedUuids.contains(eip2.uuid)
            assert (host1SyncCmds[0].inFlightEipUuids ?: []).isEmpty()

            // Next single ping immediately retries sync because failed sync re-marked host1 dirty
            sendPingHost(host1.uuid)
            retryInSecs {
                assert host1SyncCmds.size() == 2
                assert !flatEipBackend.isHostEipSyncInProgress(host1.uuid)
            }

            // Once clean & baseline-synced, count = 0 disables periodic sync
            host1SyncCmds.clear()
            appliedVipEvents.clear()
            sendPingHost(host1.uuid)
            sendPingHost(host1.uuid)
            assert host1SyncCmds.isEmpty()

            updateGlobalConfig {
                category = FlatNetwordProviderGlobalConfig.CATRGORY
                name = FlatNetwordProviderGlobalConfig.EIP_SYNC_ON_HOST_PING_COUNT.name
                value = "3"
            }

            sendPingHost(host1.uuid)
            sendPingHost(host1.uuid)
            assert host1SyncCmds.isEmpty()

            sendPingHost(host1.uuid)
            retryInSecs {
                assert host1SyncCmds.size() == 1
                assert !flatEipBackend.isHostEipSyncInProgress(host1.uuid)
            }
            assert appliedVipEvents.isEmpty()

            // Concurrent detachEip excludes revoking EIP from expected and reports it in inFlightEipUuids
            host1SyncCmds.clear()
            env.cleanSimulatorAndMessageHandlers()
            env.simulator(FlatEipBackend.SYNC_EIP_PATH) { HttpEntity<String> entity ->
                String hUuid = entity.getHeaders().getFirst(org.zstack.header.Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)
                FlatEipBackend.SyncEipCmd body = json(entity.getBody(), FlatEipBackend.SyncEipCmd.class)
                if (hUuid == host1.uuid) {
                    host1SyncCmds.add(body)
                } else if (hUuid == host2.uuid) {
                    host2SyncCmds.add(body)
                }
                return new FlatEipBackend.SyncEipRsp()
            }
            updateGlobalConfig {
                category = FlatNetwordProviderGlobalConfig.CATRGORY
                name = FlatNetwordProviderGlobalConfig.EIP_SYNC_ON_HOST_PING_COUNT.name
                value = "1"
            }
            env.afterSimulator(FlatEipBackend.DELETE_EIP_PATH) { rsp ->
                sendPingHost(host1.uuid)
                retryInSecs {
                    assert !host1SyncCmds.isEmpty()
                }
                return rsp
            }
            detachEip {
                uuid = eip1.uuid
            }
            retryInSecs {
                assert !flatEipBackend.isHostEipSyncInProgress(host1.uuid)
            }
            assert !((host1SyncCmds[0].eips ?: []).collect { it.eipUuid }.contains(eip1.uuid))
            assert (host1SyncCmds[0].inFlightEipUuids ?: []).contains(eip1.uuid)

            env.cleanSimulatorAndMessageHandlers()
            host1SyncCmds.clear()
            env.simulator(FlatEipBackend.SYNC_EIP_PATH) { HttpEntity<String> entity ->
                String hUuid = entity.getHeaders().getFirst(org.zstack.header.Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)
                FlatEipBackend.SyncEipCmd body = json(entity.getBody(), FlatEipBackend.SyncEipCmd.class)
                if (hUuid == host1.uuid) {
                    host1SyncCmds.add(body)
                } else if (hUuid == host2.uuid) {
                    host2SyncCmds.add(body)
                }
                return new FlatEipBackend.SyncEipRsp()
            }
            attachEip {
                eipUuid = eip1.uuid
                vmNicUuid = vm1.vmNics[0].uuid
            }
            sendPingHost(host1.uuid)
            retryInSecs {
                assert host1SyncCmds.size() == 1
                assert !flatEipBackend.isHostEipSyncInProgress(host1.uuid)
            }
            assert (host1SyncCmds[0].eips ?: []).collect { it.eipUuid } == [eip1.uuid]

            // Malformed revoke reply must still release in-flight tracking and mark host dirty
            host1SyncCmds.clear()
            updateGlobalConfig {
                category = FlatNetwordProviderGlobalConfig.CATRGORY
                name = FlatNetwordProviderGlobalConfig.EIP_SYNC_ON_HOST_PING_COUNT.name
                value = "0"
            }
            env.simulator(FlatEipBackend.DELETE_EIP_PATH) {
                return [success: ["malformed"]]
            }
            expect(AssertionError.class) {
                detachEip {
                    uuid = eip1.uuid
                }
            }
            env.simulator(FlatEipBackend.DELETE_EIP_PATH) {
                return new FlatNetworkServiceConstant.AgentRsp()
            }
            sendPingHost(host1.uuid)
            retryInSecs {
                assert host1SyncCmds.size() == 1
                assert !flatEipBackend.isHostEipSyncInProgress(host1.uuid)
            }
            assert (host1SyncCmds[0].inFlightEipUuids ?: []).isEmpty()
            assert (host1SyncCmds[0].eips ?: []).collect { it.eipUuid } == [eip1.uuid]
            updateGlobalConfig {
                category = FlatNetwordProviderGlobalConfig.CATRGORY
                name = FlatNetwordProviderGlobalConfig.EIP_SYNC_ON_HOST_PING_COUNT.name
                value = "1"
            }

            host2SyncCmds.clear()
            env.afterSimulator(KVMConstant.KVM_MIGRATE_VM_PATH) { rsp ->
                sendPingHost(host2.uuid)
                retryInSecs {
                    assert !host2SyncCmds.isEmpty()
                }
                return rsp
            }

            migrateVm {
                vmInstanceUuid = vm1.uuid
                hostUuid = host2.uuid
            }
            assert (host2SyncCmds[0].inFlightEipUuids ?: []).contains(eip1.uuid)
            List<String> destExpectedDuringMigrate = (host2SyncCmds[0].eips ?: []).collect { it.eipUuid }
            assert !destExpectedDuringMigrate.contains(eip1.uuid)

            migrateVm {
                vmInstanceUuid = vm1.uuid
                hostUuid = host1.uuid
            }
            updateGlobalConfig {
                category = FlatNetwordProviderGlobalConfig.CATRGORY
                name = FlatNetwordProviderGlobalConfig.EIP_SYNC_ON_HOST_PING_COUNT.name
                value = "10"
            }
        } finally {
            pluginRgty.getExtensionList(AfterApplyFlatEipExtensionPoint.class).remove(dynamicExt)
            env.cleanSimulatorAndMessageHandlers()
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
