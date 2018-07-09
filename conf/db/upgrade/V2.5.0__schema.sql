CREATE TABLE IF NOT EXISTS `StackTemplateVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `type` VARCHAR(32) NOT NULL,
    `version` VARCHAR(32) NOT NULL,
    `state` tinyint(1) unsigned DEFAULT 1,
    `content` text NOT NULL,
    `md5sum` varchar(255) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `name` (`name`),
    UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `ResourceStackVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `type` VARCHAR(32) NOT NULL,
    `version` VARCHAR(32) NOT NULL,
    `templateContent` text NOT NULL,
    `paramContent` text DEFAULT NULL,
    `status` VARCHAR(32) NOT NULL,
    `reason` VARCHAR(2048) DEFAULT NULL,
    `enableRollback` boolean NOT NULL DEFAULT TRUE,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `name` (`name`),
    UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `CloudFormationStackResourceRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `stackUuid` VARCHAR(32) NOT NULL,
    `resourceUuid` VARCHAR(32) NOT NULL,
    `resourceType` VARCHAR(255) NOT NULL,
    `reserve` boolean NOT NULL DEFAULT TRUE,
    `round` int(10) unsigned,
    CONSTRAINT `fkCloudFormationStackResourceRefVOResourceStackVO` FOREIGN KEY (`stackUuid`) REFERENCES ResourceStackVO (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkCloudFormationStackResourceRefVOResourceVO` FOREIGN KEY (`resourceUuid`) REFERENCES ResourceVO (`uuid`) ON DELETE CASCADE,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `CloudFormationStackEventVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `stackUuid` VARCHAR(32) NOT NULL,
    `action` VARCHAR(64) NOT NULL,
    `resourceName` VARCHAR(128) NOT NULL,
    `description` VARCHAR(128) DEFAULT TRUE,
    `content` text NOT NULL,
    `actionStatus` VARCHAR(16) NOT NULL,
    `duration` VARCHAR(64) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    CONSTRAINT `fkCloudFormationStackEventVOResourceStackVO` FOREIGN KEY (`stackUuid`) REFERENCES ResourceStackVO (`uuid`) ON DELETE CASCADE,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# create AccountResourceRefVO for PciDeviceVO
DELIMITER $$
CREATE PROCEDURE getAdminAccountUUid(OUT adminAccountUuid VARCHAR(32))
    BEGIN
        SELECT uuid INTO adminAccountUuid from zstack.AccountVO account where account.name="admin";
    END $$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE getMaxAccountResourceRefVO(OUT refId bigint(20) unsigned)
    BEGIN
        SELECT max(id) INTO refId from zstack.AccountResourceRefVO;
    END $$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE generatePciDeviceVOAccountRef()
    BEGIN
        DECLARE pciDeviceUuid VARCHAR(32);
        DECLARE adminAccountUuid VARCHAR(32);
        DECLARE refId bigint(20) unsigned;
        DECLARE done INT DEFAULT FALSE;
        DECLARE cur CURSOR FOR SELECT uuid FROM zstack.PciDeviceVO where uuid not in (SELECT DISTINCT resourceUuid from zstack.AccountResourceRefVO where resourceType="PciDeviceVO");
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO pciDeviceUuid;
            IF done THEN
                LEAVE read_loop;
            END IF;

            CALL getAdminAccountUUid(adminAccountUuid);
            CALL getMaxAccountResourceRefVO(refId);
            INSERT INTO zstack.AccountResourceRefVO (id, accountUuid, ownerAccountUuid, resourceUuid, resourceType, permission, isShared, lastOpDate, createDate)
                                     values(refId + 1, adminAccountUuid, adminAccountUuid, pciDeviceUuid, 'PciDeviceVO', 2, 0, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

        END LOOP;
        CLOSE cur;
        # work around a bug of mysql : jira.mariadb.org/browse/MDEV-4602
        SELECT CURTIME();
    END $$
DELIMITER ;

CALL generatePciDeviceVOAccountRef();
DROP PROCEDURE IF EXISTS generatePciDeviceVOAccountRef;
DROP PROCEDURE IF EXISTS getAdminAccountUUid;
DROP PROCEDURE IF EXISTS getMaxAccountResourceRefVO;
