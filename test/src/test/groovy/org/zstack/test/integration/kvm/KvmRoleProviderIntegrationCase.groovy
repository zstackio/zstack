package org.zstack.test.integration.kvm

import org.zstack.core.db.Q
import org.zstack.header.server.PhysicalServerRoleVO
import org.zstack.header.server.PhysicalServerRoleVO_
import org.zstack.kvm.KVMHostVO
import org.zstack.kvm.KVMHostVO_
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.PhysicalServerInventory
import org.zstack.sdk.PhysicalServerRoleInventory
import org.zstack.sdk.ServerPoolInventory
import org.zstack.sdk.ZoneInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.kvm.host.HostEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import static org.zstack.kvm.KVMConstant.KVM_HOST_FACT_PATH

/**
 * Phase 2D 首件集成 harness — KvmRoleProvider 端到端原子性验证。
 *
 * 覆盖 5 个 AC：
 *   AC-1  RoleVO + HostVO 原子持久化（同事务）
 *   AC-2  attachRoleVO 锁互斥（重复 attach 被拒 + 并发 UNIQUE 约束）
 *   AC-3  @Transactional 回滚（roleConfig 缺字段 → 无残留）
 *   AC-4  detach 幂等（第二次 detach 为 no-op，不抛异常）
 *   AC-5  删除 KVM Host 级联清理 PhysicalServerRoleVO
 */
class KvmRoleProviderIntegrationCase extends SubCase {
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
            // Enable simulator path so AddKVMHostMsg walk-through succeeds
            env.afterSimulator(KVM_HOST_FACT_PATH) { rsp -> rsp }

            testAc1AtomicPersistence()
            testAc2DuplicateAttachRejected()
            testAc2ConcurrentAttachUniqueConstraint()
            testAc3RollbackMissingPassword()
            testAc3RollbackMissingUsername()
            testAc3RollbackInvalidSshPort()
            testAc4DetachIdempotent()
            testAc5DeleteHostCascadesRoleVo()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private ServerPoolInventory createPool(String suffix, String zUuid) {
        return createServerPool {
            name = "pool-kvm-${suffix}"
            delegate.zoneUuid = zUuid
        } as ServerPoolInventory
    }

    private PhysicalServerInventory createServer(String suffix, String zUuid, String pUuid, String ip) {
        return createPhysicalServer {
            name = "server-kvm-${suffix}"
            delegate.zoneUuid = zUuid
            delegate.poolUuid = pUuid
            managementIp = ip
        } as PhysicalServerInventory
    }

    // ----------------------------------------------------------------
    // AC-1: RoleVO + HostVO 原子持久化
    // ----------------------------------------------------------------

    void testAc1AtomicPersistence() {
        def zone = env.inventoryByName("zone") as ZoneInventory
        def cluster = env.inventoryByName("cluster") as ClusterInventory
        def pool = createPool("ac1", zone.uuid)
        def server = createServer("ac1", zone.uuid, pool.uuid, "127.0.0.10")

        long kvmCountBefore = Q.New(KVMHostVO.class).count()

        def role = attachPhysicalServerRole {
            serverUuid = server.uuid
            roleType = "KVM_HOST"
            clusterUuid = cluster.uuid
            roleConfig = [username: "root", password: "password", sshPort: "22"]
        } as PhysicalServerRoleInventory

        // RoleVO must be persisted
        PhysicalServerRoleVO roleVO = Q.New(PhysicalServerRoleVO.class)
                .eq(PhysicalServerRoleVO_.serverUuid, server.uuid)
                .eq(PhysicalServerRoleVO_.roleType, "KVM_HOST")
                .find()
        assert roleVO != null : "AC-1 失败: attach KVM_HOST 后 PhysicalServerRoleVO 未落库，serverUuid=${server.uuid}"
        assert roleVO.roleUuid != null : "AC-1 失败: PhysicalServerRoleVO.roleUuid 为 null，serverUuid=${server.uuid}"

        // KVMHostVO must be created and its uuid must equal roleVO.roleUuid
        KVMHostVO hostVO = Q.New(KVMHostVO.class)
                .eq(KVMHostVO_.uuid, roleVO.roleUuid)
                .find()
        assert hostVO != null : "AC-1 失败: KVMHostVO[uuid=${roleVO.roleUuid}] 未落库，roleVO.roleUuid 应等于 KVMHostVO.uuid"

        // KVMHostVO count increased by 1 — structural proxy for "initialization
        // relatively simultaneous". HostAO / PhysicalServerRoleVO have no
        // @PrePersist for createDate so a timestamp-window assertion is unreliable.
        long kvmCountAfter = Q.New(KVMHostVO.class).count()
        assert kvmCountAfter == kvmCountBefore + 1 : "AC-1 失败: KVMHostVO 数量未增加 1，before=${kvmCountBefore} after=${kvmCountAfter}"

        // SDK inventory fields
        assert role.uuid != null : "AC-1 失败: role.uuid 为 null"
        assert role.serverUuid == server.uuid : "AC-1 失败: role.serverUuid 不等于 server.uuid"
        assert role.roleType == "KVM_HOST" : "AC-1 失败: role.roleType 不是 KVM_HOST，actual=${role.roleType}"

        // Cleanup
        detachPhysicalServerRole { serverUuid = server.uuid; roleType = "KVM_HOST" }
        deletePhysicalServer { uuid = server.uuid }
        deleteServerPool { uuid = pool.uuid }
    }

    // ----------------------------------------------------------------
    // AC-2: 重复 attach 同 roleType 被拒绝（顺序场景）
    // ----------------------------------------------------------------

    void testAc2DuplicateAttachRejected() {
        def zone = env.inventoryByName("zone") as ZoneInventory
        def cluster = env.inventoryByName("cluster") as ClusterInventory
        def pool = createPool("ac2-seq", zone.uuid)
        def server = createServer("ac2-seq", zone.uuid, pool.uuid, "127.0.0.11")

        // 第一次 attach 成功
        attachPhysicalServerRole {
            serverUuid = server.uuid
            roleType = "KVM_HOST"
            clusterUuid = cluster.uuid
            roleConfig = [username: "root", password: "password", sshPort: "22"]
        }

        // 第二次 attach 同 server 同 roleType 必须被拒
        expect(AssertionError.class) {
            attachPhysicalServerRole {
                serverUuid = server.uuid
                roleType = "KVM_HOST"
                clusterUuid = cluster.uuid
                roleConfig = [username: "root", password: "password", sshPort: "22"]
            }
        }

        // RoleVO 计数必须 == 1，证明第二次没漏写
        long cnt = Q.New(PhysicalServerRoleVO.class)
                .eq(PhysicalServerRoleVO_.serverUuid, server.uuid)
                .eq(PhysicalServerRoleVO_.roleType, "KVM_HOST")
                .count()
        assert cnt == 1 : "AC-2 失败: 重复 attach 后 RoleVO 计数应为 1，actual=${cnt}"

        // Cleanup
        detachPhysicalServerRole { serverUuid = server.uuid; roleType = "KVM_HOST" }
        deletePhysicalServer { uuid = server.uuid }
        deleteServerPool { uuid = pool.uuid }
    }

    // ----------------------------------------------------------------
    // AC-2: 并发 attach 同 server — UNIQUE 约束 / DB 锁确保最终只有 1 条 RoleVO
    // ----------------------------------------------------------------

    void testAc2ConcurrentAttachUniqueConstraint() {
        def zone = env.inventoryByName("zone") as ZoneInventory
        def cluster = env.inventoryByName("cluster") as ClusterInventory
        def pool = createPool("ac2-conc", zone.uuid)
        def server = createServer("ac2-conc", zone.uuid, pool.uuid, "127.0.0.12")

        def errors = Collections.synchronizedList([])

        // 两个线程同时 attach KVM_HOST，lockPhysicalServerForAttach（PESSIMISTIC_WRITE）
        // 会序列化执行；其中一个将因 UNIQUE(serverUuid, roleType) 约束或先到者已写 RoleVO
        // 的互斥检查而失败。最终只有 1 条 RoleVO 落库。
        def t1 = Thread.start {
            try {
                attachPhysicalServerRole {
                    serverUuid = server.uuid
                    roleType = "KVM_HOST"
                    clusterUuid = cluster.uuid
                    roleConfig = [username: "root", password: "password", sshPort: "22"]
                }
            } catch (Throwable e) {
                errors.add(e)
            }
        }

        def t2 = Thread.start {
            try {
                attachPhysicalServerRole {
                    serverUuid = server.uuid
                    roleType = "KVM_HOST"
                    clusterUuid = cluster.uuid
                    roleConfig = [username: "root", password: "password", sshPort: "22"]
                }
            } catch (Throwable e) {
                errors.add(e)
            }
        }

        t1.join()
        t2.join()

        // 至少一个线程成功（否则是测试环境问题，而非锁失效）
        assert errors.size() <= 1 : "AC-2 并发失败: 两个线程都抛异常，测试环境异常，errors=${errors*.message}"

        // 恰好一个成功，一个失败（或两者都以异常结束，但 RoleVO 只有 1 条）
        long cnt = Q.New(PhysicalServerRoleVO.class)
                .eq(PhysicalServerRoleVO_.serverUuid, server.uuid)
                .eq(PhysicalServerRoleVO_.roleType, "KVM_HOST")
                .count()
        assert cnt == 1 : "AC-2 并发失败: 两个并发 attach 后 RoleVO 计数应 == 1，actual=${cnt}，errors=${errors.size()}"

        // Cleanup
        detachPhysicalServerRole { serverUuid = server.uuid; roleType = "KVM_HOST" }
        deletePhysicalServer { uuid = server.uuid }
        deleteServerPool { uuid = pool.uuid }
    }

    // ----------------------------------------------------------------
    // AC-3: @Transactional 回滚 — roleConfig 缺 password
    // ----------------------------------------------------------------

    void testAc3RollbackMissingPassword() {
        def zone = env.inventoryByName("zone") as ZoneInventory
        def cluster = env.inventoryByName("cluster") as ClusterInventory
        def pool = createPool("ac3-pw", zone.uuid)
        def server = createServer("ac3-pw", zone.uuid, pool.uuid, "127.0.0.20")

        long kvmCountBefore = Q.New(KVMHostVO.class).count()

        // KvmRoleProvider.createRoleEntity 第 138 行抛 OperationFailureException(ORG_ZSTACK_KVM_10163)
        expect(AssertionError.class) {
            attachPhysicalServerRole {
                serverUuid = server.uuid
                roleType = "KVM_HOST"
                clusterUuid = cluster.uuid
                roleConfig = [username: "root"]   // 缺 password
            }
        }

        // PhysicalServerRoleVO 不得有残留
        long roleCnt = Q.New(PhysicalServerRoleVO.class)
                .eq(PhysicalServerRoleVO_.serverUuid, server.uuid)
                .count()
        assert roleCnt == 0 : "@Transactional 回滚失败: roleConfig 缺 password 后 RoleVO 残留，actual=${roleCnt}"

        // KVMHostVO 不得有残留
        long kvmCountAfter = Q.New(KVMHostVO.class).count()
        assert kvmCountAfter == kvmCountBefore : "@Transactional 回滚失败: roleConfig 缺 password 后 KVMHostVO 残留，before=${kvmCountBefore} after=${kvmCountAfter}"

        // Cleanup (no role to detach)
        deletePhysicalServer { uuid = server.uuid }
        deleteServerPool { uuid = pool.uuid }
    }

    // ----------------------------------------------------------------
    // AC-3: @Transactional 回滚 — roleConfig 缺 username
    // ----------------------------------------------------------------

    void testAc3RollbackMissingUsername() {
        def zone = env.inventoryByName("zone") as ZoneInventory
        def cluster = env.inventoryByName("cluster") as ClusterInventory
        def pool = createPool("ac3-user", zone.uuid)
        def server = createServer("ac3-user", zone.uuid, pool.uuid, "127.0.0.21")

        long kvmCountBefore = Q.New(KVMHostVO.class).count()

        // KvmRoleProvider.createRoleEntity 抛 OperationFailureException(ORG_ZSTACK_KVM_10165)
        expect(AssertionError.class) {
            attachPhysicalServerRole {
                serverUuid = server.uuid
                roleType = "KVM_HOST"
                clusterUuid = cluster.uuid
                roleConfig = [password: "password"]   // 缺 username
            }
        }

        long roleCnt = Q.New(PhysicalServerRoleVO.class)
                .eq(PhysicalServerRoleVO_.serverUuid, server.uuid)
                .count()
        assert roleCnt == 0 : "@Transactional 回滚失败: roleConfig 缺 username 后 RoleVO 残留，actual=${roleCnt}"

        long kvmCountAfter = Q.New(KVMHostVO.class).count()
        assert kvmCountAfter == kvmCountBefore : "@Transactional 回滚失败: roleConfig 缺 username 后 KVMHostVO 残留，before=${kvmCountBefore} after=${kvmCountAfter}"

        deletePhysicalServer { uuid = server.uuid }
        deleteServerPool { uuid = pool.uuid }
    }

    // ----------------------------------------------------------------
    // AC-3: @Transactional 回滚 — roleConfig sshPort 非整数
    // ----------------------------------------------------------------

    void testAc3RollbackInvalidSshPort() {
        def zone = env.inventoryByName("zone") as ZoneInventory
        def cluster = env.inventoryByName("cluster") as ClusterInventory
        def pool = createPool("ac3-port", zone.uuid)
        def server = createServer("ac3-port", zone.uuid, pool.uuid, "127.0.0.22")

        long kvmCountBefore = Q.New(KVMHostVO.class).count()

        // KvmRoleProvider.createRoleEntity 抛 OperationFailureException(ORG_ZSTACK_KVM_10164)
        expect(AssertionError.class) {
            attachPhysicalServerRole {
                serverUuid = server.uuid
                roleType = "KVM_HOST"
                clusterUuid = cluster.uuid
                roleConfig = [username: "root", password: "password", sshPort: "abc"]
            }
        }

        long roleCnt = Q.New(PhysicalServerRoleVO.class)
                .eq(PhysicalServerRoleVO_.serverUuid, server.uuid)
                .count()
        assert roleCnt == 0 : "@Transactional 回滚失败: sshPort 非整数后 RoleVO 残留，actual=${roleCnt}"

        long kvmCountAfter = Q.New(KVMHostVO.class).count()
        assert kvmCountAfter == kvmCountBefore : "@Transactional 回滚失败: sshPort 非整数后 KVMHostVO 残留，before=${kvmCountBefore} after=${kvmCountAfter}"

        deletePhysicalServer { uuid = server.uuid }
        deleteServerPool { uuid = pool.uuid }
    }

    // ----------------------------------------------------------------
    // AC-4: detach 幂等
    // ----------------------------------------------------------------

    void testAc4DetachIdempotent() {
        def zone = env.inventoryByName("zone") as ZoneInventory
        def cluster = env.inventoryByName("cluster") as ClusterInventory
        def pool = createPool("ac4", zone.uuid)
        def server = createServer("ac4", zone.uuid, pool.uuid, "127.0.0.30")

        attachPhysicalServerRole {
            serverUuid = server.uuid
            roleType = "KVM_HOST"
            clusterUuid = cluster.uuid
            roleConfig = [username: "root", password: "password", sshPort: "22"]
        }

        // 第一次 detach — 成功，RoleVO 被删除
        detachPhysicalServerRole {
            serverUuid = server.uuid
            roleType = "KVM_HOST"
        }

        long cntAfterFirst = Q.New(PhysicalServerRoleVO.class)
                .eq(PhysicalServerRoleVO_.serverUuid, server.uuid)
                .count()
        assert cntAfterFirst == 0 : "AC-4 失败: 第一次 detach 后 RoleVO 仍存在，count=${cntAfterFirst}"

        // 第二次 detach — PhysicalServerManagerImpl 对不存在的 role 返回 success (no-op，见 handle 第 476 行)
        // 不应抛异常
        detachPhysicalServerRole {
            serverUuid = server.uuid
            roleType = "KVM_HOST"
        }

        long cntAfterSecond = Q.New(PhysicalServerRoleVO.class)
                .eq(PhysicalServerRoleVO_.serverUuid, server.uuid)
                .count()
        assert cntAfterSecond == 0 : "AC-4 失败: 第二次 detach 后 RoleVO 不应为非 0，actual=${cntAfterSecond}"

        // Cleanup
        deletePhysicalServer { uuid = server.uuid }
        deleteServerPool { uuid = pool.uuid }
    }

    // ----------------------------------------------------------------
    // AC-5: 删除 KVM Host 级联清理 PhysicalServerRoleVO
    // ----------------------------------------------------------------

    void testAc5DeleteHostCascadesRoleVo() {
        def zone = env.inventoryByName("zone") as ZoneInventory
        def cluster = env.inventoryByName("cluster") as ClusterInventory
        def pool = createPool("ac5", zone.uuid)
        def server = createServer("ac5", zone.uuid, pool.uuid, "127.0.0.31")

        attachPhysicalServerRole {
            serverUuid = server.uuid
            roleType = "KVM_HOST"
            clusterUuid = cluster.uuid
            roleConfig = [username: "root", password: "password", sshPort: "22"]
        }

        PhysicalServerRoleVO roleVO = Q.New(PhysicalServerRoleVO.class)
                .eq(PhysicalServerRoleVO_.serverUuid, server.uuid)
                .eq(PhysicalServerRoleVO_.roleType, "KVM_HOST")
                .find()
        assert roleVO != null : "AC-5 失败: attach KVM_HOST 后 RoleVO 未落库，serverUuid=${server.uuid}"
        String hostUuid = roleVO.roleUuid
        assert Q.New(KVMHostVO.class).eq(KVMHostVO_.uuid, hostUuid).count() == 1L :
                "AC-5 失败: KVMHostVO[uuid=${hostUuid}] 未落库"

        deleteHost { uuid = hostUuid }

        long residualRoleCount = Q.New(PhysicalServerRoleVO.class)
                .eq(PhysicalServerRoleVO_.serverUuid, server.uuid)
                .eq(PhysicalServerRoleVO_.roleType, "KVM_HOST")
                .count()
        assert residualRoleCount == 0L :
                "AC-5 失败: 删除 KVM Host 后 PhysicalServerRoleVO 仍残留，serverUuid=${server.uuid}"

        long residualHostCount = Q.New(KVMHostVO.class)
                .eq(KVMHostVO_.uuid, hostUuid)
                .count()
        assert residualHostCount == 0L :
                "AC-5 失败: deleteHost 后 KVMHostVO 仍残留，hostUuid=${hostUuid}"

        deletePhysicalServer { uuid = server.uuid }
        deleteServerPool { uuid = pool.uuid }
    }
}
