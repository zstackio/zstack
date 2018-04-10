package org.zstack.test.integration.kvm.vm

import org.zstack.core.db.Q
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

///**
// * Created by kayo on 2018/3/20.
// */
class CreateVmConcurrentlyCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

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
                    url  = "http://zstack.org/download/test.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "host1"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                        totalMem = SizeUnit.GIGABYTE.toByte(1000)
                        totalCpu = 1000
                    }

                    kvm {
                        name = "host2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                        totalMem = SizeUnit.GIGABYTE.toByte(1000)
                        totalCpu = 1000
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

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.110.100"
                            netmask = "255.255.0.0"
                            gateway = "192.168.100.1"
                        }
                    }
                }

                attachBackupStorage("sftp")
            }

            vm {
                name = "vm"
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
//            testCreate1000Vm()
        }
    }

    // This case is for ZSTAC-8576
    // PR system will met API timeout (api timeout is 25s)
    // Can be execute separately if needed
    void testCreate1000Vm() {
        def instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        def image = env.inventoryByName("image") as ImageInventory
        def l3 = env.inventoryByName("l3") as L3NetworkInventory

        def list = []
        for (int i = 0; i < 1000; i++) {
            def thread = Thread.start {
                VmInstanceInventory inv = createVmInstance {
                    name = "test-vm"
                    instanceOfferingUuid = instanceOffering.uuid
                    imageUuid = image.uuid
                    l3NetworkUuids = [l3.uuid]
                } as VmInstanceInventory

                assert Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, inv.uuid).eq(VmInstanceVO_.state, VmInstanceState.Running).isExists()
            }

            list.add(thread)
        }

        list.each { it.join() }

        retryInSecs(25, 3) {
            assert Q.New(VmInstanceVO.class).eq(VmInstanceVO_.state, VmInstanceState.Running).count() == (long) 1001
        }

    }
}
