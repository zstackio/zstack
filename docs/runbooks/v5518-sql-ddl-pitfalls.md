# V5.5.18 SQL/DDL Pitfalls Runbook

v5.5.18 Unified Hardware schema 迁移中踩过的 8 个通用 MySQL/MariaDB 坑。
**写给未来的自己**：下次再做跨 MySQL/MariaDB 的迁移、RENAME、VIEW 化工作时，
先翻本文一遍。

---

## #1 DEFINER trap（mysqldump 导出的 VIEW 无法 restore）

**症状**: `ERROR 1356 (HY000): View 'xxx' references invalid definer`
**场景**: 从 prod MySQL 用 mysqldump 导出的 dump 里 VIEW DDL 带 `DEFINER=<remote>@<host>`，
restore 到本地 MySQL 时 DEFINER 用户不存在。

**修复**:
```bash
sed 's|DEFINER=[^ ]*@[^ ]* |DEFINER=`root`@`localhost` |g;
     s|SQL SECURITY DEFINER|SQL SECURITY INVOKER|g' \
    dump.sql > dump-patched.sql
```

**预防**: 本项目所有 VIEW 建表固定用 `SQL SECURITY INVOKER`
（见 [ADR-005](../decisions/ADR-005-hcv-view-algorithm-merge.md)）。

---

## #2 InnoDB RENAME errno 150（有 inbound FK 时 RENAME 失败）

**症状**: `ERROR 1025 (HY000): Error on rename of ... errno: 150`
**原因**: InnoDB 要保证 FK 引用的 parent 存在且名字一致，直接 RENAME 会违反约束。

**修复** — drop-rename-readd 三步：
```sql
-- 1. DROP 所有指向该表的 FK
ALTER TABLE ChildVO DROP FOREIGN KEY fk_child_to_parent;

-- 2. RENAME parent
RENAME TABLE OldParentVO TO NewParentVO;

-- 3. 按新名字重建 FK（名字也改，见 pitfall #8 / ADR-008）
ALTER TABLE ChildVO
  ADD CONSTRAINT fk_child_to_newparent
  FOREIGN KEY (parentUuid) REFERENCES NewParentVO(uuid);
```

V5.5.18 Stage 3 是这个 pattern 的实战样板。

---

## #3 VALUES(table.col) 不可移植

**症状**: MariaDB 10.3 / MySQL 8 报语法错（各种奇怪 near-to 报错）。
**原因**: `INSERT ... ON DUPLICATE KEY UPDATE col = VALUES(table.col)` 只在老 MySQL 上允许；
标准写法 `VALUES(col)` 只吃裸列名，不带表前缀。

**修复**: 把 `VALUES(table.col)` 改成 `VALUES(col)`。

**检测**:
```bash
grep -rn 'VALUES([A-Za-z_][A-Za-z0-9_]*\.' conf/db/upgrade/
```

---

## #4 ON DUPLICATE KEY UPDATE col = col ambiguous（错误 1052）

**症状**: `ERROR 1052 (23000): Column 'col' in field list is ambiguous`
**场景**: SELECT 有别名产生同名列时，`ODKU` 的目标列不带表限定符会 ambiguous。

**修复**: 目标列显式 table-qualified：
```sql
INSERT INTO ServerPoolVO (uuid, lastOpDate, ...)
SELECT ... FROM source s LEFT JOIN existing e ON ...
ON DUPLICATE KEY UPDATE
  ServerPoolVO.lastOpDate = ServerPoolVO.lastOpDate;  -- ⚠️ 加表前缀
```

---

## #5 BM2 status 10 → PS status 3 的 CASE 映射

**场景**: BM2 有更多 status 值，统一硬件的 `PhysicalServerVO.state` 只有 3 态。
迁移时需做 N:1 映射。

**映射**:
```sql
CASE bm2.status
  WHEN 'HardwareInfoUnknown' THEN 'Connecting'
  WHEN 'IPxeBooting'         THEN 'Connecting'
  WHEN 'IPxeBootFailed'      THEN 'Connecting'
  WHEN 'WrongBootMode'       THEN 'Connecting'
  WHEN 'WrongArchitecture'   THEN 'Connecting'
  WHEN 'Available'           THEN 'Connecting'
  WHEN 'Allocated'           THEN 'Connecting'
  ELSE 'Connecting'  -- fallback
END
```

**注意**: 所有 BM2 status 当前都映射到 `Connecting` 是保守策略。后续 U-unit 如果
要细分需要同步修改此映射。

---

## #6 BM2 / PSPN enum coupling（必须同步扩展）

**场景**: `BareMetal2ProvisionNetworkState` 和 `ProvisionNetworkState`（新模型）
当前都是 `{Enabled, Disabled}`。因为有 VIEW/同步关系，**任何一方加值都必须同步加另一方**。

**检查点**: 在 `V5.5.18__schema.sql` 的 BM2 PN / PSPN 相关 CREATE/VIEW 段前后 grep：
```bash
grep -nE 'ProvisionNetworkState|BareMetal2ProvisionNetworkState' \
  header/ utils/ plugin/ premium/ conf/
```

**失败模式**: 若不同步，BM2 VIEW 读取时 **静默失败**（不抛异常，行数为 0）。

---

## #7 PSC seed ~1 tick stale（升级首个 heartbeat 前的分配会读到历史值）

**场景**: V5.5.18 Block 8 用 pre-migration HCV 值种 PSC。**升级完成后首个 heartbeat
到达前**，进来的 capacity 分配请求读的是历史值，可能和实际状态有 1 tick 的偏差。

**影响**: 极少数场景下首次分配会失败或过分配，下一个 heartbeat（默认 60s）自动纠正。

**Operator 处理**: 升级 5 min 内跑一次 `RecalculateHostCapacityMsg` 强制全部 host
重新上报，消除窗口期。命令见 [U29 rollback runbook](v5518-unified-hardware-rollback.md) §post-upgrade。

---

## #8 FK rename convention（审计锚点）

**约定**: FK constraint 名字必须跟 parent 表名一致。改 parent 名时同步改 FK 名。
详见 [ADR-008](../decisions/ADR-008-fk-rename-follows-parent.md)。

**检测 schema drift**:
```bash
# FK 名里的 parent 部分应与 REFERENCED_TABLE_NAME 一致
mysql zstack_test -e "
SELECT CONSTRAINT_NAME, TABLE_NAME, REFERENCED_TABLE_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE REFERENCED_TABLE_NAME IS NOT NULL
  AND CONSTRAINT_NAME NOT LIKE CONCAT('%', REFERENCED_TABLE_NAME, '%');
"
# 期望：空结果；非空 = FK 名与 parent 漂移了
```

**超 64 字符限制时**: 截断 child 部分（如 `BareMetal2` → `BM2`），parent 部分保留完整。
