
CALL ADD_COLUMN('SdnControllerVO', 'vendorVersion', 'VARCHAR(32)', 0, 'V1');

CREATE TABLE IF NOT EXISTS `zstack`.`H3cSdnControllerTenantVO` (
                                                                   `uuid` varchar(32) NOT NULL UNIQUE,
    `sdnControllerUuid` varchar(32) NOT NULL,
    `tenantUuid` varchar(255) DEFAULT NULL,
    `vdsUuid` varchar(255) DEFAULT NULL,
    `tenantName` varchar(255) DEFAULT NULL,
    `vdsName` varchar(255) DEFAULT NULL,
    `cloudDomainName` varchar(255) DEFAULT NULL,
    `state` varchar(32) NOT NULL DEFAULT "Enabled",
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkH3cSdnControllerTenantVOSdnControllerVO` FOREIGN KEY (`sdnControllerUuid`) REFERENCES `SdnControllerVO` (`uuid`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`H3cSdnSubnetIpRangeRefVO` (
                                                                   `id` BIGINT UNSIGNED NOT NULL UNIQUE AUTO_INCREMENT,
                                                                   `sdnControllerUuid` varchar(32) NOT NULL,
    `ipRangeUuid` varchar(32) NOT NULL,
    `subnetUuid` varchar(255) NOT NULL,
    `l2NetworkUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`id`),
    CONSTRAINT `fkH3cSdnSubnetIpRangeRefVOSdnControllerVO` FOREIGN KEY (`sdnControllerUuid`) REFERENCES `SdnControllerVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkH3cSdnSubnetIpRangeRefVOIpRangeVO` FOREIGN KEY (`ipRangeUuid`) REFERENCES `IpRangeEO` (`uuid`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
