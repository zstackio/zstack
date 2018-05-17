CREATE TABLE IF NOT EXISTS `NasFileSystemVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `name` VARCHAR(255) NOT NULL,
    `protocol` VARCHAR(16) NOT NULL,
    `fileSystemId` VARCHAR(32) NOT NULL,
    `type` VARCHAR(16) NOT NULL,
    `description` VARCHAR(1024) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `AliyunNasFileSystemVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `dataCenterUuid` VARCHAR(32) NOT NULL,
    `storageType` VARCHAR(32) NOT NULL,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkAliyunNasFileSystemVODataCenterVO` FOREIGN KEY (`dataCenterUuid`) REFERENCES `zstack`.`DataCenterVO` (`uuid`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `AliyunEbsPrimaryStorageVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `appName` varchar(64) DEFAULT NULL,
    `aZone` varchar(255) NOT NULL,
    `oceanUrl` varchar(255) NOT NULL,
    `secretKey` varchar(255) NOT NULL,
    `riverClusterId` varchar(255) NOT NULL,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `AliyunNasAccessGroupVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `name` VARCHAR(255) NOT NULL,
    `dataCenterUuid` VARCHAR(32) NOT NULL,
    `type` VARCHAR(16) NOT NULL,
    `description` VARCHAR(1024) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkAliyunNasAccessGroupVODataCenterVO` FOREIGN KEY (`dataCenterUuid`) REFERENCES `zstack`.`DataCenterVO` (`uuid`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `AliyunNasAccessRuleVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `accessGroupUuid` VARCHAR(32) NOT NULL,
    `rule` VARCHAR(16) NOT NULL,
    `priority` int(10) unsigned,
    `sourceCidr` VARCHAR(32) NOT NULL,
    `userAccess` VARCHAR(32) NOT NULL,
    `ruleId` VARCHAR(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkAliyunNasAccessRuleVOAliyunNasAccessGroupVO` FOREIGN KEY (`accessGroupUuid`) REFERENCES `zstack`.`AliyunNasAccessGroupVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `NasMountTargetVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `name` VARCHAR(255) NOT NULL,
    `nasFileSystemUuid` VARCHAR(32) NOT NULL,
    `mountDomain` VARCHAR(255) NOT NULL,
    `type` VARCHAR(16) NOT NULL,
    `description` VARCHAR(1024) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkNasMountTargetVONasFileSystemVO` FOREIGN KEY (`nasFileSystemUuid`) REFERENCES `zstack`.`NasFileSystemVO` (`uuid`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `AliyunNasMountTargetVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `accessGroupUuid` VARCHAR(32) NOT NULL,
    `status` VARCHAR(32) NOT NULL,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkAliyunNasMountTargetVOAliyunNasAccessGroupVO` FOREIGN KEY (`accessGroupUuid`) REFERENCES `zstack`.`AliyunNasAccessGroupVO` (`uuid`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `AliyunNasPrimaryStorageFileSystemRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `primaryStorageUuid` varchar(32) NOT NULL,
    `nasFileSystemUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkPSNasRefVONasFileSystemVO` FOREIGN KEY (`nasFileSystemUuid`) REFERENCES `zstack`.`NasFileSystemVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkPSNasRefVOPrimaryStorageVO` FOREIGN KEY (`primaryStorageUuid`) REFERENCES `zstack`.`PrimaryStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `AliyunNasMountVolumeRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `volumeUuid` varchar(32) DEFAULT NULL,
    `imageUuid` varchar(32) DEFAULT NULL,
    `nasMountUuid` varchar(32) NOT NULL,
    `hostUuid` varchar(32) NOT NULL,
    `sourceType` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkAliyunNasMountVolumeRefVOVolumeEO` FOREIGN KEY (`volumeUuid`) REFERENCES `zstack`.`VolumeEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkAliyunNasMountVolumeRefVOImageEO` FOREIGN KEY (`imageUuid`) REFERENCES `zstack`.`ImageEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkAliyunNasMountVolumeRefVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `zstack`.`HostEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkAliyunNasMountVolumeRefVOAliyunNasMountTargetVO` FOREIGN KEY (`nasMountUuid`) REFERENCES `zstack`.`AliyunNasMountTargetVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`PricePciDeviceOfferingRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `priceUuid` varchar(32) NOT NULL,
    `pciDeviceOfferingUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`id`),
    CONSTRAINT `fkPricePciDeviceOfferingRefVOPriceVO` FOREIGN KEY (`priceUuid`) REFERENCES `zstack`.`PriceVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkPricePciDeviceOfferingRefVOPciDeviceOfferingVO` FOREIGN KEY (`pciDeviceOfferingUuid`) REFERENCES `zstack`.`PciDeviceOfferingVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`PciDeviceUsageVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `pciDeviceUuid` varchar(32) NOT NULL,
    `vendorId` varchar(64) NOT NULL,
    `deviceId` varchar(64) NOT NULL,
    `subvendorId` varchar(64) DEFAULT NULL,
    `subdeviceId` varchar(64) DEFAULT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `vmUuid` varchar(32) NOT NULL,
    `vmName` varchar(255) DEFAULT NULL,
    `status` varchar(64) NOT NULL,
    `accountUuid` varchar(32) NOT NULL,
    `dateInLong` bigint unsigned NOT NULL,
    `inventory` text DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
