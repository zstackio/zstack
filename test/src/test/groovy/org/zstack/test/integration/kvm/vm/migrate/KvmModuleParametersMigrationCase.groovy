package org.zstack.test.integration.kvm.vm.migrate

import org.springframework.http.HttpEntity
import org.zstack.core.CoreGlobalProperty
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.header.Constants
import org.zstack.header.host.HostVO
import org.zstack.header.tag.SystemTagVO
import org.zstack.header.tag.SystemTagVO_
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMGlobalConfig
import org.zstack.kvm.KVMSystemTags
import org.zstack.sdk.HostInventory
import org.zstack.sdk.MigrateVmAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class KvmModuleParametersMigrationCase extends SubCase {
    EnvSpec env
    VmInstanceInventory vm
    HostInventory source, target
    Map<String, Map> hostFacts = new ConcurrentHashMap<>()
    Map<String, Map> factsAfterConnect = new ConcurrentHashMap<>()
    List<Map> factReports = new CopyOnWriteArrayList<>()
    List<String> factQueries = new CopyOnWriteArrayList<>()
    List<String> firmwareHosts = new CopyOnWriteArrayList<>()
    List<Map> migrations = new CopyOnWriteArrayList<>()
    List<KVMAgentCommands.StartVmCmd> starts = new CopyOnWriteArrayList<>()

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = VmMigrateEnv.oneVmThreeHostsNfsStorage()
    }

    @Override
    void test() {
        installSimulators()
        env.create {
            vm = env.inventoryByName("vm") as VmInstanceInventory
            source = env.inventoryByName("kvm") as HostInventory
            target = env.inventoryByName("kvm1") as HostInventory
            for (String name : ["kvm", "kvm1", "kvm2"]) {
                String uuid = env.inventoryByName(name).uuid
                assert factQueries.contains(uuid)
                assertModuleTags(uuid, "true", "true")
            }

            testConnectRefreshAfterModuleReload()
            for (String mode : [KVMConstant.CPU_MODE_NONE, KVMConstant.CPU_MODE_HOST_PASSTHROUGH]) {
                startWithCpuMode(mode)
                testMatchingAndMismatchingParameters()
            }
            testReconnectRefreshesSnapshot()
            testAmdAndNonX86Hosts()
            testUnavailableParametersClearOldTag()
            testMissingParameterTag()
            testMigrateFromDestination()
            testSameMacLiveMigration()
            testSameHostAndColdRelocation()
        }
    }

    private void installSimulators() {
        env.afterSimulator(KVMConstant.KVM_HOST_FACT_PATH) { rsp, HttpEntity<String> entity ->
            String hostUuid = entity.headers.getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)
            assert hostUuid != null
            factQueries.add(hostUuid)
            // Keep all normal host facts and vary only the module-parameter protocol.
            // A raw response lets the test omit the field as an older agent would.
            Map response = JSONObjectUtil.toObject(JSONObjectUtil.toJsonString(rsp), Map.class)
            response.remove("kvmModuleParameters")
            response.putAll(hostFacts.get(hostUuid) ?: factData(intelParameters(true, true)))
            factReports.add([hostUuid: hostUuid, parameters: response.kvmModuleParameters])
            return response
        }
        env.afterSimulator(KVMConstant.KVM_CONNECT_PATH) { rsp, HttpEntity<String> entity ->
            KVMAgentCommands.ConnectCmd cmd = JSONObjectUtil.toObject(entity.body, KVMAgentCommands.ConnectCmd.class)
            Map afterConnect = factsAfterConnect.remove(cmd.hostUuid)
            if (afterConnect != null) {
                assert !cmd.pageTableExtensionDisabled
                hostFacts[cmd.hostUuid] = afterConnect
            }
            return rsp
        }
        env.simulator(KVMConstant.CLEAN_FIRMWARE_FLASH) { HttpEntity<String> entity ->
            firmwareHosts.add(entity.headers.getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID))
            return new KVMAgentCommands.AgentResponse()
        }
        env.afterSimulator(KVMConstant.KVM_MIGRATE_VM_PATH) { rsp, HttpEntity<String> entity ->
            migrations.add([
                    hostUuid: entity.headers.getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID),
                    cmd: JSONObjectUtil.toObject(entity.body, KVMAgentCommands.MigrateVmCmd.class)
            ])
            return rsp
        }
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> entity ->
            starts.add(JSONObjectUtil.toObject(entity.body, KVMAgentCommands.StartVmCmd.class))
            return rsp
        }
    }

    private void testConnectRefreshAfterModuleReload() {
        setIntelParameters(intelParameters(true, true), intelParameters(true, true))
        def creator = KVMSystemTags.KVM_MODULE_NESTED.newSystemTagCreator(target.uuid)
        creator.inherent = false
        creator.unique = false
        creator.setTagByTokens([nested: "true"])
        def userTag = creator.create()
        boolean updatePackages = CoreGlobalProperty.UPDATE_PKG_WHEN_CONNECT
        try {
            // Both post-connect paths must record the values after the module reload.
            for (boolean enabled : [true, false]) {
                CoreGlobalProperty.UPDATE_PKG_WHEN_CONNECT = enabled
                factsAfterConnect[target.uuid] = factData(intelParameters(true, false))
                refreshHost(target.uuid, factData(intelParameters(false, true)))
                assert factsAfterConnect.isEmpty()
                assert factReports*.parameters == [intelParameters(false, true), intelParameters(true, false)]
                assertModuleTags(target.uuid, "true", "false")
            }
            assert dbFindByUuid(userTag.uuid, SystemTagVO.class).tag == userTag.tag
            assertRejected(target.uuid, [source.uuid, target.uuid], "nested")
            refreshHost(source.uuid, factData(intelParameters(true, false)))
            assertMigrated(target.uuid)
            assertMigrated(source.uuid)
        } finally {
            CoreGlobalProperty.UPDATE_PKG_WHEN_CONNECT = updatePackages
            bean(DatabaseFacade.class).removeByPrimaryKey(userTag.uuid, SystemTagVO.class)
        }
        setIntelParameters(intelParameters(true, true), intelParameters(true, true))
    }

    private void startWithCpuMode(String mode) {
        stopVmInstance { uuid = vm.uuid }
        updateResourceConfig {
            category = KVMGlobalConfig.CATEGORY
            name = KVMGlobalConfig.NESTED_VIRTUALIZATION.name
            resourceUuid = vm.uuid
            value = mode
        }
        setIntelParameters(intelParameters(true, true), intelParameters(true, true))
        clearRequests()
        starts.clear()
        startVmInstance { uuid = vm.uuid; hostUuid = source.uuid }
        assert starts.size() == 1
        assert starts[0].nestedVirtualization == mode
        assert factQueries.empty
        assertRunningOn(source.uuid)
    }

    private void testMatchingAndMismatchingParameters() {
        for (String parameter : ["ept", "nested"]) {
            Map enabled = intelParameters(true, true)
            Map disabled = intelParameters(true, true)
            disabled[parameter] = false
            setIntelParameters(enabled, disabled)
            assertRejected(target.uuid, [source.uuid, target.uuid], parameter)
            setIntelParameters(disabled, enabled)
            assertRejected(target.uuid, [source.uuid, target.uuid], parameter)
            setIntelParameters(enabled, enabled)
            assertMigrated(target.uuid)
            setIntelParameters(disabled, disabled)
            assertMigrated(source.uuid)
        }
    }

    private void testReconnectRefreshesSnapshot() {
        setIntelParameters(intelParameters(true, true), intelParameters(true, true))
        // Runtime changes are collected by reconnect, never by the migration itself.
        hostFacts[target.uuid] = factData(intelParameters(false, true))
        assertMigrated(target.uuid)
        assertMigrated(source.uuid)
        assertModuleTags(target.uuid, "true", "true")

        refreshHost(target.uuid, hostFacts[target.uuid])
        assertModuleTags(target.uuid, "false", "true")
        assertRejected(target.uuid, [source.uuid, target.uuid], "ept")
        refreshHost(target.uuid, hostFacts[target.uuid])
        assertModuleTags(target.uuid, "false", "true")
        refreshHost(source.uuid, factData(intelParameters(false, true)))
        assertModuleTags(source.uuid, "false", "true")
        assertMigrated(target.uuid)
        assertMigrated(source.uuid)
    }

    private void testAmdAndNonX86Hosts() {
        for (boolean nested : [true, false]) {
            refreshHost(source.uuid, factData([nested: nested], "x86_64", "svm"))
            refreshHost(target.uuid, factData([nested: nested], "x86_64", "svm"))
            assertModuleTags(source.uuid, "na", nested.toString())
            assertModuleTags(target.uuid, "na", nested.toString())
            assertMigrated(target.uuid)
            assertMigrated(source.uuid)
            refreshHost(target.uuid, factData([nested: !nested], "x86_64", "svm"))
            assertRejected(target.uuid, [source.uuid, target.uuid], "nested")
        }
        refreshHost(source.uuid, factData(intelParameters(true, true)))
        refreshHost(target.uuid, factData([nested: true], "x86_64", "svm"))
        // The existing vendor allocator rejects VMX/SVM before the module-tag check.
        assertRejected(target.uuid, [source.uuid, target.uuid], "Intel (VMX)")

        refreshHost(source.uuid, factData([:], "aarch64", "vt"))
        refreshHost(target.uuid, factData([:], "aarch64", "vt"))
        assertModuleTags(source.uuid, "na", "na")
        assertModuleTags(target.uuid, "na", "na")
        assertMigrated(target.uuid)
        assertMigrated(source.uuid)
        refreshHost(target.uuid, factData(intelParameters(true, true)))
        assertRejected(target.uuid, [source.uuid, target.uuid], "ept")
        setIntelParameters(intelParameters(true, true), intelParameters(true, true))
    }

    private void testUnavailableParametersClearOldTag() {
        for (String brokenHost : [source.uuid, target.uuid]) {
            for (Map fact : [[cpuArchitecture: "x86_64", hvmCpuFlag: "vmx"], factData(null)]) {
                assertModuleTags(brokenHost, "true", "true")
                refreshHost(brokenHost, fact)
                assert moduleTags(brokenHost).empty
                assertRejected(target.uuid, [brokenHost], "reconnect")
                refreshHost(brokenHost, factData(intelParameters(true, true)))
                assertModuleTags(brokenHost, "true", "true")
            }
        }
        // Keep both vendors equal so the existing vendor allocator does not mask
        // the missing module data on an AMD destination.
        refreshHost(source.uuid, factData([nested: true], "x86_64", "svm"))
        refreshHost(target.uuid, factData(null, "x86_64", "svm"))
        assert moduleTags(target.uuid).empty
        assertRejected(target.uuid, [target.uuid], "reconnect")
        setIntelParameters(intelParameters(true, true), intelParameters(true, true))
    }

    private void testMissingParameterTag() {
        for (String hostUuid : [source.uuid, target.uuid]) {
            for (def tag : [KVMSystemTags.KVM_MODULE_EPT, KVMSystemTags.KVM_MODULE_NESTED]) {
                tag.deleteInherentTag(hostUuid)
                assert moduleTags(hostUuid).size() == 1
                assertRejected(target.uuid, [hostUuid], "reconnect")
                refreshHost(hostUuid, factData(intelParameters(true, true)))
                assertModuleTags(hostUuid, "true", "true")
            }
        }
    }

    private void testMigrateFromDestination() {
        setIntelParameters(intelParameters(true, true), intelParameters(false, true))
        assertRejected(target.uuid, [source.uuid, target.uuid], "ept", fromDestination: true)
        setIntelParameters(intelParameters(true, true), intelParameters(true, true))
        assertMigrated(target.uuid, true)
        assertMigrated(source.uuid)
    }

    private void testSameMacLiveMigration() {
        def zone = env.inventoryByName("zone")
        def cluster = env.inventoryByName("cluster1-1")
        def l2 = createL2VlanNetwork {
            name = "second-nic-vlan"
            zoneUuid = zone.uuid
            physicalInterface = "eth0"
            vlan = 42
        }
        try {
            attachL2NetworkToCluster { l2NetworkUuid = l2.uuid; clusterUuid = cluster.uuid }
            def l3 = createL3Network { name = "second-nic-l3"; l2NetworkUuid = l2.uuid }
            addIpRange {
                name = "second-nic-range"
                l3NetworkUuid = l3.uuid
                startIp = "192.168.120.10"
                endIp = "192.168.120.20"
                netmask = "255.255.255.0"
                gateway = "192.168.120.1"
            }

            String sharedMac = vm.vmNics.first().mac
            stopVmInstance { uuid = vm.uuid }
            changeVmNicState { vmNicUuid = vm.vmNics.first().uuid; state = "disable" }
            def attached = attachL3NetworkToVm {
                vmInstanceUuid = vm.uuid
                l3NetworkUuid = l3.uuid
                systemTags = ["customMac::${l3.uuid}::${sharedMac}".toString()]
            }
            def expected = attached.vmNics.collectEntries { [(it.uuid): it.internalName] }
            assert expected.size() == 2
            assert expected.values().toSet().size() == 2
            startVmInstance { uuid = vm.uuid; hostUuid = source.uuid }

            setIntelParameters(intelParameters(true, true), intelParameters(true, true))
            for (String destination : [target.uuid, source.uuid]) {
                assertMigrated(destination)
                def nics = migrations.first().cmd.nics
                assert nics.size() == 2
                assert nics.every { it.mac == sharedMac }
                assert nics.collectEntries { [(it.uuid): it.nicInternalName] } == expected
            }
        } finally {
            deleteL2Network { uuid = l2.uuid }
        }
    }

    private void testSameHostAndColdRelocation() {
        // No saved parameter data is required when there is no cross-host live migration.
        refreshHost(source.uuid, factData(null))
        refreshHost(target.uuid, factData(null))
        clearRequests()
        assert migrate(source.uuid).error != null
        assert factQueries.empty
        assert firmwareHosts.empty
        assert migrations.empty
        assertRunningOn(source.uuid)

        stopVmInstance { uuid = vm.uuid }
        clearRequests()
        startVmInstance { uuid = vm.uuid; hostUuid = target.uuid }
        assertRunningOn(target.uuid)
        assert factQueries.empty
        assert firmwareHosts.empty
        assert migrations.empty
    }

    private void assertRejected(Map options = [:], String destination, List<String> hostsInError, String detail) {
        String originalHost = dbFindByUuid(vm.uuid, VmInstanceVO.class).hostUuid
        clearRequests()
        def result = migrate(destination, options.fromDestination ?: false)
        assert result.error != null
        String error = JSONObjectUtil.toJsonString(result.error)
        assert error.toLowerCase().contains(detail.toLowerCase()): error
        hostsInError.each { assert error.contains(it): error }
        if (detail in ["ept", "nested"]) {
            hostsInError.each { hostUuid ->
                def tag = detail == "ept" ? KVMSystemTags.KVM_MODULE_EPT : KVMSystemTags.KVM_MODULE_NESTED
                def recorded = moduleTags(hostUuid).find { tag.isMatch(it.tag) }
                assert error.contains("uuid:${hostUuid}, value:${tag.getTokenByTag(recorded.tag, detail)}"): error
            }
        }
        assert factQueries.empty
        assert migrations.empty
        assert firmwareHosts.empty
        assertRunningOn(originalHost)
    }

    private void assertMigrated(String destination, boolean fromDestination = false) {
        String originalHost = dbFindByUuid(vm.uuid, VmInstanceVO.class).hostUuid
        clearRequests()
        def result = migrate(destination, fromDestination)
        assert result.error == null: result.error
        assert factQueries.empty
        assert firmwareHosts == [destination]
        assert migrations.size() == 1
        assert migrations[0].hostUuid == (fromDestination ? destination : originalHost)
        KVMAgentCommands.MigrateVmCmd cmd = migrations[0].cmd as KVMAgentCommands.MigrateVmCmd
        assert cmd.vmUuid == vm.uuid
        assert cmd.migrateFromDestination == fromDestination
        assertRunningOn(destination)
        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).lastHostUuid == originalHost
    }

    private void assertRunningOn(String hostUuid) {
        VmInstanceVO actual = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert actual.hostUuid == hostUuid
        assert actual.state == VmInstanceState.Running
    }

    private List<SystemTagVO> moduleTags(String hostUuid) {
        return Q.New(SystemTagVO.class)
                .eq(SystemTagVO_.resourceUuid, hostUuid)
                .eq(SystemTagVO_.resourceType, HostVO.simpleName)
                .eq(SystemTagVO_.inherent, true)
                .like(SystemTagVO_.tag, "kvmModuleParameter::%")
                .list()
    }

    private void assertModuleTags(String hostUuid, String ept, String nested) {
        assert moduleTags(hostUuid)*.tag.sort() == [
                "kvmModuleParameter::ept::${ept}".toString(),
                "kvmModuleParameter::nested::${nested}".toString()
        ]
    }

    private void refreshHost(String uuid, Map fact) {
        hostFacts[uuid] = fact
        factQueries.clear()
        factReports.clear()
        def host = reconnectHost { delegate.uuid = uuid }
        assert host.status == "Connected"
        assert !factQueries.empty
        assert factQueries.every { it == uuid }
    }

    private void setIntelParameters(Map sourceParameters, Map targetParameters) {
        refreshHost(source.uuid, factData(sourceParameters))
        refreshHost(target.uuid, factData(targetParameters))
        assertModuleTags(source.uuid, sourceParameters.ept.toString(), sourceParameters.nested.toString())
        assertModuleTags(target.uuid, targetParameters.ept.toString(), targetParameters.nested.toString())
    }

    private void clearRequests() {
        factQueries.clear()
        factReports.clear()
        firmwareHosts.clear()
        migrations.clear()
    }

    private static Map factData(Map parameters, String architecture = "x86_64", String hvmFlag = "vmx") {
        return [cpuArchitecture: architecture, hvmCpuFlag: hvmFlag, kvmModuleParameters: parameters]
    }

    private static Map intelParameters(boolean ept, boolean nested) {
        return [ept: ept, nested: nested]
    }

    private MigrateVmAction.Result migrate(String destination, boolean fromDestination = false) {
        def action = new MigrateVmAction()
        action.vmInstanceUuid = vm.uuid
        action.hostUuid = destination
        action.migrateFromDestination = fromDestination
        action.sessionId = adminSession()
        return action.call()
    }

    @Override
    void clean() {
        env.delete()
    }
}
