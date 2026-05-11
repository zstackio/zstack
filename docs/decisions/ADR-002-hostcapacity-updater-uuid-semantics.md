# ADR-002: HostCapacityUpdater POJO uuid 字段保持 hostUuid 语义

**Status**: Accepted
**Date**: U4 实施期间
**Source**: `next-session.md` 关键决策表（U4）

## Context

U4 把 capacity write path 从 HCV 改到 PSC 时，`HostCapacityUpdater` 的 POJO `uuid` 字段
有两个候选语义：
1. 继续表示 hostUuid（老语义，调用方期望）
2. 改为 serverUuid（更贴近 PSC 新模型）

选 (2) 会让 Updater runnable 的调用语义改变，所有调用方都要 diff 跟进。

## Decision

**保持 `uuid` 为 hostUuid**。内部自行调用 [ADR-001](ADR-001-hostcapacity-updater-static-resolve.md)
的静态方法做 server 解析，对调用方透明。

## Consequences

- ✅ Runnable 语义保持兼容，老调用点零改动
- ✅ NFR-005 "不动已有接口，只动实现" 得到遵守
- ⚠️ Updater 内部每次写 PSC 前都隐含一次 host→server 解析，DB 访问量上升
  （mitigated：HCV cache 可避免重复查询）
- ⚠️ 阅读代码时 `this.uuid` 不等于 PSC 的 serverUuid，容易误解；需配注释提醒
