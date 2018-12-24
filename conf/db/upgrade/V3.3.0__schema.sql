CREATE TABLE IF NOT EXISTS `ElaborationVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `errorInfo` text NOT NULL,
  `md5sum` varchar(32) NOT NULL,
  `distance` double NOT NULL,
  `matched` boolean NOT NULL DEFAULT FALSE,
  `repeats` bigint(20) unsigned NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX idxElaborationVOmd5sum ON ElaborationVO (md5sum);

CREATE TABLE  `zstack`.`VCenterResourcePoolVO` (
    `uuid` varchar(32) NOT NULL COMMENT 'VCenter Resource Pool uuid',
    `vCenterClusterUuid` varchar(32) NOT NULL COMMENT 'VCenter cluster uuid',
    `name` varchar(256) NOT NULL COMMENT 'VCenter Resource Pool name',
    `morVal` varchar(256) NOT NULL COMMENT 'VCenter Resource Pool management object value in vcenter',
    `parentUuid` varchar(32) COMMENT 'Parent Resource Pool uuid or NULL',
    `CPULimit` bigint(64),
    `CPUOverheadLimit` bigint(64),
    `CPUReservation` bigint(64),
    `CPUShares` bigint(64),
    `CPULevel` varchar(64),
    `MemoryLimit` bigint(64),
    `MemoryOverheadLimit` bigint(64),
    `MemoryReservation` bigint(64),
    `MemoryShares` bigint(64),
    `MemoryLevel` varchar(64),
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkVCenterResourcePoolVOVCenterClusterVO` FOREIGN KEY (`vCenterClusterUuid`) REFERENCES `VCenterClusterVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`VCenterResourcePoolUsageVO` (
    `uuid` varchar(32) NOT NULL COMMENT 'VCenter Resource Pool usage uuid',
    `vCenterResourcePoolUuid` varchar(32) NOT NULL COMMENT 'VCenter Resource Pool uuid',
    `resourceUuid` varchar(32) NOT NULL COMMENT 'VCenter Resource resource uuid',
    `resourceType` varchar(256) NOT NULL COMMENT 'VCenter Resource resource type',
    `resourceName` varchar(256) COMMENT 'VCenter Resource resource name',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
     PRIMARY KEY  (`uuid`),
     UNIQUE KEY `VCenterResourcePoolUsageVO` (`vCenterResourcePoolUuid`, `resourceUuid`) USING BTREE,
     CONSTRAINT `fkVCenterResourcePoolUsageVOVCenterResourcePoolVO` FOREIGN KEY (`vCenterResourcePoolUuid`) REFERENCES `VCenterResourcePoolVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
