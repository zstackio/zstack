package org.zstack.test.integration.kvm.vm

import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.SQL
import org.zstack.header.allocator.AllocationScene
import org.zstack.header.allocator.DesignatedAllocateHostMsg
import org.zstack.header.host.HostState
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.header.message.AbstractBeforeDeliveryMessageInterceptor
import org.zstack.header.message.MessageReply
import org.zstack.header.message.Message
import org.zstack.header.vm.HaStartVmInstanceMsg
import org.zstack.header.vm.HaStartVmJudger
import org.zstack.header.vm.StartVmInstanceMsg
import org.zstack.header.vm.StartVmInstanceReply
import org.zstack.header.vm.VmInstanceConstant
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

class HaStartVmWithNoNicCase extends SubCase {
    EnvSpec env
    CloudBus bus

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(1)
                cpu = 1
            }

            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(20)
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

                cluster {
                    name = "cluster1"
                    hypervisorType = "KVM"

                    kvm {
                        name = "host1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "host2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }

                cluster {
                    name = "cluster2"
                    hypervisorType = "KVM"

                    kvm {
                        name = "host3"
                        managementIp = "127.0.0.3"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "/nfs_root"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }
                }

                attachBackupStorage("sftp")
            }

            vm {
                name = "vm-prefer-cluster"
                useInstanceOffering("instanceOffering")
                useImage("image")
                useL3Networks("l3")
                useRootDiskOffering("diskOffering")
                useHost("host1")
            }

            vm {
                name = "vm-auto-prefer-cluster"
                useInstanceOffering("instanceOffering")
                useImage("image")
                useL3Networks("l3")
                useRootDiskOffering("diskOffering")
                useHost("host1")
            }

            vm {
                name = "vm-fallback-cluster"
                useInstanceOffering("instanceOffering")
                useImage("image")
                useL3Networks("l3")
                useRootDiskOffering("diskOffering")
                useHost("host1")
            }

            vm {
                name = "vm-no-fallback"
                useInstanceOffering("instanceOffering")
                useImage("image")
                useL3Networks("l3")
                useRootDiskOffering("diskOffering")
                useHost("host1")
            }

            vm {
                name = "vm-with-nic"
                useInstanceOffering("instanceOffering")
                useImage("image")
                useL3Networks("l3")
                useRootDiskOffering("diskOffering")
                useHost("host1")
            }
        }
    }

    @Override
    void test() {
        env.create {
            bus = bean(CloudBus.class)

            testPreferOriginalCluster()
            testAutoStartVmPreferOriginalCluster()
            testHaStartVmWithNicPreferClusterWithoutHardRestriction()
            VmInstanceInventory fallbackVm = prepareStoppedNoNicVm(env.inventoryByName("vm-fallback-cluster") as VmInstanceInventory)
            VmInstanceInventory noFallbackVm = prepareStoppedNoNicVm(env.inventoryByName("vm-no-fallback") as VmInstanceInventory)

            makeOriginalClusterUnavailable()
            testFallbackToAnotherCluster(fallbackVm)
            testNoFallbackWhenHaAcrossClustersDisabled(noFallbackVm)
        }
    }

    void testPreferOriginalCluster() {
        VmInstanceInventory vm = prepareStoppedNoNicVm(env.inventoryByName("vm-prefer-cluster") as VmInstanceInventory)
        ClusterInventory cluster1 = env.inventoryByName("cluster1") as ClusterInventory
        HostInventory host1 = env.inventoryByName("host1") as HostInventory
        HostInventory host2 = env.inventoryByName("host2") as HostInventory

        List<DesignatedAllocateHostMsg> allocateHostMsgs = recordAllocateHostMsgs()

        MessageReply reply = haStartVm(vm, [host1.uuid])

        assert reply.success
        assert !allocateHostMsgs.isEmpty()
        assert allocateHostMsgs.every { it.preferClusterUuid == cluster1.uuid }
        assert allocateHostMsgs.every { it.clusterUuids.isEmpty() }
        VmInstanceVO vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vo.clusterUuid == cluster1.uuid
        assert vo.hostUuid == host2.uuid
    }

    void testAutoStartVmPreferOriginalCluster() {
        VmInstanceInventory vm = prepareStoppedNoNicVm(env.inventoryByName("vm-auto-prefer-cluster") as VmInstanceInventory)
        ClusterInventory cluster1 = env.inventoryByName("cluster1") as ClusterInventory
        HostInventory host1 = env.inventoryByName("host1") as HostInventory
        HostInventory host2 = env.inventoryByName("host2") as HostInventory

        List<DesignatedAllocateHostMsg> allocateHostMsgs = recordAllocateHostMsgs()

        StartVmInstanceReply reply = autoStartVm(vm, [host1.uuid])

        assert reply.success
        assert !allocateHostMsgs.isEmpty()
        assert allocateHostMsgs.every { it.preferClusterUuid == cluster1.uuid }
        assert allocateHostMsgs.every { it.clusterUuids.isEmpty() }
        VmInstanceVO vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vo.clusterUuid == cluster1.uuid
        assert vo.hostUuid == host2.uuid
    }

    void testFallbackToAnotherCluster(VmInstanceInventory vm) {
        ClusterInventory cluster2 = env.inventoryByName("cluster2") as ClusterInventory
        HostInventory host1 = env.inventoryByName("host1") as HostInventory

        MessageReply reply = haStartVm(vm, [host1.uuid])

        assert reply.success
        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).clusterUuid == cluster2.uuid
    }

    void testNoFallbackWhenHaAcrossClustersDisabled(VmInstanceInventory vm) {
        ClusterInventory cluster1 = env.inventoryByName("cluster1") as ClusterInventory
        HostInventory host1 = env.inventoryByName("host1") as HostInventory

        updateResourceConfig {
            category = VmGlobalConfig.CATEGORY
            name = VmGlobalConfig.VM_HA_ACROSS_CLUSTERS.name
            resourceUuid = vm.uuid
            value = false
        }

        MessageReply reply = haStartVm(vm, [host1.uuid])

        assert !reply.success
        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).clusterUuid == cluster1.uuid
    }

    void testHaStartVmWithNicPreferClusterWithoutHardRestriction() {
        VmInstanceInventory vm = env.inventoryByName("vm-with-nic") as VmInstanceInventory
        ClusterInventory cluster1 = env.inventoryByName("cluster1") as ClusterInventory
        assert !vm.vmNics.isEmpty()

        if (vm.state != VmInstanceState.Stopped.toString()) {
            vm = stopVmInstance {
                uuid = vm.uuid
            } as VmInstanceInventory
        }

        List<DesignatedAllocateHostMsg> allocateHostMsgs = recordAllocateHostMsgs()

        MessageReply reply = haStartVm(vm, [])

        assert reply.success
        assert !allocateHostMsgs.isEmpty()
        assert allocateHostMsgs.every { it.preferClusterUuid == cluster1.uuid }
        assert allocateHostMsgs.every { it.clusterUuids.isEmpty() }
    }

    List<DesignatedAllocateHostMsg> recordAllocateHostMsgs() {
        List<DesignatedAllocateHostMsg> allocateHostMsgs = []
        bus.installBeforeDeliveryMessageInterceptor(new AbstractBeforeDeliveryMessageInterceptor() {
            @Override
            void beforeDeliveryMessage(Message msg) {
                allocateHostMsgs.add(msg as DesignatedAllocateHostMsg)
            }
        }, DesignatedAllocateHostMsg.class)
        return allocateHostMsgs
    }

    VmInstanceInventory prepareStoppedNoNicVm(VmInstanceInventory vm) {
        vm = detachAllNics(vm)
        if (vm.state != VmInstanceState.Stopped.toString()) {
            vm = stopVmInstance {
                uuid = vm.uuid
            } as VmInstanceInventory
        }

        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).state == VmInstanceState.Stopped
        return vm
    }

    VmInstanceInventory detachAllNics(VmInstanceInventory vm) {
        while (!vm.vmNics.isEmpty()) {
            vm = detachL3NetworkFromVm {
                vmNicUuid = vm.vmNics[0].uuid
            } as VmInstanceInventory
        }

        assert vm.vmNics.isEmpty()
        return vm
    }

    MessageReply haStartVm(VmInstanceInventory vm, List<String> softAvoidHostUuids) {
        HaStartVmInstanceMsg msg = new HaStartVmInstanceMsg()
        msg.vmInstanceUuid = vm.uuid
        msg.judgerClassName = ZSTAC72149HaStartVmJudger.class.name
        msg.softAvoidHostUuids = softAvoidHostUuids
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vm.uuid)
        return bus.call(msg)
    }

    StartVmInstanceReply autoStartVm(VmInstanceInventory vm, List<String> softAvoidHostUuids) {
        StartVmInstanceMsg msg = new StartVmInstanceMsg()
        msg.vmInstanceUuid = vm.uuid
        msg.allocationScene = AllocationScene.Auto
        msg.softAvoidHostUuids = softAvoidHostUuids
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vm.uuid)
        return bus.call(msg) as StartVmInstanceReply
    }

    void makeOriginalClusterUnavailable() {
        HostInventory host1 = env.inventoryByName("host1") as HostInventory
        HostInventory host2 = env.inventoryByName("host2") as HostInventory

        SQL.New(HostVO.class)
                .in(HostVO_.uuid, [host1.uuid, host2.uuid])
                .set(HostVO_.state, HostState.Disabled)
                .update()
    }

    @Override
    void clean() {
        env.delete()
    }
}

class ZSTAC72149HaStartVmJudger implements HaStartVmJudger {
    @Override
    boolean whetherStartVm(org.zstack.header.vm.VmInstanceInventory vm) {
        return true
    }
}
