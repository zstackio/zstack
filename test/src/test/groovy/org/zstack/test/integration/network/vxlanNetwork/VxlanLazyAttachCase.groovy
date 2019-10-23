package org.zstack.test.integration.network.vxlanNetwork

import org.apache.commons.collections.list.SynchronizedList
import org.springframework.http.HttpEntity
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkGlobalConfig
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanKvmAgentCommands
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolConstant
import org.zstack.sdk.*
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil
/**
 * @author: zhanyong.miao
 * @date: 2019-10-23
 * */
class VxlanLazyAttachCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(1)
                cpu = 1
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

                image {
                    name = "vr"
                    url = "http://zstack.org/download/vr.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster1"
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
                        managementIp = "127.0.0.1"
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

                attachBackupStorage("sftp")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testVxlanCreateCase()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testVxlanCreateCase() {
        def zone = env.inventoryByName("zone") as ZoneInventory
        def cluster = env.inventoryByName("cluster1") as ClusterInventory
        def host1 = env.inventoryByName("kvm1") as KVMHostInventory
        def host2 = env.inventoryByName("kvm2") as KVMHostInventory

        VxlanNetworkGlobalConfig.CLUSTER_LAZY_ATTACH.@value = false;
        assert VxlanNetworkGlobalConfig.CLUSTER_LAZY_ATTACH.value(boolean .class) == false


        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_REALIZE_L2VXLAN_NETWORK_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            return new VxlanKvmAgentCommands.CreateVxlanBridgeResponse()
        }

        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_POPULATE_FDB_L2VXLAN_NETWORK_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            return new VxlanKvmAgentCommands.PopulateVxlanFdbResponse()
        }

        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_POPULATE_FDB_L2VXLAN_NETWORKS_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            return new VxlanKvmAgentCommands.PopulateVxlanNetworksFdbCmd()
        }

        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_REALIZE_L2VXLAN_NETWORKS_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            return new VxlanKvmAgentCommands.CreateVxlanBridgesCmd()
        }

        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_CHECK_L2VXLAN_NETWORK_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            def resp = new VxlanKvmAgentCommands.CheckVxlanCidrResponse() as VxlanKvmAgentCommands.CheckVxlanCidrResponse
            if (entity.getHeaders().get("X-Resource-UUID")[0] == host1.uuid) {
                resp.vtepIp = "192.168.102.10"
            } else {
                resp.vtepIp = "192.168.100.11"
            }
            resp.setSuccess(true)
            return resp
        }

        def pool = createL2VxlanNetworkPool {
            name = "TestVxlanPool1"
            zoneUuid = zone.uuid
        } as L2VxlanNetworkPoolInventory

        attachL2NetworkToCluster {
            l2NetworkUuid = pool.uuid
            clusterUuid = cluster.uuid
            systemTags = ["l2NetworkUuid::${pool.getUuid()}::clusterUuid::${cluster.uuid}::cidr::{192.168.0.0/16}".toString()]
        }

        createVniRange {
            startVni = 100
            endVni = 10000
            l2NetworkUuid = pool.uuid
            name = "TestRange1"
        }

        boolean disconnected = true
        KVMAgentCommands.ConnectCmd reConnectCmd = null
        env.afterSimulator(KVMConstant.KVM_CONNECT_PATH) { KVMAgentCommands.AgentResponse rsp, HttpEntity<String> e ->
            reConnectCmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.ConnectCmd.class)
            if ((reConnectCmd.hostUuid == host1.uuid) && disconnected) {
                rsp.success = false
            }
            return rsp
        }

        def realizeRecords = [] as SynchronizedList<String>
        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_REALIZE_L2VXLAN_NETWORK_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(entity.body, VxlanKvmAgentCommands.CreateVxlanBridgeCmd.class)
            realizeRecords.add(cmd.l2NetworkUuid)
            return new VxlanKvmAgentCommands.CreateVxlanBridgesCmd()
        }

        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_POPULATE_FDB_L2VXLAN_NETWORKS_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(entity.body, VxlanKvmAgentCommands.PopulateVxlanNetworksFdbCmd.class)
            realizeRecords.addAll(cmd.networkUuids)
            return new VxlanKvmAgentCommands.PopulateVxlanNetworksFdbCmd()
        }

        expect(AssertionError.class) {
            reconnectHost { uuid = host1.uuid }
        }

        createL2VxlanNetwork {
            poolUuid = pool.uuid
            name = "TestVxlan1"
            zoneUuid = zone.uuid
        } as L2VxlanNetworkInventory

        retryInSecs {
            assert realizeRecords.size() == 1
        }

        disconnected = false
        realizeRecords.clear()
        reconnectHost { uuid = host1.uuid }

        createL2VxlanNetwork {
            poolUuid = pool.uuid
            name = "TestVxlan2"
            zoneUuid = zone.uuid
        } as L2VxlanNetworkInventory
        
        retryInSecs {
            assert realizeRecords.size() == 2
        }
    }
}