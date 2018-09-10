ALTER TABLE `CaptchaVO` DROP COLUMN `attempts`;

CREATE TABLE `LoginAttemptsVO` (
  `uuid` VARCHAR(32) NOT NULL,
  `targetResourceIdentity` VARCHAR(256) NOT NULL,
  `attempts` int(10) unsigned DEFAULT 0,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `IscsiServerVO` (
  `uuid` VARCHAR(32) NOT NULL,
  `name` VARCHAR(256) NOT NULL,
  `ip` VARCHAR(64) NOT NULL,
  `port` smallint unsigned DEFAULT 3260,
  `state` VARCHAR(32) NOT NULL,
  `chapUserName` VARCHAR(256) DEFAULT NULL,
  `chapUserPassword` VARCHAR(256) DEFAULT NULL,
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`)
)  ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `IscsiServerClusterRefVO` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  `clusterUuid` VARCHAR(32) NOT NULL,
  `iscsiServerUuid` VARCHAR(32) NOT NULL,
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  CONSTRAINT `fkIscsiServerClusterRefVOIscsiServerVO` FOREIGN KEY (`iscsiServerUuid`) REFERENCES IscsiServerVO (`uuid`),
  CONSTRAINT `fkIscsiServerClusterRefVOClusterEO` FOREIGN KEY (`clusterUuid`) REFERENCES ClusterEO (`uuid`) ON DELETE CASCADE
)  ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `IscsiTargetVO` (
  `uuid` VARCHAR(32) NOT NULL,
  `iqn` VARCHAR(256) NOT NULL,
  `state` VARCHAR(32) NOT NULL,
  `iscsiServerUuid` VARCHAR(32) NOT NULL,
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  CONSTRAINT `fkIscsiTargetVOIscsiServerVO` FOREIGN KEY (`iscsiServerUuid`) REFERENCES IscsiServerVO (`uuid`)
)  ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `IscsiLunVO` (
  `uuid` VARCHAR(32) NOT NULL,
  `wwid` VARCHAR(256) NOT NULL,
  `vendor` VARCHAR(256) DEFAULT NULL,
  `model` VARCHAR(256) DEFAULT NULL,
  `wwn` VARCHAR(256) DEFAULT NULL,
  `serial` VARCHAR(256) DEFAULT NULL,
  `hctl` VARCHAR(64) DEFAULT NULL,
  `type` VARCHAR(128) NOT NULL,
  `path` VARCHAR(128) DEFAULT NULL,
  `size` bigint unsigned NOT NULL,
  `iscsiTargetUuid` VARCHAR(32) NOT NULL,
  `multipathDeviceUuid` VARCHAR(32) DEFAULT NULL,
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  CONSTRAINT `fkIscsiLunVOIscsiTargetVO` FOREIGN KEY (`iscsiTargetUuid`) REFERENCES IscsiTargetVO (`uuid`)
)  ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`VolumeBackupVO` ADD COLUMN `groupUuid` varchar(32) DEFAULT NULL;

CREATE TABLE `zstack`.`IAM2TicketFlowCollectionVO` (
  `uuid` varchar(32) NOT NULL,
  `projectUuid` varchar(32) NOT NULL,
  PRIMARY KEY  (`uuid`),
	CONSTRAINT fkIAM2TicketFlowCollectionVOTicketFlowCollectionVO FOREIGN KEY (uuid) REFERENCES TicketFlowCollectionVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE,
	CONSTRAINT fkIAM2TicketFlowCollectionVOIAM2ProjectVO FOREIGN KEY (projectUuid) REFERENCES IAM2ProjectVO (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `TicketFlowCollectionVO` ADD COLUMN `state` varchar(64) NOT NULL;
ALTER TABLE `TicketFlowCollectionVO` ADD COLUMN `status` varchar(64) NOT NULL;
ALTER TABLE `TicketFlowCollectionVO` ADD COLUMN `type` varchar(64) NOT NULL;

UPDATE `TicketFlowCollectionVO` set `state` = 'Enabled', `status` = 'Valid', `type` = 'iam2';

CREATE TABLE `zstack`.`IAM2TicketFlowVO` (
  `uuid` varchar(32) NOT NULL,
  `approverUuid` varchar(32) NOT NULL,
  `valid` tinyint(1) unsigned NOT NULL,
  PRIMARY KEY  (`uuid`),
	CONSTRAINT fkIAM2TicketFlowVOTicketFlowVO FOREIGN KEY (uuid) REFERENCES TicketFlowVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.name, "TicketFlowVO" FROM TicketFlowVO t;
INSERT INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.name, "TicketFlowCollectionVO" FROM TicketFlowCollectionVO t;

CREATE TABLE IF NOT EXISTS `DatabaseBackupVO` (
    `uuid`        VARCHAR(32)     NOT NULL,
    `name`        VARCHAR(255)    NOT NULL,
    `description` VARCHAR(2048)   DEFAULT NULL,
    `size`        BIGINT UNSIGNED NOT NULL,
    `state`       VARCHAR(64)     NOT NULL,
    `status`      VARCHAR(64)     NOT NULL,
    `metadata`    TEXT            DEFAULT NULL,
    `lastOpDate`  TIMESTAMP       NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate`  TIMESTAMP       NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# Foreign keys for table DatabaseBackupVO
ALTER TABLE DatabaseBackupVO ADD CONSTRAINT fkDatabaseBackupVOResourceVO FOREIGN KEY (uuid) REFERENCES ResourceVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;

CREATE TABLE IF NOT EXISTS `DatabaseBackupStorageRefVO` (
    `id`                 BIGINT UNSIGNED NOT NULL UNIQUE AUTO_INCREMENT,
    `backupStorageUuid`  VARCHAR(32)     NOT NULL,
    `databaseBackupUuid` VARCHAR(32)     NOT NULL,
    `status`             VARCHAR(64)     NOT NULL,
    `installPath`        VARCHAR(2048)   NOT NULL,
    `exportUrl`          VARCHAR(255)    DEFAULT NULL,
    `lastOpDate`         TIMESTAMP       NOT NULL        DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate`         TIMESTAMP       NOT NULL        DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# Foreign keys for table DatabaseBackupStorageRefVO
ALTER TABLE DatabaseBackupStorageRefVO ADD CONSTRAINT fkDatabaseBackupStorageRefVOBackupStorageEO FOREIGN KEY (backupStorageUuid) REFERENCES BackupStorageEO (uuid) ON DELETE CASCADE;
ALTER TABLE DatabaseBackupStorageRefVO ADD CONSTRAINT fkDatabaseBackupStorageRefVODatabaseBackupVO FOREIGN KEY (databaseBackupUuid) REFERENCES DatabaseBackupVO (uuid) ON DELETE CASCADE;

ALTER TABLE EcsImageVO ADD COLUMN `osName` varchar(256) DEFAULT NULL;

ALTER TABLE EventSubscriptionVO ADD COLUMN name varchar(255) DEFAULT NULL;

ALTER TABLE `VolumeBackupVO` DROP FOREIGN KEY `fkVolumeBackupVOVolumeEO`;