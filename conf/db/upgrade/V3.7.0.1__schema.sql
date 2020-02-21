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
    `balancedState` varchar(255) not null,
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

ALTER TABLE `zstack`.`AlarmVO` ADD COLUMN `emergencyLevel` varchar(64) DEFAULT NULL;
ALTER TABLE `zstack`.`EventSubscriptionVO` ADD COLUMN `emergencyLevel` varchar(64) DEFAULT NULL;
UPDATE `zstack`.`AlarmVO` SET emergencyLevel = "Emergent" where uuid in ("065f9609dce141bb952c80f729f58af4","44e6f054a59a451fb1b535accff64fc2","5d3bb9d271a349b283893317f531f723","65e8f1a4892231b692cc7a881581f3da","66dfdee6fd314aac96ca3779774ad977","712c3dec6aa94ed2b3bcd32192c22f69","b632652cc16044cdb6b4f516ed93a118","bf7359930ee444d286fb88d2e51acf51","ded02f9786444c6296e9bc3efb8eb484","e47db726090c47de84521bebc640cfc2");
UPDATE `zstack`.`AlarmVO` SET emergencyLevel = "Important" where uuid not in ("065f9609dce141bb952c80f729f58af4","44e6f054a59a451fb1b535accff64fc2","5d3bb9d271a349b283893317f531f723","65e8f1a4892231b692cc7a881581f3da","66dfdee6fd314aac96ca3779774ad977","712c3dec6aa94ed2b3bcd32192c22f69","b632652cc16044cdb6b4f516ed93a118","bf7359930ee444d286fb88d2e51acf51","ded02f9786444c6296e9bc3efb8eb484","e47db726090c47de84521bebc640cfc2");
UPDATE `zstack`.`EventSubscriptionVO` SET emergencyLevel = "Emergent" where uuid in ("14a991d4d7d54a66b14e398ffc510bd6","4a3cb114b10d41e19545ab693222c134","5e75230bd2ea4f47abf6ff92fa816a20","8eca1096feb34419913087d2b281ecec","98f9c802604e4852bd84716f66cf4f73","d59397479d2548d7abfe4ad31a575390");
UPDATE `zstack`.`EventSubscriptionVO` SET emergencyLevel = "Normal" where uuid in ("10d9c4e69fc2456bb8c6c6d456bb5038","1a7a3eb433904df89f5c42a1fa4e0716","39d2b6689efa4e4a96c239716cb6f3ea","55365763fed244c39b4642bef6c5daf9","f56795b8c34b452f84bcf25cb89bded2");
UPDATE `zstack`.`EventSubscriptionVO` SET emergencyLevel = "Important" where uuid not in ("10d9c4e69fc2456bb8c6c6d456bb5038","14a991d4d7d54a66b14e398ffc510bd6","1a7a3eb433904df89f5c42a1fa4e0716","39d2b6689efa4e4a96c239716cb6f3ea","4a3cb114b10d41e19545ab693222c134","55365763fed244c39b4642bef6c5daf9","5e75230bd2ea4f47abf6ff92fa816a20","8eca1096feb34419913087d2b281ecec","98f9c802604e4852bd84716f66cf4f73","d59397479d2548d7abfe4ad31a575390","f56795b8c34b452f84bcf25cb89bded2");
