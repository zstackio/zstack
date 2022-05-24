CREATE TABLE IF NOT EXISTS `zstack`.`ImagePackageVO` (
    `uuid` char(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `vmUuid` char(32),
    `backupStorageUuid` char(32) NOT NULL,
    `state` varchar(32) NOT NULL,
    `exportUrl` varchar(2048) DEFAULT NULL,
    `md5Sum` char(32) DEFAULT NULL,
    `format` varchar(16) DEFAULT NULL,
    `size` bigint unsigned DEFAULT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkImagePackageVOVmInstanceEO` FOREIGN KEY (`vmUuid`) REFERENCES VmInstanceEO(`uuid`) ON DELETE SET NULL,
    CONSTRAINT `fkImagePackageVOBackupStorageEO` FOREIGN KEY (`backupStorageUuid`) REFERENCES BackupStorageEO(`uuid`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8;