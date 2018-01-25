package org.zstack.test.integration.storage.primary.local

import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by lining on 2018/01/24.
 */
class CreateDataVolumeSnapshotCase extends SubCase {
    EnvSpec env


    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
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
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }
                }

                attachBackupStorage("sftp")
            }

            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useDiskOfferings("diskOffering", "diskOffering")
                useImage("image")
                useL3Networks("l3")
            }
        }
    }

    @Override
    void test() {
        env.create {
            VmInstanceInventory vm = env.inventoryByName("vm")

            String dataVolume1 = vm.allVolumes.find { it.uuid != vm.rootVolumeUuid }.uuid
            String dataVolume2 = vm.allVolumes.find { it.uuid != vm.rootVolumeUuid && it.uuid != dataVolume1 }.uuid
            assert dataVolume1 != dataVolume2

            createVolumeSnapshot {
                name = "data-volume-snapshot"
                volumeUuid = dataVolume1
            }

            createVolumeSnapshot {
                name = "data-volume-snapshot"
                volumeUuid = dataVolume2
            }
            createVolumeSnapshot {
                name = "data-volume-snapshot"
                volumeUuid = dataVolume2
            }
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
