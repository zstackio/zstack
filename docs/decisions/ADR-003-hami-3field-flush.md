# ADR-003: HAMI:256 cap.setTotalCpu 静默丢弃（NB-22 3-field flush）

**Status**: Accepted
**Date**: U4 实施期间
**Source**: `next-session.md` 关键决策表（U4）

## Context

HAMI 插件在 capacity flush 时会调用 `cap.setTotalCpu(256)` 这类设置，但在新 PSC 架构下
PSC 的 totalCpu 由硬件发现（U2 scheduler）权威维护，HAMI 不该写这个字段。

行为有两种处理：
1. 抛异常阻断（强语义，但会 break HAMI 现网）
2. 静默丢弃（兼容现网，但 HAMI 作者可能不知情）

## Decision

选 (2)：在 NB-22 3-field flush 里**静默丢弃** `totalCpu` 的写入，只 flush 三个合法字段
（usedCpu / totalMemory / usedMemory）。

## Consequences

- ✅ HAMI 插件零改动，升级过程无中断
- ✅ PSC 的 totalCpu 权威来源单一（scheduler），不会被 HAMI 覆盖
- ⚠️ HAMI 作者在老接口下"看起来设置成功"但实际被丢弃，需在 HAMI 对接文档里说明
- ⚠️ 如果将来 HAMI 真的需要写 totalCpu，要改为**先经过 PhysicalServerCapacityVO API**
  而不是重新打开 updater 的 setter
