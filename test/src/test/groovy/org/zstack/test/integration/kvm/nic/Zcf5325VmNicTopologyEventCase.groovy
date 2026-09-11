package org.zstack.test.integration.kvm.nic

import org.zstack.core.cloudbus.EventCallback
import org.zstack.core.cloudbus.EventFacade
import org.zstack.header.host.HostCanonicalEvents
import org.zstack.header.vm.VmCanonicalEvents
import org.zstack.header.vm.VmNicCanonicalEvents
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.FreeIpInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class Zcf5325VmNicTopologyEventCase extends SubCase {
    EnvSpec env

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
            VmInstanceInventory vm = env.inventoryByName("vm")
            HostInventory host = env.inventoryByName("kvm")
            VmNicInventory nic = vm.vmNics[0]
            L3NetworkInventory l3 = env.inventoryByName("l3")
            List<VmNicCanonicalEvents.VmNicInfoChangedData> events =
                    Collections.synchronizedList([])
            List<HostCanonicalEvents.HostInfoChangedData> hostEvents =
                    Collections.synchronizedList([])
            List<VmCanonicalEvents.VmCreatedData> vmCreatedEvents =
                    Collections.synchronizedList([])
            List<VmCanonicalEvents.VmInfoChangedData> vmInfoChangedEvents =
                    Collections.synchronizedList([])
            bean(EventFacade.class).onLocal(
                    VmNicCanonicalEvents.VM_NIC_INFO_CHANGED_PATH,
                    new EventCallback<VmNicCanonicalEvents.VmNicInfoChangedData>() {
                        @Override
                        protected void run(Map<String, String> tokens,
                                VmNicCanonicalEvents.VmNicInfoChangedData data) {
                            if (data.vmNicUuid == nic.uuid) {
                                events.add(data)
                            }
                        }
                    })
            bean(EventFacade.class).onLocal(
                    VmCanonicalEvents.VM_CREATED_PATH,
                    new EventCallback<VmCanonicalEvents.VmCreatedData>() {
                        @Override
                        protected void run(Map<String, String> tokens,
                                VmCanonicalEvents.VmCreatedData data) {
                            vmCreatedEvents.add(data)
                        }
                    })
            bean(EventFacade.class).onLocal(
                    VmCanonicalEvents.VM_INFO_CHANGED_PATH,
                    new EventCallback<VmCanonicalEvents.VmInfoChangedData>() {
                        @Override
                        protected void run(Map<String, String> tokens,
                                VmCanonicalEvents.VmInfoChangedData data) {
                            vmInfoChangedEvents.add(data)
                        }
                    })
            bean(EventFacade.class).onLocal(
                    HostCanonicalEvents.HOST_INFO_CHANGED_PATH,
                    new EventCallback<HostCanonicalEvents.HostInfoChangedData>() {
                        @Override
                        protected void run(Map<String, String> tokens,
                                HostCanonicalEvents.HostInfoChangedData data) {
                            if (data.hostUuid == host.uuid) {
                                hostEvents.add(data)
                            }
                        }
                    })

            VmInstanceInventory createdVm = createVmInstance {
                name = "zcf5325-created-vm"
                clusterUuid = (env.inventoryByName("cluster") as ClusterInventory).uuid
                hostUuid = host.uuid
                instanceOfferingUuid = (env.inventoryByName("instanceOffering") as InstanceOfferingInventory).uuid
                imageUuid = (env.inventoryByName("image1") as ImageInventory).uuid
                l3NetworkUuids = [l3.uuid]
            }
            retryInSecs {
                assert vmCreatedEvents.any { data -> data.vmUuid == createdVm.uuid }
            }
            updateVmInstance {
                uuid = createdVm.uuid
                name = "zcf5325-renamed-vm"
            }
            retryInSecs {
                assert vmInfoChangedEvents.any { data -> data.vmUuid == createdVm.uuid }
            }

            updateHost {
                uuid = host.uuid
                name = "zcf5325-renamed-host"
            }
            retryInSecs {
                assert hostEvents.any { data -> data.hostUuid == host.uuid }
            }

            stopVmInstance { uuid = vm.uuid }
            FreeIpInventory freeIp = getFreeIp {
                l3NetworkUuid = l3.uuid
                limit = 1
            }[0]
            setVmStaticIp {
                vmInstanceUuid = vm.uuid
                l3NetworkUuid = l3.uuid
                ip = freeIp.ip
            }
            retryInSecs {
                assert events.any { data ->
                    data.vmInstanceUuid == vm.uuid &&
                            data.changeType == VmNicCanonicalEvents.VmNicInfoChangeType.IP
                }
            }
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
