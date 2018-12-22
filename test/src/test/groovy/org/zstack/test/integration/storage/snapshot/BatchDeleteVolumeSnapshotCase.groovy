package org.zstack.test.integration.storage.snapshot

import org.springframework.http.HttpEntity
import org.zstack.header.storage.snapshot.BatchDeleteVolumeSnapshotReply
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.BatchDeleteVolumeSnapshotAction
import org.zstack.sdk.BatchDeleteVolumeSnapshotResult
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.storage.snapshot.VolumeSnapshotGlobalConfig
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Create by weiwang at 2018-12-22
 */
public class BatchDeleteVolumeSnapshotCase extends SubCase {
    EnvSpec env
    VmInstanceInventory cephVm
    VmInstanceInventory localVm

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
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(2)
                cpu = 1
            }

            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(20)
            }

            cephBackupStorage {
                name = "bs"
                description = "Test"
                totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                availableCapacity = SizeUnit.GIGABYTE.toByte(100)
                url = "/bk"
                fsid = "7ff218d9-f525-435f-8a40-3618d1772a64"
                monUrls = ["root:password@127.0.0.1/?monPort=7777"]

                image {
                    name = "ceph-image1"
                    url = "http://zstack.org/download/image.qcow2"
                }
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "127.0.0.2"

                image {
                    name = "sftp-image1"
                    url  = "http://zstack.org/download/test.qcow2"
                }
            }

            zone {
                name = "zone"


                cephPrimaryStorage {
                    name = "ceph-ps"
                    description = "Test"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                    availableCapacity = SizeUnit.GIGABYTE.toByte(100)
                    url = "ceph://pri"
                    fsid = "7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls = ["root:password@127.0.0.1/?monPort=7777"]
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                cluster {
                    name = "ceph-cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "ceph-host"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("ceph-ps")
                    attachL2Network("l2")
                }

                cluster {
                    name = "local-cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
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

                attachBackupStorage("bs")
                attachBackupStorage("sftp")
            }

            vm {
                name = "cephVm"
                useCluster("ceph-cluster")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
                useImage("ceph-image1")
            }

            vm {
                name = "localVm"
                useCluster("local-cluster")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
                useImage("sftp-image1")
            }
        }
    }

    @Override
    void test() {
        env.create {
            cephVm = env.inventoryByName("cephVm") as VmInstanceInventory
            localVm = env.inventoryByName("localVm") as VmInstanceInventory

            updateGlobalConfig {
                category= VolumeSnapshotGlobalConfig.CATEGORY
                name = VolumeSnapshotGlobalConfig.MAX_INCREMENTAL_SNAPSHOT_NUM.name
                value = 4
            }

            testBatchDeleteVolumeSnapshotOnLocal()
            testBatchDeleteVolumeSnapshotOnCeph()
        }
    }

    //NOTE(weiw): 1. create 20 snapshots
    //            2. delete 1 and 2 and no need to merge
    //            3. delete all and only one merge called
    void testBatchDeleteVolumeSnapshotOnLocal() {
        for (i in 1..22) {
            def snapName = "localsnap${i}".toString()
            createVolumeSnapshot {
                volumeUuid = localVm.rootVolumeUuid
                name = snapName
            }
        }

        List<KVMAgentCommands.MergeSnapshotCmd> cmds = Collections.synchronizedList(new ArrayList<>())
        env.afterSimulator(KVMConstant.KVM_MERGE_SNAPSHOT_PATH) { rsp, HttpEntity<String> e ->
            def mergeCmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.MergeSnapshotCmd.class)
            cmds.add(mergeCmd)
            return rsp
        }

        VolumeSnapshotInventory snap1 = queryVolumeSnapshot {conditions=["name=localsnap1".toString()]}[0]
        VolumeSnapshotInventory snap2 = queryVolumeSnapshot {conditions=["name=localsnap2".toString()]}[0]

        batchDeleteVolumeSnapshot {
            uuids = [snap1, snap2].uuid
        }

        assert cmds.size() == 0


        VolumeSnapshotInventory snap6 = queryVolumeSnapshot {conditions=["name=localsnap6".toString()]}[0]
        VolumeSnapshotInventory snap8 = queryVolumeSnapshot {conditions=["name=localsnap8".toString()]}[0]

        batchDeleteVolumeSnapshot {
            uuids = [snap6, snap8].uuid
        }
        assert (queryVolumeSnapshot {} as List<VolumeSnapshotInventory>).size() == 14
        assert cmds.size() == 0

        List<VolumeSnapshotInventory> snaps = queryVolumeSnapshot {}
        batchDeleteVolumeSnapshot {
            uuids = snaps.uuid
        }

        assert cmds.size() == 1
        assert (queryVolumeSnapshot {} as List<VolumeSnapshotInventory>).size() == 0
    }

    void testBatchDeleteVolumeSnapshotOnCeph() {
        for (i in 1..22) {
            def snapName = "cephsnap${i}".toString()
            createVolumeSnapshot {
                volumeUuid = cephVm.rootVolumeUuid
                name = snapName
            }
        }
        List<CephPrimaryStorageBase.DeleteSnapshotCmd> cmds = Collections.synchronizedList(new ArrayList<>())
        env.simulator(CephPrimaryStorageBase.DELETE_SNAPSHOT_PATH) { HttpEntity<String> e ->
            def rsp = new CephPrimaryStorageBase.DeleteSnapshotRsp()
            def delCmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.DeleteSnapshotCmd.class)
            cmds.add(delCmd)
            return rsp
        }


        VolumeSnapshotInventory snap1 = queryVolumeSnapshot {conditions=["name=cephsnap1".toString()]}[0]
        VolumeSnapshotInventory snap2 = queryVolumeSnapshot {conditions=["name=cephsnap2".toString()]}[0]

        batchDeleteVolumeSnapshot {
            uuids = [snap1, snap2].uuid
        }

        assert cmds.size() == 2
        cmds.clear()
        env.cleanSimulatorHandlers()
        assert (queryVolumeSnapshot {} as List<VolumeSnapshotInventory>).size() == 20

        VolumeSnapshotInventory snap3 = queryVolumeSnapshot {conditions=["name=cephsnap3".toString()]}[0]

        List<VolumeSnapshotInventory> snaps = queryVolumeSnapshot {}
        env.simulator(CephPrimaryStorageBase.DELETE_SNAPSHOT_PATH) { HttpEntity<String> e ->
            def rsp = new CephPrimaryStorageBase.DeleteSnapshotRsp()
            def delCmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.DeleteSnapshotCmd.class)
            cmds.add(delCmd)

            if (delCmd.snapshotPath == snap3.primaryStorageInstallPath) {
                rsp.setSuccess(false)
                rsp.setError("test error")
            }
            return rsp
        }

        def result = batchDeleteVolumeSnapshot {
            uuids = snaps.uuid
        } as BatchDeleteVolumeSnapshotResult

        assert cmds.size() == 20
        assert result.results.error.toSet() == [null].toSet()
        // delete error will not return error, check org.zstack.storage.snapshot.VolumeSnapshotBase.handle(org.zstack.header.storage.snapshot.VolumeSnapshotPrimaryStorageDeletionMsg)
    }
}
