DELETE FROM `SystemTagVO` WHERE `resourceUuid` IN (SELECT uuid FROM HostCapacityVO WHERE cpuSockets != 1) AND tag LIKE "cpuProcessorNum::%";

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

ALTER TABLE `zstack`.`LoadBalancerServerGroupVO` ADD COLUMN `ipVersion` int(10) unsigned NOT NULL DEFAULT 4 AFTER `loadBalancerUuid`;
UPDATE `zstack`.`VipVO` set serviceProvider='SLB' where uuid in (select vipUuid from LoadBalancerVO where type='SLB');
UPDATE `zstack`.`VipVO` set serviceProvider='SLB' where uuid in (select ipv6VipUuid from LoadBalancerVO where type='SLB');

ALTER TABLE `zstack`.`VmVfNicVO` ADD COLUMN `haState` varchar(32) NOT NULL DEFAULT "Disabled" AFTER `pciDeviceUuid`;

CREATE TABLE IF NOT EXISTS `zstack`.`VpcSharedQosVO` (
    `uuid`          varchar(32)  NOT NULL UNIQUE,
    `name`          varchar(255) NOT NULL,
    `description`   varchar(255)          DEFAULT NULL,
    `l3NetworkUuid` varchar(32)  NOT NULL,
    `vpcUuid`       varchar(32)           DEFAULT NULL,
    `bandwidth`     bigint unsigned,
    `lastOpDate`    timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate`    timestamp    NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT fkVpcSharedQosVOL3NetworkEO FOREIGN KEY (l3NetworkUuid) REFERENCES L3NetworkEO (uuid) ON DELETE CASCADE,
    CONSTRAINT fkVpcSharedQosVOApplianceVmVO FOREIGN KEY (vpcUuid) REFERENCES ApplianceVmVO (uuid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`VpcSharedQosRefVipVO` (
    `id`            bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `sharedQosUuid` varchar(32) NOT NULL,
    `vipUuid`       varchar(32) NOT NULL,
    `lastOpDate`    timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate`    timestamp   NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`id`),
    CONSTRAINT fkVpcSharedQosRefVipVOVpcSharedQosVO FOREIGN KEY (sharedQosUuid) REFERENCES VpcSharedQosVO (uuid) ON DELETE CASCADE,
    CONSTRAINT fkVpcSharedQosRefVipVOVipVO FOREIGN KEY (vipUuid) REFERENCES VipVO (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ExponBlockVolumeVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `exponStatus` varchar(32) NULL,
    PRIMARY KEY (`uuid`),
    CONSTRAINT fkExponBlockVolumeVOBlockVolumeVO FOREIGN KEY (uuid) REFERENCES BlockVolumeVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
