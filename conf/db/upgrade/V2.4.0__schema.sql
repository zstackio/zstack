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
