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

CREATE TABLE IF NOT EXISTS `TagPatternVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `name` VARCHAR(128) NOT NULL,
    `value` VARCHAR(128) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `color` VARCHAR(32) DEFAULT NULL,
    `type` VARCHAR(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX idxTagPatternVOName ON `TagPatternVO` (name);
ALTER TABLE `zstack`.`UserTagVO` ADD COLUMN `tagPatternUuid` varchar(32) DEFAULT NULL;
ALTER TABLE `zstack`.`UserTagVO` ADD CONSTRAINT fkUserTagVOTagPatternVO FOREIGN KEY (tagPatternUuid) REFERENCES TagPatternVO (uuid) ON DELETE CASCADE;

DELIMITER $$
CREATE PROCEDURE migrateUserTagVO()
    BEGIN
        DECLARE done INT DEFAULT FALSE;
        DECLARE patternUuid VARCHAR(32);
        DECLARE accountUuid VARCHAR(32);
        DECLARE patternTag VARCHAR(128);
        DECLARE cur CURSOR FOR SELECT DISTINCT utag.tag, ref.accountUuid FROM zstack.UserTagVO utag, AccountResourceRefVO ref WHERE utag.resourceUuid = ref.resourceUuid;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO patternTag, accountUuid;
            IF done THEN
                LEAVE read_loop;
            END IF;

            SET patternUuid = (REPLACE(UUID(), '-', ''));
            INSERT zstack.ResourceVO(uuid, resourceName, resourceType, concreteResourceType)
            VALUES (patternUuid, patternTag, 'TagPatternVO', 'org.zstack.header.tag.TagPatternVO');

            INSERT TagPatternVO (uuid, name, value, color, type, createDate, lastOpDate)
            VALUES(patternUuid, patternTag, patternTag, '#186EAE', 'simple', NOW(), NOW());

            INSERT zstack.AccountResourceRefVO(accountUuid, ownerAccountUuid, resourceUuid, resourceType, concreteResourceType, permission, isShared, createDate, lastOpDate)
            VALUES(accountUuid, accountUuid, patternUuid,  'TagPatternVO', 'org.zstack.header.tag.TagPatternVO', 2, 0, NOW(), NOW());

            UPDATE zstack.UserTagVO utag, AccountResourceRefVO ref SET utag.tagPatternUuid = patternUuid
            WHERE left(utag.tag, 128) = patternTag
            AND utag.resourceUuid = ref.resourceUuid
            AND ref.accountUuid = accountUuid;

        END LOOP;
        CLOSE cur;
        SELECT CURTIME();
    END $$
DELIMITER ;

call migrateUserTagVO();
DROP PROCEDURE IF EXISTS migrateUserTagVO;

# create missing Shared resource for IAM2ProjectVO
DROP PROCEDURE IF EXISTS getLinkedAccountUUid;
DELIMITER $$
CREATE PROCEDURE getLinkedAccountUUid(OUT linkedAccountUuid VARCHAR(32), IN projectUuid VARCHAR(32))
    BEGIN
        SELECT accountUuid INTO linkedAccountUuid from zstack.IAM2ProjectAccountRefVO where `projectUuid` = projectUuid LIMIT 1,1;
    END $$
DELIMITER ;

DROP PROCEDURE IF EXISTS fixMissingShareResourceVO;
DELIMITER $$
CREATE PROCEDURE fixMissingShareResourceVO()
    BEGIN
        DECLARE done INT DEFAULT FALSE;
        DECLARE count_shared INT DEFAULT 0;
        DECLARE projectUuid varchar(32);
        DECLARE linkedAccountUuid varchar(32);
        DECLARE cur CURSOR FOR SELECT uuid FROM zstack.IAM2ProjectVO;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO projectUuid;
            IF done THEN
                LEAVE read_loop;
            END IF;

            SELECT count(*) into count_shared from SharedResourceVO s where
             s.receiverAccountUuid in (SELECT `accountUuid` from `IAM2ProjectAccountRefVO` where `projectUuid` = projectUuid)
            and s.ownerAccountUuid='36c27e8ff05c4780bf6d2fa65700f22e' and s.resourceType='IAM2ProjectVO' and s.resourceUuid = projectUuid;
            IF (count_shared = 0) THEN
               CALL getLinkedAccountUUid(linkedAccountUuid, projectUuid);
               INSERT INTO SharedResourceVO (`ownerAccountUuid`, `receiverAccountUuid`, `resourceType`, `permission`, `resourceUuid`,
               `lastOpDate`, `createDate`)
               values ('36c27e8ff05c4780bf6d2fa65700f22e', linkedAccountUuid, 'IAM2ProjectVO', 2, projectUuid, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
            END IF;


        END LOOP;
        CLOSE cur;
        SELECT CURTIME();
    END $$
DELIMITER ;

call fixMissingShareResourceVO();

ALTER TABLE AccountVO ADD COLUMN `passwordExpireDate` timestamp NOT NULL;