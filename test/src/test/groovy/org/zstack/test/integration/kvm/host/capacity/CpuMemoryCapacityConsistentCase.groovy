package org.zstack.test.integration.kvm.host.capacity

import org.zstack.core.db.DatabaseFacade
import org.zstack.header.host.HostStateEvent
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.GetCpuMemoryCapacityAction
import org.zstack.sdk.HostInventory
import org.zstack.sdk.KVMHostInventory
import org.zstack.sdk.QueryHostAction
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.SizeUtils
import org.zstack.utils.data.SizeUnit

class CpuMemoryCapacityConsistentCase extends SubCase{
    EnvSpec env
    DatabaseFacade dbf
    HostInventory hostInventory
    ClusterInventory clusterInventory
    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                        totalCpu = 4
                        totalMem = SizeUnit.GIGABYTE.toByte(8)
                    }
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            prepare()
            setHostMaintainAndTestGetCpuMemoryCapacityInCluster()
            setHostMaintainAndTestGetCpuMemoryCapacityOnHost()
        }
    }

    void prepare(){
        dbf = bean(DatabaseFacade.class)
        hostInventory = env.inventoryByName("kvm") as HostInventory
        clusterInventory = env.inventoryByName("cluster") as ClusterInventory
    }

    void setHostMaintainAndTestGetCpuMemoryCapacityInCluster() {
        changeHostState {
            uuid = hostInventory.uuid
            stateEvent = HostStateEvent.maintain.toString()
        }
        GetCpuMemoryCapacityAction action = new GetCpuMemoryCapacityAction()
        action.sessionId = adminSession()
        List list = new ArrayList()
        list.add(clusterInventory.uuid)
        action.clusterUuids = list
        GetCpuMemoryCapacityAction.Result res = action.call()
        assert res.error == null
        long totalMemory = res.value.totalMemory
        long managedCpuNum = res.value.managedCpuNum
        long availableMemory = res.value.availableMemory
        long availableCpu = res.value.availableCpu
        assert totalMemory == SizeUtils.sizeStringToBytes("8G")
        assert availableMemory == 0
        assert availableCpu == 0
        assert managedCpuNum == 4

    }

    void setHostMaintainAndTestGetCpuMemoryCapacityOnHost() {
        List<KVMHostInventory> host = queryHost {
            sessionId = adminSession()
            conditions = ["uuid=${hostInventory.uuid}"]
        } as List<KVMHostInventory>
        long totalCpuCapacity = host[0].totalCpuCapacity
        long availableCpuCapacity = host[0].availableCpuCapacity
        assert totalCpuCapacity == availableCpuCapacity

        GetCpuMemoryCapacityAction getCpuMemoryCapacityAction = new GetCpuMemoryCapacityAction()
        getCpuMemoryCapacityAction.sessionId = adminSession()
        List list = new ArrayList()
        list.add(hostInventory.uuid)
        getCpuMemoryCapacityAction.hostUuids = list
        GetCpuMemoryCapacityAction.Result res = getCpuMemoryCapacityAction.call()
        long totalMemory = res.value.totalMemory
        long availableMemory = res.value.availableMemory
        assert totalMemory == availableMemory
        assert totalMemory == SizeUtils.sizeStringToBytes("8G")
    }
}
