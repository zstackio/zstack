package org.zstack.test.integration.storage.primary.local

import org.zstack.core.db.Q
import org.zstack.header.storage.snapshot.VolumeSnapshotVO
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.GetVolumeSnapshotSizeResult
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.storage.primary.local.LocalStorageHostRefVO
import org.zstack.storage.primary.local.LocalStorageHostRefVO_
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

            VolumeSnapshotInventory snapshotInventory = createVolumeSnapshot {
                name = "data-volume-snapshot"
                volumeUuid = dataVolume1
            }
            GetVolumeSnapshotSizeResult result = getVolumeSnapshotSize {
                uuid = snapshotInventory.uuid
            }
            assert result.actualSize == snapshotInventory.size
            assert result.size == snapshotInventory.size

            createVolumeSnapshot {
                name = "data-volume-snapshot"
                volumeUuid = dataVolume2
            }

            // for data safe, over capacity should success too.
            env.afterSimulator(KVMConstant.KVM_TAKE_VOLUME_SNAPSHOT_PATH) {
                def rsp = new KVMAgentCommands.TakeSnapshotResponse()
                rsp.newVolumeInstallPath = "/new/volume/install/path1"
                rsp.snapshotInstallPath = "/snapshot/install/path1"
                rsp.size = SizeUnit.TERABYTE.toByte(1000)
                return rsp
            }

            createVolumeSnapshot {
                name = "data-volume-snapshot"
                volumeUuid = dataVolume2
            }

            assert Q.New(LocalStorageHostRefVO.class).select(LocalStorageHostRefVO_.availableCapacity).findValue() == 0
            assert Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.volumeUuid, dataVolume2)
                    .eq(VolumeSnapshotVO_.latest, true)
                    .select(VolumeSnapshotVO_.primaryStorageInstallPath)
                    .findValue() == "/snapshot/install/path1"

            assert Q.New(VolumeVO.class).eq(VolumeVO_.uuid, dataVolume2).select(VolumeVO_.installPath).findValue() == "/new/volume/install/path1"
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
