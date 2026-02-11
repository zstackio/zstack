ALTER TABLE `GuestVmScriptExecutedRecordDetailVO`
    MODIFY `stdout` MEDIUMTEXT CHARACTER SET `utf8mb4` COLLATE `utf8mb4_unicode_ci`;

ALTER TABLE `GuestVmScriptExecutedRecordDetailVO`
    MODIFY `stderr` MEDIUMTEXT CHARACTER SET `utf8mb4` COLLATE `utf8mb4_unicode_ci`;

CALL ADD_COLUMN('PciDeviceVO', 'vmPciDeviceAddress', 'varchar(32)', 1, NULL);
CALL ADD_COLUMN('MdevDeviceVO', 'mdevDeviceAddress', 'varchar(32)', 1, NULL);

CALL ADD_COLUMN('VolumeBackupVO', 'hypervisorType', 'varchar(255)', 0, 'kvm');
ALTER TABLE `zstack`.`VolumeBackupHistoryVO` modify column bitmap varchar(32) DEFAULT NULL;

-- Add index for modelId field to support duplicate checking
-- Use CREATE_INDEX procedure (defined in beforeMigrate.sql) for idempotent operation
CALL CREATE_INDEX('ModelVO', 'idx_modelId', 'modelId');

-- Upgrade existing data: set modelId to uuid if modelId is NULL
-- This UPDATE is idempotent: WHERE clause ensures only NULL values are updated
UPDATE ModelVO SET modelId = uuid WHERE modelId IS NULL;


DELIMITER $$

CREATE PROCEDURE UpdateBareMetal2InstanceProvisionNicVO()
BEGIN
    DECLARE instanceUuid_exists INT;
    DECLARE isPrimaryProvisionNic_exists_in_ProvisionNicVO INT;
    DECLARE isPrimaryProvisionNic_exists_in_ChassisNicVO INT;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'An error occurred during the update process.';
    END;

    START TRANSACTION;

    SELECT COUNT(*)
        INTO instanceUuid_exists
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'zstack'
          AND TABLE_NAME = 'BareMetal2InstanceProvisionNicVO'
          AND COLUMN_NAME = 'instanceUuid';

    IF instanceUuid_exists = 0 THEN
        CALL ADD_COLUMN('BareMetal2InstanceProvisionNicVO', 'instanceUuid', 'VARCHAR(32)', FALSE, '');

        UPDATE `zstack`.`BareMetal2InstanceProvisionNicVO`
        SET `instanceUuid` = `uuid`;

        ALTER TABLE `zstack`.`BareMetal2InstanceProvisionNicVO`
        DROP FOREIGN KEY `fkBareMetal2InstanceProvisionNicVOInstanceVO`;

        CALL ADD_CONSTRAINT('BareMetal2InstanceProvisionNicVO', 'fkBareMetal2InstanceProvisionNicVOInstanceVO',
                                    'instanceUuid', 'BareMetal2InstanceVO', 'uuid', 'CASCADE');

        UPDATE `zstack`.`BareMetal2InstanceProvisionNicVO`
        SET `uuid` = REPLACE(UUID(), '-', '');
    END IF;

    SELECT COUNT(*)
        INTO isPrimaryProvisionNic_exists_in_ProvisionNicVO
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'zstack'
          AND TABLE_NAME = 'BareMetal2InstanceProvisionNicVO'
          AND COLUMN_NAME = 'isPrimaryProvisionNic';

    IF isPrimaryProvisionNic_exists_in_ProvisionNicVO = 0 THEN
        CALL ADD_COLUMN('BareMetal2InstanceProvisionNicVO', 'isPrimaryProvisionNic', 'BOOLEAN', FALSE, FALSE);

        UPDATE `zstack`.`BareMetal2InstanceProvisionNicVO`
        SET `isPrimaryProvisionNic` = TRUE;
    END IF;

    SELECT COUNT(*)
        INTO isPrimaryProvisionNic_exists_in_ChassisNicVO
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'zstack'
          AND TABLE_NAME = 'BareMetal2ChassisNicVO'
          AND COLUMN_NAME = 'isPrimaryProvisionNic';

    IF isPrimaryProvisionNic_exists_in_ChassisNicVO = 0 THEN
        CALL ADD_COLUMN('BareMetal2ChassisNicVO', 'isPrimaryProvisionNic', 'BOOLEAN', FALSE, FALSE);

        UPDATE `zstack`.`BareMetal2ChassisNicVO`
        SET `isPrimaryProvisionNic` = TRUE
        WHERE `isProvisionNic` = TRUE;
    END IF;

    COMMIT;
END$$

DELIMITER ;
CALL UpdateBareMetal2InstanceProvisionNicVO();
DROP PROCEDURE IF EXISTS UpdateBareMetal2InstanceProvisionNicVO;

-- ZSTAC-81511 Host allocation restore the default
DELETE FROM `zstack`.`SystemTagVO`
WHERE `resourceType` = 'InstanceOfferingVO'
  AND (tag LIKE 'minimumCPUUsageHostAllocatorStrategyMode::%'
    OR tag LIKE 'minimumMemoryUsageHostAllocatorStrategyMode::%');

-- Fix PodGpuStatsVO unit: GpuDeviceVO.memory stores bytes, convert to MB
CREATE OR REPLACE VIEW PodGpuStatsVO AS
SELECT
    p.uuid AS podUuid,
    COUNT(g.uuid) AS gpuCount,
    COALESCE(CAST(ROUND(AVG(g.memory) / 1048576) AS SIGNED), 0) AS avgAllocatedMb,
    COALESCE(CAST(ROUND(SUM(g.memory) / 1048576) AS SIGNED), 0) AS totalGpuMemMb
FROM PodVO p
    LEFT JOIN PciDeviceVO pci ON pci.vmInstanceUuid = p.uuid
    LEFT JOIN GpuDeviceVO g ON g.uuid = pci.uuid
GROUP BY p.uuid;

ALTER TABLE `zstack`.`AccessControlListEntryVO` ADD COLUMN `redirectPort` int(10) DEFAULT NULL;

CALL ADD_COLUMN('GpuDeviceSpecVO', 'allocatorStrategy', 'varchar(32)', 1, NULL);
CALL ADD_COLUMN('MdevDeviceSpecVO', 'allocatorStrategy', 'varchar(32)', 1, NULL);

UPDATE `zstack`.`GpuDeviceSpecVO` SET `allocatorStrategy` = 'FollowGlobal' WHERE `allocatorStrategy` IS NULL;
UPDATE `zstack`.`MdevDeviceSpecVO` SET `allocatorStrategy` = 'FollowGlobal' WHERE `allocatorStrategy` IS NULL;

-- Add version column to ModelServiceVO (entity field was added without schema migration)
CALL ADD_COLUMN('ModelServiceVO', 'version', 'VARCHAR(255)', 1, NULL);

-- Add packageVersion column to ApplicationDevelopmentServiceVO (ZSTAC-81309)
CALL ADD_COLUMN('ApplicationDevelopmentServiceVO', 'packageVersion', 'VARCHAR(255)', 1, NULL);

-- Upgrade application development service version to readable format (e.g. dify:xxx -> 1.11.1 from installPath)
-- ModelServiceVO.version: extract x.y.z from installPath last segment (e.g. dify_1.11.1 -> 1.11.1)
-- ApplicationDevelopmentServiceVO.packageVersion: sync from ModelServiceVO.version when unreadable or null
DROP PROCEDURE IF EXISTS UpgradeApplicationDevelopmentServiceVersion;

DELIMITER $$

CREATE PROCEDURE UpgradeApplicationDevelopmentServiceVersion()
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'UpgradeApplicationDevelopmentServiceVersion failed.';
    END;

    START TRANSACTION;

    -- 1. ModelServiceVO: set version to readable (x.y.z) extracted from installPath for App type with unreadable version
    UPDATE `zstack`.`ModelServiceVO`
    SET `version` = SUBSTRING_INDEX(
            SUBSTRING_INDEX(
                TRIM(TRAILING '/' FROM REPLACE(REPLACE(`installPath`, 'file://', ''), 'file:', '')),
                '/', -1
            ), '_', -1
        )
    WHERE `type` = 'App'
      AND (`version` LIKE '%:%' OR `version` IS NULL OR `version` = '')
      AND `installPath` IS NOT NULL AND TRIM(`installPath`) != ''
      AND SUBSTRING_INDEX(
              SUBSTRING_INDEX(
                  TRIM(TRAILING '/' FROM REPLACE(REPLACE(`installPath`, 'file://', ''), 'file:', '')),
                  '/', -1
              ), '_', -1
          ) REGEXP '^[0-9]+\\.[0-9]+';

    -- 2. ApplicationDevelopmentServiceVO.packageVersion: sync from linked ModelServiceVO.version (join on uuid - JPA TABLE_PER_CLASS inheritance)
    UPDATE `zstack`.`ApplicationDevelopmentServiceVO` a
    INNER JOIN `zstack`.`ModelServiceInstanceGroupVO` g ON a.`uuid` = g.`uuid`
    INNER JOIN `zstack`.`ModelServiceVO` m ON g.`modelServiceUuid` = m.`uuid`
    SET a.`packageVersion` = m.`version`
    WHERE (a.`packageVersion` IS NULL OR a.`packageVersion` LIKE '%:%')
      AND m.`version` IS NOT NULL AND TRIM(m.`version`) != '';

    COMMIT;
END$$

DELIMITER ;
CALL UpgradeApplicationDevelopmentServiceVersion();
DROP PROCEDURE IF EXISTS UpgradeApplicationDevelopmentServiceVersion;

-- ZSTAC-82069: Clean up orphan GpuDeviceSpecVO records where parent spec type is not GPU
-- Root cause: ZSTAC-81489 fixed GPU detection on Agent side, but didn't handle cleanup of
-- stale GpuDeviceSpecVO records when device type changed from GPU to Generic.
-- GpuDeviceSpecVO is a child table of PciDeviceSpecVO using @PrimaryKeyJoinColumn inheritance.
-- Only GPU-type specs should have corresponding records in GpuDeviceSpecVO.
DELETE g FROM GpuDeviceSpecVO g
INNER JOIN PciDeviceSpecVO p ON g.uuid = p.uuid
WHERE p.type NOT IN (
    'GPU_Video_Controller',
    'GPU_3D_Controller',
    'GPU_Processing_Accelerators',
    'GPU_Co_Processor',
    'GPU_Communication_Controller'
);

-- ZSTAC-81566: Persist clusterId on ModelServiceInstanceVO to survive node restart
-- V5.3.22 may have already added this column as INT; ensure BIGINT to match Java Long type
CALL ADD_COLUMN('ModelServiceInstanceVO', 'clusterId', 'BIGINT', 1, NULL);

DROP PROCEDURE IF EXISTS EnsureClusterIdBigint;
DELIMITER $$
CREATE PROCEDURE EnsureClusterIdBigint()
BEGIN
    DECLARE col_type VARCHAR(64);
    SELECT DATA_TYPE INTO col_type
    FROM `INFORMATION_SCHEMA`.`COLUMNS`
    WHERE `TABLE_SCHEMA` = 'zstack'
      AND `TABLE_NAME` = 'ModelServiceInstanceVO'
      AND `COLUMN_NAME` = 'clusterId';

    IF col_type = 'int' THEN
        ALTER TABLE `zstack`.`ModelServiceInstanceVO` MODIFY COLUMN `clusterId` BIGINT NULL;
    END IF;
END$$
DELIMITER ;
CALL EnsureClusterIdBigint();
DROP PROCEDURE IF EXISTS EnsureClusterIdBigint;

-- Backfill existing data from PodVO
UPDATE ModelServiceInstanceVO msi
INNER JOIN PodVO p ON msi.vmInstanceUuid = p.uuid
SET msi.clusterId = p.clusterId
WHERE msi.clusterId IS NULL AND p.clusterId IS NOT NULL;

-- Fix GPU allocateStatus for virtualized devices
-- Issue: Virtualized physical GPUs (with vGPUs generated) should show as Unallocatable, not Unallocated
-- This UPDATE is idempotent: WHERE clause ensures only incorrect statuses are updated
UPDATE `zstack`.`GpuDeviceVO` g
INNER JOIN `zstack`.`PciDeviceVO` p ON g.`uuid` = p.`uuid`
SET g.`allocateStatus` = 'Unallocatable'
WHERE p.`virtStatus` IN ('VFIO_MDEV_VIRTUALIZED', 'SRIOV_VIRTUALIZED')
  AND p.`vmInstanceUuid` IS NULL
  AND g.`allocateStatus` != 'Unallocatable';
