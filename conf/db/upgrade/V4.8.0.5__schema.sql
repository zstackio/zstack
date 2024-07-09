ALTER TABLE `zstack`.`L2VirtualSwitchNetworkVO` ADD COLUMN `vSwitchIndex` INT unsigned DEFAULT NULL AFTER `uuid`;

CREATE TABLE IF NOT EXISTS `zstack`.`UplinkGroupVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `interfaceName` varchar(32) NOT NULL,
    `vSwitchUuid` varchar(32) NOT NULL,
    `hostUuid` varchar(32) NOT NULL,
    `type` varchar(32) NOT NULL,
    `bondingUuid` varchar(32) DEFAULT NULL,
    `interfaceUuid` varchar(32) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkUplinkGroupVOL2VirtualSwitchNetworkVO` FOREIGN KEY (`vSwitchUuid`) REFERENCES L2VirtualSwitchNetworkVO (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkUplinkGroupVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkUplinkGroupVOHostNetworkBondingVO` FOREIGN KEY (`bondingUuid`) REFERENCES HostNetworkBondingVO (`uuid`) ON DELETE SET NULL,
    CONSTRAINT `fkUplinkGroupVOHostNetworkInterfaceVO` FOREIGN KEY (`interfaceUuid`) REFERENCES HostNetworkInterfaceVO (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
