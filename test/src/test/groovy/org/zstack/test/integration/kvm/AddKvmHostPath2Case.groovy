package org.zstack.test.integration.kvm

import org.zstack.core.db.Q
import org.zstack.header.server.PhysicalServerCapacityVO
import org.zstack.header.server.PhysicalServerCapacityVO_
import org.zstack.header.server.PhysicalServerRoleVO
import org.zstack.header.server.PhysicalServerRoleVO_
import org.zstack.header.server.PhysicalServerVO
import org.zstack.header.server.PhysicalServerAO_
import org.zstack.kvm.KVMHostVO
import org.zstack.kvm.KVMHostVO_
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.PhysicalServerInventory
import org.zstack.sdk.ServerPoolInventory
import org.zstack.sdk.ZoneInventory
import org.zstack.test.integration.kvm.host.HostEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import static org.zstack.kvm.KVMConstant.KVM_HOST_FACT_PATH

/**
 * Phase 3 fix-plan Wave 1 U1a — KVM AddHost path 2 接 3 Flow + post-commit hook.
 *
 * 覆盖 AC-RS-04 (KVM 路径 2 不读 AddKVMHostMsg.serverUuid) — 具体两条主路径：
 *   AC-RS-04-A  pre-resolved serverUuid (caller 已知 PS) → 3 Flow 复用同 PS
 *   AC-RS-04-B  null serverUuid + cluster bound to pool → AutoAssociateFlow tier-3 创建新 PS
 *
 * 不覆盖（留 后续 case）：
 *   - rollback path（mid-chain failure → CreatePhysicalServerRole + InitCapacity 反向回滚）
 *     需 mock 让 connect / arch-check 失败；本 case 走 happy path 验证 ordering
 *   - path 1 attach + path 2 addHost 并发 race（依赖 lockPhysicalServerForAttach + UPSERT
 *     idempotency；KvmRoleProviderIntegrationCase.testAc2ConcurrentAttachUniqueConstraint
 *     已覆盖 lock 行为，本 case 复测增量低）
 */
class AddKvmHostPath2Case extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = HostEnv.noHostBasicEnv()
    }

    @Override
    void test() {
        env.create {
            // KVM connect flow needs simulator path mocked otherwise SSH /
            // host-fact retrieval fails inside send-connect-host-message
            env.afterSimulator(KVM_HOST_FACT_PATH) { rsp -> rsp }

            testPathTwoWithPreResolvedServerUuid()
            testPathTwoAutoAssociateTier3()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    // ----------------------------------------------------------------
    // AC-RS-04-A — pre-resolved serverUuid
    // ----------------------------------------------------------------

    void testPathTwoWithPreResolvedServerUuid() {
        def zone = env.inventoryByName("zone") as ZoneInventory
        def cluster = env.inventoryByName("cluster") as ClusterInventory

        // Create a ServerPool + PhysicalServer up front (caller-side resolution)
        def pool = createServerPool {
            name = "pool-u1a-pre"
            delegate.zoneUuid = zone.uuid
        } as ServerPoolInventory

        def ps = createPhysicalServer {
            name = "ps-u1a-pre"
            delegate.zoneUuid = zone.uuid
            delegate.poolUuid = pool.uuid
            managementIp = "127.0.0.30"
        } as PhysicalServerInventory

        long kvmCountBefore = Q.New(KVMHostVO.class).count()

        def host = addKVMHost {
            name = "host-u1a-pre"
            managementIp = "127.0.0.30"
            clusterUuid = cluster.uuid
            username = "root"
            password = "password"
            delegate.serverUuid = ps.uuid
        } as HostInventory

        // KVMHostVO appears
        long kvmCountAfter = Q.New(KVMHostVO.class).count()
        assert kvmCountAfter == kvmCountBefore + 1 : "AC-RS-04-A 失败: KVMHostVO 没增加"

        KVMHostVO hostVO = Q.New(KVMHostVO.class)
                .eq(KVMHostVO_.uuid, host.uuid)
                .find()
        assert hostVO != null : "AC-RS-04-A 失败: KVMHostVO[uuid=${host.uuid}] 未落库"

        // PhysicalServerRoleVO created — roleUuid == hostVO.uuid (ADR-012)
        PhysicalServerRoleVO roleVO = Q.New(PhysicalServerRoleVO.class)
                .eq(PhysicalServerRoleVO_.serverUuid, ps.uuid)
                .eq(PhysicalServerRoleVO_.roleType, "KVM_HOST")
                .find()
        assert roleVO != null : "AC-RS-04-A 失败: path 2 后 PhysicalServerRoleVO 未落库 — 3 Flow 没跑"
        assert roleVO.roleUuid == host.uuid :
                "AC-RS-04-A 失败: roleVO.roleUuid (${roleVO.roleUuid}) 应等于 host.uuid (${host.uuid}) per ADR-012"

        // PhysicalServerCapacityVO row exists at uuid == ps.uuid
        PhysicalServerCapacityVO psc = Q.New(PhysicalServerCapacityVO.class)
                .eq(PhysicalServerCapacityVO_.uuid, ps.uuid)
                .find()
        assert psc != null : "AC-RS-04-A 失败: PSC row 缺 — InitPhysicalServerCapacityFlow 没跑"

        // PS still exists (AutoAssociate Flow 是 NoRollbackFlow，但本 case 不 rollback)
        PhysicalServerVO psVO = Q.New(PhysicalServerVO.class)
                .eq(PhysicalServerAO_.uuid, ps.uuid)
                .find()
        assert psVO != null : "AC-RS-04-A 失败: PhysicalServerVO 不存在"

        // Cleanup — detach role, then delete host, ps, pool
        detachPhysicalServerRole { delegate.serverUuid = ps.uuid; roleType = "KVM_HOST" }
        deleteHost { uuid = host.uuid }
        deletePhysicalServer { uuid = ps.uuid }
        deleteServerPool { uuid = pool.uuid }
    }

    // ----------------------------------------------------------------
    // AC-RS-04-B — null serverUuid → AutoAssociate tier-3 (managementIp + zone) creates new PS
    // ----------------------------------------------------------------

    void testPathTwoAutoAssociateTier3() {
        def zone = env.inventoryByName("zone") as ZoneInventory
        def cluster = env.inventoryByName("cluster") as ClusterInventory

        // Bind a pool to the cluster so AutoAssociator's auto-create path is enabled
        def pool = createServerPool {
            name = "pool-u1a-auto"
            delegate.zoneUuid = zone.uuid
        } as ServerPoolInventory

        changeClusterServerPool {
            delegate.clusterUuid = cluster.uuid
            delegate.serverPoolUuid = pool.uuid
        }

        long psCountBefore = Q.New(PhysicalServerVO.class)
                .eq(PhysicalServerAO_.zoneUuid, zone.uuid)
                .count()

        def host = addKVMHost {
            name = "host-u1a-auto"
            managementIp = "127.0.0.31"
            clusterUuid = cluster.uuid
            username = "root"
            password = "password"
            // serverUuid intentionally null → triggers AutoAssociate tier-3
        } as HostInventory

        // PS auto-created (tier-3 fallback by managementIp + zoneUuid)
        long psCountAfter = Q.New(PhysicalServerVO.class)
                .eq(PhysicalServerAO_.zoneUuid, zone.uuid)
                .count()
        assert psCountAfter == psCountBefore + 1 :
                "AC-RS-04-B 失败: AutoAssociateFlow 应创建 1 个新 PS，before=${psCountBefore} after=${psCountAfter}"

        PhysicalServerVO autoCreatedPs = Q.New(PhysicalServerVO.class)
                .eq(PhysicalServerAO_.managementIp, "127.0.0.31")
                .eq(PhysicalServerAO_.zoneUuid, zone.uuid)
                .find()
        assert autoCreatedPs != null : "AC-RS-04-B 失败: 新 PS 未在 managementIp/zone 下找到"
        assert autoCreatedPs.poolUuid == pool.uuid : "AC-RS-04-B 失败: 新 PS 应绑到 cluster 的 pool"

        // RoleVO + PSC linked to the auto-created PS
        PhysicalServerRoleVO roleVO = Q.New(PhysicalServerRoleVO.class)
                .eq(PhysicalServerRoleVO_.serverUuid, autoCreatedPs.uuid)
                .eq(PhysicalServerRoleVO_.roleType, "KVM_HOST")
                .find()
        assert roleVO != null : "AC-RS-04-B 失败: 新 PS 没 RoleVO"
        assert roleVO.roleUuid == host.uuid : "AC-RS-04-B 失败: roleVO.roleUuid 应 == host.uuid"

        PhysicalServerCapacityVO psc = Q.New(PhysicalServerCapacityVO.class)
                .eq(PhysicalServerCapacityVO_.uuid, autoCreatedPs.uuid)
                .find()
        assert psc != null : "AC-RS-04-B 失败: PSC row 缺"

        // Cleanup
        detachPhysicalServerRole { delegate.serverUuid = autoCreatedPs.uuid; roleType = "KVM_HOST" }
        deleteHost { uuid = host.uuid }
        deletePhysicalServer { uuid = autoCreatedPs.uuid }
        deleteServerPool { uuid = pool.uuid }
    }
}
