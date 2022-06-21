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

CREATE TABLE IF NOT EXISTS `zstack`.`VmInstanceDeviceAddressVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `resourceUuid` char(32) NOT NULL,
    `vmInstanceUuid` char(32) NOT NULL,
    `pciAddress` varchar(128) DEFAULT NULL,
    `metadata` text DEFAULT NULL,
    `metadataClass` varchar(128) DEFAULT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `fkVmInstanceDeviceAddressVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES VmInstanceEO(`uuid`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`VmInstanceDeviceAddressGroupVO` (
    `uuid` char(32) NOT NULL UNIQUE,
    `resourceUuid` char(32) NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`VmInstanceDeviceAddressArchiveVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `resourceUuid` char(32) NOT NULL,
    `vmInstanceUuid` char(32) NOT NULL,
    `addressGroupUuid` char(32) NOT NULL,
    `pciAddress` varchar(128) DEFAULT NULL,
    `metadata` text DEFAULT NULL,
    `metadataClass` varchar(128) DEFAULT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `fkVmInstanceDeviceAddressArchiveVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES VmInstanceEO(`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkVmInstanceDeviceAddressArchiveVOVmInstanceDeviceAddressGroupVO` FOREIGN KEY (`addressGroupUuid`) REFERENCES VmInstanceDeviceAddressGroupVO (uuid) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8;
