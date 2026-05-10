# ADR-011: Derived UUID 的 MD5 salt 命名规则

**Status**: Accepted
**Date**: U27/U28 实施期间
**Source**: `next-session.md` 关键决策表（MD5 salt 命名规则）

## Context

V5.5.18 数据迁移时需要从已有资源（host / cluster / zone）**派生**新资源的 uuid，
例如：
- KVM host → 对应的 PhysicalServerVO uuid
- BM2 cluster → 专属 ServerPoolVO uuid
- 每个 PS 不同 role → PhysicalServerRoleVO uuid

派生方式有两种：
1. 新分配随机 uuid，然后在迁移表里记 mapping
2. **确定性派生**（deterministic）：从 source uuid + salt 做 MD5

(1) 需要额外 mapping 表，回滚 / 重跑迁移时难保一致；(2) 只要 salt 固定，再跑多少次
结果都一样，幂等性天然。

## Decision

统一使用 **MD5 salt derivation**，按下表规则：

| Derived UUID | 公式 |
|---|---|
| `PhysicalServerVO.uuid` | `MD5(source_uuid + '-ps')` |
| `PhysicalServerRoleVO.uuid` | `MD5(source_uuid + '-role-{kvm\|bm2\|container}')` |
| `ServerPoolVO.uuid` (BM2 cluster 1:1) | `MD5(cluster_uuid + '-pool-bm2')` |
| `ServerPoolVO.uuid` (zone shared) | `MD5(zone_uuid + '-default-pool')` |

规则：**salt 一律小写、以 `-` 开头、业务含义可读**。

## Consequences

- ✅ 迁移幂等：重跑 `V5.5.18__schema.sql` 任意次都产生相同结果，支持"升级失败
  修 bug 再升级"工作流
- ✅ DB forensics 友好：给定一个 derived uuid，知道 salt 规则就能反推 source
- ✅ 不需要 mapping 表，schema 干净
- ⚠️ MD5 不是加密用途（UUID 不需要抗碰撞保护）；salt 泄漏无安全影响
- ⚠️ **salt 规则写定后不要改**。改了等于所有老数据 uuid 变了
- ⚠️ 新增派生字段时，salt 字符串要 **全项目唯一**（避免不同派生用相同 salt）

## U14 Confirmation (2026-04-28)

U14 audit (Phase 3 Wave 3) re-confirmed the rules above against the actual
`V5.5.18__schema.sql` content and ratified the following two decisions for
AC-CB-08 / AC-CB-09:

**Decision 1 — UUID algorithm for migrated PhysicalServerVO.uuid (AC-CB-08)**:
chose option (a) `MD5(source.uuid + '-ps')` — *derivative-from-source-vo*.
Rationale: stable across mgmtIp / IP renumbering (option (b) `MD5(mgmtIp+zoneUuid)`
would re-issue uuids on every IP change, breaking ResourceVO / ARR / role
linkage). All three migration blocks (1a KVM HostEO, 1b BM2 chassis, 1c Native
container host) use the same derivation. The PRD's `MD5(mgmtIp+zoneUuid)`
candidate is rejected.

**Decision 2 — Pool naming (AC-CB-09)**:
chose option (a) `bm2-pool-<uuid8>` for per-BM2-cluster pools and `default-pool`
for the zone-shared default pool. `<uuid8>` is `SUBSTRING(cluster.uuid, 1, 8)`,
giving operators a stable readable prefix without exposing the full 32-char uuid
in cloud_prd UI. Option (b) `bm2-<name>-pool` was rejected because cluster
`name` may contain spaces / non-ASCII / duplicates across zones, breaking
uniqueness. Both pool names live in `ServerPoolVO.name` (VARCHAR(255)).
