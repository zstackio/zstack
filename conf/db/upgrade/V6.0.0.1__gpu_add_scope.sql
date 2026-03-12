-- V6.0.0.1: Add scope and chassisUuid to GpuDeviceVO for unified GPU management

DELIMITER $$
DROP PROCEDURE IF EXISTS add_gpu_scope_columns$$
CREATE PROCEDURE add_gpu_scope_columns()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'GpuDeviceVO'
                   AND COLUMN_NAME = 'scope') THEN
        ALTER TABLE `GpuDeviceVO` ADD COLUMN `scope` VARCHAR(32) DEFAULT 'VM' NOT NULL;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'GpuDeviceVO'
                   AND COLUMN_NAME = 'chassisUuid') THEN
        ALTER TABLE `GpuDeviceVO` ADD COLUMN `chassisUuid` VARCHAR(32) DEFAULT NULL;
    END IF;

    -- Mark HAMI-virtualized GPUs as CONTAINER scope
    UPDATE `GpuDeviceVO` g
        JOIN `PciDeviceVO` p ON g.`uuid` = p.`uuid`
    SET g.`scope` = 'CONTAINER'
    WHERE p.`virtStatus` = 'HAMI_VIRTUALIZED';

    -- Index for scope-based queries
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'GpuDeviceVO'
                   AND INDEX_NAME = 'idxGpuDeviceVOScope') THEN
        CREATE INDEX `idxGpuDeviceVOScope` ON `GpuDeviceVO` (`scope`);
    END IF;
END$$
DELIMITER ;

CALL add_gpu_scope_columns();
DROP PROCEDURE IF EXISTS add_gpu_scope_columns;
