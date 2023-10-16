package org.zstack.test.integration.storage.primary.addon

import org.springframework.http.HttpEntity
import org.zstack.core.cloudbus.CloudBus
import org.zstack.header.storage.backup.ExportImageToRemoteTargetMsg
import org.zstack.header.storage.backup.ExportImageToRemoteTargetReply
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.VolumeTO
import org.zstack.sdk.*
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

class ExternalPrimaryStorageCase extends SubCase {
    EnvSpec env
    ClusterInventory cluster
    InstanceOfferingInventory instanceOffering
    DiskOfferingInventory diskOffering
    ImageInventory image
    L3NetworkInventory l3
    PrimaryStorageInventory ps

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
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
                    size = SizeUnit.GIGABYTE.toByte(1)
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

                    attachL2Network("l2")
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
            cluster = env.inventoryByName("cluster") as ClusterInventory
            instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
            diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory
            image = env.inventoryByName("image") as ImageInventory
            l3 = env.inventoryByName("l3") as L3NetworkInventory
            testCreateExponStorage()
            testCreateDataVolume()
            testCreateVm()
        }
    }

    void testCreateExponStorage() {
        def zone = env.inventoryByName("zone") as ZoneInventory

        discoverExternalPrimaryStorage {
            url = "https://operator:Admin%40123@172.25.106.110:443/pool"
            identity = "expon"
        }

        ps = addExternalPrimaryStorage {
            name = "test"
            zoneUuid = zone.uuid
            url = "https://operator:Admin%40123@172.25.106.110:443/pool"
            identity = "expon"
            config = ""
            defaultOutputProtocol = "VHost"
        } as ExternalPrimaryStorageInventory

        updateExternalPrimaryStorage {
            uuid = ps.uuid
            config = '''{"pools":[{"name":"pool", "aliasName":"test"}]}'''
        }

        ps = queryPrimaryStorage {}[0] as ExternalPrimaryStorageInventory
        assert ps.getAddonInfo() != null

        attachPrimaryStorageToCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = cluster.uuid
        }
    }

    void testCreateDataVolume() {
        createDataVolume {
            name = "test"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ps.uuid
        }
    }

    void testCreateVm() {
        def result = getCandidatePrimaryStoragesForCreatingVm {
            l3NetworkUuids = [l3.uuid]
            imageUuid = image.uuid
        } as GetCandidatePrimaryStoragesForCreatingVmResult

        assert result.getRootVolumePrimaryStorages().size() == 1

        env.message(ExportImageToRemoteTargetMsg.class){ ExportImageToRemoteTargetMsg msg, CloudBus bus ->
            ExportImageToRemoteTargetReply r = new  ExportImageToRemoteTargetReply()
            assert msg.getRemoteTargetUrl().startsWith("nvme://")
            bus.reply(msg, r)
        }

        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StartVmCmd.class)
            assert cmd.rootVolume.deviceType == VolumeTO.VHOST
            assert cmd.rootVolume.installPath.startsWith("/var/run")
            assert cmd.rootVolume.format == "raw"
            return rsp
        }

        def vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
        } as VmInstanceInventory

        stopVmInstance {
            uuid = vm.uuid
        }

        startVmInstance {
            uuid = vm.uuid
        }

        rebootVmInstance {
            uuid = vm.uuid
        }

        destroyVmInstance {
            uuid = vm.uuid
        }
    }
}
