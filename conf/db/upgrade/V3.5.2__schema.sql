-- ----------------------------
--  add vpcha table
-- ----------------------------
ALTER TABLE `zstack`.`ApplianceVmVO` ADD COLUMN `haStatus` varchar(255) DEFAULT "NoHa";

CREATE TABLE `VpcHaGroupVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VpcHaGroupMonitorIpVO` (
     `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
     `vpcHaRouterUuid` varchar(32) NOT NULL,
     `monitorIp` varchar(255) NOT NULL,
     `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
     `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
     PRIMARY KEY (`id`),
     CONSTRAINT fkVpcHaGroupMonitorIpVOVpcHaGroupVO FOREIGN KEY (vpcHaRouterUuid) REFERENCES VpcHaGroupVO (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VpcHaGroupVipRefVO` (
     `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
     `vpcHaRouterUuid` varchar(32) NOT NULL,
     `vipUuid` varchar(32) NOT NULL,
     `l3NetworkUuid` varchar(32) NOT NULL,
     `ip` varchar(32) NOT NULL,
     `netmask` varchar(32) NOT NULL,
     `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
     `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
     PRIMARY KEY (`id`),
     CONSTRAINT fkVpcHaGroupVipRefVOVpcHaGroupVO FOREIGN KEY (vpcHaRouterUuid) REFERENCES VpcHaGroupVO (uuid) ON DELETE CASCADE,
     CONSTRAINT fkVpcHaGroupVipRefVOL3NetworkVO FOREIGN KEY (l3NetworkUuid) REFERENCES L3NetworkEO (uuid) ON DELETE CASCADE,
     CONSTRAINT fkVpcHaGroupVipRefVOVipVO FOREIGN KEY (vipUuid) REFERENCES VipVO (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`VpcHaGroupApplianceVmRefVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `vpcHaRouterUuid` varchar(32) DEFAULT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkVpcHaGroupApplianceVmRefVOVpcHaGroupVO FOREIGN KEY (vpcHaRouterUuid) REFERENCES VpcHaGroupVO (uuid) ON DELETE CASCADE,
    CONSTRAINT fkVpcHaGroupApplianceVmRefVOApplianceVmVO FOREIGN KEY (uuid) REFERENCES ApplianceVmVO (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`VpcHaGroupNetworkServiceRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `vpcHaRouterUuid` varchar(32) NOT NULL,
    `networkServiceName` varchar(128) NOT NULL,
    `networkServiceUuid` varchar(128) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`id`),
    CONSTRAINT fkVpcHaGroupNetworkServiceRefVOVpcHaGroupVO FOREIGN KEY (vpcHaRouterUuid) REFERENCES VpcHaGroupVO (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`NetworkRouterAreaRefVO` DROP FOREIGN KEY fkNetworkRouterAreaRefVOVpcRouterVmVO;
ALTER TABLE `zstack`.`NetworkRouterAreaRefVO` ADD COLUMN `applianceVmType` varchar(255) DEFAULT "vpcvrouter";

ALTER TABLE `zstack`.`IAM2OrganizationVO` ADD COLUMN `srcType` varchar(32) DEFAULT NULL;
UPDATE `zstack`.`IAM2OrganizationVO` SET `srcType` = "ZStack" WHERE `uuid`
 NOT IN (SELECT `resourceUuid` FROM `LdapResourceRefVO` WHERE `resourceType`='IAM2OrganizationVO');
UPDATE `zstack`.`IAM2OrganizationVO` SET `srcType` = "Ldap" WHERE `uuid`
 IN (SELECT `resourceUuid` FROM `LdapResourceRefVO` WHERE `resourceType`='IAM2OrganizationVO');

ALTER TABLE LoginAttemptsVO ADD COLUMN locked tinyint(1) unsigned NOT NULL;
ALTER TABLE LoginAttemptsVO ADD COLUMN forceChangePassword tinyint(1) unsigned NOT NULL;
ALTER TABLE LoginAttemptsVO ADD COLUMN unlockDate TIMESTAMP;

UPDATE LoginAttemptsVO SET locked = 1 WHERE locked = NULL;
UPDATE LoginAttemptsVO SET forceChangePassword = 1 WHERE forceChangePassword = NULL;
UPDATE LoginAttemptsVO SET unlockDate = CURRENT_TIMESTAMP() WHERE unlockDate = NULL;

ALTER TABLE LoginAttemptsVO ADD COLUMN successCount int(10) unsigned DEFAULT 0;

CREATE TABLE `zstack`.`AccessControlRuleVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `strategy` varchar(64) NOT NULL,
    `rule` text NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`HistoricalPasswordVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `uuid` varchar(32) NOT NULL,
    `password` varchar(255) DEFAULT NULL,
    `createDate` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DELIMITER $$
CREATE PROCEDURE generatePciDeviceSpecVOAccountRef()
    BEGIN
        DECLARE pciSpecUuid VARCHAR(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE cur CURSOR FOR SELECT uuid FROM `zstack`.`PciDeviceSpecVO` where uuid not in (SELECT DISTINCT resourceUuid from `zstack`.`AccountResourceRefVO` where resourceType="PciDeviceSpecVO");
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO pciSpecUuid;
            IF done THEN
                LEAVE read_loop;
            END IF;

            INSERT INTO `zstack`.`AccountResourceRefVO` (`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`)
            VALUES ("36c27e8ff05c4780bf6d2fa65700f22e", "36c27e8ff05c4780bf6d2fa65700f22e", pciSpecUuid, "PciDeviceSpecVO", 2, 0, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), "org.zstack.pciDevice.specification.pci.PciDeviceSpecVO");
        END LOOP;
        CLOSE cur;
        SELECT CURTIME();
    END $$
DELIMITER ;

SET FOREIGN_KEY_CHECKS = 0;
CALL generatePciDeviceSpecVOAccountRef();
SET FOREIGN_KEY_CHECKS = 1;
DROP PROCEDURE IF EXISTS generatePciDeviceSpecVOAccountRef;

ALTER TABLE `zstack`.`PciDeviceVO` ADD COLUMN `iommuGroup` VARCHAR(255) DEFAULT NULL;
