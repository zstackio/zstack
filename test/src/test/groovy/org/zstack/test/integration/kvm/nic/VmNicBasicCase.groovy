package org.zstack.test.integration.kvm.nic

import org.zstack.compute.vm.VmSystemTags
import org.zstack.core.db.Q
import org.zstack.header.network.l3.L3NetworkStateEvent
import org.zstack.header.network.l3.UsedIpVO
import org.zstack.header.network.l3.UsedIpVO_
import org.zstack.header.network.service.NetworkServiceProviderVO
import org.zstack.header.network.service.NetworkServiceProviderVO_
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmNicVO
import org.zstack.network.l3.IpRangeHelper
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.flat.FlatNetworkSystemTags
import org.zstack.sdk.CreateVmInstanceAction
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.IpRangeInventory
import org.zstack.sdk.L2NetworkInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

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

    @Override
    void clean() {
        env.delete()
    }
}
