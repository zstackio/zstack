-- ============================================================================
-- v5.5.18 — Unified Hardware Management (Phase 1 DDL + Phase 2 Data Migration)
-- ============================================================================
-- Single-shot consolidated migration. Covers:
--   - Physical layer tables: ServerPool / PhysicalServer / Role / Capacity /
--     HardwareDetail / ProvisionNetworkPoolRef
--   - Cluster → ServerPool association (ClusterEO.serverPoolUuid)
--   - BareMetal2ProvisionNetwork absorbed into unified table via RENAME
--     (BareMetal2ProvisionNetworkVO becomes a VIEW for BM2 Java compat)
--   - BM2 child FKs rewired to point at the unified table with new names
--   - Existing inventory backfilled: PhysicalServerVO + Role + Resource +
--     Capacity rows synthesised from HostEO / BareMetal2ChassisVO / NativeHostVO
--   - vcenter ESXi capacity rows seeded directly (option-C half-migration)
--   - HostCapacityVO becomes an ALGORITHM=MERGE VIEW over PhysicalServerCapacityVO
--   - BareMetal2ProvisionNetworkClusterRefVO stays as a real table for v5.5.18
--     (Option A interim per ADR-013; full pool-only rewrite deferred to U23-U26)
--
-- Pre-upgrade requirement: full DB backup (operator-owned). No *_backup tables
-- are retained by this script; rollback relies on the pre-upgrade backup.
--
-- Admin account UUID hardcoded: 36c27e8ff05c4780bf6d2fa65700f22e (NB-15).
-- BM1 chassis (BaremetalChassisVO) are out of scope — not migrated.
--
-- Idempotency strategy: this is a Flyway versioned migration (single-run in
-- production). DDL is unguarded (fresh apply only). Data INSERTs use
-- ON DUPLICATE KEY UPDATE / INSERT IGNORE so the data-migration stages are
-- safe to retry from a failed mid-apply if the caller cleans up and reruns.

-- ============================================================================
-- STAGE 1: Baseline catchup (envs that skipped V5.4.0, e.g. 4.8.x upgrade line)
-- ============================================================================

CALL ADD_COLUMN('HostCapacityVO', 'cpuCoreNum', 'INT UNSIGNED', 0, '0');

-- Followup #25: persist K8s nodeInfo onto NativeHostVO so
-- ContainerNodeInfoDiscoveryAdapter can populate the full UnifiedHardwareInfo
-- surface (was 1/15 fields, becomes 7/15 after this — architecture from
-- HostAO + 6 nodeInfo columns added here). Mirrors the U6 transient-DTO
-- fields (KubernetesNodeInventory.systemUUID/machineID/capacity*/allocatable*).
--
-- Guarded by @has_native because NativeHostVO is created in V5.3.6 only when
-- the container plugin is installed; on envs without the container plugin the
-- table is absent and this ALTER must be a no-op (same idiom as Block 1c
-- below). All columns nullable: pre-followup rows have no nodeInfo data and
-- must remain valid until the next K8s sync re-populates them.
SET @has_native := (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = 'zstack' AND TABLE_NAME = 'NativeHostVO'
);
SET @sql := IF(@has_native = 1,
    'ALTER TABLE `NativeHostVO`
        ADD COLUMN `systemUUID` VARCHAR(64) DEFAULT NULL,
        ADD COLUMN `machineID` VARCHAR(64) DEFAULT NULL,
        ADD COLUMN `capacityCpu` BIGINT DEFAULT NULL,
        ADD COLUMN `capacityMemory` BIGINT DEFAULT NULL,
        ADD COLUMN `allocatableCpu` BIGINT DEFAULT NULL,
        ADD COLUMN `allocatableMemory` BIGINT DEFAULT NULL',
    'DO 0'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================================
-- STAGE 2: Physical-layer tables (ServerPool / PS / Role / HardwareDetail / Capacity)
-- ============================================================================

CREATE TABLE IF NOT EXISTS `ServerPoolVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `zoneUuid` VARCHAR(32) NOT NULL,
    `physicalLocation` VARCHAR(2048) DEFAULT NULL,
    `networkTopology` VARCHAR(2048) DEFAULT NULL,
    `state` VARCHAR(32) NOT NULL DEFAULT 'Enabled',
    `isDefault` tinyint(1) unsigned DEFAULT 0,
    `lastOpDate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` TIMESTAMP NOT NULL DEFAULT '2000-01-01 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkServerPoolVOZoneEO` FOREIGN KEY (`zoneUuid`)
        REFERENCES `ZoneEO` (`uuid`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `ClusterEO` ADD COLUMN `serverPoolUuid` VARCHAR(32) DEFAULT NULL;

-- Recreate the ClusterVO view to expose the new serverPoolUuid column.
-- Without this, JPA INSERT/SELECT on ClusterVO fails with "Unknown column 'serverPoolUuid'"
-- because the view (created in V0.6 / extended in V3.10.0.2) only projects pre-V5.5.18 columns.
DROP VIEW IF EXISTS `ClusterVO`;
CREATE VIEW `ClusterVO` AS SELECT uuid, zoneUuid, name, type, description, state, hypervisorType, createDate, lastOpDate, managementNodeId, architecture, serverPoolUuid FROM `ClusterEO` WHERE deleted IS NULL;

CREATE TABLE IF NOT EXISTS `PhysicalServerVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `zoneUuid` VARCHAR(32) NOT NULL,
    `poolUuid` VARCHAR(32) NOT NULL,
    `managementIp` VARCHAR(255) DEFAULT NULL,
    `architecture` VARCHAR(32) DEFAULT NULL,
    `serialNumber` VARCHAR(255) DEFAULT NULL,
    `manufacturer` VARCHAR(255) DEFAULT NULL,
    `model` VARCHAR(255) DEFAULT NULL,
    `state` VARCHAR(32) NOT NULL DEFAULT 'Enabled',
    `powerStatus` VARCHAR(32) NOT NULL DEFAULT 'POWER_UNKNOWN',
    `oobManagementType` VARCHAR(32) DEFAULT NULL,
    `oobAddress` VARCHAR(255) DEFAULT NULL,
    `oobPort` INT DEFAULT NULL,
    `oobUsername` VARCHAR(255) DEFAULT NULL,
    `oobPassword` VARCHAR(255) DEFAULT NULL,
    `lastOpDate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` TIMESTAMP NOT NULL DEFAULT '2000-01-01 00:00:00',
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `ukPhysicalServerZoneSerial` (`zoneUuid`, `serialNumber`),
    CONSTRAINT `fkPhysicalServerVOZoneEO` FOREIGN KEY (`zoneUuid`)
        REFERENCES `ZoneEO` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fkPhysicalServerVOServerPoolVO` FOREIGN KEY (`poolUuid`)
        REFERENCES `ServerPoolVO` (`uuid`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- idx_role_uuid_type is required by HostCapacityVO VIEW JOIN (AC-CM-PERF-01):
--   LEFT JOIN PhysicalServerRoleVO r ON r.roleUuid = h.uuid AND r.roleType = 'KVM_HOST'
-- UNIQUE(serverUuid, roleType) would not serve a leading-column lookup on roleUuid.
CREATE TABLE IF NOT EXISTS `PhysicalServerRoleVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `serverUuid` VARCHAR(32) NOT NULL,
    `roleType` VARCHAR(32) NOT NULL,
    `roleUuid` VARCHAR(32) DEFAULT NULL,
    `schedulingMode` VARCHAR(32) NOT NULL,
    `lastOpDate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` TIMESTAMP NOT NULL DEFAULT '2000-01-01 00:00:00',
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `ukPhysicalServerRole` (`serverUuid`, `roleType`),
    KEY `idx_role_uuid_type` (`roleUuid`, `roleType`),
    CONSTRAINT `fkPhysicalServerRoleVOPhysicalServerVO` FOREIGN KEY (`serverUuid`)
        REFERENCES `PhysicalServerVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `PhysicalServerHardwareDetailVO` (
    `id` BIGINT AUTO_INCREMENT,
    `serverUuid` VARCHAR(32) NOT NULL,
    `type` VARCHAR(32) NOT NULL,
    `itemModel` VARCHAR(255) DEFAULT NULL,
    `specification` VARCHAR(1024) DEFAULT NULL,
    `firmwareVersion` VARCHAR(255) DEFAULT NULL,
    `healthStatus` VARCHAR(255) DEFAULT NULL,
    `extraInfo` TEXT DEFAULT NULL,
    `lastOpDate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` TIMESTAMP NOT NULL DEFAULT '2000-01-01 00:00:00',
    PRIMARY KEY (`id`),
    KEY `idxHardwareDetailServerUuid` (`serverUuid`),
    CONSTRAINT `fkHardwareDetailVOPhysicalServerVO` FOREIGN KEY (`serverUuid`)
        REFERENCES `PhysicalServerVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- PhysicalServerHardwareInfoVO (U16 NB-19): unified flat-summary hardware-info row,
-- one per PhysicalServer. Sibling to PhysicalServerHardwareDetailVO (which holds
-- per-device rows). Populated by PhysicalServerHardwareService.discoverHardware()
-- via mergeNonNull from KVM SSH / BM2 IPMI FRU / Container kubelet adapters.
-- PK = serverUuid (1:1 with PhysicalServerVO), FK CASCADE: deleting a PS drops
-- its hardware summary atomically.
-- Column types match the JPA entity at header/.../PhysicalServerHardwareInfoVO.java
-- (bare @Column → VARCHAR(255) for strings, INT for Integer, BIGINT for Long,
-- TIMESTAMP for java.sql.Timestamp). Nullable on every non-PK column to support
-- discover-time mergeNonNull semantics (each adapter only sets fields it knows).
CREATE TABLE IF NOT EXISTS `PhysicalServerHardwareInfoVO` (
    `serverUuid` VARCHAR(32) NOT NULL,
    `manufacturer` VARCHAR(255) DEFAULT NULL,
    `model` VARCHAR(255) DEFAULT NULL,
    `serialNumber` VARCHAR(255) DEFAULT NULL,
    `biosVersion` VARCHAR(255) DEFAULT NULL,
    `cpuModel` VARCHAR(255) DEFAULT NULL,
    `cpuSockets` INT DEFAULT NULL,
    `cpuCores` INT DEFAULT NULL,
    `cpuArchitecture` VARCHAR(255) DEFAULT NULL,
    `totalMemoryBytes` BIGINT DEFAULT NULL,
    `memoryModuleCount` INT DEFAULT NULL,
    `totalDiskBytes` BIGINT DEFAULT NULL,
    `diskCount` INT DEFAULT NULL,
    `nicCount` INT DEFAULT NULL,
    `gpuCount` INT DEFAULT NULL,
    `healthStatus` VARCHAR(255) DEFAULT NULL,
    `discoverSource` VARCHAR(255) DEFAULT NULL,
    `lastDiscoverDate` TIMESTAMP NULL DEFAULT NULL,
    `lastOpDate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` TIMESTAMP NOT NULL DEFAULT '2000-01-01 00:00:00',
    PRIMARY KEY (`serverUuid`),
    CONSTRAINT `fkHardwareInfoVOPhysicalServerVO` FOREIGN KEY (`serverUuid`)
        REFERENCES `PhysicalServerVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- PhysicalServerCapacityVO: no FK to PhysicalServerVO because vcenter option-C
-- half-migration writes rows with uuid = ESXi host uuid without a matching
-- PhysicalServerVO row. Application-level cascade via PhysicalServerCascadeExtension.
-- Column types aligned with legacy HostCapacityVO production schema.
CREATE TABLE IF NOT EXISTS `PhysicalServerCapacityVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `totalMemory` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `totalCpu` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `cpuNum` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `cpuSockets` INT UNSIGNED NOT NULL DEFAULT 0,
    `cpuCoreNum` INT UNSIGNED NOT NULL DEFAULT 0,
    `availableMemory` BIGINT NOT NULL DEFAULT 0,
    `availableCpu` BIGINT NOT NULL DEFAULT 0,
    `totalPhysicalMemory` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `availablePhysicalMemory` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `cpuOverprovisioningRatio` FLOAT NOT NULL DEFAULT 1.0,
    `memoryOverprovisioningRatio` FLOAT NOT NULL DEFAULT 1.0,
    `reservedMemory` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `totalDisk` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `availableDisk` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `capacityState` VARCHAR(32) DEFAULT NULL,
    `lastOpDate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` TIMESTAMP NOT NULL DEFAULT '2000-01-01 00:00:00',
    PRIMARY KEY (`uuid`),
    KEY `idx_ps_cap_state` (`capacityState`),
    KEY `idx_ps_cap_avail_cpu` (`availableCpu`),
    KEY `idx_ps_cap_avail_memory` (`availableMemory`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ============================================================================
-- STAGE 3: BareMetal2ProvisionNetworkVO → PhysicalServerProvisionNetworkVO
--
-- In-place rename preserves all BM2 data and keeps the original
-- dhcpRangeNetworkCidr column. FK constraints are dropped then re-added with
-- renamed constraint names reflecting the new parent table.
-- ============================================================================

-- Drop inbound FKs to BM2ProvisionNetworkVO so RENAME doesn't hit errno 150.
ALTER TABLE `BareMetal2InstanceProvisionNicVO`
    DROP FOREIGN KEY `fkBareMetal2InstanceProvisionNicVONetworkVO`;

ALTER TABLE `BareMetal2GatewayProvisionNicVO`
    DROP FOREIGN KEY `fkBareMetal2GatewayProvisionNicVONetworkVO`;

-- ZSTAC-84191: previously dropped fkBareMetal2ProvisionNetworkVONetworkVO on
-- BareMetal2ProvisionNetworkClusterRefVO here in anticipation of converting the
-- ref table to a VIEW; STAGE 6 reversed that decision (ADR-013 keeps the table
-- real), but the DROP was left in place without a matching re-add. Result:
-- delete ProvisionNetwork no longer cascade-cleans ClusterRefVO at the DB
-- layer. We rely on RENAME TABLE auto-following the FK (so the original
-- fkBareMetal2ProvisionNetworkVONetworkVO now correctly references
-- PhysicalServerProvisionNetworkVO after the rename at line ~252 below).

-- Drop outbound FK on BM2PNVO so we can re-add it with a name matching the new
-- parent table. (Could be kept via auto-rename on RENAME TABLE, but user
-- directive "改名后 外键也要同步改" — we surface the rename in the constraint name.)
ALTER TABLE `BareMetal2ProvisionNetworkVO`
    DROP FOREIGN KEY `fkBareMetal2ProvisionNetworkVOZoneEO`;

-- Extend BM2PNVO with `type` column (will be the unified table's discriminator).
-- Default 'GATEWAY_PXE' matches BM2 semantics; additional provision types populate
-- different rows.
ALTER TABLE `BareMetal2ProvisionNetworkVO`
    ADD COLUMN `type` VARCHAR(32) NOT NULL DEFAULT 'GATEWAY_PXE' AFTER `zoneUuid`;

-- In-place rename — preserves all existing rows, indexes, and (conceptually)
-- the table as the unified parent.
RENAME TABLE `BareMetal2ProvisionNetworkVO` TO `PhysicalServerProvisionNetworkVO`;

-- Re-add outbound FK on the renamed table with new constraint name.
ALTER TABLE `PhysicalServerProvisionNetworkVO`
    ADD CONSTRAINT `fkPhysicalServerProvisionNetworkVOZoneEO`
    FOREIGN KEY (`zoneUuid`) REFERENCES `ZoneEO` (`uuid`) ON DELETE RESTRICT;

-- Re-attach the two remaining inbound FKs with names reflecting the new parent.
-- (BM2 ClusterRef FK is NOT re-added — that ref table is retired later in this
-- script and replaced by a VIEW over PoolRef.)
-- FK constraint names shortened to fit MySQL's 64-char identifier limit;
-- still carry the "PS" prefix on the parent portion to signal the renamed target.
ALTER TABLE `BareMetal2InstanceProvisionNicVO`
    ADD CONSTRAINT `fkBareMetal2InstanceProvisionNicVOPSNetworkVO`
    FOREIGN KEY (`networkUuid`) REFERENCES `PhysicalServerProvisionNetworkVO` (`uuid`)
    ON DELETE CASCADE;

ALTER TABLE `BareMetal2GatewayProvisionNicVO`
    ADD CONSTRAINT `fkBareMetal2GatewayProvisionNicVOPSNetworkVO`
    FOREIGN KEY (`networkUuid`) REFERENCES `PhysicalServerProvisionNetworkVO` (`uuid`)
    ON DELETE CASCADE;

-- VIEW keeps BM2 Java read/write paths working unchanged.
-- ALGORITHM=MERGE inlines the VIEW into caller WHERE filters;
-- SQL SECURITY INVOKER avoids the DEFINER=remote_host@...  1356 trap when the
-- DB is restored via mysqldump on a host where the dump user does not exist.
-- WITH CHECK OPTION: writes through the VIEW that don't satisfy type='GATEWAY_PXE'
-- fail loudly. BM2 Java VO has no `type` field, so INSERTs through the VIEW
-- omit `type` → the unified table's DEFAULT 'GATEWAY_PXE' satisfies CHECK OPTION.
--
-- GUARDRAIL: `BareMetal2ProvisionNetworkState` and `ProvisionNetworkState`
-- currently share identical literals {Enabled, Disabled}. Adding a value to
-- either enum without adding the same value to the other will silently corrupt
-- BM2 reads through this VIEW. Ownership transfers to a later Phase 2 Java
-- rewrite; any value-set change MUST update both enums or retire BM2PNVO.
CREATE OR REPLACE
    ALGORITHM = MERGE
    SQL SECURITY INVOKER
VIEW `BareMetal2ProvisionNetworkVO` AS
SELECT
    `uuid`, `name`, `description`, `zoneUuid`,
    `dhcpInterface`, `dhcpRangeStartIp`, `dhcpRangeEndIp`,
    `dhcpRangeNetmask`, `dhcpRangeGateway`, `dhcpRangeNetworkCidr`,
    `state`, `createDate`, `lastOpDate`
FROM `PhysicalServerProvisionNetworkVO`
WHERE `type` = 'GATEWAY_PXE'
WITH CHECK OPTION;

-- ============================================================================
-- STAGE 4: PoolRef table (now that PSPNVO exists as the FK target)
-- ============================================================================

CREATE TABLE IF NOT EXISTS `PhysicalServerProvisionNetworkPoolRefVO` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `networkUuid` VARCHAR(32) NOT NULL,
    `poolUuid` VARCHAR(32) NOT NULL,
    `lastOpDate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` TIMESTAMP NOT NULL DEFAULT '2000-01-01 00:00:00',
    PRIMARY KEY (`id`),
    UNIQUE KEY `ukPNPoolRef` (`networkUuid`, `poolUuid`),
    CONSTRAINT `fkPNPoolRefVONetwork` FOREIGN KEY (`networkUuid`)
        REFERENCES `PhysicalServerProvisionNetworkVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkPNPoolRefVOServerPool` FOREIGN KEY (`poolUuid`)
        REFERENCES `ServerPoolVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `PhysicalServerProvisionNetworkClusterRefVO` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `networkUuid` VARCHAR(32) NOT NULL,
    `clusterUuid` VARCHAR(32) NOT NULL,
    `lastOpDate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` TIMESTAMP NOT NULL DEFAULT '2000-01-01 00:00:00',
    PRIMARY KEY (`id`),
    UNIQUE KEY `ukPNClusterRef` (`networkUuid`, `clusterUuid`),
    CONSTRAINT `fkPNClusterRefVONetwork` FOREIGN KEY (`networkUuid`)
        REFERENCES `PhysicalServerProvisionNetworkVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkPNClusterRefVOCluster` FOREIGN KEY (`clusterUuid`)
        REFERENCES `ClusterEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ============================================================================
-- STAGE 5: Data migration
--
-- Audit log table created up-front so post-migration log inserts (end of this
-- stage) have a target. MigrationLogVO is a DB-only artifact for ops awareness
-- (NB-25): no JPA entity backs it, schema lives only in this Flyway script.
-- UNIQUE KEY on message gives idempotent INSERT IGNORE: re-running the
-- migration with unchanged source counts is a no-op; if counts change between
-- runs, the new message string differs and a new row is appended (an audit
-- trail of count drift). Keep VARCHAR(255) aligned with the unique key so
-- long-message prefix collisions cannot silently collapse distinct rows.
-- ============================================================================
CREATE TABLE IF NOT EXISTS `MigrationLogVO` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `message` VARCHAR(255) NOT NULL,
    `createDate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `ukMigrationLogMessage` (`message`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ============================================================================
-- STAGE 5: Data migration body
--
-- Source → target deterministic UUID derivation (per ADR-011):
--   PhysicalServerVO.uuid  = MD5(source.uuid + '-ps')        -- option (a)
--                                                               derivative-from-source
--   PhysicalServerRoleVO.uuid = MD5(source.uuid + '-role-{type}')
--   ServerPoolVO.uuid (BM2 cluster 1:1) = MD5(cluster.uuid + '-pool-bm2')
--   ServerPoolVO.uuid (zone shared)     = MD5(zone.uuid + '-default-pool')
-- Deterministic so rerun of data migration is idempotent.
--
-- Pool naming (AC-CB-09): BM2-bearing cluster pools = `bm2-pool-<uuid8>` (8-char
-- prefix of cluster uuid for operator readability without exposing full uuid);
-- zone-shared default pool = `default-pool`. Names appear in cloud_prd UI.
--
-- serialNumber extraction policy (AC-CB-Step0a/Step0b): ALL THREE blocks (1a/1b/1c)
-- leave serialNumber NULL at migration time. U16's PhysicalServerHardwareService
-- backfills via discover-time IPMI FRU / SSH dmidecode / kubelet node-info into
-- the new PhysicalServerHardwareInfoVO row (created above). Note the unique key
-- ukPhysicalServerZoneSerial(zoneUuid, serialNumber) tolerates multiple NULL
-- rows under MySQL's UNIQUE-NULL semantics. Pre-discovery, PhysicalServerVO
-- records have serialNumber=NULL; post-discovery, U16 populates EITHER the
-- PhysicalServerVO.serialNumber column OR the PhysicalServerHardwareInfoVO row
-- (per U16 design). Spec deviation from §U14 plan: BM2 LEFT JOIN
-- BareMetal2HardwareInfoVO is INFEASIBLE — that table does not exist
-- (BareMetal2ChassisVO has no serialNumber column; chassis-level serialNumber
-- only materialises post-discovery via BareMetal2ChassisHardwareInfoSyncer
-- writing into per-PCI/per-GPU device tables).
-- ============================================================================

-- Block 0a: one ServerPool per BM2-bearing cluster (NB-4 isolation).
INSERT INTO `ServerPoolVO`
    (`uuid`, `name`, `description`, `zoneUuid`, `state`, `createDate`, `lastOpDate`)
SELECT
    MD5(CONCAT(c.`uuid`, '-pool-bm2'))                AS `uuid`,
    CONCAT('bm2-pool-', SUBSTRING(c.`uuid`, 1, 8))    AS `name`,
    'auto-created for BM2 chassis (v5.5.18 migration)' AS `description`,
    c.`zoneUuid`                                      AS `zoneUuid`,
    'Enabled'                                         AS `state`,
    NOW()                                             AS `createDate`,
    NOW()                                             AS `lastOpDate`
FROM `ClusterEO` c
WHERE c.`deleted` IS NULL
  AND EXISTS (SELECT 1 FROM `BareMetal2ChassisVO` b WHERE b.`clusterUuid` = c.`uuid`)
ON DUPLICATE KEY UPDATE
    `ServerPoolVO`.`lastOpDate` = `ServerPoolVO`.`lastOpDate`;

UPDATE `ClusterEO` c
SET c.`serverPoolUuid` = MD5(CONCAT(c.`uuid`, '-pool-bm2'))
WHERE c.`deleted` IS NULL
  AND c.`serverPoolUuid` IS NULL
  AND EXISTS (SELECT 1 FROM `BareMetal2ChassisVO` b WHERE b.`clusterUuid` = c.`uuid`);

-- Block 0b: one shared ServerPool per zone (covers non-BM2 clusters).
INSERT INTO `ServerPoolVO`
    (`uuid`, `name`, `description`, `zoneUuid`, `state`, `isDefault`, `createDate`, `lastOpDate`)
SELECT
    MD5(CONCAT(z.`uuid`, '-default-pool'))            AS `uuid`,
    'default-pool'                                    AS `name`,
    'auto-created zone-shared pool (v5.5.18 migration)' AS `description`,
    z.`uuid`                                          AS `zoneUuid`,
    'Enabled'                                         AS `state`,
    1                                                 AS `isDefault`,
    NOW()                                             AS `createDate`,
    NOW()                                             AS `lastOpDate`
FROM `ZoneEO` z
WHERE z.`deleted` IS NULL
ON DUPLICATE KEY UPDATE
    `ServerPoolVO`.`lastOpDate` = `ServerPoolVO`.`lastOpDate`;

UPDATE `ClusterEO` c
SET c.`serverPoolUuid` = MD5(CONCAT(c.`zoneUuid`, '-default-pool'))
WHERE c.`deleted` IS NULL
  AND c.`serverPoolUuid` IS NULL;

-- Block 1a: PhysicalServerVO from KVM HostEO.
-- Blocks 1a/1b/1c silently skip source rows whose cluster has no serverPoolUuid
-- (should not happen: 0a/0b populate every live cluster; a soft-deleted cluster
-- with live hosts is an upstream data-integrity issue). Block 1.5's EXISTS
-- guard keeps Role rows consistent with skipped PS rows.
INSERT INTO `PhysicalServerVO`
    (`uuid`, `name`, `description`, `zoneUuid`, `poolUuid`, `managementIp`,
     `architecture`, `state`, `powerStatus`, `createDate`, `lastOpDate`)
SELECT
    MD5(CONCAT(h.`uuid`, '-ps')),
    h.`name`,
    CONCAT('migrated from KVM host ', h.`uuid`),
    h.`zoneUuid`,
    c.`serverPoolUuid`,
    h.`managementIp`,
    h.`architecture`,
    h.`state`,
    'POWER_UNKNOWN',
    h.`createDate`,
    h.`lastOpDate`
FROM `HostEO` h
JOIN `ClusterEO` c ON c.`uuid` = h.`clusterUuid` AND c.`deleted` IS NULL
WHERE h.`deleted` IS NULL
  AND h.`hypervisorType` = 'KVM'
  AND c.`serverPoolUuid` IS NOT NULL
ON DUPLICATE KEY UPDATE
    `PhysicalServerVO`.`lastOpDate` = `PhysicalServerVO`.`lastOpDate`;

-- Block 1b: PhysicalServerVO from BM2 chassis.
-- BareMetal2ChassisVO has no `deleted` column (cascade-release model);
-- physical row absence is the liveness signal.
-- LEFT JOIN BareMetal2IpmiChassisVO to backfill OOB credentials. BM2's IPMI
-- subtype rows live on the same uuid (JOINED inheritance via @PrimaryKeyJoinColumn);
-- non-IPMI chassis types yield NULL OOB columns. oobManagementType is hard-coded
-- 'IPMI' for matched rows because BareMetal2ChassisVO.chassisType='ipmi' (lowercase)
-- maps to PhysicalServerVO.oobManagementType='IPMI' (uppercase, validated by
-- @APIParam validValues in PhysicalServer Update API).
INSERT INTO `PhysicalServerVO`
    (`uuid`, `name`, `description`, `zoneUuid`, `poolUuid`, `managementIp`,
     `architecture`, `state`, `powerStatus`,
     `oobAddress`, `oobPort`, `oobUsername`, `oobPassword`, `oobManagementType`,
     `createDate`, `lastOpDate`)
SELECT
    MD5(CONCAT(b.`uuid`, '-ps')),
    b.`name`,
    CONCAT('migrated from BM2 chassis ', b.`uuid`),
    b.`zoneUuid`,
    c.`serverPoolUuid`,
    NULL,
    NULL,
    b.`state`,
    b.`powerStatus`,
    i.`ipmiAddress`,
    i.`ipmiPort`,
    i.`ipmiUsername`,
    i.`ipmiPassword`,
    IF(i.`uuid` IS NOT NULL, 'IPMI', NULL),
    b.`createDate`,
    b.`lastOpDate`
FROM `BareMetal2ChassisVO` b
JOIN `ClusterEO` c ON c.`uuid` = b.`clusterUuid` AND c.`deleted` IS NULL
LEFT JOIN `BareMetal2IpmiChassisVO` i ON i.`uuid` = b.`uuid`
WHERE c.`serverPoolUuid` IS NOT NULL
ON DUPLICATE KEY UPDATE
    `PhysicalServerVO`.`lastOpDate` = `PhysicalServerVO`.`lastOpDate`;

-- Block 1c: PhysicalServerVO from NativeHost (container host) via HostEO join.
-- NativeHostVO is created by Hibernate only when the container plugin is
-- installed. On envs without container (e.g. upgrades from pre-container
-- releases), the table is absent when Flyway runs. Guard the INSERT with a
-- prepared statement so the migration is safe on both deployment shapes.
-- No hypervisorType filter: NativeHostVO presence is the discriminator;
-- HostEO.hypervisorType can be any value set by the container plugin.
SET @has_native := (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = 'zstack' AND TABLE_NAME = 'NativeHostVO'
);
SET @sql := IF(@has_native = 1,
    'INSERT INTO `PhysicalServerVO` (`uuid`, `name`, `description`, `zoneUuid`, `poolUuid`, `managementIp`, `architecture`, `state`, `powerStatus`, `createDate`, `lastOpDate`) SELECT MD5(CONCAT(h.`uuid`, ''-ps'')), h.`name`, CONCAT(''migrated from NativeHost '', h.`uuid`), h.`zoneUuid`, c.`serverPoolUuid`, h.`managementIp`, h.`architecture`, h.`state`, ''POWER_UNKNOWN'', h.`createDate`, h.`lastOpDate` FROM `HostEO` h JOIN `NativeHostVO` n ON n.`uuid` = h.`uuid` JOIN `ClusterEO` c ON c.`uuid` = h.`clusterUuid` AND c.`deleted` IS NULL WHERE h.`deleted` IS NULL AND c.`serverPoolUuid` IS NOT NULL ON DUPLICATE KEY UPDATE `PhysicalServerVO`.`lastOpDate` = `PhysicalServerVO`.`lastOpDate`',
    'DO 0'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Block 1.5: PhysicalServerRoleVO (KVM_HOST INTERNAL_SHARED, BAREMETAL_V2
-- INTERNAL_EXCLUSIVE per AC-V2-ROLE-09, CONTAINER_HOST INTERNAL_SHARED).
-- roleUuid = raw source entity uuid for reverse lookup; serverUuid = MD5-derived.

INSERT INTO `PhysicalServerRoleVO`
    (`uuid`, `serverUuid`, `roleType`, `roleUuid`, `schedulingMode`,
     `createDate`, `lastOpDate`)
SELECT
    MD5(CONCAT(h.`uuid`, '-role-kvm')),
    MD5(CONCAT(h.`uuid`, '-ps')),
    'KVM_HOST',
    h.`uuid`,
    'INTERNAL_SHARED',
    h.`createDate`,
    h.`lastOpDate`
FROM `HostEO` h
WHERE h.`deleted` IS NULL
  AND h.`hypervisorType` = 'KVM'
  AND EXISTS (SELECT 1 FROM `PhysicalServerVO` p WHERE p.`uuid` = MD5(CONCAT(h.`uuid`, '-ps')))
ON DUPLICATE KEY UPDATE
    `PhysicalServerRoleVO`.`lastOpDate` = `PhysicalServerRoleVO`.`lastOpDate`;

INSERT INTO `PhysicalServerRoleVO`
    (`uuid`, `serverUuid`, `roleType`, `roleUuid`, `schedulingMode`,
     `createDate`, `lastOpDate`)
SELECT
    MD5(CONCAT(b.`uuid`, '-role-bm2')),
    MD5(CONCAT(b.`uuid`, '-ps')),
    'BAREMETAL_V2',
    b.`uuid`,
    'INTERNAL_EXCLUSIVE',
    b.`createDate`,
    b.`lastOpDate`
FROM `BareMetal2ChassisVO` b
WHERE EXISTS (SELECT 1 FROM `PhysicalServerVO` p WHERE p.`uuid` = MD5(CONCAT(b.`uuid`, '-ps')))
ON DUPLICATE KEY UPDATE
    `PhysicalServerRoleVO`.`lastOpDate` = `PhysicalServerRoleVO`.`lastOpDate`;

-- CONTAINER_HOST role — guarded by the same NativeHostVO existence check as
-- Block 1c. @has_native is re-evaluated here for locality (user variable
-- scope is session-wide, but re-reading keeps the two blocks independently
-- portable if someone rearranges).
SET @has_native := (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = 'zstack' AND TABLE_NAME = 'NativeHostVO'
);
SET @sql := IF(@has_native = 1,
    'INSERT INTO `PhysicalServerRoleVO` (`uuid`, `serverUuid`, `roleType`, `roleUuid`, `schedulingMode`, `createDate`, `lastOpDate`) SELECT MD5(CONCAT(h.`uuid`, ''-role-container'')), MD5(CONCAT(h.`uuid`, ''-ps'')), ''CONTAINER_HOST'', h.`uuid`, ''INTERNAL_SHARED'', h.`createDate`, h.`lastOpDate` FROM `HostEO` h JOIN `NativeHostVO` n ON n.`uuid` = h.`uuid` WHERE h.`deleted` IS NULL AND EXISTS (SELECT 1 FROM `PhysicalServerVO` p WHERE p.`uuid` = MD5(CONCAT(h.`uuid`, ''-ps''))) ON DUPLICATE KEY UPDATE `PhysicalServerRoleVO`.`lastOpDate` = `PhysicalServerRoleVO`.`lastOpDate`',
    'DO 0'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Block 1.6: ResourceVO parent registration for JOINED inheritance children.
-- PhysicalServerVO / ServerPoolVO / PhysicalServerRoleVO all extend ResourceVO;
-- production code reaches them via dbf.persist (Hibernate writes parent then
-- child atomically), but manual INSERT into the child table here bypasses
-- that, so we must seed the parent ResourceVO row ourselves. Without this,
-- @Entity JPQL queries (e.g. /v1/server-pools, /v1/physical-server-roles)
-- return empty inventories even though child rows are present.
--
-- AccountResourceRefVO insert intentionally omitted: all four APIs are
-- @Action(adminOnly=true), and admin queries do not filter through
-- AccountResourceRefVO. Verified empirically on .83 (2026-05-07): deleting
-- pre-existing ARR rows for these resourceTypes left admin queries fully
-- functional.
INSERT INTO `ResourceVO`
    (`uuid`, `resourceName`, `resourceType`, `concreteResourceType`)
SELECT
    p.`uuid`,
    p.`name`,
    'PhysicalServerVO',
    'org.zstack.header.server.PhysicalServerVO'
FROM `PhysicalServerVO` p
ON DUPLICATE KEY UPDATE
    `ResourceVO`.`resourceName` = VALUES(`resourceName`);

INSERT INTO `ResourceVO`
    (`uuid`, `resourceName`, `resourceType`, `concreteResourceType`)
SELECT
    p.`uuid`,
    p.`name`,
    'ServerPoolVO',
    'org.zstack.header.server.ServerPoolVO'
FROM `ServerPoolVO` p
ON DUPLICATE KEY UPDATE
    `ResourceVO`.`resourceName` = VALUES(`resourceName`);

-- PhysicalServerRoleVO has no name column; synthesize a stable resourceName
-- from a uuid prefix so admin UI list views still render something readable.
INSERT INTO `ResourceVO`
    (`uuid`, `resourceName`, `resourceType`, `concreteResourceType`)
SELECT
    r.`uuid`,
    CONCAT('role-', SUBSTRING(r.`uuid`, 1, 8)),
    'PhysicalServerRoleVO',
    'org.zstack.header.server.PhysicalServerRoleVO'
FROM `PhysicalServerRoleVO` r
ON DUPLICATE KEY UPDATE
    `ResourceVO`.`resourceName` = VALUES(`resourceName`);

-- Block 8: PhysicalServerCapacityVO from HostCapacityVO (still a table at this
-- point — VIEW-ization happens at Stage 7). Two branches:
--   - vcenter ESXi: uuid = ESXHostVO.uuid (NOT MD5-salted). Feeds the HCV VIEW
--     COALESCE fallback for hosts lacking a RoleVO (option-C half-migration).
--   - KVM / NativeHost: uuid = MD5(source_uuid + '-ps'). Seeds PSC so the first
--     post-cutover capacity read returns non-zero; subsequent HostCapacityUpdater
--     writes keep it current.
INSERT INTO `PhysicalServerCapacityVO`
    (`uuid`, `totalMemory`, `totalCpu`, `cpuNum`, `cpuSockets`, `cpuCoreNum`,
     `availableMemory`, `availableCpu`, `totalPhysicalMemory`,
     `availablePhysicalMemory`, `cpuOverprovisioningRatio`,
     `memoryOverprovisioningRatio`, `reservedMemory`, `totalDisk`,
     `availableDisk`, `capacityState`, `createDate`, `lastOpDate`)
SELECT
    hc.`uuid`,
    hc.`totalMemory`, hc.`totalCpu`, hc.`cpuNum`, hc.`cpuSockets`, hc.`cpuCoreNum`,
    hc.`availableMemory`, hc.`availableCpu`,
    hc.`totalPhysicalMemory`, hc.`availablePhysicalMemory`,
    1.0, 1.0, 0, 0, 0, 'Ready',
    NOW(), NOW()
FROM `HostCapacityVO` hc
JOIN `ESXHostVO` e ON e.`uuid` = hc.`uuid`
ON DUPLICATE KEY UPDATE
    `PhysicalServerCapacityVO`.`totalMemory` = VALUES(`totalMemory`),
    `PhysicalServerCapacityVO`.`totalCpu` = VALUES(`totalCpu`),
    `PhysicalServerCapacityVO`.`cpuNum` = VALUES(`cpuNum`),
    `PhysicalServerCapacityVO`.`cpuSockets` = VALUES(`cpuSockets`),
    `PhysicalServerCapacityVO`.`cpuCoreNum` = VALUES(`cpuCoreNum`),
    `PhysicalServerCapacityVO`.`availableMemory` = VALUES(`availableMemory`),
    `PhysicalServerCapacityVO`.`availableCpu` = VALUES(`availableCpu`),
    `PhysicalServerCapacityVO`.`totalPhysicalMemory` = VALUES(`totalPhysicalMemory`),
    `PhysicalServerCapacityVO`.`availablePhysicalMemory` = VALUES(`availablePhysicalMemory`),
    `PhysicalServerCapacityVO`.`lastOpDate` = `PhysicalServerCapacityVO`.`lastOpDate`;

INSERT INTO `PhysicalServerCapacityVO`
    (`uuid`, `totalMemory`, `totalCpu`, `cpuNum`, `cpuSockets`, `cpuCoreNum`,
     `availableMemory`, `availableCpu`, `totalPhysicalMemory`,
     `availablePhysicalMemory`, `cpuOverprovisioningRatio`,
     `memoryOverprovisioningRatio`, `reservedMemory`, `totalDisk`,
     `availableDisk`, `capacityState`, `createDate`, `lastOpDate`)
SELECT
    MD5(CONCAT(hc.`uuid`, '-ps')),
    hc.`totalMemory`, hc.`totalCpu`, hc.`cpuNum`, hc.`cpuSockets`, hc.`cpuCoreNum`,
    hc.`availableMemory`, hc.`availableCpu`,
    hc.`totalPhysicalMemory`, hc.`availablePhysicalMemory`,
    1.0, 1.0, 0, 0, 0, 'Ready',
    NOW(), NOW()
FROM `HostCapacityVO` hc
JOIN `PhysicalServerVO` p ON p.`uuid` = MD5(CONCAT(hc.`uuid`, '-ps'))
ON DUPLICATE KEY UPDATE
    `PhysicalServerCapacityVO`.`totalMemory` = VALUES(`totalMemory`),
    `PhysicalServerCapacityVO`.`totalCpu` = VALUES(`totalCpu`),
    `PhysicalServerCapacityVO`.`cpuNum` = VALUES(`cpuNum`),
    `PhysicalServerCapacityVO`.`cpuSockets` = VALUES(`cpuSockets`),
    `PhysicalServerCapacityVO`.`cpuCoreNum` = VALUES(`cpuCoreNum`),
    `PhysicalServerCapacityVO`.`availableMemory` = VALUES(`availableMemory`),
    `PhysicalServerCapacityVO`.`availableCpu` = VALUES(`availableCpu`),
    `PhysicalServerCapacityVO`.`totalPhysicalMemory` = VALUES(`totalPhysicalMemory`),
    `PhysicalServerCapacityVO`.`availablePhysicalMemory` = VALUES(`availablePhysicalMemory`),
    `PhysicalServerCapacityVO`.`lastOpDate` = `PhysicalServerCapacityVO`.`lastOpDate`;

-- Block B1: PoolRef from BM2 ClusterRef history (via ClusterEO.serverPoolUuid).
-- DISTINCT dedupes when multiple clusters sharing the same pool both attached
-- the same network; UNIQUE(networkUuid, poolUuid) + INSERT IGNORE enforces
-- idempotency. Clusters whose serverPoolUuid is still NULL are skipped.
INSERT IGNORE INTO `PhysicalServerProvisionNetworkPoolRefVO`
    (`networkUuid`, `poolUuid`, `createDate`, `lastOpDate`)
SELECT DISTINCT
    ref.`networkUuid`,
    c.`serverPoolUuid`,
    ref.`createDate`,
    ref.`lastOpDate`
FROM `BareMetal2ProvisionNetworkClusterRefVO` ref
JOIN `ClusterEO` c ON c.`uuid` = ref.`clusterUuid` AND c.`deleted` IS NULL
WHERE c.`serverPoolUuid` IS NOT NULL;

-- ============================================================================
-- STAGE 5b: Migration audit log (M18 / NB-25)
--
-- Two ops-facing audit rows: BM V1 chassis count (skipped per ADR-010) and
-- vcenter ESXi rows that received PSC seeding (Block 8 first SELECT). The
-- counts are computed against the post-migration state, so rerunning yields
-- the same message string until the source data changes.
--
-- INSERT IGNORE + UNIQUE(message) is the idempotency construct: identical
-- repeat run → row already exists → no-op; count changes between runs →
-- different message string → new row, preserving an audit trail.
--
-- BM V1 chassis are NOT migrated to PhysicalServerVO (ADR-010). The log row
-- records the count for ops-team visibility — operators upgrading from a
-- BM1-using deployment must know the chassis are intentionally left in
-- BaremetalChassisVO and excluded from the unified hardware view.
-- ============================================================================

SELECT COUNT(*) INTO @bmv1_cnt FROM `BaremetalChassisVO`;
INSERT IGNORE INTO `MigrationLogVO` (`message`)
    VALUES (CONCAT('BM V1 chassis count: ', @bmv1_cnt, ', skipped per ADR-010'));

-- vcenter ESXi count: rows in PhysicalServerCapacityVO whose uuid matches an
-- ESXHostVO row (Block 8 first SELECT path: HostCapacityVO JOIN ESXHostVO).
-- Counting against the post-migration target (PSC) rather than the source
-- (HostCapacityVO, which is dropped at STAGE 7) gives a stable, post-migration-
-- observable number. On envs with no vcenter integration, ESXHostVO is empty
-- and the count is 0 — acceptable and recorded.
SELECT COUNT(*) INTO @vc_esxi_cnt
FROM `PhysicalServerCapacityVO` c
JOIN `ESXHostVO` e ON e.`uuid` = c.`uuid`;
INSERT IGNORE INTO `MigrationLogVO` (`message`)
    VALUES (CONCAT('vcenter ESXi hosts migrated: ', @vc_esxi_cnt, ' rows'));

-- ============================================================================
-- STAGE 6: BM2 ClusterRef stays as real table (Option A interim per ADR-013)
--
-- Earlier drafts of this migration converted BareMetal2ProvisionNetworkClusterRefVO
-- into a join VIEW over PoolRef JOIN ClusterEO. The VIEW filter required
-- ClusterEO.serverPoolUuid IS NOT NULL, but BM2 clusters are born pool-less
-- (BareMetal2ClusterFactory.createCluster does not assign a pool, and the
-- attach-network-to-cluster API never enforced one). The VIEW therefore
-- silently dropped freshly-created BM2 clusters from view, breaking both the
-- Bm2RoleProviderIntegrationCase attach path (DML on VIEW → MySQL 1394) and
-- 16 production read sites that look up (networkUuid, clusterUuid) tuples.
--
-- Block B1 above still backfills PoolRef so the open-source PSPNVO PoolRef
-- path is populated; BM2 reads/writes continue against the existing table.
-- The full pool-only rewrite (Phase 2 PRD U23-U26) supersedes this once the
-- API contract change is staged.
-- ============================================================================

-- ============================================================================
-- STAGE 7: HostCapacityVO table → MERGE VIEW over PhysicalServerCapacityVO
--
-- Data already migrated by Block 8. Drop legacy FK + source table (operator
-- backup handles rollback). MERGE inlines the VIEW into caller WHERE filters;
-- COALESCE(r.serverUuid, h.uuid) covers both KVM-host-with-RoleVO path and
-- vcenter-ESXi-no-RoleVO fallback (option-C half-migration).
-- ============================================================================

ALTER TABLE `HostCapacityVO` DROP FOREIGN KEY `fkHostCapacityVOHostEO`;
DROP TABLE `HostCapacityVO`;

CREATE OR REPLACE
    ALGORITHM = MERGE
    SQL SECURITY INVOKER
VIEW `HostCapacityVO` AS
SELECT
    h.`uuid`                       AS `uuid`,
    c.`totalMemory`                AS `totalMemory`,
    c.`totalCpu`                   AS `totalCpu`,
    c.`cpuNum`                     AS `cpuNum`,
    c.`cpuSockets`                 AS `cpuSockets`,
    c.`cpuCoreNum`                 AS `cpuCoreNum`,
    c.`availableMemory`            AS `availableMemory`,
    c.`availableCpu`               AS `availableCpu`,
    c.`totalPhysicalMemory`        AS `totalPhysicalMemory`,
    c.`availablePhysicalMemory`    AS `availablePhysicalMemory`
FROM `HostVO` h
LEFT JOIN `PhysicalServerRoleVO` r
    ON r.`roleUuid` = h.`uuid` AND r.`roleType` = 'KVM_HOST'
JOIN `PhysicalServerCapacityVO` c
    ON c.`uuid` = COALESCE(r.`serverUuid`, h.`uuid`);
