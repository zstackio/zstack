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

CREATE TABLE IF NOT EXISTS `zstack`.`OAuthClientDetailsVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `clientId` varchar(128) DEFAULT NULL,
    `clientSecret` varchar(128) DEFAULT NULL,
    `codeUri` varchar(256) DEFAULT NULL,
    `tokenUri` varchar(256) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
