package org.zstack.test.integration.network.vxlanNetwork

import org.apache.commons.collections.list.SynchronizedList
import org.springframework.http.HttpEntity
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanKvmAgentCommands
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolConstant
import org.zstack.network.l2.vxlan.vtep.RemoteVtepVO;
import org.zstack.network.l2.vxlan.vtep.RemoteVtepVO_;
import org.zstack.sdk.*
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.core.db.Q
import java.util.stream.Collectors
import org.zstack.sdk.ApiException


class AddRemoteVxlanVtepIpCase extends SubCase {
    private static final String IPV4_REMOTE_VTEP_IP = "1.1.1.1"
    private static final String IPV6_REMOTE_VTEP_IP = "2001:db8:ffff::10"
    private static final String IPV6_REMOTE_VTEP_FULL_IP = "2001:0db8:ffff:0000:0000:0000:0000:0010"
    private static final String INVALID_REMOTE_VTEP_IP = "not-a-vtep-ip"

    EnvSpec env

    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster1"
                    hypervisorType = "KVM"
                }

                cluster {
                    name = "cluster2"
                    hypervisorType = "KVM"
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            testRemoteVxlanVtepIp()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testRemoteVxlanVtepIp() {
        def zone = env.inventoryByName("zone") as ZoneInventory
        def cluster = env.inventoryByName("cluster1") as ClusterInventory
        def cluster2 = env.inventoryByName("cluster2") as ClusterInventory

        def pool = createL2VxlanNetworkPool {
            name = "TestVxlanPool1"
            type = "VxlanNetworkPool"
            zoneUuid = zone.uuid
        } as L2VxlanNetworkPoolInventory

        createVniRange {
            startVni = 100
            endVni = 10000
            l2NetworkUuid = pool.uuid
            name = "TestVxlanPool1"
        }

        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_CHECK_L2VXLAN_NETWORK_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            def resp = new VxlanKvmAgentCommands.CheckVxlanCidrResponse() as VxlanKvmAgentCommands.CheckVxlanCidrResponse
            resp.setSuccess(true)
            return resp
        }

        attachL2NetworkToCluster {
            l2NetworkUuid = pool.uuid
            clusterUuid = cluster.uuid
            systemTags = ["l2NetworkUuid::${pool.getUuid()}::clusterUuid::${cluster.uuid}::cidr::{192.168.0.0/16}".toString()]
        }

        attachL2NetworkToCluster {
            l2NetworkUuid = pool.uuid
            clusterUuid = cluster2.uuid
            systemTags = ["l2NetworkUuid::${pool.getUuid()}::clusterUuid::${cluster2.uuid}::cidr::{192.127.0.0/16}".toString()]
        }

        createVxlanPoolRemoteVtep {
            l2NetworkUuid = pool.uuid
            clusterUuid = cluster.uuid
            remoteVtepIp = IPV4_REMOTE_VTEP_IP
        }
        assert Q.New(RemoteVtepVO.class).eq(RemoteVtepVO_.poolUuid, pool.uuid).isExists()
        assert Q.New(RemoteVtepVO.class).eq(RemoteVtepVO_.poolUuid, pool.uuid).eq(RemoteVtepVO_.clusterUuid,cluster.uuid).isExists()
        assert Q.New(RemoteVtepVO.class).eq(RemoteVtepVO_.poolUuid, pool.uuid).count() == 1
        assert Q.New(RemoteVtepVO.class).eq(RemoteVtepVO_.poolUuid, pool.uuid).eq(RemoteVtepVO_.clusterUuid,cluster.uuid).count() == 1 

        createVxlanPoolRemoteVtep {
            l2NetworkUuid = pool.uuid
            clusterUuid = cluster2.uuid
            remoteVtepIp = IPV4_REMOTE_VTEP_IP
        }
        assert Q.New(RemoteVtepVO.class).eq(RemoteVtepVO_.poolUuid, pool.uuid).isExists()
        assert Q.New(RemoteVtepVO.class).eq(RemoteVtepVO_.poolUuid, pool.uuid).eq(RemoteVtepVO_.clusterUuid,cluster2.uuid).isExists()
        assert Q.New(RemoteVtepVO.class).eq(RemoteVtepVO_.poolUuid, pool.uuid).count() == 2
        assert Q.New(RemoteVtepVO.class).eq(RemoteVtepVO_.poolUuid, pool.uuid).eq(RemoteVtepVO_.clusterUuid,cluster2.uuid).count() == 1 
        expect(AssertionError.class) {
            createVxlanPoolRemoteVtep {
                l2NetworkUuid = pool.uuid
                clusterUuid = cluster2.uuid
                remoteVtepIp = IPV4_REMOTE_VTEP_IP
            }        
        }

        deleteVxlanPoolRemoteVtep {
            l2NetworkUuid = pool.uuid
            clusterUuid = cluster.uuid 
            remoteVtepIp = IPV4_REMOTE_VTEP_IP
        }
        assert !Q.New(RemoteVtepVO.class).eq(RemoteVtepVO_.poolUuid, pool.uuid).eq(RemoteVtepVO_.clusterUuid,cluster.uuid).isExists()
        assert Q.New(RemoteVtepVO.class).eq(RemoteVtepVO_.poolUuid, pool.uuid).count() == 1

        deleteVxlanPoolRemoteVtep {
            l2NetworkUuid = pool.uuid
            clusterUuid = cluster2.uuid 
            remoteVtepIp = IPV4_REMOTE_VTEP_IP
        }
        assert !Q.New(RemoteVtepVO.class).eq(RemoteVtepVO_.poolUuid, pool.uuid).isExists()

        createVxlanPoolRemoteVtep {
            l2NetworkUuid = pool.uuid
            clusterUuid = cluster.uuid
            remoteVtepIp = IPV6_REMOTE_VTEP_IP
        }
        assert Q.New(RemoteVtepVO.class)
                .eq(RemoteVtepVO_.poolUuid, pool.uuid)
                .eq(RemoteVtepVO_.clusterUuid, cluster.uuid)
                .eq(RemoteVtepVO_.vtepIp, IPV6_REMOTE_VTEP_IP)
                .isExists()
        assert Q.New(RemoteVtepVO.class).eq(RemoteVtepVO_.poolUuid, pool.uuid).count() == 1
        expect(AssertionError.class) {
            createVxlanPoolRemoteVtep {
                l2NetworkUuid = pool.uuid
                clusterUuid = cluster.uuid
                remoteVtepIp = " ${IPV6_REMOTE_VTEP_FULL_IP}\n"
            }
        }
        deleteVxlanPoolRemoteVtep {
            l2NetworkUuid = pool.uuid
            clusterUuid = cluster.uuid
            remoteVtepIp = " ${IPV6_REMOTE_VTEP_FULL_IP}\n"
        }
        assert !Q.New(RemoteVtepVO.class)
                .eq(RemoteVtepVO_.poolUuid, pool.uuid)
                .eq(RemoteVtepVO_.clusterUuid, cluster.uuid)
                .eq(RemoteVtepVO_.vtepIp, IPV6_REMOTE_VTEP_IP)
                .isExists()

        expect(AssertionError.class) {
            createVxlanPoolRemoteVtep {
                l2NetworkUuid = pool.uuid
                clusterUuid = cluster2.uuid
                remoteVtepIp = INVALID_REMOTE_VTEP_IP
            }
        }
        assert !Q.New(RemoteVtepVO.class)
                .eq(RemoteVtepVO_.poolUuid, pool.uuid)
                .eq(RemoteVtepVO_.vtepIp, INVALID_REMOTE_VTEP_IP)
                .isExists()
    }
}
