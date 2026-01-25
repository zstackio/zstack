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
