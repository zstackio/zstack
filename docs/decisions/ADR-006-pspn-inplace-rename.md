# ADR-006: BareMetal2ProvisionNetworkVO → PhysicalServerProvisionNetworkVO 用 in-place RENAME

**Status**: Accepted
**Date**: V5.5.18 consolidate 期间
**Source**: `next-session.md` 关键决策表（V5.5.18 consolidate）

## Context

BM2 的 `BareMetal2ProvisionNetworkVO` (BM2PN) 在统一硬件后要改为
`PhysicalServerProvisionNetworkVO` (PSPN)，服务于所有 server type。

两条候选路径：
1. **COPY + VIEW**：新建 PSPN 表 + COPY 数据 + 老表保留为 VIEW
2. **in-place RENAME**：`RENAME TABLE BareMetal2ProvisionNetworkVO TO PhysicalServerProvisionNetworkVO`
   + 同步改 inbound FK

(1) 的优点是回滚简单（DROP 新表即可），缺点是数据双写、schema 复杂、存储翻倍。
(2) 的优点是零拷贝、语义清晰，缺点是 RENAME 遇到 inbound FK 会 errno 150 失败
（[见 pitfall #2](../runbooks/v5518-sql-ddl-pitfalls.md)）。

## Decision

选 (2) **in-place RENAME**，通过 drop-rename-readd 三步绕过 errno 150：
1. 对所有 inbound FK (`BareMetal2InstanceProvisionNicVO`, `BareMetal2GatewayProvisionNicVO`,
   `BareMetal2ProvisionNetworkClusterRefVO`) 先 `DROP FOREIGN KEY`
2. 执行 `RENAME TABLE`
3. 按新表名 + [ADR-008](ADR-008-fk-rename-follows-parent.md) 约定重建 FK constraint

## Consequences

- ✅ 数据零拷贝，升级时间 O(1)
- ✅ 老查询自动命中新表（MySQL RENAME 是原子的）
- ⚠️ 回滚比 COPY 方案复杂：需反向 RENAME + 反向 FK 重建（U29 runbook 已覆盖）
- ⚠️ BM2 plugin 未安装的客户：V5.5.18 Stage 3 的 DROP FK 无条件执行，需 `information_schema`
  guard（见 [U29 runbook](../runbooks/v5518-unified-hardware-rollback.md) 已知问题章节）
- ⚠️ 升级前 DB 备份必须由 operator 完成（见 [ADR-007](ADR-007-no-backup-tables.md)）
