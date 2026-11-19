package org.zstack.test.integration.kvm.nic

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmSystemTags
import org.zstack.core.db.Q
import org.zstack.header.network.l3.L3NetworkStateEvent
import org.zstack.header.network.l3.UsedIpVO
import org.zstack.header.network.l3.UsedIpVO_
import org.zstack.header.network.service.NetworkServiceProviderVO
import org.zstack.header.network.service.NetworkServiceProviderVO_
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmNicVO
import org.zstack.header.vm.devices.DeviceAddress
import org.zstack.header.vm.devices.VirtualDeviceInfo
import org.zstack.header.vm.devices.VmInstanceDeviceManager
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMSystemTags
import org.zstack.network.l3.IpRangeHelper
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.flat.FlatNetworkSystemTags
import org.zstack.sdk.ChangeVmNicNetworkAction
import org.zstack.sdk.CreateVmInstanceAction
import org.zstack.sdk.GetIpAddressCapacityResult
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.IpRangeInventory
import org.zstack.sdk.L2NetworkInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.SetVmStaticIpAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
/**
 * Created by lining on 2018/5/19.
 */
class VmNicBasicCase extends SubCase {
    EnvSpec env

    VmNicInventory nic
    String usedIpUuid

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
            testCreateVmNic()
            testAttachVmNicToVm()
            testDetachVmNic()
            testDeleteVmNic()
            testStaticIpLifecycleAndConcurrency()
            testSetStaticIpRejectsL3Gateway()
            testSameMacMultiNicPciAddressMapping()
        }
    }

    void testCreateVmNic() {
        L3NetworkInventory pubL3 = env.inventoryByName("pubL3")

        // an invalid static ip
        expect(AssertionError.class) {
            createVmNic {
                l3NetworkUuid = pubL3.uuid
                ip = "0.0.0.1"
            }
        }

        changeL3NetworkState {
            uuid = pubL3.uuid
            stateEvent = L3NetworkStateEvent.disable
        }

        // attach a disable network to vm nic
        expect(AssertionError.class) {
            createVmNic {
                l3NetworkUuid = pubL3.uuid
            }
        }

        changeL3NetworkState {
            uuid = pubL3.uuid
            stateEvent = L3NetworkStateEvent.enable
        }

        nic = createVmNic {
            l3NetworkUuid = pubL3.uuid
        }
        assert nic.uuid != null
        assert nic.l3NetworkUuid == pubL3.uuid
        assert nic.deviceId == 1
        assert nic.vmInstanceUuid == null
        assert nic.ip != null
        assert nic.mac != null
        assert nic.usedIps.size() != 0
        assert dbFindByUuid(nic.uuid, VmNicVO.class).driverType == null

        IpRangeInventory ipRangeInventory = pubL3.ipRanges.get(0)
        assert nic.gateway == ipRangeInventory.gateway
        assert nic.netmask == ipRangeInventory.netmask

        assert Q.New(UsedIpVO.class).eq(UsedIpVO_.vmNicUuid, nic.uuid).isExists()

        // an used static ip
        expect(AssertionError.class) {
            createVmNic {
                l3NetworkUuid = pubL3.uuid
                ip = nic.ip
            }
        }
    }

    void testAttachVmNicToVm () {
        L3NetworkInventory l3 = env.inventoryByName("l3")
        VmInstanceInventory vm = env.inventoryByName("vm")
        String expectedDriverType = vm.vmNics[0].driverType

        changeL3NetworkState {
            uuid = nic.l3NetworkUuid
            stateEvent = L3NetworkStateEvent.disable
        }

        // attach a disable network to vm
        expect(AssertionError.class) {
            attachVmNicToVm {
                vmInstanceUuid = vm.uuid
                vmNicUuid = nic.uuid
            }
        }

        changeL3NetworkState {
            uuid = nic.l3NetworkUuid
            stateEvent = L3NetworkStateEvent.enable
        }

        pauseVmInstance {
            uuid = vm.uuid
        }

        // attach the nic to a vm state paused
        expect(AssertionError.class) {
            attachVmNicToVm {
                vmInstanceUuid = vm.uuid
                vmNicUuid = nic.uuid
            }
        }

        resumeVmInstance {
            uuid = vm.uuid
        }

        vm.defaultL3NetworkUuid = l3.uuid
        vm = attachVmNicToVm {
            vmInstanceUuid = vm.uuid
            vmNicUuid = nic.uuid
        }
        vm.defaultL3NetworkUuid = l3.uuid

        VmInstanceVO vmInstanceVO = dbFindByUuid(vm.getUuid(), VmInstanceVO.class)

        VmNicVO vmNicVO = dbFindByUuid(nic.uuid, VmNicVO.class)
        assert vmNicVO.deviceId == 1
        assert vmNicVO.internalName == VmNicVO.generateNicInternalName(vmInstanceVO.getInternalId(), 1)
        assert vmNicVO.vmInstanceUuid == vm.getUuid()
        assert vmNicVO.driverType == expectedDriverType
        assert vm.vmNics.find { it.uuid == nic.uuid }.driverType == expectedDriverType

        usedIpUuid = vmNicVO.usedIpUuid

        // re-attach the same nic
        expect(AssertionError.class) {
            attachVmNicToVm {
                vmInstanceUuid = vm.uuid
                vmNicUuid = nic.uuid
            }
        }

    }

    void testDetachVmNic() {
        L3NetworkInventory l3 = env.inventoryByName("l3")
        VmInstanceInventory vm = env.inventoryByName("vm")

        detachL3NetworkFromVm {
            vmNicUuid = nic.uuid
            systemTags = [VmSystemTags.RELEASE_NIC_AFTER_DETACH_NIC.instantiateTag([(VmSystemTags.RELEASE_NIC_AFTER_DETACH_NIC_TOKEN): false])]
        }

        VmInstanceVO vmInstanceVO = dbFindByUuid(vm.getUuid(), VmInstanceVO.class)
        vmInstanceVO.defaultL3NetworkUuid = l3.uuid

        VmNicVO vmNicVO = dbFindByUuid(nic.uuid, VmNicVO.class)
        assert vmNicVO.deviceId == 1
        assert vmNicVO.internalName == null
        assert vmNicVO.vmInstanceUuid == null
    }

    void testDeleteVmNic() {
        deleteVmNic {
            uuid = nic.uuid
        }
        VmNicVO vmNicVO = dbFindByUuid(nic.uuid, VmNicVO.class)
        assert vmNicVO == null

        UsedIpVO usedIpVO = dbFindByUuid(usedIpUuid, UsedIpVO.class)
        assert usedIpVO == null
    }

    void testStaticIpLifecycleAndConcurrency() {
        L2NetworkInventory l2 = env.inventoryByName("l2")
        InstanceOfferingInventory offering = createInstanceOffering {
            name = "tiny-concurrent-offering"
            cpuNum = 1
            memorySize = SizeUnit.MEGABYTE.toByte(128)
        }
        ImageInventory image = env.inventoryByName("image1")
        String flatProviderUuid = Q.New(NetworkServiceProviderVO.class)
                .eq(NetworkServiceProviderVO_.type, FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING)
                .select(NetworkServiceProviderVO_.uuid)
                .findValue()

        L3NetworkInventory noDhcpL3 = createL3Network {
            name = "no-dhcp-static-l3"
            l2NetworkUuid = l2.uuid
        }

        String staticIp = "192.168.210.50"
        String expectedUsedIpUuid = IpRangeHelper.makeDeterministicUsedIpUuid(noDhcpL3.uuid, staticIp)
        List<String> staticTags = [
                VmSystemTags.STATIC_IP.instantiateTag([
                        (VmSystemTags.STATIC_IP_L3_UUID_TOKEN): noDhcpL3.uuid,
                        (VmSystemTags.STATIC_IP_TOKEN)        : staticIp
                ]),
                VmSystemTags.IPV4_NETMASK.instantiateTag([
                        (VmSystemTags.IPV4_NETMASK_L3_UUID_TOKEN): noDhcpL3.uuid,
                        (VmSystemTags.IPV4_NETMASK_TOKEN)        : "255.255.255.0"
                ]),
                VmSystemTags.IPV4_GATEWAY.instantiateTag([
                        (VmSystemTags.IPV4_GATEWAY_L3_UUID_TOKEN): noDhcpL3.uuid,
                        (VmSystemTags.IPV4_GATEWAY_TOKEN)        : "192.168.210.1"
                ])
        ]

        int threads = 4
        CountDownLatch startGate = new CountDownLatch(1)
        CountDownLatch doneGate = new CountDownLatch(threads)
        AtomicInteger successCount = new AtomicInteger(0)
        AtomicInteger failCount = new AtomicInteger(0)
        List<VmInstanceInventory> createdVms = Collections.synchronizedList(new ArrayList<>())
        List<org.zstack.sdk.ErrorCode> failedErrors = Collections.synchronizedList(new ArrayList<>())

        for (int i = 0; i < threads; i++) {
            final int idx = i
            Thread.start {
                try {
                    startGate.await(10, TimeUnit.SECONDS)
                    CreateVmInstanceAction action = new CreateVmInstanceAction(
                            name: "concurrent-static-vm-${idx}",
                            instanceOfferingUuid: offering.uuid,
                            imageUuid: image.uuid,
                            l3NetworkUuids: [noDhcpL3.uuid],
                            systemTags: new ArrayList<>(staticTags),
                            sessionId: adminSession()
                    )
                    CreateVmInstanceAction.Result res = action.call()
                    if (res.error == null) {
                        createdVms.add(res.value.inventory)
                        successCount.incrementAndGet()
                    } else {
                        failedErrors.add(res.error)
                        failCount.incrementAndGet()
                    }
                } finally {
                    doneGate.countDown()
                }
            }
        }
        startGate.countDown()
        assert doneGate.await(60, TimeUnit.SECONDS)
        assert successCount.get() == 1
        assert failCount.get() == threads - 1
        assert failedErrors.size() == threads - 1
        failedErrors.each { err ->
            assert err.cause != null
            assert err.cause.code == "SYS.1007"
            assert err.details.contains("has already been allocated")
        }

        List<UsedIpVO> usedIps = Q.New(UsedIpVO.class)
                .eq(UsedIpVO_.l3NetworkUuid, noDhcpL3.uuid)
                .eq(UsedIpVO_.ip, staticIp)
                .list()
        assert usedIps.size() == 1
        assert usedIps[0].uuid == expectedUsedIpUuid
        assert usedIps[0].ipRangeUuid == null

        VmInstanceInventory winnerVm = createdVms[0]
        stopVmInstance {
            uuid = winnerVm.uuid
        }
        String tempIp = "192.168.210.51"
        setVmStaticIp {
            vmInstanceUuid = winnerVm.uuid
            l3NetworkUuid = noDhcpL3.uuid
            ip = tempIp
            netmask = "255.255.255.0"
            gateway = "192.168.210.1"
        }
        assert dbFindByUuid(expectedUsedIpUuid, UsedIpVO.class) == null
        assert dbFindByUuid(IpRangeHelper.makeDeterministicUsedIpUuid(noDhcpL3.uuid, tempIp), UsedIpVO.class) != null

        setVmStaticIp {
            vmInstanceUuid = winnerVm.uuid
            l3NetworkUuid = noDhcpL3.uuid
            ip = staticIp
            netmask = "255.255.255.0"
            gateway = "192.168.210.254"
        }
        UsedIpVO afterSameIpUpdate = dbFindByUuid(expectedUsedIpUuid, UsedIpVO.class)
        assert afterSameIpUpdate != null
        assert afterSameIpUpdate.gateway == "192.168.210.254"

        IpRangeInventory iprOther = addIpRange {
            name = "ipr-210-other"
            l3NetworkUuid = noDhcpL3.uuid
            startIp = "192.168.210.101"
            endIp = "192.168.210.200"
            netmask = "255.255.255.0"
            gateway = "192.168.210.1"
        }
        UsedIpVO afterAddOtherIpr = dbFindByUuid(expectedUsedIpUuid, UsedIpVO.class)
        assert afterAddOtherIpr != null
        assert afterAddOtherIpr.ipRangeUuid == null

        addReservedIpRange {
            l3NetworkUuid = noDhcpL3.uuid
            startIp = "192.168.210.48"
            endIp = "192.168.210.52"
        }

        deleteIpRange {
            uuid = iprOther.uuid
        }
        UsedIpVO afterDelOtherIpr = dbFindByUuid(expectedUsedIpUuid, UsedIpVO.class)
        assert afterDelOtherIpr != null
        assert afterDelOtherIpr.ipRangeUuid == null

        IpRangeInventory ipr2 = addIpRange {
            name = "ipr-210-matching"
            l3NetworkUuid = noDhcpL3.uuid
            startIp = "192.168.210.10"
            endIp = "192.168.210.100"
            netmask = "255.255.255.0"
            gateway = "192.168.210.1"
        }
        UsedIpVO afterReAddIpr = dbFindByUuid(expectedUsedIpUuid, UsedIpVO.class)
        assert afterReAddIpr != null
        assert afterReAddIpr.ipRangeUuid == ipr2.uuid
        assert afterReAddIpr.vmNicUuid == winnerVm.vmNics[0].uuid
        assert dbFindByUuid(IpRangeHelper.makeDeterministicUsedIpUuid(noDhcpL3.uuid, "192.168.210.48"), UsedIpVO.class) != null
        assert dbFindByUuid(IpRangeHelper.makeDeterministicUsedIpUuid(noDhcpL3.uuid, "192.168.210.52"), UsedIpVO.class) != null

        expect(AssertionError.class) {
            attachNetworkServiceToL3Network {
                l3NetworkUuid = noDhcpL3.uuid
                networkServices = [(flatProviderUuid): ["DHCP"]]
                systemTags = [FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.instantiateTag([
                        (FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_TOKEN)     : staticIp,
                        (FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_UUID_TOKEN): "null"
                ])]
            }
        }

        attachNetworkServiceToL3Network {
            l3NetworkUuid = noDhcpL3.uuid
            networkServices = [(flatProviderUuid): ["DHCP"]]
        }

        addIpv6Range {
            name = "ipr6-210"
            l3NetworkUuid = noDhcpL3.uuid
            startIp = "2001:db8:210::10"
            endIp = "2001:db8:210::100"
            gateway = "2001:db8:210::1"
            prefixLen = 64
            addressMode = "Stateful-DHCP"
        }

        changeL3NetworkDhcpIpAddress {
            l3NetworkUuid = noDhcpL3.uuid
            dhcpv6ServerIp = "2001:db8:210:0:0:0:0:5"
        }

        expect(AssertionError.class) {
            createVmInstance {
                name = "dhcp-enabled-dup-static-vm"
                instanceOfferingUuid = offering.uuid
                imageUuid = image.uuid
                l3NetworkUuids = [noDhcpL3.uuid]
                systemTags = [VmSystemTags.STATIC_IP.instantiateTag([
                        (VmSystemTags.STATIC_IP_L3_UUID_TOKEN): noDhcpL3.uuid,
                        (VmSystemTags.STATIC_IP_TOKEN)        : staticIp
                ])]
            }
        }

        destroyVmInstance {
            uuid = winnerVm.uuid
        }
        expungeVmInstance {
            uuid = winnerVm.uuid
        }
        deleteL3Network {
            uuid = noDhcpL3.uuid
        }
        deleteInstanceOffering {
            uuid = offering.uuid
        }
    }

    void testSetStaticIpRejectsL3Gateway() {
        L2NetworkInventory l2 = env.inventoryByName("l2")
        ImageInventory image = env.inventoryByName("image1")
        L3NetworkInventory pubL3 = env.inventoryByName("pubL3")
        String flatProviderUuid = Q.New(NetworkServiceProviderVO.class)
                .select(NetworkServiceProviderVO_.uuid)
                .eq(NetworkServiceProviderVO_.type, FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING)
                .findValue()

        InstanceOfferingInventory offering = createInstanceOffering {
            name = "gw-static-ip-offering"
            memorySize = SizeUnit.MEGABYTE.toByte(512)
            cpuNum = 1
        }

        L3NetworkInventory flatL3 = createL3Network {
            name = "flat-l3-gw-guard"
            l2NetworkUuid = l2.uuid
        }
        addIpRange {
            name = "ipr-v4-gw-guard"
            l3NetworkUuid = flatL3.uuid
            startIp = "10.211.83.2"
            endIp = "10.211.83.254"
            netmask = "255.255.255.0"
            gateway = "10.211.83.1"
        }
        addIpv6Range {
            name = "ipr-v6-gw-guard"
            l3NetworkUuid = flatL3.uuid
            startIp = "2001:db8:83::2"
            endIp = "2001:db8:83::ff"
            gateway = "2001:db8:83::1"
            prefixLen = 64
            addressMode = "Stateful-DHCP"
        }
        attachNetworkServiceToL3Network {
            l3NetworkUuid = flatL3.uuid
            networkServices = [(flatProviderUuid): ["DHCP"]]
        }

        VmInstanceInventory vm = createVmInstance {
            name = "vm-gw-guard"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [flatL3.uuid]
        }

        VmNicInventory nicBefore = queryVmNic {
            conditions = ["vmInstanceUuid=${vm.uuid}"]
        }[0]
        String origIp = nicBefore.ip
        String origUsedIpUuid = dbFindByUuid(nicBefore.uuid, VmNicVO.class).usedIpUuid
        List<String> origUsedIps = nicBefore.usedIps.collect { it.ip }.sort()
        GetIpAddressCapacityResult capBefore = getIpAddressCapacity {
            l3NetworkUuids = [flatL3.uuid]
        }

        // 1. SetVmStaticIp with IPv4 L3 gateway (omitting netmask & gateway) must fail
        def action1 = new SetVmStaticIpAction()
        action1.vmInstanceUuid = vm.uuid
        action1.l3NetworkUuid = flatL3.uuid
        action1.ip = "10.211.83.1"
        action1.sessionId = adminSession()
        def res1 = action1.call()
        assert res1.error != null : "SetVmStaticIp with IPv4 L3 gateway must be rejected"
        assert res1.error.details.contains("gateway")

        // 2. SetVmStaticIp with IPv4 L3 gateway and explicit netmask & gateway must also fail
        def action2 = new SetVmStaticIpAction()
        action2.vmInstanceUuid = vm.uuid
        action2.l3NetworkUuid = flatL3.uuid
        action2.ip = "10.211.83.1"
        action2.netmask = "255.255.255.0"
        action2.gateway = "10.211.83.1"
        action2.sessionId = adminSession()
        def res2 = action2.call()
        assert res2.error != null : "SetVmStaticIp with explicit IPv4 L3 gateway must be rejected"
        assert res2.error.details.contains("gateway")

        // 3. SetVmStaticIp with IPv6 L3 gateway must fail
        def action3 = new SetVmStaticIpAction()
        action3.vmInstanceUuid = vm.uuid
        action3.l3NetworkUuid = flatL3.uuid
        action3.ip6 = "2001:db8:83::1"
        action3.sessionId = adminSession()
        def res3 = action3.call()
        assert res3.error != null : "SetVmStaticIp with IPv6 L3 gateway must be rejected"
        assert res3.error.details.contains("gateway")

        // Verify zero side effects on NIC, UsedIp, L3 capacity, and staticIp tags
        VmNicInventory nicAfter = queryVmNic {
            conditions = ["uuid=${nicBefore.uuid}"]
        }[0]
        assert nicAfter.ip == origIp
        assert dbFindByUuid(nicBefore.uuid, VmNicVO.class).usedIpUuid == origUsedIpUuid
        assert nicAfter.usedIps.collect { it.ip }.sort() == origUsedIps
        assert !Q.New(UsedIpVO.class).eq(UsedIpVO_.l3NetworkUuid, flatL3.uuid).eq(UsedIpVO_.ip, "10.211.83.1").isExists()
        assert !Q.New(UsedIpVO.class).eq(UsedIpVO_.l3NetworkUuid, flatL3.uuid).eq(UsedIpVO_.ip, "2001:db8:83::1").isExists()
        assert VmSystemTags.STATIC_IP.getTags(vm.uuid).isEmpty()
        GetIpAddressCapacityResult capAfter = getIpAddressCapacity {
            l3NetworkUuids = [flatL3.uuid]
        }
        assert capAfter.availableCapacity == capBefore.availableCapacity

        // 4. ChangeVmNicNetwork with staticIp param equal to destination L3 gateway must also fail
        String pubGw = pubL3.ipRanges[0].gateway
        def changeAction = new ChangeVmNicNetworkAction()
        changeAction.vmNicUuid = nicBefore.uuid
        changeAction.destL3NetworkUuid = pubL3.uuid
        changeAction.staticIp = pubGw
        changeAction.sessionId = adminSession()
        def changeRes = changeAction.call()
        assert changeRes.error != null : "ChangeVmNicNetwork with dest L3 gateway staticIp must be rejected"
        assert changeRes.error.details.contains("gateway")

        destroyVmInstance {
            uuid = vm.uuid
        }
        expungeVmInstance {
            uuid = vm.uuid
        }
        deleteL3Network {
            uuid = flatL3.uuid
        }
        deleteInstanceOffering {
            uuid = offering.uuid
        }
    }

    void testSameMacMultiNicPciAddressMapping() {
        L3NetworkInventory pubL3 = env.inventoryByName("pubL3")
        VmInstanceInventory vm = env.inventoryByName("vm")
        VmInstanceDeviceManager vidManager = bean(VmInstanceDeviceManager.class)

        String sharedMac = vm.vmNics[0].mac

        stopVmInstance {
            uuid = vm.uuid
        }
        changeVmNicState {
            vmNicUuid = vm.vmNics[0].uuid
            state = "disable"
        }
        vm = attachL3NetworkToVm {
            vmInstanceUuid = vm.uuid
            l3NetworkUuid = pubL3.uuid
            systemTags = ["customMac::${pubL3.uuid}::${sharedMac}".toString()]
        }
        List<VmNicInventory> sortedNics = vm.vmNics.sort { it.deviceId }
        assert sortedNics.size() == 2
        assert sortedNics[0].mac == sharedMac
        assert sortedNics[1].mac == sharedMac

        KVMAgentCommands.StartVmCmd capturedCmd = null
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { KVMAgentCommands.StartVmResponse rsp, HttpEntity<String> e ->
            KVMAgentCommands.StartVmCmd cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StartVmCmd.class)
            if (cmd.vmInstanceUuid == vm.uuid) {
                capturedCmd = cmd
                DeviceAddress addr0 = new DeviceAddress(type: "pci", domain: "0x0000", bus: "0x00", slot: "0x03", function: "0x0")
                DeviceAddress addr1 = new DeviceAddress(type: "pci", domain: "0x0000", bus: "0x00", slot: "0x04", function: "0x0")
                KVMAgentCommands.VmNicInfo info1 = new KVMAgentCommands.VmNicInfo(
                        macAddress: sharedMac,
                        nicInternalName: sortedNics[1].internalName,
                        deviceAddress: addr1
                )
                KVMAgentCommands.VmNicInfo info0 = new KVMAgentCommands.VmNicInfo(
                        macAddress: sharedMac,
                        nicInternalName: sortedNics[0].internalName,
                        deviceAddress: addr0
                )
                rsp.nicInfos = [info1, info0]
                VirtualDeviceInfo balloon = new VirtualDeviceInfo()
                balloon.deviceAddress = new DeviceAddress(type: "pci", domain: "0x0000", bus: "0x00", slot: "0x07", function: "0x0")
                rsp.memBalloonInfo = balloon
                rsp.virtualDeviceInfoList = []
            }
            return rsp
        }

        startVmInstance {
            uuid = vm.uuid
        }

        DeviceAddress saved0 = vidManager.getVmDeviceAddress(sortedNics[0].uuid, vm.uuid)
        DeviceAddress saved1 = vidManager.getVmDeviceAddress(sortedNics[1].uuid, vm.uuid)
        assert saved0 != null && saved0.slot == "0x03"
        assert saved1 != null && saved1.slot == "0x04"

        String tag0 = KVMSystemTags.VMNIC_PCI_ADDRESS.getTokenByResourceUuid(sortedNics[0].uuid, KVMSystemTags.VMNIC_PCI_ADDRESS_TOKEN)
        String tag1 = KVMSystemTags.VMNIC_PCI_ADDRESS.getTokenByResourceUuid(sortedNics[1].uuid, KVMSystemTags.VMNIC_PCI_ADDRESS_TOKEN)
        assert tag0 != null && tag0.contains("0x03")
        assert tag1 != null && tag1.contains("0x04")

        // Also verify gray-upgrade fallback when old kvmagent omits nicInternalName
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { KVMAgentCommands.StartVmResponse rsp, HttpEntity<String> e ->
            KVMAgentCommands.StartVmCmd cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StartVmCmd.class)
            if (cmd.vmInstanceUuid == vm.uuid) {
                capturedCmd = cmd
                DeviceAddress addr0 = new DeviceAddress(type: "pci", domain: "0x0000", bus: "0x00", slot: "0x05", function: "0x0")
                DeviceAddress addr1 = new DeviceAddress(type: "pci", domain: "0x0000", bus: "0x00", slot: "0x06", function: "0x0")
                KVMAgentCommands.VmNicInfo info0 = new KVMAgentCommands.VmNicInfo(
                        macAddress: sharedMac,
                        deviceAddress: addr0
                )
                KVMAgentCommands.VmNicInfo info1 = new KVMAgentCommands.VmNicInfo(
                        macAddress: sharedMac,
                        deviceAddress: addr1
                )
                rsp.nicInfos = [info0, info1]
                VirtualDeviceInfo balloon = new VirtualDeviceInfo()
                balloon.deviceAddress = new DeviceAddress(type: "pci", domain: "0x0000", bus: "0x00", slot: "0x07", function: "0x0")
                rsp.memBalloonInfo = balloon
                rsp.virtualDeviceInfoList = []
            }
            return rsp
        }

        rebootVmInstance {
            uuid = vm.uuid
        }
        assert capturedCmd != null
        KVMAgentCommands.NicTO to0 = capturedCmd.nics.find { it.nicInternalName == sortedNics[0].internalName }
        KVMAgentCommands.NicTO to1 = capturedCmd.nics.find { it.nicInternalName == sortedNics[1].internalName }
        assert to0 != null && to0.deviceAddress != null && to0.deviceAddress.slot == "0x03"
        assert to1 != null && to1.deviceAddress != null && to1.deviceAddress.slot == "0x04"

        DeviceAddress graySaved0 = vidManager.getVmDeviceAddress(sortedNics[0].uuid, vm.uuid)
        DeviceAddress graySaved1 = vidManager.getVmDeviceAddress(sortedNics[1].uuid, vm.uuid)
        assert graySaved0 != null && graySaved0.slot == "0x05"
        assert graySaved1 != null && graySaved1.slot == "0x06"
    }

    @Override
    void clean() {
        env.delete()
    }
}
