CREATE TABLE `AccessKeyVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `accountUuid` VARCHAR(32) NOT NULL,
    `userUuid` VARCHAR(32) NOT NULL,
    `AccessKeyID` VARCHAR(128) NOT NULL,
    `AccessKeySecret` VARCHAR(128) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `state` varchar(64) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE IF NOT EXISTS `AliyunPanguPartitionVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `identityZoneUuid` VARCHAR(32) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `appName` VARCHAR(255) NOT NULL,
    `partitionName` VARCHAR(255) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uuid` (`uuid`),
    CONSTRAINT `fkAliyunPanguPartitionVOIdentityZoneVO` FOREIGN KEY (`identityZoneUuid`) REFERENCES `IdentityZoneVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE AliyunEbsPrimaryStorageVO DROP COLUMN `appName`;
ALTER TABLE AliyunEbsPrimaryStorageVO DROP COLUMN `aZone`;
ALTER TABLE AliyunEbsPrimaryStorageVO DROP COLUMN `oceanUrl`;
ALTER TABLE AliyunEbsPrimaryStorageVO DROP COLUMN `secretKey`;
ALTER TABLE AliyunEbsPrimaryStorageVO DROP COLUMN `riverClusterId`;
ALTER TABLE AliyunEbsPrimaryStorageVO ADD COLUMN `panguAppName` VARCHAR(255) DEFAULT NULL;
ALTER TABLE AliyunEbsPrimaryStorageVO ADD COLUMN `panguPartitionName` VARCHAR(255) DEFAULT NULL;
ALTER TABLE AliyunEbsPrimaryStorageVO ADD COLUMN `defaultIoType` VARCHAR(16) NOT NULL;
ALTER TABLE AliyunEbsPrimaryStorageVO ADD COLUMN `identityZoneUuid` VARCHAR(32) NOT NULL;
ALTER TABLE AliyunEbsPrimaryStorageVO ADD CONSTRAINT fkAliyunEbsPrimaryStorageVOIdentityZoneVO FOREIGN KEY (identityZoneUuid) REFERENCES IdentityZoneVO (uuid) ON DELETE RESTRICT;

CREATE TABLE IF NOT EXISTS `AliyunEbsBackupStorageVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `ossBucketUuid` VARCHAR(32) NOT NULL,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uuid` (`uuid`),
    CONSTRAINT `fkAliyunEbsBackupStorageVOOssBucketVO` FOREIGN KEY (`ossBucketUuid`) REFERENCES `OssBucketVO` (`uuid`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `OssBucketDomainVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `ossBucketUuid` varchar(32) NOT NULL,
  `ossDomain` varchar(256) NOT NULL,
  `ossKey` varchar(127) NOT NULL,
  `ossSecret` varchar(127) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  CONSTRAINT fkOssBucketDomainVOOssBucketVO FOREIGN KEY (ossBucketUuid) REFERENCES OssBucketVO (uuid) ON DELETE CASCADE,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE DataCenterVO ADD COLUMN `endpoint` VARCHAR(127) DEFAULT NULL;
UPDATE GlobalConfigVO SET category='aliyunNas' WHERE category ='aliyunNasPrimaryStorage';

ALTER TABLE VolumeEO ADD COLUMN volumeQos VARCHAR(128) DEFAULT NULL COMMENT 'volumeQos format like total=1048576';

DROP VIEW IF EXISTS `zstack`.`VolumeVO`;
CREATE VIEW `zstack`.`VolumeVO` AS SELECT uuid, name, description, primaryStorageUuid, vmInstanceUuid, diskOfferingUuid, rootImageUuid, installPath, type, status, size, actualSize, deviceId, format, state, createDate, lastOpDate, isShareable, volumeQos FROM `zstack`.`VolumeEO` WHERE deleted IS NULL;

CREATE TABLE IF NOT EXISTS `zstack`.`PubIpVmNicBandwidthUsageVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `vmNicUuid` varchar(32) NOT NULL,
    `vmInstanceUuid` varchar(32),
    `bandwidthOut` bigint unsigned NOT NULL,
    `bandwidthIn` bigint unsigned NOT NULL,
    `vmNicIp` varchar(128) DEFAULT NULL,
    `vmNicStatus` varchar(64) NOT NULL,
    `l3NetworkUuid` varchar(64) NOT NULL,
    `accountUuid` varchar(32) NOT NULL,
    `dateInLong` bigint unsigned NOT NULL,
    `inventory` text DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`PubIpVipBandwidthUsageVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `vipUuid` varchar(32) NOT NULL,
    `vipName` varchar(255) DEFAULT NULL,
    `vipIp` varchar(128) NOT NULL,
    `bandwidthOut` bigint unsigned NOT NULL,
    `bandwidthIn` bigint unsigned NOT NULL,
    `l3NetworkUuid` varchar(64) NOT NULL,
    `vipStatus` varchar(64) NOT NULL,
    `accountUuid` varchar(32) NOT NULL,
    `dateInLong` bigint unsigned NOT NULL,
    `inventory` text DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `VolumeBackupVO` ADD COLUMN `mode` VARCHAR(32) DEFAULT 'incremental';
ALTER TABLE IdentityZoneVO MODIFY COLUMN zoneId VARCHAR(64) NOT NULL;
ALTER TABLE DataCenterVO MODIFY COLUMN regionId VARCHAR(64) NOT NULL;
