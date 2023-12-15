UPDATE `zstack`.`L2NetworkHostRefVO` set `attachStatus` = 'Detached' WHERE `attachStatus` = '0' or `attachStatus` = '1' or `attachStatus` = '2';
UPDATE `zstack`.`L2NetworkHostRefVO` set `attachStatus` = 'Attached' WHERE `attachStatus` = '3';

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
SELECT REPLACE(UUID(),'-',''), t.uuid, 'rx_only', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP() FROM `zstack`.`HostNetworkInterfaceVO` t;

CREATE TABLE IF NOT EXISTS `zstack`.`HostKernelInterfaceVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(255) DEFAULT NULL,
    `hostUuid` varchar(32) NOT NULL,
    `l2NetworkUuid` varchar(32) NOT NULL,
    `l3NetworkUuid` varchar(32) DEFAULT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkHostKernelInterfaceVOHostVO` FOREIGN KEY (`hostUuid`) REFERENCES HostEO (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkHostKernelInterfaceVOL2NetworkVO` FOREIGN KEY (`l2NetworkUuid`) REFERENCES L2NetworkEO (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkHostKernelInterfaceVOL3NetworkVO` FOREIGN KEY (`l3NetworkUuid`) REFERENCES L3NetworkEO (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`HostKernelInterfaceUsedIpVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `hostKernelInterfaceUuid` varchar(32) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkHostKernelInterfaceUsedIpVOUsedIpVO` FOREIGN KEY (`uuid`) REFERENCES UsedIpVO (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkHostKernelInterfaceUsedIpVOHostKernelInterfaceVO` FOREIGN KEY (`hostKernelInterfaceUuid`) REFERENCES HostKernelInterfaceVO (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`HostKernelInterfaceTrafficTypeVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `hostKernelInterfaceUuid` varchar(32) NOT NULL,
    `trafficType` varchar(128) NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkHostKernelInterfaceTrafficTypeVOHostKernelInterfaceVO` FOREIGN KEY (`hostKernelInterfaceUuid`) REFERENCES HostKernelInterfaceVO (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE ConsoleProxyVO ADD COLUMN `expiredDate` timestamp NOT NULL;
UPDATE ImageEO SET md5sum = NULL where md5sum != 'not calculated';

ALTER TABLE `zstack`.`KVMHostVO` ADD COLUMN `iscsiInitiatorName` varchar(256) DEFAULT NULL;