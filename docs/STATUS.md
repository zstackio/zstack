# v5.5.18 Unified Hardware Management — 全局状态 (Project Status)

> **每个 session 进来先读这一份**。它告诉你：feature 整体在哪一步、source of truth 在哪、本 session 该读什么。
>
> 跟 `docs/brainstorms/next-session.md` 的区别：next-session 是"上一轮 diff"，本文件是"全局静态视野"。session 切换时只更新 next-session；阶段里程碑撞线时同步更新本文件。

**Last updated**: 2026-05-09 (PSC writer collapse Layer 1+2 hot-deployed on 172.26.201.160；7 NativeHost PSC.totalCpu sync 后从 0 → K8s 真值 8/8/8/16/120/192/192 cores，KVM host PSC.availableCpu=72=80-cpuBuffer，Layer 2 recalculate 唯一虚拟量入口 production-validated)
**Current phase**: Phase 3 validation/polish（业务逻辑代码基本写完；test infra rot 阻 IT 端到端；commit/push 待）
**Branch**: `feature/unifi-host-dev` (latest pushed; use `git rev-parse --short HEAD` for the exact local commit)
**PRD pin**: cloud_prd commit `f9928ec` (NB-1..34 final consolidation)

> **2026-05-05 update**: 直接 grep 代码而非 STATUS.md，发现 §5 ❌ 三项「完全缺失」**全是 stale 文档**：(1) 路径 2 FlowChain 接入实际在 `HostManagerImpl.java:37,426` + `BareMetal2ChassisManagerImpl.java:69-70,128-141,458` + `ContainerEndpointBase.java:706,1146`；(2) Container Pod 聚合在 `ContainerRoleProvider.java:96-117`（SUM cpu/memory FROM PodVO state=Running）；(3) Hardware discover AC-CB-18 在 `PhysicalServerManagerImpl.java:573,916` + `PhysicalServerEnqueueDiscoveryHookImpl`. ❌ 区清空，移到 ✅。Gateway-agent ping production-path wiring 落地：`Bm2GatewayPingHelper` 改 `bus.send(PingTargetInGatewayMsg)` 走 gateway agent，撤回之前从 MN 直跑 ping 的 v1.1+ 妥协。
>
> **2026-05-03 update**: LongJob stage-based phase 持久化在 jobData，MN 重启 resume 不重触发 PXE。Gateway-agent ping helper 实装；timeout default 1800s。撤回 2026-05-02 PRD 修正中『OS install 完成监听 deferred』激进措辞——本 phase 已 cover。
>
> **2026-05-02 update**: GATEWAY_PXE data-plane wiring complete — Bm2GatewayDataPlane 实装 implementing PhysicalServerProvisionDataPlane, calls existing PrepareProvisionNetworkInGatewayMsg agent flow without requiring BM2 Gateway as装机 precondition. PhysicalServerIpmiPowerExecutor 加 powerOnPxe (chassis bootdev pxe + power reset). ProvisionPhysicalServerBm2Case now exercises real agent dispatch instead of stub no-op success. Fire-and-forget装机：success = network prepared + BMC PXE boot triggered；OS install monitoring deferred to physical-server-pxe-real-env-validation.md runbook.
>
> **2026-05-01 checkpoint**: RoleProvider PRD integration acceptance coverage is >=95% under the current IT scope: KVM 5/5 AC, BM2 8/8 AC, Container 7/7 AC, total 20/20 AC GREEN. Power API AC-CB-14/15/16 is GREEN via `PhysicalServerPowerCase`, with BM2 fallback regression covered by `PowerAndDiscoverPhysicalServerCase`. Cordon AC-CM-14/15/16 is GREEN via `ContainerNodeCordonServiceCase`. ScanPhysicalServers is GREEN via `PhysicalServerOpsCase` after a clean woven reactor build. **ProvisionProvider focused harness is now GREEN per PhysicalServer-first contract (Tasks 1–4)**: `PhysicalServerProvisionTarget` / `PhysicalServerProvisionService` / `PhysicalServerGatewayPxeProvisionProvider` all ship no BM2 Gateway/Instance dependency; `ProvisionPhysicalServerBm2Case` premium harness 1/1 GREEN (no gateway fixture); `TestPhysicalServerProvisionService` OSS unit harness 10/10 GREEN. Real PXE installation data-plane (Task 6) and broader CI/nightly (Task 8) still pending. This is functional AC coverage on contract layer, not JaCoCo line coverage; the IT/unit runs used `-DskipJacoco=true` and the worktree-local repo `-Dmaven.repo.local=/home/mj/zstack-workspace/zstack-unifi-host/.m2/repository`.

---

## 1. Feature 一句话

把 ZStack 的 KVM Host / BareMetal2 Chassis / Container NativeHost 三类硬件抽象成统一的 PhysicalServer 模型，引入 RoleProvider SPI，把容量管理 / 自动关联 / 硬件发现 / 电源管理 / 角色生命周期统一到一个真表 + 一套 SPI。

**Out of scope**:
- BM1 (legacy baremetal) 退场不迁移（ADR-010）
- vcenter ESXi 半迁移（option C，仅共享 capacity 真表，不入统一 PS 模型，NB-25）
- ServerAllocator R2 Group C → 推 v5.5.18.x

---

## 2. Source of truth

**5 份 PRD** 在外部仓 `/home/mj/zstack-workspace/cloud_prd/prd/v5.5.18-unified-hardware/`（pin: `f9928ec`）:

| PRD | 主题 | FR / AC 范围 |
|---|---|---|
| `server/feat-physical_server_model_prd.md` | PhysicalServer 模型 + RoleVO | FR-001..012 |
| `capacity/feat-unified_capacity_management_prd.md` | 容量真表 + 分配引擎 | FR-013..021, AC-CM-*, AC-AL-* |
| `server/feat-role_spi_adapter_prd.md` | RoleProvider SPI v3 + 4 角色适配 | FR-022..027, AC-RS-* |
| `provision/feat-unified_provision_network_prd.md` | ProvisionNetwork 统一 + ProvisionProvider SPI | FR-009..012 子集 |
| `compat/feat-legacy_migration_and_unified_infra_prd.md` | 存量迁移 + 统一查询/电源/硬件发现 | FR-030..033, AC-CB-* |

**11 份 ADR** 在 `docs/decisions/`（详见 [README](decisions/README.md)）— 决策定型，不再重议。

**3 份 runbook** 在 `docs/runbooks/`：
- `v5518-sql-ddl-pitfalls.md` — DDL 反模式
- `v5518-unified-hardware-rollback.md` — 升级回滚预案
- `testing-envs.md` — 测试环境连接信息

---

## 3. Phase progression

```
Phase 1 (v5.5.18 内部) — 骨架               [DONE — 2026-04 中]
   ├── Tasks 1-11: VO/CRUD/ServerPool/ProvisionNetwork/KvmRoleProvider stub/tests
   └── deliverable: PhysicalServer*VO 全家族 + RoleProvider SPI 接口 + 三家 stub implements

Phase 2 (v5.5.18 内部) — 容量+分配+迁移+角色补全 [DONE 主体 — 2026-04-27]
   ├── 2D 收尾: KVM/Container/BM2 三 RoleProviderIntegrationCase 全绿
   ├── PRD audit: 72 AC checked, 21 ❌ + 13 ⚠️ + 6 🔁 + 3 🅿
   ├── ADR-013: BM2 ClusterRef 撤回 VIEW 化
   └── deliverable: HostCapacityVO VIEW + 三家 RoleProvider wire 真实 + Attach/Detach API

Phase 3 — fix audit gaps                     [READY TO START]
   ├── 22 critical-gap U-unit 待起草
   ├── Wave 1 P0 unblock (并行 6 unit)
   ├── Wave 2 Cordon stack (3 unit, depends Wave 1)
   ├── Wave 3 P1 一致性 (7 unit)
   └── Wave 4 性能验证 + PRD 上游改写

Phase 3+ (v1.1+) — Backlog                    [NOT PLANNED]
   ├── ServerAllocatorChain (R2 Group C)
   ├── Cross-role serialNumber 归一化 (AC-RS-13-P2)
   ├── HardwareDiscoveryStrategy SPI (现 3 private method)
   └── ProvisionAndAttachRole orchestrator API
```

---

## 4. Per-phase deliverables (links + status)

### Phase 2 master plan + audit
| Doc | 状态 |
|---|---|
| [docs/plans/2026-04-22-001-feat-v5518-unified-hardware-phase2-plan.md](plans/2026-04-22-001-feat-v5518-unified-hardware-phase2-plan.md) | Phase 2 master, R1-R12 + U1-U31, 91.5K（U-unit checkbox 全 unchecked，进度按 audit 反推见 §4.1 / §4.2） |
| [docs/plans/2026-04-23-001-u28-flyway-data-migration.md](plans/2026-04-23-001-u28-flyway-data-migration.md) | U28 Flyway 子计划（schema + data migration）|
| [docs/plans/2026-04-27-001-feat-v5518-phase2-prd-audit-plan.md](plans/2026-04-27-001-feat-v5518-phase2-prd-audit-plan.md) | PRD audit plan, lean rewrite (Q1=C/Q2/Q3=B) |
| [docs/audits/2026-04-27-phase2-prd-audit.md](audits/2026-04-27-phase2-prd-audit.md) | **Phase 2 audit report — 72 AC + Phase 3 fix-plan 骨架** |

### 4.1 Phase 2 R-unit progress（按 audit 反推）

R-unit 来自 `2026-04-22-001-...-phase2-plan.md` §Requirement-level groups。状态字段是本 audit 的 roll-up。

| R# | 主题 | 状态 | 备注 |
|---|---|---|---|
| R1 | AC-V2-CAP-01..12 + AC-CM-PERF-01 — Unified capacity ledger (PSC 真表 + HCV VIEW MERGE + W1-W9 + @Immutable) | ✅ DONE | U1+U4+U5+U6+U7+U27 全 ✅。AC-CM-PERF-01 EXPLAIN 验证留 Phase 3 性能测试 |
| R2 | AC-V2-ALLOC-01..07 — ServerAllocatorChain (7 Flows + 2 ExtensionPoint) | 🔁 DEFERRED | Group C 推 v5.5.18.x（plan §Scope Boundaries 明示）|
| R3 | AC-CM-13..19 — Mixed-deployment Cordon + Pod 聚合 | ✅ DONE | Pod 聚合 ✅（`ContainerRoleProvider.getCapacityConsumption` SUM PodVO state=Running）；AC-CM-13 reservation extension ✅（`ContainerCordonReservedCapacityExtension` 把 `isHostCordoned` host 的 free 转 reserved）；2026-05-09 production triggers 落地（plan: [docs/plans/2026-05-09-001-cordon-production-trigger-plan.md](plans/2026-05-09-001-cordon-production-trigger-plan.md)）：(1) K8s 反向 mirror `cordonService.mirrorFromK8s` 在 `ContainerEndpointBase.processNodeTransactional` 里调，把 `KubernetesNodeInventory.unschedulable` 写进 in-memory `cordonedHostUuids`，operator 手动 cordon 实时可见；(2) capacity-driven hysteresis `cordonService.evaluate` 在 `ContainerEndpointBase.success()` recalculate 之后调，free<buffer 触发 ZStack 主动 cordon、free>2×buffer 触发 uncordon（仅 zstack 标签存在时）；(3) buffer 计算抽到 `PhysicalServerCapacityBuffers.calc{Cpu,Mem}Buffer` 静态 helper，跨 recalculate + evaluate 统一口径 |
| R4 | AC-V2-ROLE-01..09 — RoleProvider wire-up (KVM/BM2/Container) | ✅ DONE | U8/U9 path 1+2 ✅；U10 Container Layer 1 `syncNodesFromCluster` 写 `PSC.total{Cpu,Memory}`、Layer 2 调 `PhysicalServerCapacityUpdater.recalculate` 派生 `available*`；2026-05-09 真机 7 NativeHost PSC.totalCpu 0 → 8/8/8/16/120/192/192，KVM host availableCpu 80→72（减 cpuBuffer 8） |
| R5 | Server PRD §2.5.1 — AddHost/AddChassis FlowChain tail extension (3 Flow + post-commit hook) | ❌ NOT STARTED | U11/U12/U13 全缺。这是 Phase 3 Wave 1 U1 的核心 |
| R6 | FR-033 + NB-19 — PhysicalServerHardwareService (3 private discover + Scheduler) | ⚠️ PARTIAL | U2 ✅ skeleton + GlobalConfig；U16 ✅ Scheduler；U15 ❌ 3 discover 全 stub；U17 ❌ handler 未接 |
| R7 | FR-010..012 + NB-4 — PoolRef + BM2 ProvisionNetwork VIEW | ⚠️ MIXED | U3 ✅ PoolRef + Attach/Detach API；U23/U24 在 ADR-013 撤回 VIEW 化后变成 N/A，pool-only 重写推 v1.1+ |
| R8 | FR-012 + provision PRD §2.3 — ProvisionProvider SPI (PhysicalServer-first PXE) | ✅ DONE | contract + GATEWAY_PXE data-plane stage-based GREEN；OS install monitoring via gateway-agent ping (B-L2) GREEN；自动 attach Host 仍 v1.1+；2026-05-05 production-deployed on 172.26.201.160 with PhysicalServer add-host API end-to-end GREEN |
| R9 | FR-030 + AC-CB-ROLLBACK-01..03 — Idempotent migration script | ✅ DONE (with 🅿) | U28 schema migration ✅；ROLLBACK-01..03 标 🅿 PRD-stale per ADR-007 不进 fix list |
| R10 | FR-032 + NB-10 — Unified power API IPMI-only | ✅ DONE | AC-CB-14/15/16 GREEN：OOB-first direct IPMI + no-OOB error + BM2 legacy fallback regression |
| R11 | NB-15 admin-only — `@Action(adminOnly=true)` on 24 PS API Msgs | ✅ DONE | U30 ✅。audit AC-CB-NB15-AdminAction 全过 |
| R12 | NB-23 + NB-20 — `roleConfig: @NoLogging` + `credentials: @NoLogging` | ✅ DONE | Phase 1 已落，Phase 2 verify 即过 |

**Roll-up**: R1/R3/R4/R8/R9/R10/R11/R12 ✅ · R6/R7 ⚠️ · R5 ❌ · R2 🔁

### 4.2 Phase 2 U-unit status

U-unit 来自 phase2 plan §Implementation Units。本 audit 反推：

| 区段 | 范围 | 状态 |
|---|---|---|
| **U1-U7** capacity ledger + W1-W9 + @Immutable | U1 PSC entity / U2 Hardware skeleton + Scheduler + GlobalConfig / U3 PoolRef + Attach/Detach API / U4 W1-W3 / U5 W4-W6 / U6 W9 vcenter / U7 @Immutable | 全 ✅ |
| **U8-U10** RoleProvider wire-up | U8 KVM / U9 BM2 / U10 Container | ✅ path 1+2 全通；U10 Container 容量管道 Layer 1 (`syncNodesFromCluster` 写 PSC.total*) + Layer 2 (`PhysicalServerCapacityUpdater.recalculate` 派生 available*) production-validated 2026-05-09 |
| **U11-U13** FlowChain tail | U11 KVM / U12 BM2 / U13 Container per-node @Transactional | ❌ 全部未起步 — Phase 3 Wave 1 U1 |
| **U14-U17** Hardware discovery | U14 K8s NodeInventory 字段 / U15 3 private discover / U16 Scheduler retry / U17 handler | U16 ✅；U14/U15/U17 ❌ |
| **U18-U20** ProvisionProvider SPI | U18 SPI / U19 PhysicalServer-first PXE provider / U20 LongJob | ✅ DONE | stage-based + ping monitoring GREEN，phase tracked in LongJobVO.jobData (no schema change)；2026-05-05 production-validated on 172.26.201.160 (CreatePhysicalServer + AttachPhysicalServerRole(KVM_HOST) → RoleVO + HostVO/KVMHostVO + HostCapacityVO + PhysicalServerCapacityVO 全建) |
| **U21-U22** Container Cordon | U21 ContainerNodeCordonService / U22 recalculate Cordon 集成 | ⚠️ U21 GREEN；capacity reserved extension exists，broader mixed-deployment still separate |
| **U23-U24** BM2 ProvisionNetwork pool-only 重写 | U23 BM2 manager redirect / U24 cascade removal | N/A — ADR-013 反向，推 v1.1+ |
| **U25-U26** SDK + DSL 清理 | U25 testlib DSL / U26 删 deprecated VO + 4 SDK Action | ⚠️ 待审；apihelper changeClusterServerPool blocker (next-session §3 #2) 同源 |
| **U27-U29** Schema + rollback | U27 V5.5.18.1 schema / U28 V5.5.18.2 data migration / U29 rollback runbook | U27 ✅（已合并到 V5.5.18__schema.sql）/ U28 ⚠️（data migration 部分 ✅，BM2 VIEW-ization 撤回 per ADR-013，仍按 ADR-013 落地）/ U29 ✅ |
| **U30-U31** admin-only + power stubs | U30 24 API admin-only / U31 power operr stubs | U30 ✅；U31 ✅：OOB-first power handler + no-OOB operr |

> **U-unit checkbox 状态** 在 phase2 plan 文件里全部还是 `- [ ]`（unchecked）。phase2 plan **不再回头逐个勾**——本 STATUS.md §4.1/§4.2 的 audit-derived roll-up 即权威进度。Phase 3 fix-plan 起新 U-编号体系（U1-U22 共 22 unit 见 audit report §Phase 3 fix-plan 骨架），不延用 phase2 U#。

### 4.3 NB-XX 实装状态（cloud_prd consolidation 决策）

**NB-XX** = cloud_prd 在 NB-1..34 final consolidation pass 中编号的 brainstorm decision notes，散落在 5 份 PRD 里。本表 cross-ref 每条 NB 到主题 + 实装状态。

| NB | 主题 | 出处 | 状态 | 备注 |
|---|---|---|---|---|
| NB-4 | HardwareDiscoveryQueue 限流（concurrency=8 / timeout=60s / retry=3）+ MN 启动补漏 + Step 0 ServerPool 初始化 BM2 粒度对齐 | role-SPI §2.5b · cleanup §2.3 · provision | ✅ | `HardwareDiscoveryScheduler` + 3 GlobalConfig 全实装；schema Step 0 实装 |
| NB-5 | Container Cordon 熔断（Taint→Cordon 简化）+ Pod 聚合 `max(Σinit, Σmain) + overhead` | capacity §2.9-§2.10 | ⚠️ | Cordon service + RBAC + hysteresis GREEN；Pod 聚合仍按独立 scope 跟踪 |
| NB-7 | Container per-node `@Transactional` 事务边界澄清 | role-SPI §2.4 | ✅ | PSC writer collapse 把 per-node 事务边界落到 `PhysicalServerCapacityUpdater.recalculate(serverUuid)` 单 PESSIMISTIC_WRITE（NB-30），ContainerEndpointBase 在 fan-out 内逐 NativeHost 调；不再用 `@Transactional` 注解（原始诉求是"事务边界清晰可追"，单锁单 server 已达成） |
| NB-8 | 补偿机制诚实限定（FlowChain Saga 反向 rollback，硬件明细 eventual consistency） | server PRD §2.5.1 | N/A | 设计原则陈述，无可验证 AC |
| NB-9 | 统一 power 砍 SPI 只做 OOB（不做 plugin SPI 框架） | cleanup §2.5 | ✅ | Power handler 已接入 OOB-first direct IPMI；BM2 role fallback 仅兼容 roleConfig 老数据 |
| NB-10 | 统一 power 砍 agent 兜底（无 OOB 直接 operr 转 KVM legacy API） | cleanup §2.5 | ✅ | 无 OOB 且无兼容 role fallback 时明确 operr；PS Manager 不引入 KVM 类型 |
| NB-11 | RoleProvider wire-up 原子性（createRoleEntity wire 真实时同 PR 接通 delete/capacity/workload） | role-SPI §2.1 | ✅ | KVM/BM2 全 wire；Container createRoleEntity 显式抛错符合 EXTERNAL_READONLY 语义。**ADR-012** 把 ordering normative 化 |
| NB-12 | `oobManagementType validValues={"IPMI"} required=false` IPMI-only 简化 | server PRD §2.4 · cleanup §2.5 | ✅ | `APICreate/UpdatePhysicalServerMsg.oobManagementType` 已 ✅ |
| NB-15 | admin-only accountUuid 硬编码 `36c27e8ff05c4780bf6d2fa65700f22e` + PhysicalServerAO 不 implements OwnedByAccount | server §4.2 · cleanup §2.3 | ✅ | 24 PS API 全部 `@Action(adminOnly=true)`；schema admin UUID 硬编码 |
| NB-16 | 混部 4 已知限制（迟滞陷阱 / Polling race / K8s 删 node / label 篡改） | capacity §2.9 | N/A | PRD 显式 v5.5.18 不守，留 v1.1+ 反馈再考虑 |
| NB-19 | `PhysicalServerHardwareService` 砍 SPI 用 3 private method 直调 + mergeNonNull | role-SPI §2.5b · cleanup §2.6 · server | ⚠️ | service 类骨架 + UnifiedHardwareInfo flat DTO ✅；3 private discover 仍 stub（U15 deferred） |
| NB-20 | 凭据 @NoLogging 脱敏（`roleConfig` + `credentials` + `oobPassword`） | role-SPI §2.5b · server | ✅ | Phase 1 已落 |
| NB-22 | `HostCapacityVO` POJO 例外（lockCapacity/originalCopy）+ 字段与 PSC 10 字段对齐 | capacity §2.1 · role-SPI | ✅ | W3 实装符合 NB-22；POJO 例外文档化在 ADR-001/002 |
| NB-24 | `resolveServerUuidOrThrow` fail-loud（撤销 NB-22 的 silent log+null）→ ADR-012 | capacity §2.1 W3 | ✅ | 落 commit `4f78791cb1`，**ADR-012** normative 化 ordering |
| NB-25 | vcenter 半迁移 option C（capacity 真表共享但**不**写 PS/RoleVO/AccountResourceRefVO） | capacity §2.1 W9 · cleanup §2.3 | ✅ | schema Block 8 + 配套 ADR-009 |
| NB-28 | 标识变更场景（BMC/主板更换 serialNumber/oobAddress 变）需运维手动清理 | server PRD §2.6 | N/A | operator-side 责任，不是代码 task |
| NB-30 | 所有 PESSIMISTIC_WRITE 以 `serverUuid` 为唯一锁 key（不混用 hostUuid） | capacity §2.1 W3 | ✅ | `HostCapacityUpdater` + 后续 `PhysicalServerCapacityUpdater.recalculate` 必守 |

**Roll-up**: NB ✅ 12 条 · ⚠️ 2 条 (NB-5/NB-19) · ❌ 0 条 · N/A 3 条 (NB-8/16/28)

> **NB 不是 R/U 编号体系的并行轨道**。NB-XX 是 PRD 内的"决策痕迹"，落码点散在 R-unit / U-unit 内。R/U 关心"什么 task 做了"，NB 关心"为什么这样设计"。两者交叉：4 条 ❌ NB 全部对应 §4.1 R-unit 的 ❌/⚠️ 项（NB-5 → R3 / NB-7 → R5 / NB-9-10 → R10）。Phase 3 fix-plan 实装这些 R-unit 时同步消除对应 NB 的 ❌。
>
> 编号断口（1-3, 6, 13-14, 17-18, 21, 23, 26-27, 29, 31-34 不出现）是 cloud_prd brainstorm 期间作废的中间决策，不是丢失。

### Phase 3 (待创建)
| Doc | 状态 |
|---|---|
| `docs/plans/2026-04-28-001-fix-phase2-prd-gaps-plan.md` | 待起草 — 直接消费 audit report §Phase 3 fix-plan U-unit 骨架 |

---

## 5. 当前进度快照（2026-04-27）

### 已完整落地 ✅
- PhysicalServer*VO 全家族 + Hibernate 注册
- HostCapacityVO TABLE→VIEW（ALGORITHM=MERGE + COALESCE 半迁移）+ `@Immutable`
- W1-W6 写路径全改 `PhysicalServerCapacityVO`（NB-22/24/30 实现细则）
- W3b ReportHostCapacityExtensionPoint dead-code 删除
- PhysicalServerRoleProvider SPI v3 五方法签名 + Javadoc
- KVM/BM2/Container 三家 RoleProvider implements 完整（Phase 2D wire 通真实 Add*Msg）
- APIAttachPhysicalServerRoleMsg / APIDetachPhysicalServerRoleMsg（admin-only + roleConfig）
- AutoAssociator 三级降级算法（serialNumber / oobAddress / managementIp）
- HardwareDiscoveryScheduler 限流队列（3 GlobalConfig）
- PhysicalServerHardwareService 类骨架 + UnifiedHardwareInfo flat DTO
- Schema 迁移：Step 0 ServerPool / Step 1+ PS·Role / vcenter 半迁移 / BM V1 跳过 / ResourceVO+ARR / admin-only AccountRef
- 3 RoleProviderIntegrationCase 全绿（KVM 81s / Container 206s / BM2 193s）
- 4 PhysicalServer*Case 移到 `premium/test-premium/.../server/` 全绿（2026-05-07 12a refactor 后）— `PhysicalServerCapacityCase` 121s · `PhysicalServerRoleCase` 129s · `PhysicalServerCompatCase` 113s · `ServerPoolCrudCase` 117s。fixture playbook 9 项：BM2 cluster + ipmi roleConfig，KVM_HOST 用 127.0.0.x 回环 IP（外网 IP 5s timeout），**CONTAINER_HOST 走真 K8s sync API**（`addContainerManagementEndpoint` + `syncContainerManagementEndpoint` + `K8sApiMocks.mockSingleZakuCluster` + `mockK8sNodesWithIps` — 12a 红线 no manual persist），`BareMetal2Test.springSpec` 加 `container.xml` + `iam2Container.xml`（zaku provider），Groovy DSL 闭包 `it`/同名参数避坑（如 `chassisUuid = chassisUuid` 解析为 delegate property），`role.createDate` 不在 API event 里，`oobPassword` 用反射检查 SDK 字段缺失，`expect(Throwable)` 兼容 SDK + server 失败路径，NB-12 锁 IPMI（详见 `docs/brainstorms/next-session.md` 顶部）
- ProvisionPhysicalServer LongJob stage-based phase tracking (jobData persistence, MN restart resume safe)
- Bm2GatewayDataPlane 4-stage orchestration (NotStarted→NetworkPrepared→PxeTriggered→Pinging→Done)
- Gateway-agent ping production wiring：`Bm2GatewayPingHelper.pingOnce` 走 `bus.send(PingTargetInGatewayMsg)` → `BareMetal2Gateway.handle(...)` → `restf.asyncJsonPost(PING_TARGET_PATH)`，不再 from-MN 跑 ICMP（AC-PN-14 production-path 闭环）
- 路径 2（传统 AddHost/AddChassis/AddNode）FlowChain 接入 — `HostManagerImpl.java:37,426` PhysicalServerPathTwoExtensionPoint hook · `BareMetal2ChassisManagerImpl.java` 委托 `PhysicalServerPathTwoOrchestrator.runStandalone(chassisVO,...)`（chassis-as-HostVO override）· `ContainerEndpointBase.syncNodesFromCluster` per-NativeHost fan-out `orchestrator.runStandalone(nativeHost, RoleMatchContext, cluster.uuid, completion)` → `AutoAssociateFlow` (tier1/2/3 by serialNumber/oobAddress/managementIp) → `CreatePhysicalServerRoleFlow` → `InitPhysicalServerCapacityFlow` → `enqueueDiscoveryHook`；`ContainerEndpointBase.saveAsNativeClusters` 在 `cluster.serverPoolUuid==null` 时 auto-create `<cluster-name>-pool`，避免 manual pool 前置（AC-RS-04/07/10 + 真机 201.160 sync→7 RoleVO 闭环）
- Container Pod 容量聚合 — `ContainerRoleProvider.java:96-117` `getCapacityConsumption` SUM(cpu) + SUM(memory) FROM PodVO WHERE state=Running；recalculate 路径 `available = total - consumed - buffer` 把 Pod 占用导出到 PSC（Layer 2 sole writer，不再回写 HostCapacityVO POJO）
- Hardware discover end-to-end (AC-CB-18) — `PhysicalServerManagerImpl.java:573,916` + `PhysicalServerEnqueueDiscoveryHookImpl` chain，路径 2 add-host / Discover API / orphan boot-scan 三条触发线全通
- **2026-05-05 production deploy** on 172.26.201.160 — bin install all 16 steps PASS · V5.5.18 Flyway migration row written (success=1) · `HostCapacityVO.cpuCoreNum INT UNSIGNED NOT NULL DEFAULT 0` 列在生产 DB · PhysicalServer 全家族 8 表全建出 · PhysicalServer-first add-host 端到端流程 GREEN（CreatePhysicalServer → PhysicalServerVO → AttachPhysicalServerRole(KVM_HOST) via REST `/v1/physical-servers/{uuid}/roles` → 异步 job 完成 → RoleVO + HostVO/KVMHostVO + HostCapacityVO + PhysicalServerCapacityVO 全建）· invariants 持：`RoleVO.roleUuid == HostCapacityVO.uuid == HostVO.uuid` (NB-22/24/ADR-012) + `PSC.uuid == PhysicalServerVO.uuid` (NB-22/30) · capacity 真值 `totalCpu=80, totalMem=16.5G, cpuCoreNum=8, cpuSockets=2`
- **PSC writer collapse — Layer 1 (KVM/Container sync) + Layer 2 (recalculate sole writer)** — Two-Layer Capacity Model 落地（plan: [docs/plans/2026-05-08-001-psc-writer-collapse-plan.md](plans/2026-05-08-001-psc-writer-collapse-plan.md)）。Layer 1 各模块 sync 入口写 PSC.total{Cpu,Memory}（KVM `HostAllocatorManagerImpl` host 周期 `/host/capacity` callback、Container `ContainerEndpointBase.syncNodesFromCluster` per-NativeHost）；Layer 2 唯一虚拟量入口 `PhysicalServerCapacityUpdater.recalculate(serverUuid)` 单 PESSIMISTIC_WRITE 锁 serverUuid（NB-30），`available = total - consumed - buffer - reserved`，`reserved` 由 `ServerReservedCapacityExtensionPoint` 收集（含 `ContainerCordonReservedCapacityExtension` 把 cordoned NativeHost free 全转 reserved，AC-CM-13）。`HostCapacityUpdater` POJO 路径标 `@Deprecated`（VM allocator 仍用，下个 phase 砍）。IT case 3/3 PASS（`KvmReportHostCapacityRecalcCase` / `ContainerSyncRecalcCase` / `ContainerCordonReservedCase`）。**2026-05-09 真机 172.26.201.160 hot-deploy** 7 zstack + 4 premium commit + premium `HostAllocatorManager.xml`（mirror `physicalServerCapacityUpdater` bean）+ MN restart：endpoint `ef554bb8255d4ce0b891a1367841b88b` sync 后 7 NativeHost PSC.totalCpu 0 → 8/8/8/16/120/192/192 cores（Layer 1 ✅），KVM host `d066db930a0041138640fcae28c1514d` PSC.availableCpu 80 → 72（减 cpuBuffer=8，Layer 2 recalculate ✅）。Cordon AC-CM-13 reservation extension 已实装并 IT 3/3 PASS（`ContainerCordonReservedCase`），但 **production 触发点缺失**：`cordonService.cordon()` / `evaluate()` / K8s 反向 mirror `isUnschedulable(V1Node)` 全 0 caller，`cordonedHostUuids` 生产侧永远空 → 下个 phase 必补 trigger（在 `recalculate` 后调 `evaluate`，在 `syncNodesFromCluster` 里 mirror K8s `spec.unschedulable`）。本轮真机只验证了 (a) Layer 1 + (b) Layer 2，(c) 因 production trigger 缺位无法验。

### 实装但偏离规约 ⚠️ (13 项)
见 [audit report](audits/2026-04-27-phase2-prd-audit.md) — 多数是 cosmetic drift（pool naming / UUID 算法）或部分实现（Hardware service 3 private discover 仍 stub / 超分比 read path 没绑定 PSC 列）。

### 测试基础设施约束 ⚠️（已修，记录避免再踩）
1. ~~**IT Spring init NPE**~~ 跟 ~~**StageTest 7 errors AspectJ ITDF**~~ 都是 **stale .m2 jar / 增量 `-am` build 与 AspectJ CTW 织造时序冲突** 引起的——`runMavenProfile premium` 全 reactor clean install 后全部消失。现状：19 cases (10 OSS unit + 4 BM2 lookup + 4 stage + 1 IT) 全绿（Jenkins dev.jenkins.zstack.io/job/build/190 SUCCESS, 22.5min）。
2. **教训**：本仓 AspectJ CTW 对 jar 安装顺序敏感，`mvn install -pl X -am` 增量会导致下游 module weaving 不完整 → 假阳性 `Bm2GatewayDataPlaneStageTest` 7 errors / `prepareTimeoutGlobalConfig` Spring init NPE。**测试不绿先 `runMavenProfile premium`，再判定**。

### 已知 deferred 🔁 (6 项, 不进 Phase 3)
- AC-AL-01..05: ServerAllocatorChain → v5.5.18.x
- AC-RS-13-P2: 跨角色 serialNumber 归一化 → v1.1+

### PRD stale per ADR 🅿 (3 项)
- AC-CB-ROLLBACK-01..03: PRD 期望保留 `*_backup` 表，但 ADR-007 明示无 backup（备份归 operator）。upstream cloud_prd 应改写

---

## 6. 当前 active blockers（非已 RESOLVED）

见 [next-session.md §3](brainstorms/next-session.md)，5 项 active blocker：
1. testlib-premium 默认 spec 加 PhysicalServerManager.xml 影响面广 — 跑 nightly 看回归
2. `changeClusterServerPool` 没被 apihelper 生成
3. test resources Kvm.xml 跟生产漂移
4. parked tests (CoalesceQueueCase + KVMHostUtilsTest) — 等 upstream 修
5. mvn-safe-install.sh stale-guard 范围窄

---

## 7. 启动新 session 时该读什么（按场景）

| 场景 | 先读 | 然后读 |
|---|---|---|
| 接续上一轮工作 | `docs/brainstorms/next-session.md` (整个) | 本文件 §5 + audit report |
| 决策追溯 / "为什么这么设计" | `docs/decisions/` 对应 ADR | PRD 对应章节 |
| 当前 Phase 完整任务表 | `docs/plans/<latest>-plan.md` | 引用的 PRD / ADR |
| 写代码踩坑 | `docs/runbooks/v5518-sql-ddl-pitfalls.md` + `next-session.md §0` (铁律) | — |
| 升级失败 / 回滚 | `docs/runbooks/v5518-unified-hardware-rollback.md` | ADR-007 + 13 |
| 测试环境连接 | `docs/runbooks/testing-envs.md` | — |
| 整盘视野（这个 feature 在干啥 / 到哪步） | **本文件** | — |
| 上次 session 都干了啥 | `docs/brainstorms/next-session.md §1` | git log |

---

## 8. Update protocol

**何时刷新本文件**:
- Phase 切换（2 → 3 等）
- audit / 完整状态盘点 后
- 新 ADR 落地（同步加进 §2 列表）
- 新 PRD 加入 / 删除（cloud_prd 维护者通知）

**不在本文件**:
- 单 session 进度（→ next-session.md）
- 具体代码改动（→ git log + plan U-unit checkbox）
- 临时调试笔记（→ next-session.md §0）

**Git blame 友好**: 每次更新只改受影响 section + bump §Last updated 行。**不**整体 rewrite，让 blame 能追溯每条信息何时何故加的。

---

## 9. 维度索引（给 agent / subagent 用）

**Module → Owner agent 映射**（见 CLAUDE.md "Agent Routing"）:
- `compute/` 容量写路径 / HostAllocatorChain / @Immutable VIEW → `compute-resource-allocator`
- `plugin/kvm/` KVM host / KvmRoleProvider → `kvm-host-expert`
- `premium/baremetal2/` BM2 chassis / IPMI / Bm2RoleProvider → `baremetal2-architect`
- `premium/plugin-premium/container/` NativeHostVO / Cordon / ContainerRoleProvider → `container-module-architect`
- `header/` 跨模块接口 / SPI / 4 模块协调 → `hardware-unified-arch-lead`

**核心代码 root**:
- `header/src/main/java/org/zstack/header/server/` — PhysicalServer*VO + SPI + API messages
- `header/src/main/java/org/zstack/header/allocator/HostCapacityVO.java` — VIEW-mapped entity
- `compute/src/main/java/org/zstack/compute/allocator/` — HostAllocator + HostCapacityUpdater + OverProvisioningManager
- `plugin/physicalServer/src/main/java/org/zstack/server/` — Manager + AutoAssociator + HardwareService
- `plugin/kvm/src/main/java/org/zstack/kvm/KvmRoleProvider.java`
- `premium/baremetal2/src/main/java/org/zstack/baremetal2/server/Bm2RoleProvider.java`
- `premium/plugin-premium/container/src/main/java/org/zstack/container/server/ContainerRoleProvider.java`
- `conf/db/upgrade/V5.5.18__schema.sql` — Flyway DDL

**集成测试 case**:
- `test/.../kvm/KvmRoleProviderIntegrationCase.groovy` ✅
- `premium/test-premium/.../baremetal2/Bm2RoleProviderIntegrationCase.groovy` ✅
- `premium/test-premium/.../container/ContainerRoleProviderIntegrationCase.groovy` ✅
