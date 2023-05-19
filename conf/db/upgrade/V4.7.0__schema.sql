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
    CONSTRAINT `ukHostIpmiVO` UNIQUE (`ipmiAddress`, `ipmiPort`),
    CONSTRAINT `fkHostIpmiVO` FOREIGN KEY (`uuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8;
