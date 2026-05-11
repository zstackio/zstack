# ADR-004: PhysicalServerCapacityVO 无 DB FK 指向 PhysicalServerVO

**Status**: Accepted
**Date**: U27 实施期间
**Source**: `next-session.md` 关键决策表（U27）

## Context

`PhysicalServerCapacityVO` (PSC) 的 serverUuid 天然指向 `PhysicalServerVO` (PS)，按 ZStack
常规建模应该加 FK CASCADE。但 vcenter 场景走 [ADR-009](ADR-009-vcenter-option-c.md) 的 option C
半迁移：vcenter ESXi 不在 PS 表里产生行（没有 KVM 那样的 host factory），而是走 direct
PSC 插入（参见 V5.5.18 consolidate Block 1c 中 ESXi direct 分支）。

如果 PSC 加 FK to PS：
- vcenter direct PSC 行插入会 FK 违反
- 或者必须给每个 ESXi 构造一个 phantom PS 行（增加复杂度 + 历史包袱）

## Decision

**PSC 不建 DB FK 指向 PS**。一致性在应用层保证（service 层删 PS 时级联删 PSC）。

## Consequences

- ✅ vcenter option C 方案可行，不需要 phantom PS
- ✅ schema 更简单，RENAME / DROP 操作不受 FK 150 错误约束
- ⚠️ 应用层必须在 PS 删除路径显式清理 PSC，否则会有悬挂记录
- ⚠️ 运维查询"孤儿 PSC" 需要脚本/监控：`SELECT ... FROM PSC LEFT JOIN PS ... WHERE PS.uuid IS NULL`
- 参见 [U29 rollback runbook](../runbooks/v5518-unified-hardware-rollback.md) §5 孤儿清理
