CREATE TABLE IF NOT EXISTS `zstack`.`L2VirtualSwitchNetworkVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `isDistributed` boolean NOT NULL DEFAULT TRUE,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`L2PortGroupNetworkVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `vSwitchUuid` varchar(32) NOT NULL,
    `vlanMode` varchar(32) NOT NULL default 'ACCESS',
    `vlanId` int unsigned NOT NULL,
    `vlanRanges` varchar(256) default NULL,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkL2PortGroupNetworkVOL2VirtualSwitchNetworkVO` FOREIGN KEY (`vSwitchUuid`) REFERENCES L2VirtualSwitchNetworkVO (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  IF NOT EXISTS `zstack`.`L2NetworkHostRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `l2NetworkUuid` varchar(32) NOT NULL,
    `hostUuid` varchar(32) NOT NULL,
    `l2ProviderType` varchar(32) default NULL,
    `attachStatus` varchar(32) default NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkL2NetworkHostRefVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES HostEO (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkL2NetworkHostRefVOL2NetworkEO` FOREIGN KEY (`l2NetworkUuid`) REFERENCES L2NetworkEO (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`L2NetworkClusterRefVO` ADD COLUMN `l2ProviderType` varchar(32) default NULL;
