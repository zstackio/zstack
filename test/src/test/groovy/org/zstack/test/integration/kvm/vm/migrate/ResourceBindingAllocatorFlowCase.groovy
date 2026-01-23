package org.zstack.test.integration.kvm.vm.migrate

import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.MigrateVmAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Test ResourceBindingAllocatorFlow truth table (8 combinations)
 *
 * | # | vm.ha.across.clusters | resourceBinding.strategy | Cluster Has Resources | Expected Behavior |
 * |---|----------------------|-------------------------|----------------------|-------------------|
 * | 1 | true                 | Hard                    | true                 | Migrate Freely    |
 * | 2 | true                 | Hard                    | false                | Migrate Freely    |
 * | 3 | true                 | Soft                    | true                 | Migrate Freely    |
 * | 4 | true                 | Soft                    | false                | Migrate Freely    |
 * | 5 | false                | Hard                    | true                 | Migrate in Current Cluster |
 * | 6 | false                | Hard                    | false                | Fail              |
 * | 7 | false                | Soft                    | true                 | Migrate in Current Cluster |
 * | 8 | false                | Soft                    | false                | Try Other Clusters |
 *
 * ZSTAC-75428
 */
class ResourceBindingAllocatorFlowCase extends SubCase {
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
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
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
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster1"
                    hypervisorType = "KVM"

                    kvm {
                        name = "host1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "host2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }

                cluster {
                    name = "cluster2"
                    hypervisorType = "KVM"

                    kvm {
                        name = "host3"
                        managementIp = "127.0.0.3"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "localhost:/nfs"
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
                useImage("image1")
                useL3Networks("l3")
                useCluster("cluster1")
                useHost("host1")
            }
        }
    }

    @Override
    void test() {
        env.create {
            // Truth table #1-4: vm.ha.across.clusters = true
            testCase1_AcrossTrue_Hard_ResourceTrue()
            testCase2_AcrossTrue_Hard_ResourceFalse()
            testCase3_AcrossTrue_Soft_ResourceTrue()
            testCase4_AcrossTrue_Soft_ResourceFalse()

            // Truth table #5-8: vm.ha.across.clusters = false
            testCase5_AcrossFalse_Hard_ResourceTrue()
            testCase6_AcrossFalse_Hard_ResourceFalse()
            testCase7_AcrossFalse_Soft_ResourceTrue()
            testCase8_AcrossFalse_Soft_ResourceFalse()
        }
    }

    // #1: across=true, strategy=Hard, resource=true -> Migrate Freely
    void testCase1_AcrossTrue_Hard_ResourceTrue() {
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory
        HostInventory host2 = env.inventoryByName("host2") as HostInventory
        HostInventory host3 = env.inventoryByName("host3") as HostInventory

        updateGlobalConfig {
            category = VmGlobalConfig.CATEGORY
            name = "vm.ha.across.clusters"
            value = "true"
        }
        updateGlobalConfig {
            category = VmGlobalConfig.CATEGORY
            name = "resourceBinding.strategy"
            value = "Hard"
        }

        // Can migrate to host2 (same cluster)
        migrateVm {
            vmInstanceUuid = vm.uuid
            hostUuid = host2.uuid
        }
        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).hostUuid == host2.uuid

        // Can migrate to host3 (different cluster)
        migrateVm {
            vmInstanceUuid = vm.uuid
            hostUuid = host3.uuid
        }
        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).hostUuid == host3.uuid

        // Reset VM to host1
        resetVmToHost1(vm)
    }

    // #2: across=true, strategy=Hard, resource=false -> Migrate Freely
    void testCase2_AcrossTrue_Hard_ResourceFalse() {
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory
        HostInventory host2 = env.inventoryByName("host2") as HostInventory
        HostInventory host3 = env.inventoryByName("host3") as HostInventory

        updateGlobalConfig {
            category = VmGlobalConfig.CATEGORY
            name = "vm.ha.across.clusters"
            value = "true"
        }
        updateGlobalConfig {
            category = VmGlobalConfig.CATEGORY
            name = "resourceBinding.strategy"
            value = "Hard"
        }

        // Disable host2 to simulate resource=false in current cluster
        changeHostState {
            uuid = host2.uuid
            stateEvent = "disable"
        }

        // Can still migrate to host3 (different cluster)
        migrateVm {
            vmInstanceUuid = vm.uuid
            hostUuid = host3.uuid
        }
        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).hostUuid == host3.uuid

        // Re-enable host2
        changeHostState {
            uuid = host2.uuid
            stateEvent = "enable"
        }

        // Reset VM to host1
        resetVmToHost1(vm)
    }

    // #3: across=true, strategy=Soft, resource=true -> Migrate Freely
    void testCase3_AcrossTrue_Soft_ResourceTrue() {
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory
        HostInventory host2 = env.inventoryByName("host2") as HostInventory
        HostInventory host3 = env.inventoryByName("host3") as HostInventory

        updateGlobalConfig {
            category = VmGlobalConfig.CATEGORY
            name = "vm.ha.across.clusters"
            value = "true"
        }
        updateGlobalConfig {
            category = VmGlobalConfig.CATEGORY
            name = "resourceBinding.strategy"
            value = "Soft"
        }

        // Can migrate to host2 (same cluster)
        migrateVm {
            vmInstanceUuid = vm.uuid
            hostUuid = host2.uuid
        }
        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).hostUuid == host2.uuid

        // Can migrate to host3 (different cluster)
        migrateVm {
            vmInstanceUuid = vm.uuid
            hostUuid = host3.uuid
        }
        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).hostUuid == host3.uuid

        // Reset VM to host1
        resetVmToHost1(vm)
    }

    // #4: across=true, strategy=Soft, resource=false -> Migrate Freely
    void testCase4_AcrossTrue_Soft_ResourceFalse() {
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory
        HostInventory host2 = env.inventoryByName("host2") as HostInventory
        HostInventory host3 = env.inventoryByName("host3") as HostInventory

        updateGlobalConfig {
            category = VmGlobalConfig.CATEGORY
            name = "vm.ha.across.clusters"
            value = "true"
        }
        updateGlobalConfig {
            category = VmGlobalConfig.CATEGORY
            name = "resourceBinding.strategy"
            value = "Soft"
        }

        // Disable host2 to simulate resource=false in current cluster
        changeHostState {
            uuid = host2.uuid
            stateEvent = "disable"
        }

        // Can still migrate to host3 (different cluster)
        migrateVm {
            vmInstanceUuid = vm.uuid
            hostUuid = host3.uuid
        }
        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).hostUuid == host3.uuid

        // Re-enable host2
        changeHostState {
            uuid = host2.uuid
            stateEvent = "enable"
        }

        // Reset VM to host1
        resetVmToHost1(vm)
    }

    // #5: across=false, strategy=Hard, resource=true -> Migrate in Current Cluster
    void testCase5_AcrossFalse_Hard_ResourceTrue() {
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory
        HostInventory host2 = env.inventoryByName("host2") as HostInventory
        HostInventory host3 = env.inventoryByName("host3") as HostInventory
        ClusterInventory cluster1 = env.inventoryByName("cluster1") as ClusterInventory

        updateGlobalConfig {
            category = VmGlobalConfig.CATEGORY
            name = "vm.ha.across.clusters"
            value = "false"
        }
        updateGlobalConfig {
            category = VmGlobalConfig.CATEGORY
            name = "resourceBinding.strategy"
            value = "Hard"
        }

        // Can migrate to host2 (same cluster)
        migrateVm {
            vmInstanceUuid = vm.uuid
            hostUuid = host2.uuid
        }
        VmInstanceVO vmvo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vmvo.hostUuid == host2.uuid
        assert vmvo.clusterUuid == cluster1.uuid

        // Cannot migrate to host3 (different cluster)
        MigrateVmAction action = new MigrateVmAction()
        action.vmInstanceUuid = vm.uuid
        action.hostUuid = host3.uuid
        action.sessionId = adminSession()
        MigrateVmAction.Result ret = action.call()
        assert ret.error != null

        // Reset VM to host1
        resetVmToHost1(vm)
    }

    // #6: across=false, strategy=Hard, resource=false -> Fail
    void testCase6_AcrossFalse_Hard_ResourceFalse() {
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory
        HostInventory host2 = env.inventoryByName("host2") as HostInventory

        updateGlobalConfig {
            category = VmGlobalConfig.CATEGORY
            name = "vm.ha.across.clusters"
            value = "false"
        }
        updateGlobalConfig {
            category = VmGlobalConfig.CATEGORY
            name = "resourceBinding.strategy"
            value = "Hard"
        }

        // Disable host2 to simulate resource=false in current cluster
        changeHostState {
            uuid = host2.uuid
            stateEvent = "disable"
        }

        // Migration should fail (no available host in current cluster)
        MigrateVmAction action = new MigrateVmAction()
        action.vmInstanceUuid = vm.uuid
        action.sessionId = adminSession()
        MigrateVmAction.Result ret = action.call()
        assert ret.error != null

        // Re-enable host2
        changeHostState {
            uuid = host2.uuid
            stateEvent = "enable"
        }
    }

    // #7: across=false, strategy=Soft, resource=true -> Migrate in Current Cluster
    void testCase7_AcrossFalse_Soft_ResourceTrue() {
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory
        HostInventory host2 = env.inventoryByName("host2") as HostInventory
        ClusterInventory cluster1 = env.inventoryByName("cluster1") as ClusterInventory

        updateGlobalConfig {
            category = VmGlobalConfig.CATEGORY
            name = "vm.ha.across.clusters"
            value = "false"
        }
        updateGlobalConfig {
            category = VmGlobalConfig.CATEGORY
            name = "resourceBinding.strategy"
            value = "Soft"
        }

        // Can migrate to host2 (same cluster)
        migrateVm {
            vmInstanceUuid = vm.uuid
            hostUuid = host2.uuid
        }
        VmInstanceVO vmvo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vmvo.hostUuid == host2.uuid
        assert vmvo.clusterUuid == cluster1.uuid

        // Reset VM to host1
        resetVmToHost1(vm)
    }

    // #8: across=false, strategy=Soft, resource=false -> Try Other Clusters
    void testCase8_AcrossFalse_Soft_ResourceFalse() {
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory
        HostInventory host2 = env.inventoryByName("host2") as HostInventory
        HostInventory host3 = env.inventoryByName("host3") as HostInventory
        ClusterInventory cluster2 = env.inventoryByName("cluster2") as ClusterInventory

        updateGlobalConfig {
            category = VmGlobalConfig.CATEGORY
            name = "vm.ha.across.clusters"
            value = "false"
        }
        updateGlobalConfig {
            category = VmGlobalConfig.CATEGORY
            name = "resourceBinding.strategy"
            value = "Soft"
        }

        // Disable host2 to simulate resource=false in current cluster
        changeHostState {
            uuid = host2.uuid
            stateEvent = "disable"
        }

        // Can migrate to host3 (different cluster, soft strategy allows fallback)
        migrateVm {
            vmInstanceUuid = vm.uuid
            hostUuid = host3.uuid
        }
        VmInstanceVO vmvo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vmvo.hostUuid == host3.uuid
        assert vmvo.clusterUuid == cluster2.uuid

        // Re-enable host2
        changeHostState {
            uuid = host2.uuid
            stateEvent = "enable"
        }

        // Reset VM to host1
        resetVmToHost1(vm)
    }

    private void resetVmToHost1(VmInstanceInventory vm) {
        HostInventory host1 = env.inventoryByName("host1") as HostInventory

        // Temporarily enable cross-cluster to reset
        updateGlobalConfig {
            category = VmGlobalConfig.CATEGORY
            name = "vm.ha.across.clusters"
            value = "true"
        }

        VmInstanceVO vmvo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        if (vmvo.hostUuid != host1.uuid) {
            migrateVm {
                vmInstanceUuid = vm.uuid
                hostUuid = host1.uuid
            }
        }
    }
}
