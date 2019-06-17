-- -----------------------------------
--  BEGIN OF PCI DEVICE VIRTUALIZATION
-- -----------------------------------
ALTER TABLE `zstack`.`PciDeviceVO` ADD COLUMN `name` VARCHAR(255) NOT NULL;
ALTER TABLE `zstack`.`PciDeviceVO` ADD COLUMN `virtStatus` VARCHAR(32) DEFAULT NULL;
ALTER TABLE `zstack`.`PciDeviceVO` ADD COLUMN `parentUuid` VARCHAR(32) DEFAULT NULL;
ALTER TABLE `zstack`.`PciDeviceVO` ADD COLUMN `pciSpecUuid` VARCHAR(32) DEFAULT NULL;
ALTER TABLE `zstack`.`PciDeviceVO` ADD CONSTRAINT `fkPciDeviceVOPciDeviceVO` FOREIGN KEY (`parentUuid`) REFERENCES `PciDeviceVO` (`uuid`) ON DELETE CASCADE;
ALTER TABLE `zstack`.`PciDeviceVO` ADD CONSTRAINT `fkPciDeviceVOPciDeviceSpecVO` FOREIGN KEY (`pciSpecUuid`) REFERENCES `PciDeviceSpecVO` (`uuid`) ON DELETE SET NULL;
CREATE INDEX `idxPciDeviceVOtype` ON PciDeviceVO (`type`);
CREATE INDEX `idxPciDeviceVOhostUuid` ON PciDeviceVO (`hostUuid`);
CREATE INDEX `idxPciDeviceVOparentUuid` ON PciDeviceVO (`parentUuid`);
CREATE INDEX `idxPciDeviceVOpciSpecUuid` ON PciDeviceVO (`pciSpecUuid`);

ALTER TABLE `zstack`.`PciDeviceSpecVO` ADD COLUMN `type` VARCHAR(32) NOT NULL;
ALTER TABLE `zstack`.`PciDeviceSpecVO` ADD COLUMN `state` VARCHAR(32) NOT NULL;
ALTER TABLE `zstack`.`PciDeviceSpecVO` ADD COLUMN `isVirtual` tinyint(1) NOT NULL DEFAULT 0;
ALTER TABLE `zstack`.`PciDeviceSpecVO` ADD COLUMN `maxPartNum` INT DEFAULT NULL;
ALTER TABLE `zstack`.`PciDeviceSpecVO` ADD COLUMN `ramSize` VARCHAR(32) DEFAULT NULL;
UPDATE `zstack`.`PciDeviceSpecVO` SET type = "GPU_Video_Controller";
UPDATE `zstack`.`PciDeviceSpecVO` SET state = "Enabled";

ALTER TABLE `zstack`.`PciDeviceOfferingVO` ADD COLUMN `ramSize` VARCHAR(32) DEFAULT NULL;

CREATE TABLE IF NOT EXISTS `zstack`.`MdevDeviceSpecVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `name` VARCHAR(32) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `specification` TEXT DEFAULT NULL,
    `type` VARCHAR(32) NOT NULL,
    `state` VARCHAR(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`PciDeviceMdevSpecRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `pciDeviceUuid` VARCHAR(32) NOT NULL,
    `mdevSpecUuid` VARCHAR(32) NOT NULL,
    `effective` tinyint(1) unsigned DEFAULT 0,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkSpecRefPciDeviceUuid` FOREIGN KEY (`pciDeviceUuid`) REFERENCES `PciDeviceVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkSpecRefMdevSpecUuid` FOREIGN KEY (`mdevSpecUuid`) REFERENCES `MdevDeviceSpecVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`MdevDeviceVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `name` VARCHAR(32) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `hostUuid` VARCHAR(32) NOT NULL,
    `parentUuid` VARCHAR(32) NOT NULL,
    `vmInstanceUuid` VARCHAR(32) DEFAULT NULL,
    `mdevSpecUuid` VARCHAR(32) DEFAULT NULL,
    `type` VARCHAR(32) NOT NULL,
    `state` VARCHAR(32) NOT NULL,
    `status` VARCHAR(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    INDEX `idxMdevDeviceVOtype` (`type`),
    INDEX `idxMdevDeviceVOhostUuid` (`hostUuid`),
    INDEX `idxMdevDeviceVOparentUuid` (`parentUuid`),
    INDEX `idxMdevDeviceVOmdevSpecUuid` (`mdevSpecUuid`),
    CONSTRAINT `fkMdevDeviceVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkMdevDeviceVOPciDeviceVO` FOREIGN KEY (`parentUuid`) REFERENCES `PciDeviceVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkMdevDeviceVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE SET NULL,
    CONSTRAINT `fkMdevDeviceVOMdevSpecVO` FOREIGN KEY (`mdevSpecUuid`) REFERENCES `MdevDeviceSpecVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`VmInstancePciDeviceSpecRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `vmInstanceUuid` VARCHAR(32) NOT NULL,
    `pciSpecUuid` VARCHAR(32) NOT NULL,
    `pciDeviceNumber` int unsigned DEFAULT 1,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkVmPciSpecRefVmInstanceUuid` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkVmPciSpecRefPciSpecUuid` FOREIGN KEY (`pciSpecUuid`) REFERENCES `PciDeviceSpecVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`VmInstancePciSpecDeviceRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `vmInstanceUuid` VARCHAR(32) NOT NULL,
    `pciSpecUuid` VARCHAR(32) NOT NULL,
    `pciDeviceUuid` VARCHAR(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkVmPciDeviceRefVmInstanceUuid` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkVmPciDeviceRefPciSpecUuid` FOREIGN KEY (`pciSpecUuid`) REFERENCES `PciDeviceSpecVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkVmPciDeviceRefPciDeviceUuid` FOREIGN KEY (`pciDeviceUuid`) REFERENCES `PciDeviceVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`VmInstanceMdevDeviceSpecRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `vmInstanceUuid` VARCHAR(32) NOT NULL,
    `mdevSpecUuid` VARCHAR(32) NOT NULL,
    `mdevDeviceNumber` int unsigned DEFAULT 1,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkVmMdevSpecRefVmInstanceUuid` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkVmMdevSpecRefMdevSpecUuid` FOREIGN KEY (`mdevSpecUuid`) REFERENCES `MdevDeviceSpecVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`VmInstanceMdevSpecDeviceRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `vmInstanceUuid` VARCHAR(32) NOT NULL,
    `mdevSpecUuid` VARCHAR(32) NOT NULL,
    `mdevDeviceUuid` VARCHAR(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkVmMdevDeviceRefVmInstanceUuid` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkVmMdevDeviceRefMdevSpecUuid` FOREIGN KEY (`mdevSpecUuid`) REFERENCES `MdevDeviceSpecVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkVmMdevDeviceRefMdevDeviceUuid` FOREIGN KEY (`mdevDeviceUuid`) REFERENCES `MdevDeviceVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DELIMITER $$
CREATE PROCEDURE handleLegacyPciSpecUuidTags()
    BEGIN
        DECLARE done INT DEFAULT FALSE;
        DECLARE tagUuid VARCHAR(32);
        DECLARE vmInstanceUuid VARCHAR(32);
        DECLARE pciSpecUuidTag VARCHAR(64);
        DECLARE pciSpecUuid VARCHAR(32);
        DECLARE pciDeviceUuid VARCHAR(32);
        DECLARE autoReleaseTagUuid VARCHAR(32);
        DEClARE cur CURSOR FOR SELECT `uuid`, `resourceUuid`, `tag` FROM `zstack`.`SystemTagVO`
            WHERE `resourceType` = 'VmInstanceVO' AND `tag` LIKE 'pciSpecUuid::%';
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO tagUuid, vmInstanceUuid, pciSpecUuidTag;
            IF done THEN
                LEAVE read_loop;
            END IF;

            -- create records in VmInstancePciDeviceSpecRefVO
            SET pciSpecUuid = substring(pciSpecUuidTag, LENGTH('pciSpecUuid::') + 1);
            IF pciSpecUuid IS NOT NULL THEN
                INSERT INTO `zstack`.`VmInstancePciDeviceSpecRefVO` (`vmInstanceUuid`, `pciSpecUuid`, `pciDeviceNumber`, `lastOpDate`, `createDate`)
                VALUES (vmInstanceUuid, pciSpecUuid, 1, NOW(), NOW());
            END IF;

            -- create records in VmInstancePciSpecDeviceRefVO
            SELECT pci.uuid INTO pciDeviceUuid FROM `zstack`.`PciDeviceVO` pci WHERE pci.vmInstanceUuid = vmInstanceUuid AND pci.type = 'GPU_Video_Controller' LIMIT 1;
            IF pciDeviceUuid IS NOT NULL THEN
                INSERT INTO `zstack`.`VmInstancePciSpecDeviceRefVO` (`vmInstanceUuid`, `pciSpecUuid`, `pciDeviceUuid`, `lastOpDate`, `createDate`)
                VALUES (vmInstanceUuid, pciSpecUuid, pciDeviceUuid, NOW(), NOW());
            END IF;

            -- create autoReleaseSpecReleatedPhysicalPciDevice tag for vm
            SET autoReleaseTagUuid = REPLACE(UUID(), '-', '');
            INSERT INTO `zstack`.`SystemTagVO` (`uuid`, `resourceUuid`, `resourceType`, `inherent`, `type`, `tag`, `createDate`, `lastOpDate`)
                VALUES (autoReleaseTagUuid, vmInstanceUuid, 'VmInstanceVO', 0, 'System', 'autoReleaseSpecReleatedPhysicalPciDevice', NOW(), NOW());

            -- delete legacy pciSpecUuid tag
            DELETE FROM `zstack`.`SystemTagVO` WHERE `uuid` = tagUuid;
        END LOOP;
        CLOSE cur;
        SELECT CURTIME();
    END $$
DELIMITER ;

SET FOREIGN_KEY_CHECKS = 0;
call handleLegacyPciSpecUuidTags();
SET FOREIGN_KEY_CHECKS = 1;
DROP PROCEDURE IF EXISTS handleLegacyPciSpecUuidTags;
DELETE FROM `zstack`.`SystemTagVO` WHERE `resourceType` = 'InstanceOfferingVO' AND `tag` LIKE 'pciSpecUuid::%';
-- ---------------------------------
--  END OF PCI DEVICE VIRTUALIZATION
-- ---------------------------------

CREATE TABLE IF NOT EXISTS `BillingResourceLabelVO` (
  `resourceUuid` varchar(32) NOT NULL,
  `labelKey` varchar(255) DEFAULT NULL,
  `labelValue` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`resourceUuid`, `labelKey`),
  KEY `resourceUuid` (`resourceUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE VmCPUBillingVO ADD COLUMN cpuNum int(10) unsigned NOT NULL;
ALTER TABLE VmMemoryBillingVO ADD COLUMN memorySize bigint(20) unsigned NOT NULL;

ALTER TABLE `BillingVO` ADD INDEX idxAccountUuidCreateDate (`accountUuid`, `createDate`);

DELIMITER $$
CREATE PROCEDURE modifyVipNetworkServicesRefVO()
    modifyVipNetworkServicesRefVO:BEGIN
        DECLARE curUuid VARCHAR(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE snatUuidExists INT DEFAULT 0;
        DEClARE cur CURSOR FOR SELECT r.uuid FROM VipNetworkServicesRefVO r WHERE r.serviceType='SNAT';
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        SELECT COUNT(uuid) INTO snatUuidExists FROM VipNetworkServicesRefVO r where r.serviceType='SNAT';

        IF (snatUuidExists = 0) THEN
            LEAVE modifyVipNetworkServicesRefVO;
        END IF;

        OPEN cur;
        delete_loop: LOOP
            FETCH cur INTO curUuid;
            IF done THEN
                LEAVE delete_loop;
            END IF;
            DELETE FROM VipNetworkServicesRefVO WHERE uuid = curUuid;
        END LOOP;
        CLOSE cur;

        INSERT INTO VipNetworkServicesRefVO (`uuid`, `serviceType`, `vipUuid`, `lastOpDate`, `createDate`)
        SELECT vr.uuid, "SNAT", vr.uuid, current_timestamp(), current_timestamp() FROM VirtualRouterVipVO vr, VipVO vip, VmInstanceVO vm, VmNicVO n WHERE vr.uuid = vip.uuid
        AND vm.uuid=vr.virtualRouterVmUuid AND n.vmInstanceUuid=vm.uuid AND n.l3NetworkUuid=vip.l3NetworkUuid AND n.ip=vip.ip;

    END $$
DELIMITER ;

call modifyVipNetworkServicesRefVO();
DROP PROCEDURE IF EXISTS modifyVipNetworkServicesRefVO;
