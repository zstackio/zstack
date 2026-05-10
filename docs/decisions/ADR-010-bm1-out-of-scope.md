# ADR-010: BM1 chassis 不迁移到统一硬件模型（out of scope）

**Status**: Accepted
**Date**: U27/U29 实施期间
**Source**: `next-session.md` 关键决策表（U27/U29）

## Context

ZStack 有两套 baremetal 实现：
- **BM1**：老 baremetal 插件，`BaremetalChassisVO` 为主模型，基于 PXE
- **BM2**：新 baremetal 插件，`BareMetal2ChassisVO` / `BareMetal2ProvisionNetworkVO`
  为主模型，基于 IPMI/Redfish

V5.5.18 的目标是"Unified Hardware"——抽象 KVM/BM2/Container 三类 server。是否把 BM1
也纳入？

## Decision

**BM1 out of scope**。V5.5.18 只覆盖 KVM / BM2 / Container 三类；BM1 继续走
`BaremetalChassisVO` 老路径，不进 PhysicalServerVO。

## Consequences

- ✅ Scope 收敛，V5.5.18 交付周期可控
- ✅ BM1 客户升级无感知（老 chassis 表零改动）
- ⚠️ Operator 必须知情：升级文档要说明"BM1 chassis 在统一硬件视图里看不到"
- ⚠️ 如果未来要纳入 BM1，需另起 ADR 并可能引入 migration（BM1 chassis 数据量通常小，
  届时可走 COPY 方案）
- ⚠️ UI/监控需要做类型判断：统一硬件面板只展示 KVM/BM2/Container
