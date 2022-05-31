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

CREATE TABLE IF NOT EXISTS `zstack`.`VmSchedHistoryVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `vmInstanceUuid` char(32) NOT NULL,
    `accountUuid` char(32) NOT NULL,
    `schedType` varchar(32) NOT NULL,
    `success` tinyint(1),
    `lastHostUuid` char(32) NOT NULL,
    `destHostUuid` char(32),
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    INDEX idxVmSchedHistoryVOVmInstanceUuid (vmInstanceUuid),
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;
