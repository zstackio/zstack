 CREATE TABLE  `zstack`.`FileVerificationVO` (
     `uuid` VARCHAR(32) NOT NULL UNIQUE,
     `path` varchar(256) NOT NULL,
     `node` varchar(32) NOT NULL,
     `hexType` varchar(32) NOT NULL,
     `digest` varchar(512) NOT NULL,
     `category` varchar(64) NOT NULL,
     `state` varchar(32) NOT NULL,
     `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
     `createDate` timestamp
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`LuksEncryptedImageVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `encryptUuid` varchar(36) UNIQUE NOT NULL,
    `hashValue` varchar(32) NOT NULL,
    `bindingVmUuid` varchar(32) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ClusterDRSVO` (
    `uuid` varchar(32) not null unique,
    `name` varchar(256) NOT NULL,
    `clusterUuid` varchar(32) not null,
    `state`  varchar(255) not null,
    `automationLevel`  varchar(255) not null,
    `thresholds`  text not null,
    `thresholdDuration`  varchar(255) not null,
    `description`  varchar(255) default null,
    `createDate` timestamp not null default '0000-00-00 00:00:00',
    `lastOpDate` timestamp not null default '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `clusterUuid` (`clusterUuid`),
    CONSTRAINT `fkClusterDRSVOClusterEO` FOREIGN KEY (`clusterUuid`) REFERENCES `ClusterEO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`DRSAdviceVO` (
    `uuid` varchar(32) not null unique,
    `drsUuid` varchar(32) not null,
    `adviceGroupUuid` varchar(32) not null,
    `vmUuid` varchar(32) not null,
    `vmSourceHostUuid` varchar(32) not null,
    `vmTargetHostUuid` varchar(32) not null,
    `reason`  varchar(255) not null,
    `createDate` timestamp not null default '0000-00-00 00:00:00',
    `lastOpDate` timestamp not null default '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    KEY `drsUuid` (`drsUuid`),
    KEY `adviceGroupUuid` (`adviceGroupUuid`),
    CONSTRAINT `fkDRSAdviceVOClusterDRSVO` FOREIGN KEY (`drsUuid`) REFERENCES `ClusterDRSVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`DRSVmMigrationActivityVO` (
    `uuid` varchar(32) not null unique,
    `drsUuid` varchar(32) default null,
    `vmUuid` varchar(32) not null,
    `vmSourceHostUuid` varchar(32) not null,
    `vmTargetHostUuid` varchar(32) not null,
    `status`  varchar(255) not null,
    `endDate` datetime default null,
    `adviceUuid` varchar(32) default null,
    `reason`  varchar(255) not null,
    `cause` varchar(64) not null,
    `result`  varchar(1024) default null,
    `createDate` timestamp not null default '0000-00-00 00:00:00',
    `lastOpDate` timestamp not null default '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    KEY `vmUuid` (`vmUuid`),
    KEY `drsUuid` (`drsUuid`),
    KEY `adviceUuid` (`adviceUuid`),
    CONSTRAINT `fkDRSVmMigrationActivityVOClusterDRSVO` FOREIGN KEY (`drsUuid`) REFERENCES `ClusterDRSVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;