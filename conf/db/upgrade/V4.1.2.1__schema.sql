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
    `retentionTimePerDay` int unsigned NOT NULL,
    `incrementalPointPerMinute` int unsigned NOT NULL,
    `recoveryPointPerSecond` int unsigned NOT NULL,
    `deleted` varchar(255) DEFAULT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE VIEW `zstack`.`CdpPolicyVO` AS SELECT uuid, name, description, retentionTimePerDay, incrementalPointPerMinute, recoveryPointPerSecond FROM `zstack`.`CdpPolicyEO` WHERE deleted IS NULL;
