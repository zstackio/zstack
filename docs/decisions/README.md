# Architecture Decision Records

本目录记录 v5.5.18 Unified Hardware feature 开发过程中**已落地且不再重议**的技术决策。

## 用法

- 每条 ADR 一个文件，文件名 `ADR-<3位序号>-<slug>.md`
- `next-session.md` 只引用不复制：写 `[ADR-004](ADR-004-psc-no-fk-vcenter.md)` 而不是复述决定
- 新决策**先写 ADR，再在代码里实现**（避免"为什么这么写"无处回溯）
- 如果要推翻某条 ADR：**不要删文件**，改 Status 为 `Superseded by ADR-NNN`

## 索引

| # | 决策 | Phase/Unit | 状态 |
|---|---|---|---|
| [ADR-001](ADR-001-hostcapacity-updater-static-resolve.md) | `HostCapacityUpdater.resolveServerUuidOrThrow` 静态方法 | U4 | Accepted |
| [ADR-002](ADR-002-hostcapacity-updater-uuid-semantics.md) | `HostCapacityUpdater` POJO `uuid` 保持 hostUuid 语义 | U4 | Accepted |
| [ADR-003](ADR-003-hami-3field-flush.md) | HAMI:256 `cap.setTotalCpu` 静默丢弃（NB-22 3-field flush） | U4 | Accepted |
| [ADR-004](ADR-004-psc-no-fk-vcenter.md) | PSC 无 DB FK to PhysicalServerVO | U27 | Accepted |
| [ADR-005](ADR-005-hcv-view-algorithm-merge.md) | HCV VIEW `ALGORITHM=MERGE` + `SQL SECURITY INVOKER` | U27 | Accepted |
| [ADR-006](ADR-006-pspn-inplace-rename.md) | BM2ProvisionNetworkVO → PSPN 用 in-place RENAME | V5.5.18 consolidate | Accepted |
| [ADR-007](ADR-007-no-backup-tables.md) | Schema 不保留 `_backup` 表，升级前备份 operator 负责 | V5.5.18 consolidate | Accepted |
| [ADR-008](ADR-008-fk-rename-follows-parent.md) | FK constraint 名跟随 parent 表名改名 | V5.5.18 consolidate | Accepted |
| [ADR-009](ADR-009-vcenter-option-c.md) | vcenter 走 option C 半迁移，不新建 VcenterHostCapacityVO | U6 | Accepted |
| [ADR-010](ADR-010-bm1-out-of-scope.md) | BM1 chassis 不迁移（operator 知情） | U27/U29 | Accepted |
| [ADR-011](ADR-011-md5-salt-uuid-derivation.md) | Derived UUID 的 MD5 salt 命名规则 | U27/U28 | Accepted |
| [ADR-012](ADR-012-roleprovider-pre-generated-role-uuid.md) | RoleProvider `preGeneratedRoleUuid` ordering for `createRoleEntity`（先写 RoleVO 再调 provider） | Phase 2D / Phase 3 U1 | Accepted |
| [ADR-013](ADR-013-bm2-clusterref-table-not-view.md) | `BareMetal2ProvisionNetworkClusterRefVO` 保留为真实表（Option A interim，U23-U26 后续重写） | Phase 2D | Accepted (interim) |
| [ADR-014](ADR-014-incremental-rebuild-antipattern.md) | Incremental rebuild 反模式 → 铁律 12 + `mvn-safe-install.sh` + `guard-mvn-stale.sh` | 开发流程 | Accepted |
