CREATE TABLE IF NOT EXISTS `zstack`.`HostNetworkInterfaceServiceRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `interfaceUuid` varchar(32) NOT NULL,
    `vlanId` int(32) NOT NULL DEFAULT 0,
    `ipAddresses` varchar(255) DEFAULT NULL,
    `gateway` varchar(128) DEFAULT NULL,
    `serviceType` varchar(128) DEFAULT 'network',
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkHostNetworkInterfaceServiceRefVOHostNetworkInterfaceVO` FOREIGN KEY (`interfaceUuid`) REFERENCES HostNetworkInterfaceVO (`uuid`)  ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`HostNetworkBondingServiceRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `bondingUuid` varchar(32) NOT NULL,
    `vlanId` int(32) NOT NULL DEFAULT 0,
    `ipAddresses` varchar(255) DEFAULT NULL,
    `gateway` varchar(128) DEFAULT NULL,
    `serviceType` varchar(128) DEFAULT 'network',
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkHostNetworkBodnuingServiceRefVOHostNetworkBondingVO` FOREIGN KEY (`bondingUuid`) REFERENCES HostNetworkBondingVO (`uuid`)  ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


ALTER TABLE `zstack`.`HostNetworkInterfaceVO` ADD COLUMN `gateway` varchar(128) DEFAULT NULL;
ALTER TABLE `zstack`.`HostNetworkInterfaceVO` ADD COLUMN `description` varchar(2048) DEFAULT NULL;

ALTER TABLE `zstack`.`HostNetworkBondingVO` ADD COLUMN `speed` BIGINT UNSIGNED DEFAULT NULL;
ALTER TABLE `zstack`.`HostNetworkBondingVO` ADD COLUMN `gateway` varchar(128) DEFAULT NULL;
ALTER TABLE `zstack`.`HostNetworkBondingVO` ADD COLUMN `description` varchar(2048) DEFAULT NULL;
