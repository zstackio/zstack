CREATE TABLE IF NOT EXISTS `zstack`.`CdpBackupStorageVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `hostname` varchar(255) NOT NULL UNIQUE,
    `username` varchar(255) NOT NULL,
    `password` varchar(255) NOT NULL,
    `sshPort` int unsigned NOT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`CdpPolicyEO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) DEFAULT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `retentionTimePerDay` int unsigned NOT NULL,
    `incrementalPointPerMinute` int unsigned NOT NULL,
    `recoveryPointPerSecond` int unsigned NOT NULL,
    `deleted` varchar(255) DEFAULT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`CdpPolicyRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `uuid` varchar(32) NOT NULL,
    `vmInstanceUuid` varchar(32) NOT NULL,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP VIEW IF EXISTS `zstack`.`CdpPolicyVO`;
CREATE VIEW `zstack`.`CdpPolicyVO` AS SELECT uuid, name, description, retentionTimePerDay, incrementalPointPerMinute, recoveryPointPerSecond FROM `zstack`.`CdpPolicyEO` WHERE deleted IS NULL;

CREATE TABLE IF NOT EXISTS `zstack`.`CdpTaskVO` (
    `vmInstanceUuid` varchar(32) NOT NULL,
    `backupStorageUuid` varchar(32) NOT NULL,
    `retentionTimePerDay` int unsigned DEFAULT NULL,
    `incrementalPointPerMinute` int unsigned DEFAULT NULL,
    `recoveryPointPerSecond` int unsigned DEFAULT NULL,
    PRIMARY KEY  (`vmInstanceUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`CdpTaskRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `uuid` varchar(32) NOT NULL,
    `volumeUuid` varchar(32) NOT NULL,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
