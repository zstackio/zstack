ALTER TABLE `GuestVmScriptExecutedRecordDetailVO`
    MODIFY `stdout` MEDIUMTEXT CHARACTER SET `utf8mb4` COLLATE `utf8mb4_unicode_ci`;

ALTER TABLE `GuestVmScriptExecutedRecordDetailVO`
    MODIFY `stderr` MEDIUMTEXT CHARACTER SET `utf8mb4` COLLATE `utf8mb4_unicode_ci`;

CALL ADD_COLUMN('PciDeviceVO', 'vmPciDeviceAddress', 'varchar(32)', 1, NULL);
CALL ADD_COLUMN('MdevDeviceVO', 'mdevDeviceAddress', 'varchar(32)', 1, NULL);

-- Add index for modelId field to support duplicate checking
-- Use CREATE_INDEX procedure (defined in beforeMigrate.sql) for idempotent operation
CALL CREATE_INDEX('ModelVO', 'idx_modelId', 'modelId');

-- Upgrade existing data: set modelId to uuid if modelId is NULL
-- This UPDATE is idempotent: WHERE clause ensures only NULL values are updated
UPDATE ModelVO SET modelId = uuid WHERE modelId IS NULL;
