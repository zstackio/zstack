ALTER TABLE `GuestVmScriptExecutedRecordDetailVO`
    MODIFY `stdout` MEDIUMTEXT CHARACTER SET `utf8mb4` COLLATE `utf8mb4_unicode_ci`;

ALTER TABLE `GuestVmScriptExecutedRecordDetailVO`
    MODIFY `stderr` MEDIUMTEXT CHARACTER SET `utf8mb4` COLLATE `utf8mb4_unicode_ci`;

CALL ADD_COLUMN('PciDeviceVO', 'vmPciDeviceAddress', 'varchar(32)', 1, NULL);
CALL ADD_COLUMN('MdevDeviceVO', 'mdevDeviceAddress', 'varchar(32)', 1, NULL);
