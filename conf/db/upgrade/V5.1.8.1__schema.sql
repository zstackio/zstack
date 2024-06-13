CREATE TABLE IF NOT EXISTS `zstack`.`ModelCenterVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(128) DEFAULT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `url` varchar(2048) DEFAULT NULL,
    `parameters` varchar(128) DEFAULT NULL,
    PRIMARY KEY  (`uuid`),
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ModelVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) DEFAULT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `modelCenterUuid` varchar(32) NOT NULL,
    `parameters` mediumtext DEFAULT NULL,
    `installPath` varchar(2048) DEFAULT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkModelVOModelCenterVO FOREIGN KEY (uuid) REFERENCES ModelCenterVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ModelServiceVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) DEFAULT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `yaml` mediumtext NOT NULL,
    `requestCpu` int(10) NOT NULL,
    `requestMem` bigint(20) NOT NULL,
    PRIMARY KEY  (`uuid`),
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ModelServiceInstanceVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `description` varchar(2048) DEFAULT NULL,
    `yaml` mediumtext NOT NULL,
    `status` varchar(255) NOT NULL,
    `url` varchar(2048) NOT NULL,
    `modelServiceUuid` varchar(32) NOT NULL,
    PRIMARY KEY  (`uuid`),
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ModelServiceRefVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `modelUuid` varchar(32) NOT NULL,
    `modelServiceUuid` varchar(32) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkModelRefVO FOREIGN KEY (modelUuid) REFERENCES ModelVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
    CONSTRAINT fkModelServiceRefVO FOREIGN KEY (modelServiceUuid) REFERENCES ModelServiceVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
