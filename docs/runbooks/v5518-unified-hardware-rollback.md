# v5.5.18 Unified Hardware Rollback Runbook

**Audience:** on-call operator, release engineer.
**Scope:** rollback of the v5.5.18 unified hardware management migration (`V5.5.18__schema.sql` — consolidated from the previous U27 + U28 split). Applies whether the migration succeeded and later needs reverting, or failed mid-apply.
**Last updated:** 2026-04-23 (commit `70d93459f0`).

---

## 1. Decision: roll back vs forward-fix

Roll back when ALL of these are true:

1. The migration **has applied** (Flyway row exists for `5.5.18`) OR **failed mid-apply** and the DB is in a partially-migrated state that cannot be cleaned manually within the maintenance window.
2. Data loss risk is unacceptable (e.g., `PhysicalServerCapacityVO` row counts look wrong, `HostCapacityVO` VIEW returns zero rows, VM allocation is failing loudly).
3. A valid pre-upgrade full DB backup exists and is **younger than one working day**.

Forward-fix (do NOT roll back) when:

- The migration succeeded, MN is running, but a single write path has a bug that can be patched in Java without schema changes.
- The migration succeeded but a non-critical VIEW is returning wrong rows (patch the VIEW directly; see §5 for DDL templates).
- The DB is healthy and only a non-critical API (e.g., capacity panel) is slow — investigate `idx_role_uuid_type` usage first.
- The backup is older than one working day (forward-fix is safer than restoring stale state).

---

## 2. Pre-rollback checks (run before touching anything)

Capture evidence of the current state for the incident report, then verify the rollback path is viable.

### 2.1 Flyway state

```sql
SELECT version, description, type, success, installed_on, execution_time
FROM schema_version
ORDER BY installed_rank DESC LIMIT 5;
```

Expected outcomes:

| success | interpretation | rollback path |
|---|---|---|
| `1` for version `5.5.18` | migration succeeded; rolling back for correctness reason | §3 full-backup-restore |
| `0` for version `5.5.18` | migration failed; Flyway aborted | §3 full-backup-restore + `DELETE` failed row |
| no row for `5.5.18` | migration never started | no rollback needed |

### 2.2 Partial-apply detection

If the migration failed mid-apply, the DB has a hybrid schema. Identify the furthest point reached:

```sql
-- Check each schema artifact in dependency order. Earliest NO is the failure point.
SELECT 'ServerPoolVO exists' AS check_name,
       EXISTS (SELECT 1 FROM information_schema.TABLES
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ServerPoolVO') AS result
UNION ALL SELECT 'PhysicalServerVO exists', EXISTS (SELECT 1 FROM information_schema.TABLES
       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'PhysicalServerVO')
UNION ALL SELECT 'PhysicalServerRoleVO exists', EXISTS (SELECT 1 FROM information_schema.TABLES
       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'PhysicalServerRoleVO')
UNION ALL SELECT 'idx_role_uuid_type exists', EXISTS (SELECT 1 FROM information_schema.STATISTICS
       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'PhysicalServerRoleVO'
         AND INDEX_NAME = 'idx_role_uuid_type')
UNION ALL SELECT 'PhysicalServerCapacityVO exists', EXISTS (SELECT 1 FROM information_schema.TABLES
       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'PhysicalServerCapacityVO')
UNION ALL SELECT 'ClusterEO.serverPoolUuid exists', EXISTS (SELECT 1 FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ClusterEO'
         AND COLUMN_NAME = 'serverPoolUuid')
UNION ALL SELECT 'BareMetal2ProvisionNetworkVO is VIEW',
       (SELECT TABLE_TYPE FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'BareMetal2ProvisionNetworkVO') = 'VIEW'
UNION ALL SELECT 'PhysicalServerProvisionNetworkVO is BASE TABLE',
       (SELECT TABLE_TYPE FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'PhysicalServerProvisionNetworkVO') = 'BASE TABLE'
UNION ALL SELECT 'HostCapacityVO is VIEW',
       (SELECT TABLE_TYPE FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'HostCapacityVO') = 'VIEW';
```

Expected fully-migrated state: all `1`. Any `0` after a `1` means the migration stopped at that point.

### 2.3 Row-count evidence

Capture before rollback so the incident review can reconstruct the state:

```sql
SELECT 'ServerPool' AS t, COUNT(*) AS n FROM ServerPoolVO UNION ALL
SELECT 'PhysicalServer', COUNT(*) FROM PhysicalServerVO UNION ALL
SELECT 'PhysicalServerRole', COUNT(*) FROM PhysicalServerRoleVO UNION ALL
SELECT 'PhysicalServerCapacity', COUNT(*) FROM PhysicalServerCapacityVO UNION ALL
SELECT 'PoolRef', COUNT(*) FROM PhysicalServerProvisionNetworkPoolRefVO UNION ALL
SELECT 'HCV view rows', COUNT(*) FROM HostCapacityVO UNION ALL
SELECT 'BM2 PN view rows', COUNT(*) FROM BareMetal2ProvisionNetworkVO UNION ALL
SELECT 'BM2 CR view rows', COUNT(*) FROM BareMetal2ProvisionNetworkClusterRefVO;
```

Save this output to the incident ticket. If you get `ERROR 1356 ... references invalid table(s)` on any VIEW, see §5 DEFINER trap.

### 2.4 Backup freshness + coverage

```bash
# Inspect the most recent ZStack DB backup (path is site-specific; default under
# /opt/zstack-backup or /data/zstack-backup).
ls -lth /opt/zstack-backup/*.sql* 2>/dev/null | head -5

# Verify it contains the critical tables pre-v5.5.18 (HostCapacityVO as a BASE
# TABLE, BareMetal2ProvisionNetworkVO as a BASE TABLE, no PhysicalServerVO).
zcat /opt/zstack-backup/<latest>.sql.gz | grep -E '^(CREATE TABLE|INSERT INTO)' \
    | grep -E 'HostCapacityVO|BareMetal2ProvisionNetwork|PhysicalServerVO' | head -20
```

If the backup lacks `HostCapacityVO` as a BASE TABLE **or** already contains `PhysicalServerVO`, the backup was taken post-migration — you cannot roll back from it; escalate.

---

## 3. Rollback procedure

### 3.1 Stop the management node

```bash
zstack-ctl stop
systemctl stop zstack-management   # if systemd-managed
# Verify no management JVM is running:
pgrep -laf 'zstack-management|ManagementServer' || echo "MN stopped"
```

No MN write traffic may hit the DB during steps 3.2 – 3.4.

### 3.2 Quiesce + snapshot (safety net)

Take a **second** backup of the current (partially or fully migrated) state before overwriting. This protects you if something is wrong with the pre-upgrade backup.

```bash
mysqldump -u root -p<pw> --single-transaction --skip-triggers --no-tablespaces \
    zstack > /var/tmp/zstack-before-rollback-$(date +%Y%m%d%H%M).sql
gzip /var/tmp/zstack-before-rollback-*.sql
```

### 3.3 Restore the pre-upgrade backup

```bash
# DROP and recreate the database to clear all migrated state.
mysql -u root -p<pw> -e "DROP DATABASE zstack; CREATE DATABASE zstack CHARACTER SET utf8;"

# Restore from the validated pre-upgrade backup.
zcat /opt/zstack-backup/<pre-upgrade>.sql.gz | mysql -u root -p<pw> zstack
```

### 3.4 Fix Flyway schema_version

Delete the row Flyway wrote for `5.5.18`, so the next upgrade attempt (after fixing whatever failed) starts clean:

```sql
-- Check what's there:
SELECT * FROM schema_version WHERE version LIKE '5.5.18%';

-- Remove only the v5.5.18 entry (consolidated version and any legacy split).
DELETE FROM schema_version WHERE version IN ('5.5.18', '5.5.18.1', '5.5.18.2');
```

If you see historical rows for `5.5.18.1` / `5.5.18.2` in a backup that predates the consolidation, remove those too.

### 3.5 Restart MN

```bash
zstack-ctl start
# Wait for management startup log line:
tail -f /var/log/zstack/management-server.log | grep -m1 "Management node started"
```

---

## 4. Post-rollback verification

Run as `admin` via `zstack-cli` or REST API:

```bash
# 1. Host capacity reads: VIEW should be gone; reads go to the legacy table.
zstack-cli QueryHost fields=uuid,cpuUsedCapacity,memoryUsedCapacity \
    | jq '.inventories | length'   # must be > 0

# 2. BM2 provision network reads: the original table is back.
zstack-cli QueryBareMetal2ProvisionNetwork fields=uuid,name,state \
    | jq '.inventories | length'

# 3. VM allocation smoke: create + destroy a test VM to prove the capacity
#    read path works.
zstack-cli CreateVmInstance ... ; zstack-cli DestroyVmInstance ...
```

DB-level sanity:

```sql
SELECT TABLE_NAME, TABLE_TYPE
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('HostCapacityVO', 'BareMetal2ProvisionNetworkVO',
                     'BareMetal2ProvisionNetworkClusterRefVO',
                     'PhysicalServerVO', 'ServerPoolVO');
-- Expected: HostCapacityVO + the two BM2 tables are BASE TABLE;
-- PhysicalServerVO + ServerPoolVO are missing (no row returned for those).
```

If `HostCapacityVO.availableCpu` looks stale (MN wrote to `PhysicalServerCapacityVO` briefly before rollback and those writes are lost), force a recalculation:

```bash
# From the ZStack API console:
zstack-cli RecalculateHostCapacity  # admin API; hits every cluster
```

---

## 5. Known landmines (from U27/U28/consolidation)

These are the traps caught during migration development. Any re-apply attempt after rollback must plan for them.

### 5.1 DEFINER trap on mysqldump VIEWs

**Symptom:** on a host that restored a DB from `mysqldump`, querying any VIEW returns `ERROR 1356 ... references invalid table(s) or column(s) or function(s) or definer/invoker of view lack rights`.

**Cause:** `mysqldump` writes `DEFINER=<user>@<origin_host>` into VIEW DDL. If that user doesn't exist on the restore host, the VIEW refuses to execute.

**Fix:** the consolidated V5.5.18 migration already uses `SQL SECURITY INVOKER` on every VIEW it creates. If you're restoring a dump from production and adjusting on the fly, run:

```bash
sed 's|DEFINER=[^ ]*@[^ ]* |DEFINER=`root`@`localhost` |g;
     s|SQL SECURITY DEFINER|SQL SECURITY INVOKER|g' \
    dump.sql > dump-patched.sql
```

Apply the patched dump.

### 5.2 InnoDB FK blocks RENAME (errno 150)

**Symptom:** `ALTER TABLE ... RENAME TO ...` fails with `Error on rename of './zstack/foo' to './zstack/bar' (errno: 150 "Foreign key constraint is incorrectly formed")`.

**Cause:** the table has inbound FKs from other live tables. InnoDB refuses to rename until those FKs are dropped or re-targeted.

**Fix pattern used in V5.5.18:** drop inbound FKs → rename → re-add with new constraint names. See `conf/db/upgrade/V5.5.18__schema.sql` Stage 3 (BM2ProvisionNetworkVO → PhysicalServerProvisionNetworkVO).

### 5.3 `VALUES(table.column)` is not portable

**Symptom:** `ERROR 1064 (42000): You have an error in your SQL syntax` near `VALUES(`ResourceVO`.`resourceName`)` on MariaDB 10.3 / MySQL 8.x.

**Cause:** `VALUES()` inside `ON DUPLICATE KEY UPDATE` accepts only a bare column name, not a qualified reference.

**Fix:** always write `VALUES(\`column\`)`, never `VALUES(\`table\`.\`column\`)`. Qualification on the LHS (`table.column = VALUES(column)`) is fine.

### 5.4 `lastOpDate = lastOpDate` ambiguous in ODKU

**Symptom:** `ERROR 1052 (23000): Column 'lastOpDate' in UPDATE is ambiguous`.

**Cause:** `ON DUPLICATE KEY UPDATE lastOpDate = lastOpDate` is ambiguous when the source `SELECT` aliases a column of the same name.

**Fix:** qualify the target with its table name: `ServerPoolVO.lastOpDate = ServerPoolVO.lastOpDate`. This keeps the self-assignment idempotent (ON UPDATE CURRENT_TIMESTAMP does NOT fire for `X = X`) and resolves the ambiguity.

### 5.5 BM2 chassis status has 10 values, PhysicalServerStatus has 3

**Symptom:** after migration, querying a `PhysicalServerVO` for a BM2-origin row throws `IllegalArgumentException: No enum constant PhysicalServerStatus.HardwareInfoUnknown` (or similar) on Hibernate deserialisation.

**Cause:** `BareMetal2ChassisStatus` enum values `{HardwareInfoUnknown, IPxeBooting, IPxeBootFailed, WrongBootMode, WrongArchitecture, Available, Allocated}` are not members of `PhysicalServerStatus`.

**Fix (already in V5.5.18 Block 1b):** `CASE b.status WHEN 'Connected' THEN 'Connected' WHEN 'Disconnected' THEN 'Disconnected' ELSE 'Connecting' END`. The BM2-specific transient states collapse to `Connecting`; the underlying BM2 chassis row retains its original status unchanged.

### 5.6 Enum coupling between BM2 and PhysicalServer ProvisionNetworkState

**Latent trap (no active bug):** `BareMetal2ProvisionNetworkState` and `ProvisionNetworkState` currently share identical literals `{Enabled, Disabled}`. After consolidation, the unified table stores `state` as a string, and both enums are read through the same column (BM2 via the VIEW, PhysicalServer directly).

**Guardrail:** if either enum adds a value without the other adding the same value, BM2 reads may fail to deserialise. Any PR that modifies either enum MUST modify both — or retire the BM2 VO entirely.

### 5.7 PSC seed "~1 tick stale" window

**Symptom:** immediately after MN starts post-migration, capacity reads for KVM / container hosts may reflect backup values (captured at `HostCapacityVO` dump time) rather than real-time state.

**Cause:** Block 8 seeds `PhysicalServerCapacityVO` from `HostCapacityVO` pre-migration values. Those are stale until the first `HostCapacityUpdater` heartbeat or recalculation.

**Mitigation:** run `RecalculateHostCapacityMsg` (or admin API equivalent) against each cluster within the first 5 minutes after MN ready. For extended operator-paused upgrades, run it at cutover.

### 5.8 MD5 salt conventions (DB forensics)

If you need to trace a row back to its source entity:

| Derived UUID | Formula | Reverse lookup |
|---|---|---|
| `PhysicalServerVO.uuid` | `MD5(source_entity_uuid + '-ps')` | `SELECT roleUuid FROM PhysicalServerRoleVO WHERE serverUuid = ?` |
| `PhysicalServerRoleVO.uuid` | `MD5(source + '-role-{kvm\|bm2\|container}')` | — |
| `ServerPoolVO.uuid` (BM2) | `MD5(cluster_uuid + '-pool-bm2')` | `SELECT uuid FROM ClusterEO WHERE serverPoolUuid = ?` |
| `ServerPoolVO.uuid` (shared) | `MD5(zone_uuid + '-default-pool')` | `SELECT uuid FROM ZoneEO WHERE MD5(CONCAT(uuid, '-default-pool')) = ?` |

### 5.9 FK constraint rename convention

When `BareMetal2ProvisionNetworkVO` was renamed to `PhysicalServerProvisionNetworkVO`, all FK constraint names referencing the old parent were renamed accordingly:

| Old constraint | New constraint |
|---|---|
| `fkBareMetal2ProvisionNetworkVOZoneEO` | `fkPhysicalServerProvisionNetworkVOZoneEO` |
| `fkBareMetal2InstanceProvisionNicVONetworkVO` | `fkBareMetal2InstanceProvisionNicVOPSNetworkVO` |
| `fkBareMetal2GatewayProvisionNicVONetworkVO` | `fkBareMetal2GatewayProvisionNicVOPSNetworkVO` |

Note: the "PS" prefix on the parent portion signals PhysicalServerProvisionNetworkVO
as the new target. Longer forms spelling out the full parent name exceed MySQL's
64-char identifier limit.

Audit after migration:

```sql
SELECT CONSTRAINT_NAME, TABLE_NAME, REFERENCED_TABLE_NAME
FROM information_schema.REFERENTIAL_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND REFERENCED_TABLE_NAME IN ('PhysicalServerProvisionNetworkVO',
                                'BareMetal2ProvisionNetworkVO');
```

Only rows referencing `PhysicalServerProvisionNetworkVO` should appear. Any row referencing the old BM2 name is a leftover from a pre-consolidation state.

---

## 6. BM1 chassis explicitly out of scope

`BaremetalChassisVO` (the legacy V1 baremetal table) is **not** migrated into the unified model. Post-upgrade:

- `BaremetalChassisVO` rows are untouched in the DB
- They do NOT appear in `QueryPhysicalServerMsg` results
- They continue to use the pre-v5.5.18 capacity / power / allocation paths
- `BAREMETAL_V1` is not a valid `PhysicalServerRoleVO.roleType`

This is by design per the unified hardware architecture decision. Operators must plan BM1 → BM2 migration out-of-band if they want unified-model visibility.

---

## 7. Flyway schema_version quirks

### 7.1 Repair tool

If Flyway reports a checksum mismatch after you edit `V5.5.18__schema.sql` (e.g., for an emergency patch), use:

```bash
flyway -url=jdbc:mariadb://localhost:3306/zstack -user=root -password=<pw> repair
```

Repair rewrites the `schema_version` checksum column to match the current file content, without re-running the migration. Useful in dev; **never** run in production without manager approval.

### 7.2 Manual `schema_version` delete

If Flyway is irrecoverable and you need to force-reapply a version, the manual nuclear option is:

```sql
DELETE FROM schema_version WHERE version = '5.5.18';
```

Followed by `flyway migrate` (which re-runs V5.5.18 from scratch). The v5.5.18 consolidated script is **not idempotent at the DDL level** (RENAME / DROP TABLE will fail on an already-migrated DB). You'd need to restore from backup first (§3.3) then re-run — otherwise the DDL stops at the first conflict.

### 7.3 Multi-MN cluster coordination

When multiple MN nodes are upgrading simultaneously, Flyway's table lock (`schema_version_lock`) ensures only one node runs the migration. The others wait. Rollback must still stop **all** MNs (§3.1) — a lingering MN will write to the restored DB and re-create partial PhysicalServer state.

---

## Revision history

| Date | Commit | Change |
|---|---|---|
| 2026-04-23 | `70d93459f0` | Initial runbook, post-consolidation of U27+U28 into single V5.5.18 file |
