CREATE TABLE IF NOT EXISTS `zstack`.`BlockPrimaryStorageVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `vendorName` varchar(256) NOt NULL,
    `metadata` text DEFAULT NULL,
    `encryptGatewayIp` varchar(64) DEFAULT NULL,
    `encryptGatewayPort` smallint unsigned DEFAULT 8443,
    `encryptGatewayUsername` varchar(256) DEFAULT NULL,
    `encryptGatewayPassword` varchar(256) DEFAULT NULL,
    PRIMARY KEY (`uuid`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`BlockScsiLunVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `target` varchar(256) DEFAULT NULL,
    `name` VARCHAR(256) DEFAULT NULL,
    `id` smallint unsigned NOT NULL,
    `wwn` VARCHAR(256) DEFAULT NULL,
    `type` VARCHAR(128) NOT NULL,
    `size` bigint unsigned NOT NULL,
    `lunMapId` smallint unsigned DEFAULT 0,
    `lunInitSnapshotID` bigint unsigned DEFAULT 0,
    `usedSize` bigint(20) unsigned DEFAULT 0,
    `encryptedId` smallint unsigned DEFAULT 0,
    `encryptedWwn` varchar(256) DEFAULT NULL,
    `lunType` varchar(256) NOT NULL,
    `volumeUuid` varchar(32) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkScsiLunVOVolumeVO` FOREIGN KEY (`volumeUuid`) REFERENCES `zstack`.`VolumeEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`HostInitiatorRefVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `hostUuid` varchar(32) NOT NULL UNIQUE,
    `initiatorName` varchar(256) NOT NULL UNIQUE,
    `metadata` text DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkHostInitiatorRefVOHostVo` FOREIGN KEY (`hostUuid`) REFERENCES `zstack`.`HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
