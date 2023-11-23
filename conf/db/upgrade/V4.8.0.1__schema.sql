UPDATE `zstack`.`L2NetworkHostRefVO` set `attachStatus` = 'Attached' WHERE `attachStatus` = '3';
UPDATE `zstack`.`L2NetworkHostRefVO` set `attachStatus` = 'Detached' WHERE `attachStatus` != '3';

CREATE TABLE IF NOT EXISTS `zstack`.`HostNetworkInterfaceLldpVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `interfaceUuid` varchar(32) NOT NULL UNIQUE,
    `mode` varchar(32) NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkHostNetworkInterfaceLldpVOHostNetworkInterfaceVO` FOREIGN KEY (`interfaceUuid`) REFERENCES HostNetworkInterfaceVO (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`HostNetworkInterfaceLldpRefVO` (
    `lldpUuid` varchar(32) NOT NULL,
    `chassisId` varchar(32) NOT NULL,
    `timeToLive` int(32) NOT NULL,
    `managementAddress` varchar(32) DEFAULT NULL,
    `systemName` varchar(32) NOT NULL,
    `systemDescription` varchar(255) NOT NULL,
    `systemCapabilities` varchar(32) NOT NULL,
    `portId` varchar(32) NOT NULL,
    `portDescription` varchar(255) DEFAULT NULL,
    `vlanId` int(32) DEFAULT NULL,
    `aggregationPortId` bigint unsigned DEFAULT NULL,
    `mtu` varchar(128) DEFAULT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY  (`lldpUuid`),
    CONSTRAINT `fkHostNetworkInterfaceLldpRefVOHostNetworkInterfaceLldpVO` FOREIGN KEY (`lldpUuid`) REFERENCES HostNetworkInterfaceLldpVO (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT IGNORE INTO `zstack`.`HostNetworkInterfaceLldpVO` (`uuid`, `interfaceUuid`, `mode`, `createDate`, `lastOpDate`)
SELECT REPLACE(UUID(),'-',''), t.uuid, 'rx_only', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP() FROM `zstack`.`HostNetworkInterfaceLldpVO` t;

ALTER TABLE ConsoleProxyVO ADD COLUMN `expiredDate` timestamp NOT NULL;