CREATE TABLE IF NOT EXISTS `zstack`.`HostNetworkInterfaceServiceRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `interfaceUuid` varchar(32) NOT NULL,
    `vlanId` int(32) NOT NULL DEFAULT 0,
    `serviceType` varchar(128) DEFAULT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkHostNetworkInterfaceServiceRefVOHostNetworkInterfaceVO` FOREIGN KEY (`interfaceUuid`) REFERENCES HostNetworkInterfaceVO (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`HostNetworkBondingServiceRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `bondingUuid` varchar(32) NOT NULL,
    `vlanId` int(32) NOT NULL DEFAULT 0,
    `serviceType` varchar(128) DEFAULT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkHostNetworkBodnuingServiceRefVOHostNetworkBondingVO` FOREIGN KEY (`bondingUuid`) REFERENCES HostNetworkBondingVO (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
