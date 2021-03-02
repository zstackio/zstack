package org.zstack.test.integration.network.l3network.ipv6

import org.zstack.sdk.*
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.springframework.http.HttpEntity
import org.zstack.utils.data.SizeUnit
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMAgentCommands
import org.apache.commons.collections.list.SynchronizedList


class DeleteL2NetworkBridgeCase extends SubCase {
    EnvSpec env
    ZoneInventory zone
    ClusterInventory cluster

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
        useSpring(KvmTest.springSpec)

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
                        name = "kvm1"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"

                        totalCpu = 8
                        totalMem = SizeUnit.GIGABYTE.toByte(20)
                    }

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"

                        totalCpu = 8
                        totalMem = SizeUnit.GIGABYTE.toByte(20)
                    }

                    attachPrimaryStorage("local")
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
            initEnv()
            testDeleteL2NoVlanNework()
            testDeleteL2VlanNework()
            testDeleteL2VxlanNework()
        }
    }

    void initEnv(){
        zone = env.inventoryByName("zone") as ZoneInventory
        cluster = env.inventoryByName("cluster") as ClusterInventory
    }

    void testDeleteL2NoVlanNework() {
        // br_eth456789012 is no longer than 15
        def nicName = "eth0"

        def l2_1 = createL2NoVlanNetwork {
            name = "L2-1"
            zoneUuid = zone.uuid
            physicalInterface = nicName
        } as L2NetworkInventory

        attachL2NetworkToCluster {
            l2NetworkUuid = l2_1.uuid
            clusterUuid = cluster.uuid
        }

        env.simulator(KVMConstant.KVM_DELETE_L2NOVLAN_NETWORK_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            return  new KVMAgentCommands.DeleteBridgeResponse()
        }


//        def cmds = [] as SynchronizedList<KVMAgentCommands.DeleteBridgeCmd>
//        env.afterSimulator(KVMConstant.KVM_DELETE_L2NOVLAN_NETWORK_PATH) { rsp, HttpEntity<String> e ->
//            deleteBridgeCmd = json(e.body, KVMAgentCommands.DeleteBridgeCmd.class)
//            cmds.add(deleteBridgeCmd)
//            return rsp
//        }

        deleteL2Network {
            uuid = l2_1.uuid
        }

//        assert cmds.size()==2

//        List<VirtualRouterLoadBalancerBackend.RefreshLbCmd> cmds = new ArrayList<>()
//        env.afterSimulator(VirtualRouterLoadBalancerBackend.REFRESH_LB_PATH) { rsp, HttpEntity<String> e ->
//            VirtualRouterLoadBalancerBackend.RefreshLbCmd cmd =
//                    JSONObjectUtil.toObject(e.body, VirtualRouterLoadBalancerBackend.RefreshLbCmd.class)
//            cmds.add(cmd)
//            return rsp
//        }
//        addVmNicToLoadBalancer {
//            vmNicUuids = [vm3.vmNics[0].uuid]
//            listenerUuid = listener1.uuid
//        }
//        assert cmds.size() == 1


    }

    void testDeleteL2VlanNework(){}

    void testDeleteL2VxlanNework(){}

}

