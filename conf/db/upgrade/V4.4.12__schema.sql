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

CREATE TABLE IF NOT EXISTS `zstack`.`L2GatewayVO` (
    `name`  varchar(255) NOT NULL,
    `description`  varchar(2048) DEFAULT NULL,
    `l2NetworkAUuid` varchar(32) NOT NULL,
    `l2NetworkZUuid` varchar(32) NOT NULL,
    `clusterUuid` varchar(32) NOT NULL,
    `haType`  varchar(255) NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `uuid` varchar(32) NOT NULL UNIQUE,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkL2GatewayVOL2NetworkVOA` FOREIGN KEY (`l2NetworkAUuid`) REFERENCES `zstack`.`L2NetworkEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkL2GatewayVOL2NetworkVOZ` FOREIGN KEY (`l2NetworkZUuid`) REFERENCES `zstack`.`L2NetworkEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkL2GatewayVOClusterVO` FOREIGN KEY (`clusterUuid`) REFERENCES `zstack`.`ClusterEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`L2GatewayHostRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `l2GatewayUuid` varchar(32) NOT NULL,
    `hostUuid` varchar(32) NOT NULL,
    `createDate` timestamp not null default '0000-00-00 00:00:00',
    `lastOpDate` timestamp not null default '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `fkL2GatewayHostRefVOL2GatewayVO` FOREIGN KEY (`l2GatewayUuid`) REFERENCES `zstack`.`L2GatewayVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkL2GatewayHostRefVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `zstack`.`HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
