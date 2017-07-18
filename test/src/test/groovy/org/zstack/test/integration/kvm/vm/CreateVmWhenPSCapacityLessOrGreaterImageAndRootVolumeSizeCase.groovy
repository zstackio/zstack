package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.header.image.ImageConstant
import org.zstack.header.image.ImagePlatform
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.sdk.*
import org.zstack.storage.backup.sftp.SftpBackupStorageCommands
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.BackupStorageSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import static java.util.Arrays.asList

/**
 * Created by Camile on 2017/5/17.
 */
class CreateVmWhenPSCapacityLessOrGreaterImageAndRootVolumeSizeCase extends SubCase{
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
                name = "diskOffering-10G"
                diskSize = SizeUnit.GIGABYTE.toByte(20)
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "50G"
                    url  = "http://zstack.org/download/test.iso"
                    platform = ImagePlatform.Linux.toString()
                    mediaType = ImageConstant.ImageMediaType.RootVolumeTemplate.toString()
                    format = "iso"
                    size = SizeUnit.GIGABYTE.toByte(100)
                    actualSize = SizeUnit.GIGABYTE.toByte(50)
                }

                image {
                    name = "10G"
                    url  = "http://zstack.org/download/test2.iso"
                    platform = ImagePlatform.Linux.toString()
                    mediaType = ImageConstant.ImageMediaType.RootVolumeTemplate.toString()
                    format = "iso"
                    size = SizeUnit.GIGABYTE.toByte(20)
                    actualSize = SizeUnit.GIGABYTE.toByte(10)
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
                    availableCapacity = SizeUnit.GIGABYTE.toByte(60)
                    totalCapacity = SizeUnit.GIGABYTE.toByte(60)
                }


                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }

                    l3Network {
                        name = "pubL3"

                        ip {
                            startIp = "11.168.100.10"
                            endIp = "11.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.100.1"
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
            testCreateVmWhenPSCapacityLessThanImageAndRootVolumeSuccess()
            testCreateVmWhenPSCapacityGreaterThanImageAndRootVolumeFailure()
        }
    }

    void testCreateVmWhenPSCapacityLessThanImageAndRootVolumeSuccess() {
        BackupStorageInventory bs = env.inventoryByName("sftp")
        PrimaryStorageInventory ps = env.inventoryByName("local")
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        DiskOfferingInventory doIvo = env.inventoryByName("diskOffering-10G")

        env.simulator(SftpBackupStorageConstant.DOWNLOAD_IMAGE_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.getBody(), SftpBackupStorageCommands.DownloadCmd.class)
            BackupStorageSpec bsSpec = spec.specByUuid(cmd.uuid)

            def rsp = new SftpBackupStorageCommands.DownloadResponse()
            rsp.size = SizeUnit.GIGABYTE.toByte(20)
            rsp.actualSize = SizeUnit.GIGABYTE.toByte(10)
            rsp.availableCapacity = bsSpec.availableCapacity - SizeUnit.GIGABYTE.toByte(10)
            rsp.totalCapacity = bsSpec.totalCapacity
            return rsp
        }

        def _10GImage = addImage {
            backupStorageUuids = asList(bs.uuid)
            name = "10G"
            url = "http://some-site/static/image.iso"
            format = "iso"
        } as ImageInventory


        CreateVmInstanceAction createVmInstanceAction = new CreateVmInstanceAction()
        createVmInstanceAction.name="test"
        createVmInstanceAction.instanceOfferingUuid = instanceOffering.uuid
        createVmInstanceAction.imageUuid = _10GImage.uuid
        createVmInstanceAction.l3NetworkUuids = asList(l3.uuid)
        createVmInstanceAction.rootDiskOfferingUuid = doIvo.uuid
        createVmInstanceAction.sessionId = adminSession()

        assert createVmInstanceAction.call().error == null
    }
    void testCreateVmWhenPSCapacityGreaterThanImageAndRootVolumeFailure() {
        BackupStorageInventory bs = env.inventoryByName("sftp")
        PrimaryStorageInventory ps = env.inventoryByName("local")
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        DiskOfferingInventory doIvo = env.inventoryByName("diskOffering-10G")

        env.simulator(SftpBackupStorageConstant.DOWNLOAD_IMAGE_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.getBody(), SftpBackupStorageCommands.DownloadCmd.class)
            BackupStorageSpec bsSpec = spec.specByUuid(cmd.uuid)

            def rsp = new SftpBackupStorageCommands.DownloadResponse()
            rsp.size = SizeUnit.GIGABYTE.toByte(100)
            rsp.actualSize = SizeUnit.GIGABYTE.toByte(50)
            rsp.availableCapacity = bsSpec.availableCapacity - SizeUnit.GIGABYTE.toByte(50)
            rsp.totalCapacity = bsSpec.totalCapacity
            return rsp
        }

        def _50GImage = addImage {
            backupStorageUuids = asList(bs.uuid)
            name = "50G"
            url = "http://some-site/static/image.iso"
            format = "iso"
        } as ImageInventory


        CreateVmInstanceAction createVmInstanceAction = new CreateVmInstanceAction()
        createVmInstanceAction.name="test"
        createVmInstanceAction.instanceOfferingUuid = instanceOffering.uuid
        createVmInstanceAction.imageUuid = _50GImage.uuid
        createVmInstanceAction.l3NetworkUuids = asList(l3.uuid)
        createVmInstanceAction.rootDiskOfferingUuid = doIvo.uuid
        createVmInstanceAction.sessionId = adminSession()

        assert createVmInstanceAction.call().error != null
    }
}
