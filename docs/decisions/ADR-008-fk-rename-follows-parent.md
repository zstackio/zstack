# ADR-008: FK constraint 名字跟随 parent 表名改名

**Status**: Accepted
**Date**: V5.5.18 consolidate 期间
**Source**: `next-session.md` 关键决策表（V5.5.18 consolidate）

## Context

[ADR-006](ADR-006-pspn-inplace-rename.md) 把 `BareMetal2ProvisionNetworkVO` 改名为
`PhysicalServerProvisionNetworkVO`，inbound FK constraint 原本叫
`fkBareMetal2InstanceProvisionNicVONetworkVO`（按老命名 `fk<child>VO<parent>VO`）。

三种选择：
1. FK 名不变（保持 `fkBareMetal2...NetworkVO`），后续维护看到 name 会误判 parent
2. 改名跟随 parent
3. 给 FK 加 prefix/suffix 标注版本

MySQL 64 字符限制让 (2) 在某些场景会超长（实际踩过，见本轮 bug 修复 #1）。

## Decision

**FK constraint 名跟随 parent 表名改名**：
- `fkBareMetal2InstanceProvisionNicVONetworkVO` → `fkBareMetal2InstanceProvisionNicVOPhysicalServerProvisionNetworkVO`
- 同样处理 `BareMetal2GatewayProvisionNicVO` / `BareMetal2ProvisionNetworkClusterRefVO` 的 FK

超 64 字符时**截断 child 部分**（例如用 `BM2` 代替 `BareMetal2`）：
- `fkBM2InstanceProvisionNicVOPhysicalServerProvisionNetworkVO`

## Consequences

- ✅ FK 名本身可作为 schema audit 手段：`grep` FK 名能反查当前关联的 parent
- ✅ schema drift 检测简单：FK 名指向一个已不存在/改名的 parent 立即异常
- ⚠️ 超 64 字符要截断 child 名，**parent 部分保留完整**（parent 的可读性优先）
- ⚠️ 截断规则要一致（"BM2" 缩写全项目统一使用）
- 参见 [v5518-sql-ddl-pitfalls.md](../runbooks/v5518-sql-ddl-pitfalls.md) pitfall #8
