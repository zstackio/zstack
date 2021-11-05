package org.zstack.test.integration.network.vxlanNetwork

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.identity.SharedResourceVO
import org.zstack.header.identity.SharedResourceVO_
import org.zstack.header.network.l2.L2NetworkClusterRefVO
import org.zstack.header.network.l2.L2NetworkClusterRefVO_
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanKvmAgentCommands
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolConstant
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.L2VxlanNetworkPoolInventory
import org.zstack.sdk.ZoneInventory
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by MaJin on 2017-07-18.
 */
class VxlanPoolShareCase extends SubCase{
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
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"

                        totalCpu = 8
                        totalMem = SizeUnit.GIGABYTE.toByte(20)
                    }

                    kvm {
                        name = "kvm1-2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"

                        totalCpu = 8
                        totalMem = SizeUnit.GIGABYTE.toByte(20)
                    }

                    attachPrimaryStorage("local")

                }

                cluster {
                    name = "cluster2"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.1.1"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")

                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                attachBackupStorage("sftp")
            }

            zone {
                name = "zone2"
                description = "test"
            }
        }
    }

    @Override
    void test() {
        env.create {
            testShareVxlanPool()
            testReConnectHostFail()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testShareVxlanPool(){
        ZoneInventory zone = env.inventoryByName("zone") as ZoneInventory
        L2VxlanNetworkPoolInventory poolinv = createL2VxlanNetworkPool {
            name = "TestVxlanPool"
            zoneUuid = zone.getUuid()
        } as L2VxlanNetworkPoolInventory

        shareResource {
            resourceUuids = [poolinv.uuid]
            toPublic = true
        }

        assert Q.New(SharedResourceVO.class).eq(SharedResourceVO_.resourceUuid, poolinv.uuid).isExists()

        deleteL2Network {
            uuid = poolinv.uuid
        }

        assert !Q.New(SharedResourceVO.class).eq(SharedResourceVO_.resourceUuid, poolinv.uuid).isExists()
    }

    void testReConnectHostFail() {
        HostInventory h1 = env.inventoryByName("kvm1") as HostInventory
        ClusterInventory cluster = env.inventoryByName("cluster1") as ClusterInventory
        ZoneInventory zone = env.inventoryByName("zone") as ZoneInventory

        L2VxlanNetworkPoolInventory poolinv = createL2VxlanNetworkPool {
            name = "TestVxlanPool"
            zoneUuid = zone.getUuid()
        } as L2VxlanNetworkPoolInventory

        attachL2NetworkToCluster {
            l2NetworkUuid = poolinv.uuid
            clusterUuid = cluster.uuid
            systemTags = ["l2NetworkUuid::${poolinv.getUuid()}::clusterUuid::${cluster.uuid}::cidr::{127.1.0.0/16}".toString()]
        }

        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_CHECK_L2VXLAN_NETWORK_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            VxlanKvmAgentCommands.CheckVxlanCidrResponse resp = new VxlanKvmAgentCommands.CheckVxlanCidrResponse()
            resp.setError("on purpose")
            return resp
        }

        reconnectHost {
            uuid = h1.uuid
        }

        L2NetworkClusterRefVO ref = Q.New(L2NetworkClusterRefVO.class)
                .eq(L2NetworkClusterRefVO_.clusterUuid, cluster.uuid)
                .eq(L2NetworkClusterRefVO_.l2NetworkUuid, poolinv.uuid).find()
        assert ref != null
    }
}
