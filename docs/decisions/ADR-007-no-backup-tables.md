# ADR-007: Schema 不保留 _backup 表，升级前 DB 备份由 operator 负责

**Status**: Accepted
**Date**: V5.5.18 consolidate 期间
**Source**: `next-session.md` 关键决策表（V5.5.18 consolidate）

## Context

V5.5.18 schema 里涉及 RENAME / DROP 的敏感表（BM2PN、HCV 等）早期方案是
保留 `xxx_backup` 影子表方便回滚。但：
- 存储翻倍（某些表几十 GB）
- `_backup` 的维护会混入正常 DDL 路径，容易产生 schema drift
- 回滚时 `_backup` 数据未必比 operator 的完整 mysqldump 更新

## Decision

**schema 不保留任何 `_backup` 表**。升级前的完整 DB 备份责任转移给 operator，
并在升级文档里**硬性要求**（不是建议）。U29 runbook §1 列明备份命令。

## Consequences

- ✅ 升级 DDL 路径干净，schema 无冗余
- ✅ 回滚时数据源单一（operator 的 mysqldump），无"哪份是权威" 的歧义
- ⚠️ **Operator 必须做备份**。如果没做，升级失败 = 数据丢失。升级向导应在
  交互层强制确认（目前靠文档）
- ⚠️ CI / 自动化测试环境**特别注意**：fresh DB 跑升级前要有 snapshot（参见
  [testing-envs.md](../runbooks/testing-envs.md) 的 216 快照拉取流程）
