# ADR-012 — RoleProvider `preGeneratedRoleUuid` ordering for `createRoleEntity`

**Status**: Accepted — 2026-04-27
**Supersedes**: none
**Superseded by**: none

## Context

Phase 2D 修 FlowChain timing bug 时（commit `4f78791cb1`）暴露：早期实现把 `provider.createRoleEntity(ctx)` 放在 `dbf.persist(PhysicalServerRoleVO)` 之前。`KvmRoleProvider.createRoleEntity` 内部用 `bus.call(AddKVMHostMsg)` 同步触发 host connect flow，connect flow 末段 `HostCapacityUpdater._run()` 调 `resolveServerUuidOrThrow(hostUuid)`，按 NB-24 fail-loud 规约查 `PhysicalServerRoleVO WHERE roleUuid=hostUuid AND roleType='KVM_HOST'` —— 但此时 RoleVO 还没 persist，查空 → throw → AC-1 失败。

根因是 `host.uuid` 与 `roleUuid` 必须在 RoleVO 写完之后才能从 host 反查回 PhysicalServer。同步 connect flow 不容忍中间态。

## Decision

**`PhysicalServerManagerImpl.handle(APIAttachPhysicalServerRoleMsg)` 必须按以下顺序执行**:

```text
1. roleUuid = Platform.getUuid()                          // 预生成
2. ctx.preGeneratedRoleUuid = roleUuid
3. dbf.persist(new PhysicalServerRoleVO(roleUuid, ...))   // 先写 RoleVO
4. provider.createRoleEntity(ctx)                         // 内部用 ctx.preGeneratedRoleUuid 作 Add*Msg.resourceUuid
5. failure → dbf.remove(role) rollback                    // 反向补偿
```

**`CreateRoleEntityContext` 必须有 `preGeneratedRoleUuid` 字段**，由 handler 填充，provider 实现读取并透传到 `Add*Msg.resourceUuid`（KVM 用 `AddKVMHostMsg.resourceUuid`，BM2 用 `AddBareMetal2ChassisMsg.resourceUuid`）。

**Path 2 (传统 AddHost/AddChassis) 走 FlowChain 等价路径**：`HostManagerImpl.doAddHost` / `BareMetal2ChassisManagerImpl.handle(APIAddBareMetal2ChassisMsg)` 实装 `AutoAssociateFlow → CreatePhysicalServerRoleFlow → InitPhysicalServerCapacityFlow` 三个 Flow，FlowChain 反向 rollback 等价于 path 1 的 `dbf.remove`。

**Container 例外**: `EXTERNAL_READONLY` 角色不通过 `AttachPhysicalServerRole` 入口（attach handler 提前 `if (provider.getSchedulingMode() == EXTERNAL_READONLY) return operr(...)`）。Container 走 `ContainerEndpointBase.processNodeTransactional` 单 `@Transactional` 方法，5 步原子内自然满足 ordering（per-node 事务，K8s sync 路径无外部 I/O）。

## Consequences

- **Normative for all new RoleProvider impls**: 未来 v1.1+ 新角色（如 GPU 集群）按此 ordering 落 `createRoleEntity`，否则同样掉 NB-24 fail-loud 坑
- **`AddKVMHostMsg.resourceUuid` / `AddBareMetal2ChassisMsg.resourceUuid` 必须接受 caller 预定义 UUID**（zstack 标准 `Resource Constructor` 模式，向后兼容）
- **Phase 2D integration case 全绿基于 path 1**：`KvmRoleProviderIntegrationCase` / `Bm2RoleProviderIntegrationCase` / `ContainerRoleProviderIntegrationCase` 都走 `APIAttachPhysicalServerRoleMsg`，不通过 path 2。Phase 3 fix-plan U1 (FlowChain 3 Flow) 实装 path 2 时复用本 pattern
- **失败 rollback 用反向 SQL 删除**: 原 `dbf.remove(role)` 在 Manager 同事务内已足够；FlowChain 路径靠 ZStack Saga 反向 compensation

## Alternatives considered

**Option B — Provider 自己生成 UUID 后回传**：`createRoleEntity` 返回 `String roleUuid`，handler 拿到再 persist RoleVO。看似自然但 (1) connect flow 仍可能在 provider 内部启动并触发 RoleVO lookup → 同样掉 NB-24 坑；(2) 失败时 provider 已部分提交，rollback 复杂度高。

**Option C — RoleVO 写到 Add*Msg handler 内部**（如 `HostManagerImpl.doAddHost` 写 RoleVO）：耦合性差，每个 host module 都得知道 PhysicalServer 模型，违反 SPI 抽象。Phase 2D 实测下放后 KVM/BM2/Container 三家都得改，工作量比 path 1 + path 2 各自落 FlowChain 高。

A 选定因为：(1) 解 NB-24 fail-loud 根因；(2) handler 是统一锚点，所有 RoleProvider 调用都过这一行；(3) 复用 zstack `Resource Constructor` 模式（API 接受预生成 UUID）成熟稳定。

## References

- Implementation: commit `4f78791cb1 <fix>[server]: FlowChain timing + cleanup gap`
- Trigger bug: NB-24 (`HostCapacityUpdater.resolveServerUuidOrThrow` fail-loud)，capacity PRD §2.1 W3 实现细则
- SPI 接口: `header/src/main/java/org/zstack/header/server/CreateRoleEntityContext.java`（`preGeneratedRoleUuid` 字段）
- Manager: `plugin/physicalServer/src/main/java/org/zstack/server/PhysicalServerManagerImpl.handle(APIAttachPhysicalServerRoleMsg)` (lines 433-500)
- Path 1 实现: `KvmRoleProvider.createRoleEntity` (lines 169-191), `Bm2RoleProvider.createRoleEntity` (lines 99-131)
- Path 2 待实装: Phase 3 fix-plan Wave 1 U1 (FlowChain 3 Flow)
- 相关 ADR: ADR-001 (`HostCapacityUpdater.resolveServerUuidOrThrow` 静态方法), ADR-002 (`HostCapacityUpdater` POJO uuid 语义)
