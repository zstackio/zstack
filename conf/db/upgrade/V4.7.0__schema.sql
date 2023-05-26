CREATE TABLE IF NOT EXISTS `zstack`.`FileIntegrityVerificationVO` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `path` varchar(256) NOT NULL,
    `nodeType` varchar(16) NOT NULL,
    `nodeUuid` varchar(64) NOT NULL,
    `hexType` varchar(16) NOT NULL,
    `digest` varchar(256) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`id`),
    UNIQUE KEY `node` (`nodeUuid`,`nodeType`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`HaiTaiSecretResourcePoolVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `managementIp` varchar(32) NOT NULL,
    `port` int unsigned NOT NULL,
    `realm` varchar(32) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkHaiTaiSecretResourcePoolVOSecretResourcePoolVO FOREIGN KEY (uuid) REFERENCES SecretResourcePoolVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`EncryptEntityMetadataVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `entityName` varchar(255) NOT NULL,
    `columnName` varchar(255) NOT NULL,
    `state` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT IGNORE INTO `zstack`.`EncryptEntityMetadataVO` (`entityName`, `columnName`, `state`, `lastOpDate`, `createDate`)
                VALUES ('IAM2VirtualIDVO', 'password', 'NeedDecrypt', NOW(), NOW());

CREATE TABLE IF NOT EXISTS `zstack`.`CpuFeaturesHistoryVO` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  `srcHostUuid` varchar(32) NOT NULL,
  `dstHostUuid` varchar(32) NOT NULL,
  `srcCpuModelName` varchar(64),
  `supportLiveMigration` boolean NOT NULL DEFAULT FALSE,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY  (`id`),
  CONSTRAINT CpuFeaturesHistoryVOHostVO FOREIGN KEY (srcHostUuid) REFERENCES HostEO (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DELETE FROM HostOsCategoryVO;

DELIMITER $$
CREATE PROCEDURE CheckAndCreateResourceConfig()
    BEGIN
        DECLARE done INT DEFAULT FALSE;
        DECLARE resouce_uuid VARCHAR(32);
        DECLARE config_uuid varchar(32);
        DECLARE config_name varchar(255);
        DECLARE config_category varchar(64);
        DECLARE config_resource_type varchar(64);
        DECLARE config_value varchar(64);
        DECLARE config_description varchar(255);
        DECLARE cur CURSOR FOR SELECT uuid FROM zstack.IAM2ProjectVO;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        SET config_name = 'iam2.force.enable.securityGroup';
        SET config_category = 'iam2';
        SET config_resource_type = 'IAM2ProjectVO';
        SET config_value = 'false';
        SET config_description = 'instances under the project need to bind the security group switch';

        OPEN cur;

        read_loop: LOOP
            FETCH cur INTO resouce_uuid;

            IF done THEN
                LEAVE read_loop;
            END IF;

            SELECT COUNT(*) INTO @count FROM ResourceConfigVO WHERE resourceUuid = resouce_uuid AND name = config_name;

            IF @count = 0 THEN
                SET config_uuid = REPLACE(UUID(),'-','');
                INSERT INTO ResourceConfigVO (uuid, name, description, category, value, resourceUuid, resourceType)
                VALUES (config_uuid, config_name, config_description, config_category, config_value, resouce_uuid, config_resource_type);
            END IF;
        END LOOP;

        CLOSE cur;
    END $$
DELIMITER ;
CALL CheckAndCreateResourceConfig();
DROP PROCEDURE IF EXISTS CheckAndCreateResourceConfig;


ALTER TABLE `zstack`.`HostNetworkInterfaceVO` ADD COLUMN `gateway` varchar(128) DEFAULT NULL;
ALTER TABLE `zstack`.`HostNetworkInterfaceVO` ADD COLUMN `callBackIp` varchar(128) DEFAULT NULL;
ALTER TABLE `zstack`.`HostNetworkInterfaceVO` ADD COLUMN `description` varchar(2048) DEFAULT NULL;

ALTER TABLE `zstack`.`HostNetworkBondingVO` ADD COLUMN `speed` BIGINT UNSIGNED DEFAULT NULL;
ALTER TABLE `zstack`.`HostNetworkBondingVO` ADD COLUMN `bondingType` varchar(32) DEFAULT NULL;
ALTER TABLE `zstack`.`HostNetworkBondingVO` ADD COLUMN `gateway` varchar(128) DEFAULT NULL;
ALTER TABLE `zstack`.`HostNetworkBondingVO` ADD COLUMN `callBackIp` varchar(128) DEFAULT NULL;
ALTER TABLE `zstack`.`HostNetworkBondingVO` ADD COLUMN `description` varchar(2048) DEFAULT NULL;

CREATE TABLE IF NOT EXISTS `HostIpmiVO`
(
    `uuid`            varchar(32)  NOT NULL UNIQUE,
    `ipmiAddress`     varchar(32),
    `ipmiPort`        int unsigned,
    `ipmiUsername`    varchar(255),
    `ipmiPassword`    varchar(255),
    `ipmiPowerStatus` varchar(255),
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkHostIpmiVO` FOREIGN KEY (`uuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

DELIMITER $$

DROP FUNCTION IF EXISTS `Json_simpleGetKeyValue` $$

CREATE FUNCTION `Json_simpleGetKeyValue`(
    in_JsonArray text,
    in_KeyName VARCHAR(64)
) RETURNS VARCHAR(4096) CHARSET utf8

BEGIN
    DECLARE vs_return, vs_KeyName VARCHAR(4096);
    DECLARE vs_JsonArray, vs_JsonString, vs_Json text;
    DECLARE vi_pos1, vi_pos2 SMALLINT UNSIGNED;

    SET vs_JsonArray = TRIM(in_JsonArray);
    SET vs_KeyName = TRIM(in_KeyName);

    IF vs_JsonArray = '' OR vs_JsonArray IS NULL
        OR vs_KeyName = '' OR vs_KeyName IS NULL
    THEN
        SET vs_return = NULL;
    ELSE
        SET vs_JsonArray = REPLACE(REPLACE(vs_JsonArray, '[', ''), ']', '');
        SET vs_json = REPLACE(REPLACE(vs_JsonArray, '{', ''), '}', '');
        SET vs_JsonString = CONCAT("'", vs_JsonArray, "'");

        IF vs_json = '' OR vs_json IS NULL THEN
            SET vs_return = NULL;
        ELSE
            SET vs_KeyName = CONCAT('"', vs_KeyName, '":');
            SET vi_pos1 = INSTR(vs_json, vs_KeyName);

            IF vi_pos1 > 0 THEN
                SET vi_pos1 = vi_pos1 + CHAR_LENGTH(vs_KeyName);
                SET vi_pos2 = LOCATE('","', vs_json, vi_pos1);

                IF vi_pos2 = 0 THEN
                    SET vi_pos2 = CHAR_LENGTH(vs_json) + 1;
                END IF;

            SET vs_return = REPLACE(MID(vs_json, vi_pos1, vi_pos2 - vi_pos1), '"', '');
            END IF;
        END IF;
    END IF;

    RETURN(vs_return);
END$$

DELIMITER  ;

DELIMITER $$
CREATE PROCEDURE UpdateVolumeBackupVmInstanceUuid()
    BEGIN
        DECLARE done INT DEFAULT FALSE;
        DECLARE backup_data text;
        DECLARE backup_uuid VARCHAR(32);
        DECLARE cur CURSOR FOR SELECT metadata, uuid FROM `zstack`.`VolumeBackupVO`;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

        IF NOT EXISTS( SELECT 1
                       FROM INFORMATION_SCHEMA.COLUMNS
                       WHERE table_name = 'VolumeBackupVO'
                             AND table_schema = 'zstack'
                             AND column_name = 'vmInstanceUuid') THEN

           ALTER TABLE `zstack`.`VolumeBackupVO` ADD COLUMN `vmInstanceUuid` varchar(32) DEFAULT NULL;

           OPEN cur;
           read_loop: LOOP
               FETCH cur INTO backup_data, backup_uuid;
               IF done THEN
                   LEAVE read_loop;
               END IF;

               UPDATE `zstack`.`VolumeBackupVO` SET `vmInstanceUuid`= Json_simpleGetKeyValue(backup_data, "vmInstanceUuid") WHERE `uuid`= backup_uuid;
           END LOOP;
           CLOSE cur;
        END IF;
    END $$
DELIMITER ;
CALL UpdateVolumeBackupVmInstanceUuid();
DROP PROCEDURE IF EXISTS UpdateVolumeBackupVmInstanceUuid;

CREATE TABLE IF NOT EXISTS `zstack`.`HaStrategyConditionVO` (
    `uuid` varchar(32) NOT NULL,
    `name` varchar(256),
    `fencerName` varchar(256) NOT NULL,
    `state` varchar(64) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
INSERT INTO HaStrategyConditionVO(`uuid`, `name`, `fencerName`, `state`, `lastOpDate`, `createDate`)
values ((REPLACE(UUID(), '-', '')), 'ha strategy condition', 'hostBusinessNic', 'Disable', current_timestamp(), current_timestamp());

DELIMITER $$
CREATE PROCEDURE addHaStrategyConditionOnVmHaLevel()
    BEGIN
        DECLARE isHaEnable VARCHAR(32);
        DECLARE fencerStrategy VARCHAR(32);
        DECLARE resourceUuid VARCHAR(32);
        DECLARE current_time_stamp timestamp;
        DECLARE done INT DEFAULT FALSE;
        DECLARE haCursor CURSOR FOR select value from GlobalConfigVO where category = 'ha' and name = 'enable';
        DECLARE fencerCursor CURSOR for select value from GlobalConfigVO where category = 'ha' and name = 'self.fencer.strategy';
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        OPEN haCursor;
        OPEN fencerCursor;
        read_loop: LOOP
            FETCH haCursor INTO isHaEnable;
            FETCH fencerCursor INTO fencerStrategy;
            IF done THEN
                LEAVE read_loop;
            END IF;

            SET resourceUuid = (REPLACE(UUID(), '-', ''));
            SET current_time_stamp = current_timestamp();
            IF (LOWER(isHaEnable) = 'false' OR fencerStrategy = 'Permissive') THEN
                INSERT INTO HaStrategyConditionVO(`uuid`, `name`, `fencerName`, `state`, `lastOpDate`, `createDate`) values (resourceUuid, 'ha strategy condition', 'hostStorageState', 'Disable', current_time_stamp, current_time_stamp);
            ELSE
                INSERT INTO HaStrategyConditionVO(`uuid`, `name`, `fencerName`, `state`, `lastOpDate`, `createDate`) values (resourceUuid, 'ha strategy condition', 'hostStorageState', 'Enable', current_time_stamp, current_time_stamp);
            END IF;
        END LOOP;
        CLOSE haCursor;
        CLOSE fencerCursor;
        SELECT CURTIME();
    END $$
DELIMITER ;

call addHaStrategyConditionOnVmHaLevel();
DROP PROCEDURE IF EXISTS addHaStrategyConditionOnVmHaLevel;

CREATE TABLE IF NOT EXISTS `zstack`.`HostHaStateVO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'host uuid',
    `state` varchar(32),
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DELIMITER $$
CREATE PROCEDURE addHostHaStatus()
    BEGIN
        DECLARE isHaEnable VARCHAR(32);
        DECLARE hostUuid VARCHAR(32);
        DECLARE current_time_stamp timestamp;
        DECLARE done INT DEFAULT FALSE;
        DECLARE haCursor CURSOR FOR select uuid from HostVO;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        OPEN haCursor;
        read_loop: LOOP
            FETCH haCursor INTO hostUuid;
            IF done THEN
                LEAVE read_loop;
            END IF;

            SET current_time_stamp = current_timestamp();
            insert into HostHaStateVO(`uuid`, `state`,`lastOpDate`, `createDate`)values (hostUuid, 'None', current_time_stamp, current_time_stamp);
        END LOOP;
        CLOSE haCursor;
        SELECT CURTIME();
    END $$
DELIMITER ;

call addHostHaStatus();
DROP PROCEDURE IF EXISTS addHostHaStatus;

CREATE TABLE `VolumeSnapshotReferenceTreeVO` (
    `uuid`                       varchar(32) NOT NULL,
    `rootImageUuid`              varchar(32)   DEFAULT NULL,
    `rootVolumeUuid`             VARCHAR(32)   DEFAULT NULL,
    `rootInstallUrl`             VARCHAR(1024) DEFAULT NULL,
    `rootVolumeSnapshotUuid`     VARCHAR(32)   DEFAULT NULL,
    `rootVolumeSnapshotTreeUuid` VARCHAR(32)   DEFAULT NULL,
    `primaryStorageUuid`         VARCHAR(32)   DEFAULT NULL,
    `hostUuid`                   VARCHAR(32)   DEFAULT NULL,
    `lastOpDate`                 timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate`                 timestamp,
    PRIMARY KEY (`uuid`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE `VolumeSnapshotReferenceVO` (
    `id`                       bigint(20) NOT NULL AUTO_INCREMENT,
    `parentId`                 bigint(20)    DEFAULT NULL,
    `volumeUuid`               varchar(32)   DEFAULT NULL,
    `volumeSnapshotUuid`       varchar(32)   DEFAULT NULL,
    `directSnapshotUuid`       VARCHAR(32)   DEFAULT NULL,
    `volumeSnapshotInstallUrl` varchar(1024) DEFAULT NULL,
    `directSnapshotInstallUrl` VARCHAR(1024) DEFAULT NULL,
    `treeUuid`                 VARCHAR(32)   DEFAULT NULL,
    `referenceUuid`            varchar(32)   DEFAULT NULL,
    `referenceType`            varchar(32)   DEFAULT NULL,
    `referenceInstallUrl`      varchar(1024) DEFAULT NULL,
    `referenceVolumeUuid`      varchar(32)   DEFAULT NULL,
    `lastOpDate`               timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate`               timestamp,
    PRIMARY KEY (`id`),

    INDEX `idxVolumeSnapshotReferenceVOVolumeUuid` (`volumeUuid`),
    INDEX `idxVolumeSnapshotReferenceVOVolumeSnapshotUuid` (`volumeSnapshotUuid`),
    INDEX `idxVolumeSnapshotReferenceVOReferenceUuid` (`referenceUuid`),
    CONSTRAINT `fkVolumeSnapshotReferenceReferenceVolumeUuid` FOREIGN KEY (`referenceVolumeUuid`) REFERENCES `VolumeEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkVolumeSnapshotReferenceReferenceParentId` FOREIGN KEY (`parentId`) REFERENCES `VolumeSnapshotReferenceVO` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fkVolumeSnapshotReferenceReferenceTreeUuid` FOREIGN KEY (`treeUuid`) REFERENCES `VolumeSnapshotReferenceTreeVO` (`uuid`) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

DROP PROCEDURE IF EXISTS `Alter_Volume_Snapshot_Tree_Table`;
DELIMITER $$
CREATE PROCEDURE Alter_Volume_Snapshot_Tree_Table()
    BEGIN
        IF NOT EXISTS( SELECT NULL
                       FROM INFORMATION_SCHEMA.COLUMNS
                       WHERE table_name = 'VolumeSnapshotTreeEO'
                             AND table_schema = 'zstack'
                             AND column_name = 'rootImageUuid')  THEN

            ALTER TABLE `VolumeSnapshotTreeEO` ADD `rootImageUuid` VARCHAR(32) DEFAULT NULL;
        END IF;
    END $$
DELIMITER ;

CALL Alter_Volume_Snapshot_Tree_Table();
DROP PROCEDURE Alter_Volume_Snapshot_Tree_Table;

DROP VIEW IF EXISTS `zstack`.`VolumeSnapshotTreeVO`;
CREATE VIEW `zstack`.`VolumeSnapshotTreeVO` AS SELECT uuid, volumeUuid, rootImageUuid, current, status, createDate, lastOpDate FROM `zstack`.`VolumeSnapshotTreeEO` WHERE deleted IS NULL;

DROP PROCEDURE IF EXISTS upgradeVolumeSnapshotRefSystemTags;
DELIMITER $$
CREATE PROCEDURE upgradeVolumeSnapshotRefSystemTags()
BEGIN
    DECLARE refTag VARCHAR(51);
    DECLARE snapshotUuid VARCHAR(32);
    DECLARE snapshotInstallUrl VARCHAR(1024);
    DECLARE volUuid VARCHAR(32);
    DECLARE refVolumeUuid VARCHAR(32);
    DECLARE refVolumeInstallUrl VARCHAR(32);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR SELECT systemTag.tag, systemTag.resourceUuid FROM `zstack`.`SystemTagVO` systemTag where systemTag.tag like 'backingTo::%';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO refTag, snapshotUuid;
        IF done THEN
            LEAVE read_loop;
        END IF;

        SET refVolumeUuid = SUBSTRING_INDEX(refTag, '::', 2);
        SELECT `volumeUuid`, `primaryStorageInstallPath` INTO volUuid, snapshotInstallUrl FROM `VolumeSnapshotVO` WHERE `uuid` = snapshotUuid;
        SELECT `installPath` INTO refVolumeInstallUrl FROM `VolumeVO` WHERE `uuid` = refVolumeUuid;
        INSERT INTO `VolumeSnapshotReferenceVO` (`volumeUuid`, `volumeSnapshotUuid`, `volumeSnapshotInstallUrl`, `referenceUuid`, `referenceType`, `referenceInstallUrl`, `referenceVolumeUuid`, `lastOpDate`, `createDate`)
        VALUES (volUuid, snapshotUuid, snapshotInstallUrl, refVolumeUuid, 'VolumeVO', refVolumeInstallUrl, refVolumeUuid, NOW(), NOW());

    END LOOP;
    CLOSE cur;
    SELECT CURTIME();
END $$
DELIMITER ;
CALL upgradeVolumeSnapshotRefSystemTags();

