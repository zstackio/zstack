CREATE TABLE `zstack`.`VCenterVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `zoneUuid` varchar(32) NOT NULL,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `domainName` varchar(255) NOT NULL,
    `userName` varchar(255) NOT NULL,
    `password` varchar(1024) NOT NULL,
    `https` int unsigned DEFAULT NULL,
    `state` varchar(32) NOT NULL,
    `status` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`VCenterClusterVO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'vcenter cluster uuid',
    `vCenterUuid` varchar(32) NOT NULL COMMENT 'vcenter uuid',
    `morval` varchar(64) NOT NULL COMMENT 'MOR value',
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`ESXHostVO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'host uuid',
    `vCenterUuid` varchar(32) NOT NULL COMMENT 'vcenter uuid',
    `morval` varchar(128) NOT NULL COMMENT 'MOR value',
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`VCenterBackupStorageVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `vCenterUuid` varchar(32) NOT NULL COMMENT 'vcenter uuid',
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`VCenterPrimaryStorageVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `vCenterUuid` varchar(32) NOT NULL COMMENT 'vcenter uuid',
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
