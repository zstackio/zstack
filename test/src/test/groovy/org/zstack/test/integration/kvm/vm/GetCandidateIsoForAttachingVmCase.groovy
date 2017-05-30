package org.zstack.test.integration.kvm.vm

import org.zstack.header.image.ImageConstant
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.ZoneInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by lining on 2017/5/29.
 */
class GetCandidateIsoForAttachingVmCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = env{
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(1)
                cpu = 1
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image1"
                    url = "http://zstack.org/download/test.qcow2"
                }

                image {
                    name = "iso"
                    mediaType = ImageConstant.ImageMediaType.ISO.toString()
                    url  = "http://zstack.org/download/test.iso"
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
                useL3Networks("l3")
            }
        }
    }

    @Override
    void test() {
        env.create {
            runTest()
        }
    }

    void runTest() {
        VmInstanceInventory vm = env.inventoryByName("vm")
        BackupStorageInventory bs = env.inventoryByName("sftp")
        ZoneInventory zone = env.inventoryByName("zone")

        ImageInventory iso = getCandidateIsoForAttachingVm {
            vmInstanceUuid = vm.uuid
        }[0]
        assert null != iso

        attachIsoToVmInstance {
            vmInstanceUuid = vm.uuid
            isoUuid = iso.uuid
        }

        detachBackupStorageFromZone {
            zoneUuid = zone.uuid
            backupStorageUuid = bs.uuid
        }
        assert 0 == getCandidateIsoForAttachingVm {
            vmInstanceUuid = vm.uuid
        }.size()

    }

    @Override
    void clean() {
        env.delete()
    }
}
