CREATE TABLE IF NOT EXISTS `zstack`.`VxlanMappingVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `vni` int NOT NULL,
    `hostUuid` varchar(32) NOT NULL,
    `vlanId` int,
    `physicalInterface` varchar(32),
    `createDate`   timestamp    NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate`   timestamp    NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkVxlanMappingVOVxlanNetworkVO` FOREIGN KEY (`vni`) REFERENCES `VxlanNetworkVO` (`vni`) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `fkVxlanMappingVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`PortLldpInfoVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `l2NetworkUuid` varchar(32) NOT NULL,
    `interfaceName` varchar(32) NOT NULL,
    `interfaceType` varchar(32),
    `bondIfName` varchar(32),
    `portName` varchar(32),
    `systemName` varchar(32),
    `chassisMac` varchar(32),
    `aggregated` boolean NOT NULL DEFAULT FALSE,
    `aggregatedPortID` int(10),
    `createDate`   timestamp    NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate`   timestamp    NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkPortLldpInfoVOL2NetworkEO` FOREIGN KEY (`l2NetworkUuid`) REFERENCES  `L2NetworkEO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;