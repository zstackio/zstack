# ADR-005: HostCapacityVO VIEW 用 ALGORITHM=MERGE + SQL SECURITY INVOKER

**Status**: Accepted
**Date**: U27 实施期间
**Source**: `next-session.md` 关键决策表（U27）

## Context

V5.5.18 把 `HostCapacityVO` (HCV) 从物理表改为 VIEW（底表是 PSC）。MySQL/MariaDB 建 VIEW
时有两组正交选择：

1. **ALGORITHM**: `MERGE` vs `TEMPTABLE` vs `UNDEFINED`
2. **SQL SECURITY**: `DEFINER` vs `INVOKER`

默认 `UNDEFINED` + `DEFINER` 会在 mysqldump 产生 `DEFINER=remote@host` 的 VIEW DDL，
到本地 restore 时触发 `ERROR 1356 View references invalid DEFINER`（[见 pitfall #1](../runbooks/v5518-sql-ddl-pitfalls.md)）。
`TEMPTABLE` 无法推 filter 到底表，性能不可接受。

## Decision

HCV VIEW 建立时显式指定：
```sql
CREATE OR REPLACE
ALGORITHM = MERGE
SQL SECURITY INVOKER
VIEW HostCapacityVO AS SELECT ...
```

## Consequences

- ✅ `ALGORITHM=MERGE`: WHERE/index 可下推到 PSC 底表，`EXPLAIN` 绿（AC-CM-PERF-01 验证）
- ✅ `SQL SECURITY INVOKER`: mysqldump 导出到任意目标 MySQL 都能 restore，无 DEFINER trap
- ✅ MERGE 会 fail-fast：VIEW 定义引用不存在的列会直接 DDL 失败，不会拖到运行时
- ⚠️ VIEW 不能有聚合/DISTINCT/子查询（否则 MERGE 退化）—— 当前定义满足约束
- ⚠️ INVOKER 模式下调用方必须对 PSC 有 SELECT 权限；管理员直连操作不受影响
