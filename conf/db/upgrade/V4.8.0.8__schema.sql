DELIMITER $$

CREATE PROCEDURE UpdateBareMetal2InstanceProvisionNicVO()
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'An error occurred during the update process.';
    END;

    START TRANSACTION;

    ALTER TABLE `zstack`.`BareMetal2InstanceProvisionNicVO`
    ADD COLUMN `instanceUuid` VARCHAR(32) NOT NULL DEFAULT '';

    UPDATE `zstack`.`BareMetal2InstanceProvisionNicVO`
    SET `instanceUuid` = `uuid`;

    ALTER TABLE `zstack`.`BareMetal2InstanceProvisionNicVO`
    DROP FOREIGN KEY `fkBareMetal2InstanceProvisionNicVOInstanceVO`;

    ALTER TABLE `zstack`.`BareMetal2InstanceProvisionNicVO`
    ADD CONSTRAINT `fkBareMetal2InstanceProvisionNicVOInstanceVO`
    FOREIGN KEY (`instanceUuid`) REFERENCES `BareMetal2InstanceVO` (`uuid`)
    ON DELETE CASCADE;

    UPDATE `zstack`.`BareMetal2InstanceProvisionNicVO`
    SET `uuid` = REPLACE(UUID(), '-', '');

    ALTER TABLE `zstack`.`BareMetal2InstanceProvisionNicVO`
    ADD COLUMN `isPrimaryProvisionNic` BOOLEAN NOT NULL DEFAULT FALSE;

    UPDATE `zstack`.`BareMetal2InstanceProvisionNicVO`
    SET `isPrimaryProvisionNic` = TRUE;

    ALTER TABLE `zstack`.`BareMetal2ChassisNicVO`
    ADD COLUMN `isPrimaryProvisionNic` BOOLEAN NOT NULL DEFAULT FALSE;

    UPDATE `zstack`.`BareMetal2ChassisNicVO`
    SET `isPrimaryProvisionNic` = TRUE
    WHERE `isProvisionNic` = TRUE;

    COMMIT;
END$$

DELIMITER ;
CALL UpdateBareMetal2InstanceProvisionNicVO();