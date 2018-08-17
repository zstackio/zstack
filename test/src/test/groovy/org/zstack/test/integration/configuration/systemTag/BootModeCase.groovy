package org.zstack.test.integration.configuration.systemTag

import org.zstack.header.image.ImageConstant
import org.zstack.header.image.ImagePlatform
import org.zstack.header.image.ImageVO
import org.zstack.header.vm.VmInstanceVO
import org.zstack.sdk.*
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
/**
 * Created by GuoYi on 17/08/2018.
 */
class BootModeCase extends SubCase {
    EnvSpec env

    @Override
    void environment() {
        env = env {
            zone {
                name = "zone"
                attachBackupStorage("sftp")

                cluster {
                    name = "cluster1"
                    hypervisorType = "KVM"

                    kvm {
                        name = "host1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("nfs-ps")
                    attachL2Network("l2")
                }

                nfsPrimaryStorage {
                    name = "nfs-ps"
                    url = "127.0.0.1:/nfs_root"
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
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image1"
                    url  = "http://zstack.org/download/test.iso"
                }
            }

            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
            }

            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
                useHost("host1")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testBootModeSystemTag()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    void testBootModeSystemTag() {
        // create bootMode tag for exiting image
        def img = env.inventoryByName("image1") as ImageInventory
        createSystemTag {
            resourceUuid = img.uuid
            resourceType = ImageVO.getSimpleName()
            tag = "bootMode::UEFI"
        }

        def tags = querySystemTag {
            conditions = [
                    "resourceUuid=${img.uuid}".toString(),
                    "tag=bootMode::UEFI"
            ]
        } as List<SystemTagInventory>
        assert tags.size() == 1

        // create new image with bootMode
        BackupStorageInventory bs = env.inventoryByName("sftp") as BackupStorageInventory
        img = addImage {
            name = "image2"
            backupStorageUuids = [bs.uuid]
            url = "http://zstack.org/download/test.qcow2"
            format = ImageConstant.QCOW2_FORMAT_STRING
            platform = ImagePlatform.Linux.toString()
            systemTags = ["bootMode::UEFI"]
        }
        tags = querySystemTag {
            conditions = [
                    "resourceUuid=${img.uuid}".toString(),
                    "tag=bootMode::UEFI"
            ]
        } as List<SystemTagInventory>
        assert tags.size() == 1

        updateSystemTag {
            uuid = tags.get(0).getUuid()
            tag = "bootMode::Legacy"
        }
        tags = querySystemTag {
            conditions = [
                    "resourceUuid=${img.uuid}".toString(),
                    "tag~=bootMode::Legacy"
            ]
        } as List<SystemTagInventory>
        assert tags.size() == 1

        // create bootMode tag for exiting vm instance
        def vm = env.inventoryByName("vm") as VmInstanceInventory
        createSystemTag {
            resourceUuid = vm.uuid
            resourceType = VmInstanceVO.getSimpleName()
            tag = "bootMode::UEFI"
        }

        tags = querySystemTag {
            conditions = [
                    "resourceUuid=${vm.uuid}".toString(),
                    "tag=bootMode::UEFI"
            ]
        } as List<SystemTagInventory>
        assert tags.size() == 1

        // create new vm instance using image with bootMode
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory
        vm = createVmInstance {
            name = "New-VM"
            instanceOfferingUuid = offering.uuid
            imageUuid = img.uuid
            l3NetworkUuids = [l3.uuid]
        }
        tags = querySystemTag {
            conditions = [
                    "resourceUuid=${vm.uuid}".toString(),
                    "tag=bootMode::Legacy"
            ]
        } as List<SystemTagInventory>
        assert tags.size() == 1

        updateSystemTag {
            uuid = tags.get(0).getUuid()
            tag = "bootMode::UEFI"
        }
        tags = querySystemTag {
            conditions = [
                    "resourceUuid=${vm.uuid}".toString(),
                    "tag=bootMode::UEFI"
            ]
        } as List<SystemTagInventory>
        assert tags.size() == 1

        // commit vm instance to image
        img = createRootVolumeTemplateFromRootVolume {
            name = "template"
            rootVolumeUuid = vm.getRootVolumeUuid()
            backupStorageUuids = [bs.uuid]
        }

        tags = querySystemTag {
            conditions = [
                    "resourceUuid=${img.uuid}".toString(),
                    "tag~=bootMode::UEFI"
            ]
        } as List<SystemTagInventory>
        assert tags.size() == 1
    }
}
