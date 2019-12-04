CREATE TABLE IF NOT EXISTS `AppBuildSystemVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `storageType` VARCHAR(32) NOT NULL,
    `url` VARCHAR(1024) NOT NULL,
    `hostname` VARCHAR(255) NOT NULL,
    `username` VARCHAR(255) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `sshPort` int unsigned NOT NULL,
    `status` varchar(32) NOT NULL,
    `state` varchar(32) NOT NULL,
    `totalCapacity` bigint unsigned DEFAULT 0,
    `availableCapacity` bigint unsigned DEFAULT 0,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  IF NOT EXISTS `AppBuildSystemZoneRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `buildSystemUuid` varchar(32) NOT NULL,
    `zoneUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkAppBuildSystemZoneRefVOZoneEO` FOREIGN KEY (`zoneUuid`) REFERENCES ZoneEO (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkAppBuildSystemZoneRefVOAppBuildSystemVO` FOREIGN KEY (`buildSystemUuid`) REFERENCES AppBuildSystemVO (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `BuildApplicationVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `buildSystemUuid` varchar(32) DEFAULT NULL,
    `templateContent` mediumtext NOT NULL,
    `appMetaData` mediumtext NOT NULL,
    `appId` varchar(255) NOT NULL,
    `version` varchar(127) NOT NULL,
    `installPath` varchar(1024) DEFAULT NULL,
    `status` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkBuildApplicationVOAppBuildSystemVO` FOREIGN KEY (`buildSystemUuid`) REFERENCES AppBuildSystemVO (`uuid`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `BuildAppExportHistoryVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `buildAppUuid` VARCHAR(32) NOT NULL,
    `name` VARCHAR(255) DEFAULT NULL,
    `path` VARCHAR(2048) DEFAULT NULL,
    `size` bigint unsigned DEFAULT 0,
    `md5Sum` varchar(255) NOT NULL,
    `version` varchar(127) NOT NULL,
    `status` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE INDEX idxBuildAppExportHistoryVObuildAppUuid ON BuildAppExportHistoryVO (buildAppUuid);
CREATE INDEX idxBuildAppExportHistoryVOname ON BuildAppExportHistoryVO (name);

CREATE TABLE  IF NOT EXISTS `BuildAppImageRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `imageUuid` varchar(32) NOT NULL,
    `imageName` varchar(255) NOT NULL,
    `buildAppUuid` varchar(32) NOT NULL,
    `backupStorageUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkBuildAppImageRefVOImageVO` FOREIGN KEY (`imageUuid`) REFERENCES ImageEO (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fkBuildAppImageRefVOBackupStorageEO` FOREIGN KEY (`backupStorageUuid`) REFERENCES BackupStorageEO (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fkBuildAppImageRefVOBuildApplicationVO` FOREIGN KEY (`buildAppUuid`) REFERENCES BuildApplicationVO (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `PublishAppVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `buildAppUuid` varchar(32) DEFAULT NULL,
    `templateContent` mediumtext NOT NULL,
    `appMetaData` mediumtext NOT NULL,
    `preParams` text DEFAULT NULL,
    `appId` varchar(255) NOT NULL,
    `version` varchar(127) NOT NULL,
    `type` varchar(32) NOT NULL,
    `status` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkPublishAppVOBuildApplicationVO` FOREIGN KEY (`buildAppUuid`) REFERENCES BuildApplicationVO (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  IF NOT EXISTS `PublishAppResourceRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `appUuid` VARCHAR(32) NOT NULL,
    `resourceUuid` VARCHAR(32) NOT NULL,
    `resourceType` VARCHAR(255) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    CONSTRAINT `fkPublishAppResourceRefVOPublishAppVO` FOREIGN KEY (`appUuid`) REFERENCES PublishAppVO (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkPublishAppResourceRefVOResourceVO` FOREIGN KEY (`resourceUuid`) REFERENCES ResourceVO (`uuid`) ON DELETE CASCADE,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE CloudFormationStackResourceRefVO ADD COLUMN resourceName VARCHAR(255) DEFAULT NULL;

ALTER TABLE PublishAppVO ADD COLUMN vmRelationship text DEFAULT NULL;

CREATE TABLE  IF NOT EXISTS `ResourceStackVmPortRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `stackUuid` VARCHAR(32) NOT NULL,
    `vmInstanceUuid` VARCHAR(32) NOT NULL,
    `port` int unsigned NOT NULL,
    `status` VARCHAR(64) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    CONSTRAINT `fkResourceStackVmPortRefVOResourceStackVO` FOREIGN KEY (`stackUuid`) REFERENCES ResourceStackVO (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkResourceStackVmPortRefVOVmInstanceVO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES VmInstanceEO (`uuid`) ON DELETE CASCADE,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE ResourceStackVO ADD COLUMN outputs text DEFAULT NULL;
