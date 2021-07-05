CREATE TABLE IF NOT EXISTS `zstack`.`BareMetal2VmUsageVO`
(
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `bareMetal2ChassisOfferingUuid` varchar(32) NOT NULL,
    `vmUuid` varchar(32) NOT NULL,
    `vmName` varchar(255) DEFAULT NULL,
    `state` varchar(64) NOT NULL,
    `accountUuid` varchar(32) NOT NULL,
    `dateInLong` bigint(20) unsigned NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`id`),
    KEY `idxBareMetal2VmUsageVOaccountUuid` (`accountUuid`,`dateInLong`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS  `zstack`.`PriceBareMetal2ChassisOfferingRefVO`
(
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `priceUuid` varchar(32) NOT NULL,
    `bareMetal2ChassisOfferingUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`id`),
    CONSTRAINT `fkPriceBareMetal2ChassisOfferingRefVOPriceVO` FOREIGN KEY (`priceUuid`) REFERENCES `zstack`.`PriceVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkPriceBareMetal2ChassisOfferingRefVOBareMetal2ChassisOfferingVO` FOREIGN KEY (`bareMetal2ChassisOfferingUuid`) REFERENCES `zstack`.`BareMetal2ChassisOfferingVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`BareMetalUsageHistoryVO`
(
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `bareMetal2ChassisOfferingUuid` varchar(32) NOT NULL,
    `vmUuid` varchar(32) NOT NULL,
    `vmName` varchar(255) DEFAULT NULL,
    `state` varchar(64) NOT NULL,
    `accountUuid` varchar(32) NOT NULL,
    `dateInLong` bigint(20) unsigned NOT NULL,
    `resourcePriceUserConfig` varchar(1024) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`id`),
    KEY `idxBareMetal2VmUsageVOaccountUuid` (`accountUuid`,`dateInLong`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `BareMetalBillingVO`
(
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `bareMetal2ChassisOfferingUUid` varchar(32) NOT NULL,
    `bareMetal2ChassisOfferingName` varchar(255) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `id` (`id`)
)ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO AccountResourceRefVO (`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`) SELECT "36c27e8ff05c4780bf6d2fa65700f22e", "36c27e8ff05c4780bf6d2fa65700f22e", t.uuid, "BareMetal2ChassisOfferingVO", 2, 0, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), "org.zstack.baremetal2.configuration.BareMetal2ChassisOfferingVO" FROM BareMetal2ChassisOfferingVO t where t.uuid NOT IN (SELECT resourceUuid FROM AccountResourceRefVO);
