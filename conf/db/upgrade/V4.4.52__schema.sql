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
ALTER TABLE `zstack`.`HostNumaNodeVO` MODIFY COLUMN `nodeCPUs` TEXT NOT NULL;
ALTER TABLE `zstack`.`VmInstanceNumaNodeVO` MODIFY COLUMN `vNodeCPUs` TEXT NOT NULL;
UPDATE `zstack`.`GlobalConfigVO` SET value="enable", defaultValue="enable" WHERE category="storageDevice" AND name="enable.multipath" AND value="true";
UPDATE `zstack`.`GlobalConfigVO` SET value="ignore", defaultValue="enable" WHERE category="storageDevice" AND name="enable.multipath" AND value="false";

DELIMITER $$
CREATE PROCEDURE addColumnsToSNSTextTemplateVO()
BEGIN
        IF NOT EXISTS( SELECT 1
                       FROM INFORMATION_SCHEMA.COLUMNS
                       WHERE table_name = 'SNSTextTemplateVO'
                             AND table_schema = 'zstack'
                             AND column_name = 'subject') THEN

ALTER TABLE `zstack`.`SNSTextTemplateVO` ADD COLUMN `subject` VARCHAR(2048);
END IF;
        IF NOT EXISTS( SELECT 1
                       FROM INFORMATION_SCHEMA.COLUMNS
                       WHERE table_name = 'SNSTextTemplateVO'
                             AND table_schema = 'zstack'
                             AND column_name = 'recoverySubject') THEN

ALTER TABLE `zstack`.`SNSTextTemplateVO` ADD COLUMN `recoverySubject` VARCHAR(2048);
END IF;
END $$
DELIMITER ;

call addColumnsToSNSTextTemplateVO();
DROP PROCEDURE IF EXISTS addColumnsToSNSTextTemplateVO;

DELIMITER $$
CREATE PROCEDURE UpdateHygonClusterVmCpuModeConfig()
    BEGIN
        DECLARE done INT DEFAULT FALSE;
        DECLARE tag_uuid VARCHAR(32);
        DECLARE resource_uuid VARCHAR(32);
        DECLARE resource_type varchar(64);
        DECLARE config_name varchar(255);
        DECLARE config_description varchar(255);
        DECLARE config_category varchar(64);
        DECLARE config_value varchar(64);
        DECLARE cur CURSOR FOR SELECT uuid FROM zstack.ClusterVO;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        SET config_name = 'vm.cpuMode';
        SET config_description = 'the default configuration after the management node is upgraded (only for hygon cluster)';
        SET config_category = 'kvm';
        SET resource_type = 'ClusterVO';
        SET config_value = 'Hygon_Customized';
        SET tag_uuid = REPLACE(UUID(), '-', '');

        OPEN cur;

        read_loop: LOOP
            FETCH cur INTO resource_uuid;

            IF done THEN
                LEAVE read_loop;
            END IF;

            SELECT COUNT(DISTINCT tag) INTO @hygon_tag_count FROM SystemTagVO WHERE tag LIKE 'hostCpuModelName::Hygon%' AND resourceUuid IN (SELECT uuid FROM HostVO WHERE clusterUuid = resource_uuid);

            SELECT COUNT(*) INTO @config_exist FROM ResourceConfigVO WHERE name = config_name AND category = config_category AND resourceUuid = resource_uuid AND resourceType = resource_type;

            IF @hygon_tag_count = 1 THEN
                IF @config_exist = 1 THEN
                    UPDATE ResourceConfigVO SET value = config_value WHERE name = config_name AND category = config_category AND resourceUuid = resource_uuid AND resourceType = resource_type;
                ELSE
                    INSERT INTO ResourceConfigVO (uuid, name, description, category, value, resourceUuid, resourceType, lastOpDate, createDate)
                            VALUES (tag_uuid, config_name, config_description, config_category, config_value, resource_uuid, resource_type, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
                END IF;
            END IF;
        END LOOP;

        CLOSE cur;
    END $$
DELIMITER ;
CALL UpdateHygonClusterVmCpuModeConfig();
DROP PROCEDURE IF EXISTS UpdateHygonClusterVmCpuModeConfig;

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

CREATE TABLE IF NOT EXISTS `zstack`.`RegisterLicenseApplicationVO` (
    `appId` VARCHAR(32) NOT NULL UNIQUE,
    `licenseRequestCode` text NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`appId`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

DELIMITER $$
CREATE PROCEDURE UpdateLicenseHistoryVOSchema()
    BEGIN
        IF NOT EXISTS( SELECT 1
                       FROM INFORMATION_SCHEMA.COLUMNS
                       WHERE table_name = 'LicenseHistoryVO'
                             AND table_schema = 'zstack'
                             AND column_name = 'ltsCapacity') THEN

            ALTER TABLE `zstack`.`LicenseHistoryVO` ADD COLUMN ltsCapacity int(10) DEFAULT 0;

        END IF;
    END $$
DELIMITER ;
CALL UpdateLicenseHistoryVOSchema();
DROP PROCEDURE IF EXISTS UpdateLicenseHistoryVOSchema;

CREATE TABLE IF NOT EXISTS `zstack`.`CephOsdGroupVO` (
    `uuid` varchar(32) NOT NULL,
    `primaryStorageUuid` varchar(32) NOT NULL,
    `osds` text NOT NULL,
    `availableCapacity` bigint(20) DEFAULT NULL,
    `availablePhysicalCapacity` bigint(20) unsigned NOT NULL DEFAULT 0,
    `totalPhysicalCapacity` bigint(20) unsigned NOT NULL DEFAULT 0,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    KEY `fkPrimaryStorageUuid` (`primaryStorageUuid`),
    CONSTRAINT `fkPrimaryStorageUuid` FOREIGN KEY (`primaryStorageUuid`) REFERENCES `PrimaryStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DELIMITER $$
CREATE PROCEDURE UpdateCephPrimaryStoragePoolVOSchema()
    BEGIN
        IF NOT EXISTS( SELECT 1
                       FROM INFORMATION_SCHEMA.COLUMNS
                       WHERE table_name = 'CephPrimaryStoragePoolVO'
                             AND table_schema = 'zstack'
                             AND column_name = 'osdGroupLtsUuid') THEN

            ALTER TABLE `zstack`.`CephPrimaryStoragePoolVO` ADD COLUMN `osdGroupLtsUuid` VARCHAR(32) DEFAULT NULL;

        END IF;

        IF NOT EXISTS( SELECT 1
                       FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
                       WHERE table_name = 'CephPrimaryStoragePoolVO'
                             AND table_schema = 'zstack'
                             AND CONSTRAINT_NAME = 'fkCephPrimaryStoragePoolVOOGLTSVO') THEN
            ALTER TABLE `zstack`.`CephPrimaryStoragePoolVO` ADD CONSTRAINT fkCephPrimaryStoragePoolVOOGLTSVO FOREIGN KEY (osdGroupLtsUuid) REFERENCES CephOsdGroupVO (uuid) ON DELETE SET NULL;
        END IF;
    END $$
DELIMITER ;
CALL UpdateCephPrimaryStoragePoolVOSchema();
DROP PROCEDURE IF EXISTS UpdateCephPrimaryStoragePoolVOSchema;

DELIMITER $$
CREATE PROCEDURE UpdateESXHostVOSchema()
    BEGIN
        IF NOT EXISTS( SELECT 1
                       FROM INFORMATION_SCHEMA.COLUMNS
                       WHERE table_name = 'ESXHostVO'
                             AND table_schema = 'zstack'
                             AND column_name = 'ltsEsxiVersion') THEN

            ALTER TABLE `zstack`.`ESXHostVO` ADD COLUMN `esxiVersion` varchar(32) DEFAULT '';

        END IF;
    END $$
DELIMITER ;
CALL UpdateESXHostVOSchema();
DROP PROCEDURE IF EXISTS UpdateESXHostVOSchema;