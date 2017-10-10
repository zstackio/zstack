package org.zstack.test.integration.storage.primary.local.capacity

import org.springframework.http.HttpEntity
import org.zstack.header.image.ImageConstant
import org.zstack.sdk.*
import org.zstack.storage.backup.sftp.SftpBackupStorageCommands
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant
import org.zstack.storage.primary.PrimaryStorageGlobalConfig
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.BackupStorageSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by lining on 2017/10/10.
 */
class ResumePrimaryStorageCapacityAfterCreateVmFailCase extends SubCase {
    EnvSpec env

    long psTotalSize = SizeUnit.GIGABYTE.toByte(5) - 1
    long volumeSize = SizeUnit.GIGABYTE.toByte(1)

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = env{
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
            }

            diskOffering {
                name = "diskOffering"
                diskSize = volumeSize
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

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
                    totalCapacity = psTotalSize
                    availableCapacity = psTotalSize
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
        }
    }

    @Override
    void test() {
        env.create {
            testCreateVmFailed()
        }
    }

    void testCreateVmFailed() {
        PrimaryStorageGlobalConfig.RESERVED_CAPACITY.updateValue(0)

        PrimaryStorageInventory ps = env.inventoryByName("local")
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering")
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        BackupStorageInventory bs = env.inventoryByName("sftp")

        def image_virtual_size = volumeSize
        def image_physical_size = volumeSize
        env.simulator(SftpBackupStorageConstant.DOWNLOAD_IMAGE_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.getBody(), SftpBackupStorageCommands.DownloadCmd.class)
            BackupStorageSpec bsSpec = spec.specByUuid(cmd.uuid)

            def rsp = new SftpBackupStorageCommands.DownloadResponse()
            rsp.size = image_virtual_size
            rsp.actualSize = image_physical_size 
            rsp.availableCapacity = bsSpec.availableCapacity
            rsp.totalCapacity = bsSpec.totalCapacity
            return rsp
        }
        ImageInventory sizedImage = addImage {
            name = "sized-image"
            url = "http://my-site/foo.iso"
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.ISO_FORMAT_STRING
        }

        GetPrimaryStorageCapacityResult beforeCapacityResult = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }
        assert psTotalSize == beforeCapacityResult.availableCapacity

        VmInstanceInventory vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = sizedImage.uuid
            l3NetworkUuids = [l3.uuid]
            rootDiskOfferingUuid = diskOffering.uuid
            dataDiskOfferingUuids = [diskOffering.uuid]
        }

        GetPrimaryStorageCapacityResult capacityResultAfterCreateOneVm = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }
        assert psTotalSize - volumeSize * 2 - image_virtual_size  == capacityResultAfterCreateOneVm.availableCapacity

        for(int i = 0; i < 10; i++){
            CreateVmInstanceAction createVmInstanceAction = new CreateVmInstanceAction(
                    name: "vm2",
                    instanceOfferingUuid: instanceOffering.uuid,
                    imageUuid: sizedImage.uuid,
                    l3NetworkUuids: [l3.uuid],
                    rootDiskOfferingUuid : diskOffering.uuid,
                    dataDiskOfferingUuids : [diskOffering.uuid],
                    sessionId: currentEnvSpec.session.uuid
            )
            CreateVmInstanceAction.Result result = createVmInstanceAction.call()
            assert null != result.error

            retryInSecs(3){
                GetPrimaryStorageCapacityResult capacityResult = getPrimaryStorageCapacity {
                    primaryStorageUuids = [ps.uuid]
                }
                assert psTotalSize - volumeSize * 2 - image_virtual_size  == capacityResult.availableCapacity
            }
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
