CREATE TABLE  `zstack`.`CdpBackupStorageVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `hostname` varchar(255) NOT NULL UNIQUE,
    `username` varchar(255) NOT NULL,
    `password` varchar(255) NOT NULL,
    `sshPort` int unsigned NOT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`CdpPolicyEO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) DEFAULT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `vmInstanceUuid` varchar(32) DEFAULT NULL UNIQUE,
    `retentionTimePerDay` int unsigned NOT NULL,
    `incrementalPointPerMinute` int unsigned NOT NULL,
    `recoveryPointPerSecond` int unsigned NOT NULL,
    `deleted` varchar(255) DEFAULT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`CdpPolicyRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `uuid` varchar(32) NOT NULL,
    `vmInstanceUuid` varchar(32) NOT NULL,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE VIEW `zstack`.`CdpPolicyVO` AS SELECT uuid, name, description, vmInstanceUuid, retentionTimePerDay, incrementalPointPerMinute, recoveryPointPerSecond FROM `zstack`.`CdpPolicyEO` WHERE deleted IS NULL;

CREATE TABLE  `zstack`.`CdpTaskEO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) DEFAULT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `vmInstanceUuid` varchar(32) NOT NULL,
    `retentionTimePerDay` int unsigned DEFAULT NULL,
    `incrementalPointPerMinute` int unsigned DEFAULT NULL,
    `recoveryPointPerSecond` int unsigned DEFAULT NULL,
    `deleted` varchar(255) DEFAULT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`CdpTaskRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `uuid` varchar(32) NOT NULL,
    `volumeUuid` varchar(32) NOT NULL,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE VIEW `zstack`.`CdpTaskVO` AS SELECT uuid, name, description, vmInstanceUuid, retentionTimePerDay, incrementalPointPerMinute, recoveryPointPerSecond FROM `zstack`.`CdpTaskEO` WHERE deleted IS NULL;
