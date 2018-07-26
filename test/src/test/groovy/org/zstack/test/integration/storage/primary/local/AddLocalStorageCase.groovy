package org.zstack.test.integration.storage.primary.local

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.header.storage.primary.PrimaryStorageState
import org.zstack.header.storage.primary.PrimaryStorageStateEvent
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.AddLocalPrimaryStorageAction
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.storage.primary.local.APILocalStorageMigrateVolumeMsg
import org.zstack.storage.primary.local.LocalStorageHostRefVO
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.*
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by camile on 2017/4/
 */
class AddLocalStorageCase extends SubCase {
    EnvSpec env

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
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            addErrorPathLSailure()
            testAttachLocalStorage()
        }
    }

    void addErrorPathLSailure() {
        String zoneUuid = (env.specByName("zone") as ZoneSpec).inventory.uuid
        AddLocalPrimaryStorageAction addLocalPrimaryStorageAction = new AddLocalPrimaryStorageAction()
        addLocalPrimaryStorageAction.url = "/dev/test"
        addLocalPrimaryStorageAction.name = "test2"
        addLocalPrimaryStorageAction.zoneUuid = zoneUuid
        addLocalPrimaryStorageAction.sessionId = adminSession()
        AddLocalPrimaryStorageAction.Result res= addLocalPrimaryStorageAction.call()
        assert res.error !=null
        addLocalPrimaryStorageAction.url = "/proc/test"
        res= addLocalPrimaryStorageAction.call()
        assert res.error !=null
        addLocalPrimaryStorageAction.url = "/sys/test"
        res= addLocalPrimaryStorageAction.call()
        assert res.error !=null
    }

    void testAttachLocalStorage(){
        def cluster = env.inventoryByName("cluster") as ClusterInventory
        def host = env.inventoryByName("kvm") as HostInventory
        def ls = env.inventoryByName("local") as PrimaryStorageInventory
        SQL.New(HostVO.class).set(HostVO_.status, HostStatus.Connecting).eq(HostVO_.uuid, host.uuid).update()
        attachPrimaryStorageToCluster {
            clusterUuid = cluster.uuid
            primaryStorageUuid = ls.uuid
        }
        retryInSecs(3) {
            assert Q.New(LocalStorageHostRefVO.class).count() == 1
        }

        detachPrimaryStorageFromCluster {
            clusterUuid = cluster.uuid
            primaryStorageUuid = ls.uuid
        }
    }
}
