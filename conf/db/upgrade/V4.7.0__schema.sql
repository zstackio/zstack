ALTER TABLE `zstack`.`HostNetworkInterfaceVO` ADD COLUMN `gateway` varchar(128) DEFAULT NULL;
ALTER TABLE `zstack`.`HostNetworkInterfaceVO` ADD COLUMN `description` varchar(2048) DEFAULT NULL;

ALTER TABLE `zstack`.`HostNetworkBondingVO` ADD COLUMN `speed` BIGINT UNSIGNED DEFAULT NULL;
ALTER TABLE `zstack`.`HostNetworkBondingVO` ADD COLUMN `gateway` varchar(128) DEFAULT NULL;
ALTER TABLE `zstack`.`HostNetworkBondingVO` ADD COLUMN `description` varchar(2048) DEFAULT NULL;
