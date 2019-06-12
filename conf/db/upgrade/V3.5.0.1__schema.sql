-- ------------------------------
--  for pci device virtualization
-- ------------------------------
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
    `pciDeviceUuid` VARCHAR(32) DEFAULT NULL,
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
    `mdevDeviceUuid` VARCHAR(32) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkVmMdevDeviceRefVmInstanceUuid` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkVmMdevDeviceRefMdevSpecUuid` FOREIGN KEY (`mdevSpecUuid`) REFERENCES `MdevDeviceSpecVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkVmMdevDeviceRefMdevDeviceUuid` FOREIGN KEY (`mdevDeviceUuid`) REFERENCES `MdevDeviceVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `BillingResourceLabelVO` (
  `resourceUuid` varchar(32) NOT NULL,
  `labelKey` varchar(255) DEFAULT NULL,
  `labelValue` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`resourceUuid`, `labelKey`),
  KEY `resourceUuid` (`resourceUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DELIMITER $$
CREATE PROCEDURE handleLegacyPciSpecUuidTags()
    BEGIN
        DECLARE done INT DEFAULT FALSE;
        DECLARE tagUuid VARCHAR(32);
        DECLARE vmInstanceUuid VARCHAR(32);
        DECLARE pciSpecUuidTag VARCHAR(64);
        DECLARE pciSpecUuid VARCHAR(32);
        DEClARE cur CURSOR FOR SELECT `uuid`, `resourceUuid`, `tag` from `zstack`.`SystemTagVO`
            WHERE `resourceType` = 'VmInstanceVO' AND `tag` LIKE 'pciSpecUuid::%';
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO tagUuid, vmInstanceUuid, pciSpecUuidTag;
            IF done THEN
                LEAVE read_loop;
            END IF;

            SET pciSpecUuid = substring(pciSpecUuidTag, LENGTH('pciSpecUuid::') + 1);
            INSERT INTO `zstack`.`VmInstancePciDeviceSpecRefVO` (`vmInstanceUuid`, `pciSpecUuid`, `pciDeviceNumber`, `lastOpDate`, `createDate`)
                VALUES (vmInstanceUuid, pciSpecUuid, 1, NOW(), NOW());
            DELETE FROM `zstack`.`SystemTagVO` WHERE `uuid` = tagUuid;
        END LOOP;
        CLOSE cur;
        SELECT CURTIME();
    END $$
DELIMITER ;

call handleLegacyPciSpecUuidTags();
DROP PROCEDURE IF EXISTS handleLegacyPciSpecUuidTags;
DELETE FROM `zstack`.`SystemTagVO` WHERE `resourceType` = 'InstanceOfferingVO' AND `tag` LIKE 'pciSpecUuid::%';

ALTER TABLE VmCPUBillingVO ADD COLUMN cpuNum int(10) unsigned NOT NULL;
ALTER TABLE VmMemoryBillingVO ADD COLUMN memorySize bigint(20) unsigned NOT NULL;