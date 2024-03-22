CREATE TABLE IF NOT EXISTS `zstack`.`SlbGroupMonitorIpVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `slbGroupUuid` varchar(32) NOT NULL,
    `monitorIp` varchar(128) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkSlbGroupConfigTaskVOSlbGroupVO` FOREIGN KEY (`slbGroupUuid`) REFERENCES `SlbGroupVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`SlbVmInstanceConfigTaskVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `vmInstanceUuid` varchar(32) NOT NULL,
    `configVersion` bigint unsigned DEFAULT 0 UNIQUE,
    `taskName` varchar(32) NOT NULL,
    `taskData` text NOT NULL,
    `lastFailedReason` varchar(1024) NOT NULL,
    `retryNumber` bigint unsigned DEFAULT 0,
    `status` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkSlbVmInstanceConfigTaskVOSlbVmInstanceVO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `SlbVmInstanceVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`SlbGroupVO` ADD COLUMN `configVersion` bigint unsigned DEFAULT 0;
ALTER TABLE `zstack`.`SlbVmInstanceVO` ADD COLUMN `configVersion` bigint unsigned DEFAULT 0;
UPDATE `zstack`.`SlbGroupVO` SET deployType = "NoHA";

ALTER TABLE `zstack`.`LoadBalancerServerGroupVO` ADD COLUMN `ipVersion` int(10) unsigned DEFAULT 4 AFTER `loadBalancerUuid`;
