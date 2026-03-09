CREATE TABLE IF NOT EXISTS `UKeyLicenseVO` (
    `keyId` varchar(32) NOT NULL UNIQUE,
    `managementNodeUuid` varchar(32) NOT NULL,
    `status` varchar(32) NOT NULL,
    `offline` bigint unsigned NOT NULL,
    `online` bigint unsigned NOT NULL,
    `recover` bigint unsigned NOT NULL,
    `license` text NOT NULL,
    INDEX `idxUKeyLicenseVOmanagementNodeUuid` (`managementNodeUuid`),
    PRIMARY KEY  (`keyId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
