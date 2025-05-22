CREATE TABLE IF NOT EXISTS `zstack`.`LicenseAuthorizedNodeVO` (
    `uuid` char(32) NOT NULL UNIQUE,
    `ip` varchar(255) NOT NULL,
    `lastSyncDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    `status` varchar(64) NOT NULL,
    `type` varchar(64) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`LicenseAuthorizedCapacityVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `uuid` char(32) NOT NULL UNIQUE,
    `resourceUuid` char(32) DEFAULT NULL,
    `quotaType` varchar(64) NOT NULL,
    `quota` bigint unsigned DEFAULT 0,
    `licenseType` varchar(64) NOT NULL,
    `type` varchar(64) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    PRIMARY KEY (`id`),
    CONSTRAINT `fkLicenseAuthorizedCapacityLicenseAuthorizedNode` FOREIGN KEY (`uuid`) REFERENCES `LicenseAuthorizedNodeVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`LicenseAuthorizeHistoryVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `uuid` char(32) NOT NULL UNIQUE,
    `resourceUuid` char(32) DEFAULT NULL,
    `quotaType` varchar(64) NOT NULL,
    `usageFrom` bigint unsigned DEFAULT 0,
    `usageTo` bigint unsigned DEFAULT NULL,
    `quota` bigint unsigned DEFAULT 0,
    `licenseType` varchar(64) NOT NULL,
    `type` varchar(64) NOT NULL,
    `action` varchar(255) NOT NULL,
    `result` varchar(64) DEFAULT NULL,
    `error` text DEFAULT NULL,
    `requestUuid` char(32) NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
