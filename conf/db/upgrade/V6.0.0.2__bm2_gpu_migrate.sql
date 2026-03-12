-- V6.0.0.2: Migrate BareMetal2 GPU devices to unified GpuDeviceVO via phantom hosts
-- Conditional: only runs if BareMetal2 tables exist (skipped in non-BM2 deployments)

DELIMITER $$
DROP PROCEDURE IF EXISTS bm2_gpu_migrate$$
CREATE PROCEDURE bm2_gpu_migrate()
BEGIN
    IF (SELECT COUNT(*) FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'BareMetal2ChassisGpuDeviceVO') > 0 THEN

-- Step 1: Create phantom hosts for each BM2 chassis that has GPU devices
INSERT INTO `HostEO` (
    `uuid`, `name`, `description`,
    `zoneUuid`, `clusterUuid`,
    `managementIp`, `hypervisorType`, `state`, `status`,
    `createDate`, `lastOpDate`
)
SELECT
    CONCAT('ph-', SUBSTRING(c.`uuid`, 1, 29)),
    CONCAT('[BM2] ', c.`name`),
    CONCAT('Phantom host for BareMetal2 chassis ', c.`uuid`),
    c.`zoneUuid`,
    c.`clusterUuid`,
    '',
    'BareMetal2',
    'Enabled',
    'Connected',
    c.`createDate`,
    NOW()
FROM `BareMetal2ChassisVO` c
WHERE c.`uuid` IN (
    SELECT DISTINCT bm.`chassisUuid`
    FROM `BareMetal2ChassisPciDeviceVO` bm
    WHERE bm.`uuid` IN (SELECT `uuid` FROM `BareMetal2ChassisGpuDeviceVO`)
)
AND NOT EXISTS (
    SELECT 1 FROM `HostEO` h WHERE h.`uuid` = CONCAT('ph-', SUBSTRING(c.`uuid`, 1, 29))
);

-- Step 2: Migrate PCI base data to PciDeviceVO
INSERT INTO `PciDeviceVO` (
    `uuid`, `name`, `description`,
    `hostUuid`,
    `type`, `state`, `status`, `virtStatus`,
    `vendorId`, `deviceId`, `subvendorId`, `subdeviceId`,
    `pciDeviceAddress`, `iommuGroup`,
    `vendor`, `device`,
    `createDate`, `lastOpDate`
)
SELECT
    bm.`uuid`, bm.`name`, bm.`description`,
    CONCAT('ph-', SUBSTRING(bm.`chassisUuid`, 1, 29)),
    bm.`type`, 'Enabled', 'Active', 'UNVIRTUALIZABLE',
    bm.`vendorId`, bm.`deviceId`, bm.`subvendorId`, bm.`subdeviceId`,
    bm.`pciDeviceAddress`, bm.`iommuGroup`,
    bm.`vendor`, bm.`device`,
    bm.`createDate`, bm.`lastOpDate`
FROM `BareMetal2ChassisPciDeviceVO` bm
WHERE bm.`uuid` IN (SELECT `uuid` FROM `BareMetal2ChassisGpuDeviceVO`)
AND NOT EXISTS (
    SELECT 1 FROM `PciDeviceVO` p WHERE p.`uuid` = bm.`uuid`
);

-- Step 3: Migrate GPU extension data to GpuDeviceVO
INSERT INTO `GpuDeviceVO` (
    `uuid`, `serialNumber`, `memory`, `power`, `isDriverLoaded`,
    `scope`, `chassisUuid`
)
SELECT
    bg.`uuid`, bg.`serialNumber`, bg.`memory`, bg.`power`, bg.`isDriverLoaded`,
    'BARE_METAL', bm.`chassisUuid`
FROM `BareMetal2ChassisGpuDeviceVO` bg
JOIN `BareMetal2ChassisPciDeviceVO` bm ON bg.`uuid` = bm.`uuid`
WHERE NOT EXISTS (
    SELECT 1 FROM `GpuDeviceVO` g WHERE g.`uuid` = bg.`uuid`
);

-- Step 4: Mark old table as migrated (preserve for 1 version)
-- Use information_schema check instead of MariaDB-only ADD COLUMN IF NOT EXISTS
IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'BareMetal2ChassisPciDeviceVO'
               AND COLUMN_NAME = '_migrated') THEN
    ALTER TABLE `BareMetal2ChassisPciDeviceVO` ADD COLUMN `_migrated` TINYINT(1) DEFAULT 0;
END IF;
UPDATE `BareMetal2ChassisPciDeviceVO` SET `_migrated` = 1
    WHERE `uuid` IN (SELECT `uuid` FROM `BareMetal2ChassisGpuDeviceVO`);

    END IF;
END$$
DELIMITER ;

CALL bm2_gpu_migrate();
DROP PROCEDURE IF EXISTS bm2_gpu_migrate;
