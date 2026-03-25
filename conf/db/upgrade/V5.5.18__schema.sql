-- ZNS SDN Controller support

CREATE TABLE IF NOT EXISTS `ZnsControllerVO` (
    `uuid` varchar(32) NOT NULL,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkZnsControllerVOSdnControllerVO` FOREIGN KEY (`uuid`) REFERENCES `SdnControllerVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `L2GeneveNetworkVO` (
    `uuid` varchar(32) NOT NULL,
    `geneveId` int(10) unsigned NOT NULL,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkL2GeneveNetworkVOL2NetworkEO` FOREIGN KEY (`uuid`) REFERENCES `L2NetworkEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `ZnsTransportZoneVO` (
    `uuid` varchar(32) NOT NULL,
    `name` varchar(255) DEFAULT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `type` varchar(64) DEFAULT NULL,
    `physicalNetwork` varchar(255) DEFAULT NULL,
    `status` varchar(64) DEFAULT NULL,
    `isDefault` tinyint(1) NOT NULL DEFAULT 0,
    `tags` text DEFAULT NULL,
    `znsSdnControllerUuid` varchar(32) NOT NULL,
    `createDate`         timestamp       NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate`         timestamp       NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkZnsTransportZoneVOSdnControllerVO` FOREIGN KEY (`znsSdnControllerUuid`) REFERENCES `SdnControllerVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

