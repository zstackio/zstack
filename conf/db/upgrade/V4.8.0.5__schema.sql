ALTER TABLE `zstack`.`L2VirtualSwitchNetworkVO` ADD COLUMN `vSwitchIndex` INT unsigned DEFAULT NULL AFTER `uuid`;
DELETE FROM `zstack`.`L2NetworkHostRefVO` WHERE `attachStatus` = 'Detached';
ALTER TABLE `zstack`.`L2NetworkHostRefVO` DROP COLUMN `attachStatus`;
ALTER TABLE `zstack`.`L2NetworkHostRefVO` ADD COLUMN `bridgeName` varchar(16) DEFAULT NULL AFTER `l2ProviderType`;

CREATE TABLE IF NOT EXISTS `zstack`.`UplinkGroupVO` (
    `id` bigint unsigned NOT NULL UNIQUE,
    `interfaceName` varchar(32) NOT NULL,
    `type` varchar(32) NOT NULL,
    `bondingUuid` varchar(32) DEFAULT NULL,
    `interfaceUuid` varchar(32) DEFAULT NULL,
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkUplinkGroupVOHostNetworkBondingVO` FOREIGN KEY (`bondingUuid`) REFERENCES HostNetworkBondingVO (`uuid`) ON DELETE SET NULL,
    CONSTRAINT `fkUplinkGroupVOHostNetworkInterfaceVO` FOREIGN KEY (`interfaceUuid`) REFERENCES HostNetworkInterfaceVO (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
