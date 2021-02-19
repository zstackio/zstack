package org.zstack.test.integration.kvm.vm

import org.zstack.header.volume.VolumeStatus
import org.zstack.sdk.KVMHostInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.sdk.ZoneInventory
import org.zstack.storage.volume.VolumeGlobalConfig
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by yaoning.li on 2021/2/19.
 */
class VolumeCascadeProtectionCase extends SubCase {
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
                    name = "image1"
                    architecture = "x86_64"
                    url = "http://zstack.org/download/test.qcow2"
                }

                image {
                    name = "vr"
                    architecture = "x86_64"
                    url = "http://zstack.org/download/vr.qcow2"
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
                        name = "pubL3"

                        ip {
                            startIp = "12.16.10.10"
                            endIp = "12.16.10.100"
                            netmask = "255.255.255.0"
                            gateway = "12.16.10.1"
                        }
                    }
                }

                attachBackupStorage("sftp")
            }

            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("pubL3")
            }
        }
    }

    @Override
    void test() {
        env.create {
            ZoneInventory zoneInventory = env.inventoryByName("zone")
            VmInstanceInventory vm = env.inventoryByName("vm")
            KVMHostInventory kvm = env.inventoryByName("kvm")

            VolumeInventory dataVolume = createDataVolume {
                name = 'testDataVolume'
                diskOfferingUuid = env.inventoryByName("diskOffering").uuid
                primaryStorageUuid = env.inventoryByName("local").uuid
                systemTags = Arrays.asList("localStorage::hostUuid::" + kvm.uuid)
            }

            VolumeGlobalConfig.CASCADE_ALLOWS_VOLUME_STATUS.updateValue(VolumeStatus.Deleted.name())
            expectError {
                deleteZone {
                    uuid = zoneInventory.uuid
                }
            }

            deleteDataVolume {
                uuid = dataVolume.uuid
            }
            destroyVmInstance {
                uuid = vm.uuid
            }
            deleteZone {
                uuid = zoneInventory.uuid
            }
        }
    }


    @Override
    void clean() {
        env.delete()
    }
}
