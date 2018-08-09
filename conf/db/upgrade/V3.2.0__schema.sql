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
