package org.zstack.test.integration.network.vxlanNetwork

import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.KVMHostInventory
import org.zstack.sdk.L2VxlanNetworkPoolInventory
import org.zstack.sdk.ZoneInventory
import org.springframework.http.HttpEntity
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanKvmAgentCommands
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolConstant
import org.zstack.sdk.*
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil
import java.util.stream.Collectors

class VxlanAttachClusterCase extends SubCase {
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
                        managementIp = "127.1.0.1"
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

                        totalCpu = 4
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
            testVxlanVtepIpChanged()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testVxlanVtepIpChanged() {
        def zone = env.inventoryByName("zone") as ZoneInventory
        def cluster = env.inventoryByName("cluster1") as ClusterInventory
        def host1 = env.inventoryByName("kvm1") as KVMHostInventory
        def host2 = env.inventoryByName("kvm2") as KVMHostInventory

        def pool = createL2VxlanNetworkPool {
            name = "TestVxlanPool1"
            zoneUuid = zone.uuid
        } as L2VxlanNetworkPoolInventory

        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_CHECK_L2VXLAN_NETWORK_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            def resp = new VxlanKvmAgentCommands.CheckVxlanCidrResponse() as VxlanKvmAgentCommands.CheckVxlanCidrResponse
            if (entity.getHeaders().get("X-Resource-UUID")[0] == host1.uuid) {
                resp.vtepIp = host1.managementIp
            } else(entity.getHeaders().get("X-Resource-UUID")[0] == host2.uuid) {
                resp.vtepIp = host2.managementIp
            }
            return resp
        }

        expect(AssertionError) {
            attachL2NetworkToCluster {
                l2NetworkUuid = pool.uuid
                clusterUuid = cluster.uuid
                systemTags = ["l2NetworkUuid::${pool.getUuid()}::clusterUuid::${cluster.uuid}::cidr::{127.1.0.0/16}".toString()]
            }
        }

        List<VtepInventory> vtepinvs = queryVtep {
            conditions = ["poolUuid=${pool.getUuid()}".toString()]
        }
        List<String> vtepIps = vtepinvs.stream().map{vtep -> vtep.vtepIp}.distinct().collect(Collectors.toList())
        assert vtepIps.size() == 0
    }
}
