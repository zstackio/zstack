CREATE TABLE IF NOT EXISTS `zstack`.`SharedBlockCapacityVO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'shared block uuid',
    `totalCapacity` bigint unsigned NOT NULL DEFAULT 0 COMMENT 'total capacity of shared block in bytes',
    `availableCapacity` bigint unsigned NOT NULL DEFAULT 0 COMMENT 'available capacity of shared block in bytes',
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkSharedBlockCapacityVOSharedBlockVO` FOREIGN KEY (`uuid`) REFERENCES `zstack`.`SharedBlockVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP PROCEDURE IF EXISTS `Alter_SCSI_Table`;
DELIMITER $$
CREATE PROCEDURE Alter_SCSI_Table()
    BEGIN
        IF NOT EXISTS( SELECT NULL
                       FROM INFORMATION_SCHEMA.COLUMNS
                       WHERE table_name = 'ScsiLunHostRefVO'
                             AND table_schema = 'zstack'
                             AND column_name = 'path')  THEN

            ALTER TABLE `zstack`.`ScsiLunHostRefVO`
                ADD COLUMN `hctl` VARCHAR(64) DEFAULT NULL,
                ADD COLUMN `path` VARCHAR(128) DEFAULT NULL;

            UPDATE `zstack`.`ScsiLunHostRefVO` ref
                INNER JOIN `zstack`.`ScsiLunVO` lun ON ref.scsiLunUuid = lun.uuid
            SET ref.path = lun.path, ref.hctl = lun.hctl;

        END IF;
    END $$
DELIMITER ;

CALL Alter_SCSI_Table();
DROP PROCEDURE Alter_SCSI_Table;

UPDATE `zstack`.`VmInstanceVO` t1, `zstack`.`HostVO` t2 set t1.`architecture` = t2.`architecture` where t1.`type` = 'ApplianceVm' and t1.`hostUuid` = t2.`uuid`;
UPDATE `zstack`.`VmInstanceVO` t1, `zstack`.`HostVO` t2 set t1.`architecture` = t2.`architecture` where t1.`type` = 'ApplianceVm' and t1.`architecture` IS NULL and t1.`lastHostUuid` = t2.`uuid`;
