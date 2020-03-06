
CREATE TABLE IF NOT EXISTS `zstack`.`ExternalBackupVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `name` VARCHAR(256) NOT NULL,
    `description` VARCHAR(1024) DEFAULT NULL,
    `type` VARCHAR(32) NOT NULL,
    `installPath` VARCHAR(2048) DEFAULT NULL,
    `totalSize` BIGINT UNSIGNED,
    `version` VARCHAR(32) NOT NULL,
    `state` VARCHAR(32) NOT NULL,
    `lastOpDate` TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` TIMESTAMP,

    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ZBoxBackupVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `zBoxUuid` VARCHAR(32) NOT NULL,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uuid` (`uuid`) USING BTREE,
    CONSTRAINT `fkZBoxBackupVOExternalBackupVO` FOREIGN KEY (`uuid`) REFERENCES `ExternalBackupVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkZBoxBackupVOZBoxVO` FOREIGN KEY (`zBoxUuid`) REFERENCES `ZBoxVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
