ALTER TABLE `SchedulerTriggerVO` ADD COLUMN `cron` varchar(32) DEFAULT NULL COMMENT 'interval in cron format';

CREATE TABLE IF NOT EXISTS `VolumeBackupVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `volumeUuid` VARCHAR(32) NOT NULL,
    `size` bigint unsigned NOT NULL,
    `type` varchar(64) NOT NULL,
    `state` varchar(64) NOT NULL,
    `status` varchar(64) NOT NULL,
    `metadata` text DEFAULT NULL,
    `lastOpDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# Foreign keys for table VolumeBackupVO
ALTER TABLE VolumeBackupVO ADD CONSTRAINT fkVolumeBackupVOResourceVO FOREIGN KEY (uuid) REFERENCES ResourceVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;
ALTER TABLE VolumeBackupVO ADD CONSTRAINT fkVolumeBackupVOVolumeEO FOREIGN KEY (volumeUuid) REFERENCES VolumeEO (uuid) ON DELETE CASCADE;

CREATE TABLE IF NOT EXISTS `VolumeBackupStorageRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `backupStorageUuid` varchar(32) NOT NULL,
    `volumeBackupUuid` varchar(32) NOT NULL,
    `status` varchar(64) NOT NULL,
    `installPath` VARCHAR(2048) NOT NULL,
    `lastOpDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# Foreign keys for table VolumeBackupStorageRefVO
ALTER TABLE VolumeBackupStorageRefVO ADD CONSTRAINT fkVolumeBackupStorageRefVOBackupStorageEO FOREIGN KEY (backupStorageUuid) REFERENCES BackupStorageEO (uuid) ON DELETE CASCADE;
ALTER TABLE VolumeBackupStorageRefVO ADD CONSTRAINT fkVolumeBackupStorageRefVOVolumeBackupVO FOREIGN KEY (volumeBackupUuid) REFERENCES VolumeBackupVO (uuid) ON DELETE CASCADE;

CREATE TABLE IF NOT EXISTS `VolumeBackupHistoryVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `bitmap` VARCHAR(32) NOT NULL,
    `lastBackupUuid` VARCHAR(32) NOT NULL,
    `lastOpDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# Foreign keys for table VolumeBackupHistoryVO
ALTER TABLE VolumeBackupHistoryVO ADD CONSTRAINT fkVolumeBackupHistoryVOVolumeBackupVO FOREIGN KEY (lastBackupUuid) REFERENCES VolumeBackupVO (uuid) ON DELETE CASCADE;
ALTER TABLE VolumeBackupHistoryVO ADD CONSTRAINT fkVolumeBackupHistoryVOVolumeEO FOREIGN KEY (uuid) REFERENCES VolumeEO (uuid) ON DELETE CASCADE;

SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE AccountResourceRefVO ADD COLUMN `concreteResourceType` varchar(512) NOT NULL;
UPDATE AccountResourceRefVO set concreteResourceType = 'org.zstack.network.l2.vxlan.vxlanNetwork', resourceType = 'L2NetworkVO' WHERE resourceType = 'VxlanNetworkVO';
ALTER TABLE ResourceVO ADD COLUMN `concreteResourceType` varchar(512) NOT NULL;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE `TwoFactorAuthenticationSecretVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `secret` VARCHAR(2048) NOT NULL,
    `resourceUuid` VARCHAR(32) NOT NULL,
    `resourceType` VARCHAR(256) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `CaptchaVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `captcha` text NOT NULL,
    `verifyCode` VARCHAR(32) NOT NULL,
    `targetResourceIdentity` VARCHAR(256) NOT NULL,
    `attempts` int(10) unsigned DEFAULT 0,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `AliyunNasPrimaryStorageMountPointVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `hostUuid` varchar(32) NOT NULL,
    `primaryStorageUuid` varchar(32) NOT NULL,
    `mountUrl` varchar(512) NOT NULL,
    `mountPath` varchar(512) NOT NULL,
    `lastErrInfo` varchar(1024) DEFAULT NULL,
    `checkTimes` bigint unsigned NOT NULL,
    `errorTimes` bigint unsigned DEFAULT 0,
    `lastNormalDistance` bigint unsigned DEFAULT 0,
    `status` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`id`),
    INDEX `idxMountPointVOhostUuid` (`hostUuid`),
    CONSTRAINT `fkMountPointVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES HostEO (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkMountPointVOPrimaryStorageEO` FOREIGN KEY (`primaryStorageUuid`) REFERENCES PrimaryStorageEO (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP PROCEDURE IF EXISTS updatePlatformAdminsWithoutZoneRelation;
DELIMITER $$
CREATE PROCEDURE updatePlatformAdminsWithoutZoneRelation()
    BEGIN
        DECLARE virtualIDUuid VARCHAR(32);
        DECLARE attributeUuid VARCHAR(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE cur CURSOR FOR SELECT vid.uuid FROM IAM2VirtualIDVO vid, IAM2VirtualIDAttributeVO vida WHERE vida.name = '__PlatformAdmin__' AND vida.virtualIDUuid = vid.uuid;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO virtualIDUuid;
            IF done THEN
                LEAVE read_loop;
            END IF;

            SET attributeUuid = REPLACE(UUID(), '-', '');

            IF (select count(*) from IAM2VirtualIDAttributeVO vida where vida.name = '__PlatformAdminRelatedZone__' and vida.virtualIDUuid = virtualIDUuid) = 0 THEN
            BEGIN
            INSERT INTO zstack.IAM2VirtualIDAttributeVO (`uuid`, `name`, `value`, `type`, `virtualIDUuid`, `lastOpDate`, `createDate`)
                    values (attributeUuid, '__PlatformAdminRelatedZone__', 'ALL_ZONES', 'Customized', virtualIDUuid, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
            END;
            END IF;
        END LOOP;
        CLOSE cur;
        # work around a bug of mysql : jira.mariadb.org/browse/MDEV-4602
        SELECT CURTIME();
    END $$
DELIMITER ;

CALL updatePlatformAdminsWithoutZoneRelation();
DROP PROCEDURE IF EXISTS updatePlatformAdminsWithoutZoneRelation;

-- TABLES THAT ARE NOT NEEDED IN BAREMETAL V2
SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM `zstack`.`BaremetalPxeServerVO`;
DELETE FROM `zstack`.`BaremetalChassisVO`;
DELETE FROM `zstack`.`BaremetalHardwareInfoVO`;
DROP TABLE IF EXISTS `zstack`.`BaremetalConsoleProxyVO`;
DROP TABLE IF EXISTS `zstack`.`BaremetalHostBondingVO`;
DROP TABLE IF EXISTS `zstack`.`BaremetalHostCfgVO`;
DROP TABLE IF EXISTS `zstack`.`BaremetalHostNicCfgVO`;

ALTER TABLE `zstack`.`BaremetalChassisVO` MODIFY COLUMN `ipmiPort` int DEFAULT NULL;
ALTER TABLE `zstack`.`BaremetalChassisVO` ADD COLUMN `state` varchar(32) NOT NULL;
ALTER TABLE `zstack`.`BaremetalChassisVO` ADD COLUMN `zoneUuid` varchar(32) NOT NULL;
ALTER TABLE `zstack`.`BaremetalChassisVO` ADD COLUMN `clusterUuid` varchar(32) NOT NULL;
ALTER TABLE `zstack`.`BaremetalChassisVO` ADD CONSTRAINT fkBaremetalChassisVOZoneEO FOREIGN KEY (zoneUuid) REFERENCES ZoneEO (uuid) ON DELETE RESTRICT;
ALTER TABLE `zstack`.`BaremetalChassisVO` ADD CONSTRAINT fkBaremetalChassisVOClusterEO FOREIGN KEY (clusterUuid) REFERENCES ClusterEO (uuid) ON DELETE RESTRICT;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE  `zstack`.`BaremetalInstanceVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `internalId` bigint unsigned NOT NULL,
    `zoneUuid` varchar(32) DEFAULT NULL,
    `clusterUuid` varchar(32) DEFAULT NULL,
    `chassisUuid` varchar(32) DEFAULT NULL,
    `imageUuid` varchar(32) DEFAULT NULL,
    `platform` varchar(255) NOT NULL,
    `managementIp` varchar(255) NOT NULL,
    `username` varchar(255) NOT NULL,
    `password` varchar(255) NOT NULL,
    `port` int unsigned DEFAULT NULL,
    `state` varchar(128) NOT NULL,
    `status` varchar(128) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`BaremetalInstanceVO` ADD CONSTRAINT fkBaremetalInstanceVOZoneEO FOREIGN KEY (zoneUuid) REFERENCES ZoneEO (uuid) ON DELETE SET NULL;
ALTER TABLE `zstack`.`BaremetalInstanceVO` ADD CONSTRAINT fkBaremetalInstanceVOClusterEO FOREIGN KEY (clusterUuid) REFERENCES ClusterEO (uuid) ON DELETE SET NULL;
ALTER TABLE `zstack`.`BaremetalInstanceVO` ADD CONSTRAINT fkBaremetalInstanceVOBaremetalChassisVO FOREIGN KEY (chassisUuid) REFERENCES BaremetalChassisVO (uuid) ON DELETE RESTRICT;
ALTER TABLE `zstack`.`BaremetalInstanceVO` ADD CONSTRAINT fkBaremetalInstanceVOImageEO FOREIGN KEY (imageUuid) REFERENCES ImageEO (uuid) ON DELETE SET NULL;

CREATE TABLE  `zstack`.`BaremetalNicVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `baremetalInstanceUuid` varchar(32) NOT NULL,
    `l3NetworkUuid` varchar(32) DEFAULT NULL,
    `usedIpUuid` varchar(32) DEFAULT NULL UNIQUE,
    `mac` varchar(17) NOT NULL UNIQUE,
    `ip` varchar(128) NOT NULL,
    `netmask` varchar(128) DEFAULT NULL,
    `gateway` varchar(128) DEFAULT NULL,
    `metaData` varchar(255) DEFAULT NULL,
    `pxe` tinyint(1) unsigned DEFAULT 0,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
ALTER TABLE `zstack`.`BaremetalNicVO` ADD CONSTRAINT fkBaremetalNicVOBaremetalInstanceVO FOREIGN KEY (baremetalInstanceUuid) REFERENCES BaremetalInstanceVO (uuid) ON DELETE CASCADE;
ALTER TABLE `zstack`.`BaremetalNicVO` ADD CONSTRAINT fkBaremetalNicVOL3NetworkEO FOREIGN KEY (l3NetworkUuid) REFERENCES L3NetworkEO (uuid) ON DELETE SET NULL;
ALTER TABLE `zstack`.`BaremetalNicVO` ADD CONSTRAINT fkBaremetalNicVOUsedIpVO FOREIGN KEY (usedIpUuid) REFERENCES UsedIpVO (uuid) ON DELETE SET NULL;

CREATE TABLE  `zstack`.`BaremetalInstanceSequenceNumberVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`BaremetalImageCacheVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `imageUuid` varchar(32) DEFAULT NULL,
    `url` varchar(1024) NOT NULL,
    `installUrl` varchar(1024) NOT NULL,
    `mediaType` varchar(64) NOT NULL,
    `size` bigint unsigned NOT NULL,
    `actualSize` bigint unsigned NOT NULL,
    `md5sum` varchar(255) NOT NULL,
    `utilization` bigint unsigned NOT NULL DEFAULT 0,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
ALTER TABLE BaremetalImageCacheVO ADD CONSTRAINT fkBaremetalImageCacheVOImageEO FOREIGN KEY (imageUuid) REFERENCES ImageEO (uuid) ON DELETE SET NULL;
ALTER TABLE `zstack`.`AliyunNasMountVolumeRefVO` add column dataPath varchar(1024) NOT NULL;
