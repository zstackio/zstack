-- ZSTAC-86476: Add normalizedModelName column for GPU spec dedup
CALL ADD_COLUMN('GpuDeviceSpecVO', 'normalizedModelName', 'VARCHAR(255)', 1, NULL);
CALL CREATE_INDEX('GpuDeviceSpecVO', 'idx_gpu_spec_normalized_model', 'normalizedModelName');

UPDATE VolumeSnapshotVO AS sp, PrimaryStorageVO AS ps
SET sp.primaryStorageInstallPath = REPLACE(sp.primaryStorageInstallPath, '/dev/', 'sharedblock://')
WHERE sp.primaryStorageUuid = ps.uuid AND ps.type = 'SharedBlock' AND sp.volumeType = 'Memory' AND sp.primaryStorageInstallPath LIKE '/dev/%';

UPDATE `zstack`.`ActiveAlarmTemplateVO`
SET `metricName` = 'CPUUsedUtilization'
WHERE `uuid` = 'c9e6cdca107140bea62b4ca919ff9e88'
  AND `metricName` = 'VRouterCPUAverageUsedUtilization';

UPDATE `zstack`.`AlarmVO`
SET `metricName` = 'CPUUsedUtilization'
WHERE `uuid` IN (
    SELECT `alarmUuid` FROM `zstack`.`ActiveAlarmVO`
    WHERE `templateUuid` = 'c9e6cdca107140bea62b4ca919ff9e88'
)
  AND `metricName` = 'VRouterCPUAverageUsedUtilization';

-- ZSTAC-86788: backfill one default zone for deployments affected by zone creation without isDefault.
DROP PROCEDURE IF EXISTS backfill_default_zone_if_absent;
DELIMITER $$
CREATE PROCEDURE backfill_default_zone_if_absent()
BEGIN
    DECLARE default_zone_count BIGINT DEFAULT 0;
    DECLARE first_zone_uuid VARCHAR(32) DEFAULT NULL;

    SELECT COUNT(*) INTO default_zone_count
    FROM `zstack`.`ZoneVO`
    WHERE `isDefault` = 1;

    IF default_zone_count = 0 THEN
        SET first_zone_uuid = (
            SELECT `uuid`
            FROM `zstack`.`ZoneVO`
            ORDER BY `createDate` ASC, `uuid` ASC
            LIMIT 1
        );

        IF first_zone_uuid IS NOT NULL THEN
            UPDATE `zstack`.`ZoneEO`
            SET `isDefault` = 1
            WHERE `uuid` = first_zone_uuid;
        END IF;
    END IF;
END $$
DELIMITER ;

CALL backfill_default_zone_if_absent();
DROP PROCEDURE IF EXISTS backfill_default_zone_if_absent;

ALTER TABLE `zstack`.`DRSVmMigrationActivityVO` MODIFY COLUMN `result` TEXT DEFAULT NULL;

ALTER TABLE `GuestVmScriptExecutedRecordDetailVO`
    MODIFY `stdout` MEDIUMTEXT CHARACTER SET `utf8mb4` COLLATE `utf8mb4_unicode_ci`;

ALTER TABLE `GuestVmScriptExecutedRecordDetailVO`
    MODIFY `stderr` MEDIUMTEXT CHARACTER SET `utf8mb4` COLLATE `utf8mb4_unicode_ci`;

UPDATE `zstack`.`PciDeviceMdevSpecRefVO` keepRef
JOIN (
    SELECT `pciDeviceUuid`, `mdevSpecUuid`, MAX(`id`) AS `keepId`, MAX(`effective`) AS `effective`
    FROM `zstack`.`PciDeviceMdevSpecRefVO`
    GROUP BY `pciDeviceUuid`, `mdevSpecUuid`
) groupedRef ON keepRef.`id` = groupedRef.`keepId`
SET keepRef.`effective` = groupedRef.`effective`;

DELETE duplicateRef FROM `zstack`.`PciDeviceMdevSpecRefVO` duplicateRef
JOIN `zstack`.`PciDeviceMdevSpecRefVO` keepRef
  ON duplicateRef.`pciDeviceUuid` = keepRef.`pciDeviceUuid`
 AND duplicateRef.`mdevSpecUuid` = keepRef.`mdevSpecUuid`
 AND duplicateRef.`id` < keepRef.`id`;

UPDATE `zstack`.`PciDeviceMdevSpecRefVO` ref
JOIN (
    SELECT activeRef.`id`
    FROM `zstack`.`PciDeviceMdevSpecRefVO` activeRef
    JOIN (
        SELECT `pciDeviceUuid`
        FROM `zstack`.`PciDeviceMdevSpecRefVO`
        WHERE `effective` = 1
        GROUP BY `pciDeviceUuid`
        HAVING COUNT(*) > 1
    ) duplicatedPci ON activeRef.`pciDeviceUuid` = duplicatedPci.`pciDeviceUuid`
    WHERE activeRef.`effective` = 1
      AND NOT EXISTS (
          SELECT 1
          FROM `zstack`.`MdevDeviceVO` mdev
          WHERE mdev.`parentUuid` = activeRef.`pciDeviceUuid`
            AND mdev.`mdevSpecUuid` = activeRef.`mdevSpecUuid`
      )
) staleRef ON ref.`id` = staleRef.`id`
SET ref.`effective` = 0;

UPDATE `zstack`.`PciDeviceMdevSpecRefVO` oldRef
JOIN `zstack`.`PciDeviceMdevSpecRefVO` newRef
  ON oldRef.`pciDeviceUuid` = newRef.`pciDeviceUuid`
 AND oldRef.`effective` = 1
 AND newRef.`effective` = 1
 AND oldRef.`id` < newRef.`id`
SET oldRef.`effective` = 0;

DROP PROCEDURE IF EXISTS addPciDeviceMdevSpecRefUniqueKey;
DELIMITER $$
CREATE PROCEDURE addPciDeviceMdevSpecRefUniqueKey()
BEGIN
    DECLARE index_count INT DEFAULT 0;

    SELECT COUNT(*) INTO index_count
    FROM information_schema.statistics
    WHERE table_schema = 'zstack'
      AND table_name = 'PciDeviceMdevSpecRefVO'
      AND index_name = 'ukPciDeviceMdevSpecRefVOPciUuidMdevSpecUuid';

    IF index_count < 1 THEN
        ALTER TABLE `zstack`.`PciDeviceMdevSpecRefVO`
            ADD UNIQUE KEY `ukPciDeviceMdevSpecRefVOPciUuidMdevSpecUuid` (`pciDeviceUuid`, `mdevSpecUuid`);
    END IF;

    SELECT CURTIME();
END $$
DELIMITER ;
CALL addPciDeviceMdevSpecRefUniqueKey();
DROP PROCEDURE IF EXISTS addPciDeviceMdevSpecRefUniqueKey;
