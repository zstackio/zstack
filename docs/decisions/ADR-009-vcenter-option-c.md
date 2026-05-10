# ADR-009: vcenter 走 option C 半迁移，不新建 VcenterHostCapacityVO 分叉

**Status**: Accepted
**Date**: U6 实施期间
**Source**: `next-session.md` 关键决策表（U6）

## Context

vcenter 场景的 ESXi host 在老模型下用 `HostCapacityVO` 记录容量，新模型要改为 PSC。
三个方案：
- **Option A**：完全迁移，vcenter 也进入 PhysicalServerVO + PSC
- **Option B**：新建 `VcenterHostCapacityVO` 专用表，保持 vcenter 独立分支
- **Option C**：**半迁移** —— PSC 收下 vcenter 数据（serverUuid = ESXi uuid），但不为
  每个 ESXi 建 PhysicalServerVO 行

A 方案要给 ESXi 造一堆没有实际 host factory 支撑的 PS 行；B 方案产生 schema 分叉，
监控/报表要同时查两个 capacity 表；C 方案最务实。

## Decision

**选 Option C**：PSC 直接承接 vcenter capacity，不再为 vcenter 建独立的 HostCapacity 表。
PSC → PS 不建 FK（见 [ADR-004](ADR-004-psc-no-fk-vcenter.md)），使 direct PSC 插入合法。

## Consequences

- ✅ capacity 查询路径统一（全部走 PSC 或 HCV VIEW），报表/监控零改动
- ✅ 不需要为 vcenter ESXi 造 phantom PhysicalServerVO 行
- ⚠️ PSC 的 serverUuid 语义扩展：**可能不在 PhysicalServerVO 里**（vcenter ESXi）
  应用层查询代码要知道这一点
- ⚠️ V5.5.18 consolidate Block 1c 的 ESXi direct 分支就是这个方案的落地点
