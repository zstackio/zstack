package org.zstack.test.integration.server

import org.zstack.core.Platform
import org.zstack.core.cascade.CascadeConstant
import org.zstack.core.cascade.CascadeFacade
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.header.cluster.ClusterAO_
import org.zstack.header.cluster.ClusterVO
import org.zstack.header.core.Completion
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.server.PhysicalServerAO_
import org.zstack.header.server.PhysicalServerCapacityVO
import org.zstack.header.server.PhysicalServerCapacityVO_
import org.zstack.header.server.PhysicalServerHardwareDetailVO
import org.zstack.header.server.PhysicalServerHardwareDetailVO_
import org.zstack.header.server.PhysicalServerHardwareInfoVO
import org.zstack.header.server.PhysicalServerHardwareInfoVO_
import org.zstack.header.server.PhysicalServerRoleVO
import org.zstack.header.server.PhysicalServerRoleVO_
import org.zstack.header.server.PhysicalServerConstant
import org.zstack.header.server.SchedulingMode
import org.zstack.header.server.ServerRoleType
import org.zstack.header.server.ServerPoolState
import org.zstack.header.server.ServerPoolVO
import org.zstack.header.server.ServerPoolVO_
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.PhysicalServerInventory
import org.zstack.sdk.ServerPoolInventory
import org.zstack.sdk.ZoneInventory
import org.zstack.server.PhysicalServerGlobalConfig
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class PhysicalServerCascadeCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
            zone {
                name = "zone"
                cluster {
                    name = "cluster"
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            testOnClusterCreatePolicyCreatesDefaultServerPoolWhenClusterIsCreated()
            testOnClusterCreatePolicyKeepsCustomPoolWindow()
            testOnZoneCreatePolicyCreatesDefaultServerPoolWhenZoneIsCreated()
            testManualPolicyDoesNotCreateDefaultServerPoolAutomatically()
            testDeleteZoneCascadesPhysicalServerRoleRows()
            testDeleteServerPoolCascadeDeletesPhysicalServerHierarchy()
            testDeleteServerPoolClearsClusterAssociation()
            testDeleteZoneCascadesServerPoolPhysicalServerAndClusterAssociation()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    /**
     * Fixture helper (12a-rule exempt): seeds 1 PhysicalServerHardwareDetailVO
     * for cascade coverage. PhysicalServerHardwareInfoVO is NOT seeded here
     * because attachPhysicalServerRole(KVM_HOST) already enqueues hardware
     * discovery in UT mode which writes the (PK=serverUuid) row; a second
     * persist would collide on PRIMARY key. HardwareDetailVO has an
     * AUTO_INCREMENT id so multiple seed rows are safe.
     */
    private void seedHardwareDetail(String serverUuid) {
        def dbf = bean(DatabaseFacade.class)
        def detail = new PhysicalServerHardwareDetailVO()
        detail.serverUuid = serverUuid
        detail.type = "CPU"
        detail.itemModel = "fixture-cpu"
        dbf.persistAndRefresh(detail)
    }

    void testOnClusterCreatePolicyCreatesDefaultServerPoolWhenClusterIsCreated() {
        PhysicalServerGlobalConfig.DEFAULT_SERVER_POOL_CREATION_POLICY.updateValue("OnClusterCreate")

        def zone = createZone {
            name = "zone-default-pool-on-cluster"
        } as ZoneInventory

        def poolsBeforeCluster = queryServerPool {
            conditions = ["zoneUuid=${zone.uuid}".toString(), "isDefault=true"]
        }
        assert poolsBeforeCluster.isEmpty()

        def cluster = createCluster {
            name = "cluster-default-pool"
            zoneUuid = zone.uuid
            hypervisorType = "KVM"
        } as ClusterInventory

        def pools = queryServerPool {
            conditions = ["zoneUuid=${zone.uuid}".toString(), "isDefault=true"]
        }

        assert pools.size() == 1
        assert pools[0].name == PhysicalServerConstant.DEFAULT_SERVER_POOL_NAME
        assert pools[0].state == ServerPoolState.Enabled.toString()
        assert cluster.serverPoolUuid == pools[0].uuid
    }

    void testOnClusterCreatePolicyKeepsCustomPoolWindow() {
        PhysicalServerGlobalConfig.DEFAULT_SERVER_POOL_CREATION_POLICY.updateValue("OnClusterCreate")

        def zone = createZone {
            name = "zone-custom-pool-window"
        } as ZoneInventory

        def customPool = createServerPool {
            name = "custom-pool-before-cluster"
            zoneUuid = zone.uuid
        } as ServerPoolInventory

        def cluster = createCluster {
            name = "cluster-custom-pool-window"
            zoneUuid = zone.uuid
            hypervisorType = "KVM"
        } as ClusterInventory

        def defaultPools = queryServerPool {
            conditions = ["zoneUuid=${zone.uuid}".toString(), "isDefault=true"]
        }
        def customPools = queryServerPool {
            conditions = ["uuid=${customPool.uuid}".toString()]
        }

        assert defaultPools.isEmpty()
        assert customPools.size() == 1
        assert cluster.serverPoolUuid == null
    }

    void testOnZoneCreatePolicyCreatesDefaultServerPoolWhenZoneIsCreated() {
        PhysicalServerGlobalConfig.DEFAULT_SERVER_POOL_CREATION_POLICY.updateValue("OnZoneCreate")

        def zone = createZone {
            name = "zone-default-pool-on-zone"
        } as ZoneInventory

        def pools = queryServerPool {
            conditions = ["zoneUuid=${zone.uuid}".toString(), "isDefault=true"]
        }

        assert pools.size() == 1
        assert pools[0].name == PhysicalServerConstant.DEFAULT_SERVER_POOL_NAME
        assert pools[0].state == ServerPoolState.Enabled.toString()

        PhysicalServerGlobalConfig.DEFAULT_SERVER_POOL_CREATION_POLICY.updateValue("OnClusterCreate")
    }

    void testManualPolicyDoesNotCreateDefaultServerPoolAutomatically() {
        PhysicalServerGlobalConfig.DEFAULT_SERVER_POOL_CREATION_POLICY.updateValue("Manual")

        def zone = createZone {
            name = "zone-default-pool-manual"
        } as ZoneInventory

        def cluster = createCluster {
            name = "cluster-default-pool-manual"
            zoneUuid = zone.uuid
            hypervisorType = "KVM"
        } as ClusterInventory

        def pools = queryServerPool {
            conditions = ["zoneUuid=${zone.uuid}".toString(), "isDefault=true"]
        }

        assert pools.isEmpty()
        assert cluster.serverPoolUuid == null

        PhysicalServerGlobalConfig.DEFAULT_SERVER_POOL_CREATION_POLICY.updateValue("OnClusterCreate")
    }

    void testDeleteZoneCascadesPhysicalServerRoleRows() {
        def zone = createZone {
            name = "zone-ps-cascade"
        } as ZoneInventory

        def cluster = createCluster {
            name = "cluster-ps-cascade"
            zoneUuid = zone.uuid
            hypervisorType = "KVM"
        } as ClusterInventory

        def pool = createServerPool {
            name = "pool-ps-cascade"
            zoneUuid = zone.uuid
        } as ServerPoolInventory

        def server = createPhysicalServer {
            name = "server-ps-cascade"
            zoneUuid = zone.uuid
            poolUuid = pool.uuid
            managementIp = "127.0.250.10"
        } as PhysicalServerInventory

        // Real path-2: attachPhysicalServerRole(KVM_HOST) atomically creates
        // PhysicalServerRoleVO + KVMHostVO + PhysicalServerCapacityVO via the path-2
        // orchestrator. 12a red line: no inline dbf.persist of business state.
        attachPhysicalServerRole {
            serverUuid = server.uuid
            roleType = "KVM_HOST"
            clusterUuid = cluster.uuid
            roleConfig = [username: "root", password: "password", sshPort: "22"]
        }

        long roleCountBefore = Q.New(PhysicalServerRoleVO.class)
                .eq(PhysicalServerRoleVO_.serverUuid, server.uuid)
                .count()
        assert roleCountBefore == 1L : "prep failed: PhysicalServerRoleVO was not persisted"

        seedHardwareDetail(server.uuid)
        // HardwareInfoVO is auto-created by attach's hardware-discovery hook;
        // HardwareDetailVO comes from the seed above. Assert both present so the
        // post-cascade ==0 check is non-trivial.
        assert Q.New(PhysicalServerHardwareInfoVO.class)
                .eq(PhysicalServerHardwareInfoVO_.serverUuid, server.uuid).count() >= 1L
        assert Q.New(PhysicalServerHardwareDetailVO.class)
                .eq(PhysicalServerHardwareDetailVO_.serverUuid, server.uuid).count() >= 1L

        deleteZone {
            uuid = zone.uuid
        }

        long roleCountAfter = Q.New(PhysicalServerRoleVO.class)
                .eq(PhysicalServerRoleVO_.serverUuid, server.uuid)
                .count()
        assert roleCountAfter == 0L :
                "PhysicalServerRoleCascadeExtension must delete PhysicalServerRoleVO rows when PhysicalServer is deleted by zone cascade"
        // PhysicalServerCapacityVO (auto-created by attach) must also be cascade-cleaned
        assert Q.New(PhysicalServerCapacityVO.class)
                .eq(PhysicalServerCapacityVO_.uuid, server.uuid).count() == 0L :
                "PhysicalServerCapacityVO must cascade-delete when PhysicalServer is deleted"
        assert Q.New(PhysicalServerHardwareInfoVO.class)
                .eq(PhysicalServerHardwareInfoVO_.serverUuid, server.uuid).count() == 0L :
                "PhysicalServerHardwareInfoVO must cascade-delete when PhysicalServer is deleted"
        assert Q.New(PhysicalServerHardwareDetailVO.class)
                .eq(PhysicalServerHardwareDetailVO_.serverUuid, server.uuid).count() == 0L :
                "PhysicalServerHardwareDetailVO must cascade-delete when PhysicalServer is deleted"
    }

    void testDeleteServerPoolCascadeDeletesPhysicalServerHierarchy() {
        def dbf = bean(DatabaseFacade.class)
        def casf = bean(CascadeFacade.class)

        def zone = createZone {
            name = "zone-pool-cascade"
        } as ZoneInventory

        def cluster = createCluster {
            name = "cluster-pool-cascade"
            zoneUuid = zone.uuid
            hypervisorType = "KVM"
        } as ClusterInventory

        def pool = createServerPool {
            name = "pool-cascade"
            zoneUuid = zone.uuid
        } as ServerPoolInventory

        def server = createPhysicalServer {
            name = "server-pool-cascade"
            zoneUuid = zone.uuid
            poolUuid = pool.uuid
            managementIp = "127.0.250.11"
        } as PhysicalServerInventory

        // Real path-2 attach (12a red line: no inline dbf.persist).
        attachPhysicalServerRole {
            serverUuid = server.uuid
            roleType = "KVM_HOST"
            clusterUuid = cluster.uuid
            roleConfig = [username: "root", password: "password", sshPort: "22"]
        }

        seedHardwareDetail(server.uuid)

        def poolVO = dbf.findByUuid(pool.uuid, ServerPoolVO.class)
        boolean success = false
        ErrorCode failure = null
        casf.asyncCascade(CascadeConstant.DELETION_DELETE_CODE,
                ServerPoolVO.class.simpleName,
                [org.zstack.header.server.ServerPoolInventory.valueOf(poolVO)],
                new Completion(null) {
                    @Override
                    void success() {
                        success = true
                    }

                    @Override
                    void fail(ErrorCode errorCode) {
                        failure = errorCode
                    }
                })

        assert success : "ServerPool cascade failed: ${failure}"
        assert Q.New(ServerPoolVO.class).eq(org.zstack.header.server.ServerPoolVO_.uuid, pool.uuid).count() == 0L
        assert Q.New(org.zstack.header.server.PhysicalServerVO.class)
                .eq(org.zstack.header.server.PhysicalServerAO_.uuid, server.uuid)
                .count() == 0L
        assert Q.New(PhysicalServerRoleVO.class).eq(PhysicalServerRoleVO_.serverUuid, server.uuid).count() == 0L
        // PhysicalServerCapacityVO (auto-created by attach) cascades on PhysicalServer delete
        assert Q.New(PhysicalServerCapacityVO.class)
                .eq(PhysicalServerCapacityVO_.uuid, server.uuid).count() == 0L :
                "PhysicalServerCapacityVO must cascade-delete with PhysicalServer"
        assert Q.New(PhysicalServerHardwareInfoVO.class)
                .eq(PhysicalServerHardwareInfoVO_.serverUuid, server.uuid).count() == 0L :
                "PhysicalServerHardwareInfoVO must cascade-delete with PhysicalServer"
        assert Q.New(PhysicalServerHardwareDetailVO.class)
                .eq(PhysicalServerHardwareDetailVO_.serverUuid, server.uuid).count() == 0L :
                "PhysicalServerHardwareDetailVO must cascade-delete with PhysicalServer"
    }

    void testDeleteServerPoolClearsClusterAssociation() {
        def zone = env.inventoryByName("zone") as ZoneInventory
        def cluster = env.inventoryByName("cluster") as ClusterInventory

        def pool = createServerPool {
            name = "pool-delete-cluster-link"
            zoneUuid = zone.uuid
        } as ServerPoolInventory

        changeClusterServerPool {
            clusterUuid = cluster.uuid
            serverPoolUuid = pool.uuid
        }

        deleteServerPool { uuid = pool.uuid }

        def clusters = queryCluster {
            conditions = ["uuid=${cluster.uuid}".toString()]
        }
        assert clusters[0].serverPoolUuid == null
    }

    void testDeleteZoneCascadesServerPoolPhysicalServerAndClusterAssociation() {
        def dbf = bean(DatabaseFacade.class)

        def zone = createZone {
            name = "zone-cluster-pool-ps-cascade"
        } as ZoneInventory

        def cluster = createCluster {
            name = "cluster-zone-cascade"
            zoneUuid = zone.uuid
            hypervisorType = "KVM"
        } as ClusterInventory

        def pool = createServerPool {
            name = "pool-zone-cascade"
            zoneUuid = zone.uuid
        } as ServerPoolInventory

        changeClusterServerPool {
            clusterUuid = cluster.uuid
            serverPoolUuid = pool.uuid
        }

        def server = createPhysicalServer {
            name = "server-zone-cascade"
            zoneUuid = zone.uuid
            poolUuid = pool.uuid
            managementIp = "127.0.250.12"
        } as PhysicalServerInventory

        // Real path-2 attach (12a red line: no inline dbf.persist).
        attachPhysicalServerRole {
            serverUuid = server.uuid
            roleType = "KVM_HOST"
            clusterUuid = cluster.uuid
            roleConfig = [username: "root", password: "password", sshPort: "22"]
        }

        seedHardwareDetail(server.uuid)

        assert Q.New(ClusterVO.class).eq(ClusterAO_.uuid, cluster.uuid).count() == 1L
        assert Q.New(ServerPoolVO.class).eq(ServerPoolVO_.uuid, pool.uuid).count() == 1L
        assert Q.New(org.zstack.header.server.PhysicalServerVO.class).eq(PhysicalServerAO_.uuid, server.uuid).count() == 1L

        deleteZone {
            uuid = zone.uuid
        }

        assert Q.New(ClusterVO.class).eq(ClusterAO_.uuid, cluster.uuid).count() == 0L
        assert Q.New(ServerPoolVO.class).eq(ServerPoolVO_.uuid, pool.uuid).count() == 0L
        assert Q.New(org.zstack.header.server.PhysicalServerVO.class).eq(PhysicalServerAO_.uuid, server.uuid).count() == 0L
        assert Q.New(PhysicalServerRoleVO.class).eq(PhysicalServerRoleVO_.serverUuid, server.uuid).count() == 0L
        // PhysicalServerCapacityVO (auto-created by attach) cascades on PhysicalServer delete
        assert Q.New(PhysicalServerCapacityVO.class)
                .eq(PhysicalServerCapacityVO_.uuid, server.uuid).count() == 0L :
                "PhysicalServerCapacityVO must cascade-delete with PhysicalServer"
        assert Q.New(PhysicalServerHardwareInfoVO.class)
                .eq(PhysicalServerHardwareInfoVO_.serverUuid, server.uuid).count() == 0L :
                "PhysicalServerHardwareInfoVO must cascade-delete with PhysicalServer"
        assert Q.New(PhysicalServerHardwareDetailVO.class)
                .eq(PhysicalServerHardwareDetailVO_.serverUuid, server.uuid).count() == 0L :
                "PhysicalServerHardwareDetailVO must cascade-delete with PhysicalServer"
    }

}
