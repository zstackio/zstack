-- ----------------------------
--  Table structure for `SnapshotUsageVO`
-- ----------------------------
CREATE TABLE `SnapShotUsageVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `volumeUuid` varchar(32) NOT NULL,
  `SnapshotUuid` varchar(32) NOT NULL,
  `SnapshotStatus` varchar(64) NOT NULL,
  `SnapshotName` varchar(255) NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `SnapshotSize` bigint unsigned NOT NULL,
  `dateInLong` bigint unsigned NOT NULL,
  `inventory` text,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS EcsInstanceConsoleProxyVO;

ALTER TABLE OssBucketVO MODIFY COLUMN bucketName varchar(64);
ALTER TABLE OssBucketVO ADD COLUMN regionName varchar(64) DEFAULT NULL;
ALTER TABLE OssBucketVO modify COLUMN bucketName varchar(64);
-- ----------------------------
--  Table structure for `SchedulerJobVO`
-- ----------------------------
CREATE TABLE  `zstack`.`SchedulerJobVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `targetResourceUuid` varchar(32) NOT NULL,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `jobClassName` varchar(255),
    `jobData` varchar(65535),
    `state` varchar(255),
    `managementNodeUuid` varchar(32) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkSchedulerJobVOManagementNodeVO` FOREIGN KEY (`managementNodeUuid`) REFERENCES `ManagementNodeVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `SchedulerTriggerVO`
-- ----------------------------
CREATE TABLE  `zstack`.`SchedulerTriggerVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `schedulerType` varchar(255) NOT NULL,
    `schedulerInterval` int unsigned DEFAULT NULL,
    `repeatCount` int unsigned DEFAULT NULL,
    `managementNodeUuid` varchar(32) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `startTime` timestamp,
    `stopTime` timestamp,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkSchedulerTriggerVOManagementNodeVO` FOREIGN KEY (`managementNodeUuid`) REFERENCES `ManagementNodeVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `SchedulerJobSchedulerTriggerRefVO`
-- ----------------------------
CREATE TABLE  `zstack`.`SchedulerJobSchedulerTriggerRefVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `schedulerJobUuid` varchar(32) NOT NULL,
    `schedulerTriggerUuid` varchar(32) NOT NULL,
    `jobGroup` varchar(255),
    `triggerGroup` varchar(255),
    `taskData` varchar(65535),
    `taskClassName` varchar(255),
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkSchedulerJobSchedulerTriggerRefVOSchedulerJobVO` FOREIGN KEY (`schedulerJobUuid`) REFERENCES `SchedulerJobVO` (`uuid`),
    CONSTRAINT `fkSchedulerJobSchedulerTriggerRefVOSchedulerTriggerVO` FOREIGN KEY (`schedulerTriggerUuid`) REFERENCES `SchedulerTriggerVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VRouterRouteTableVO` (
  `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
  `type` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VRouterRouteEntryVO` (
  `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
  `description` varchar(2048) DEFAULT NULL,
  `routeTableUuid` varchar(32) NOT NULL,
  `destination` varchar(64) NOT NULL,
  `target` varchar(64) DEFAULT NULL,
  `type` varchar(32) NOT NULL,
  `distance` int unsigned NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  CONSTRAINT fkRouteEntryVOVRouterRouteTableVO FOREIGN KEY (`routeTableUuid`) REFERENCES `zstack`.`VRouterRouteTableVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VirtualRouterVRouterRouteTableRefVO` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  `virtualRouterVmUuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
  `routeTableUuid` varchar(32) NOT NULL,
  CONSTRAINT `VirutalRouterVRouterRouteTableRefVOVRouterRouteTableVO` FOREIGN KEY (`routeTableUuid`) REFERENCES `zstack`.`VRouterRouteTableVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `VirutalRouterVRouterRouteTableRefVOVirtualRouterVmVO` FOREIGN KEY (`virtualRouterVmUuid`) REFERENCES `zstack`.`VirtualRouterVmVO` (`uuid`) ON DELETE CASCADE,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `ConsoleProxyAgentVO` ADD `consoleProxyOverriddenIp` varchar(255) NOT NULL;


CREATE TABLE `OssUploadPartsVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uploadId` varchar(32) NOT NULL,
  `ossBucketUuid` varchar(32) NOT NULL,
  `fileKey` varchar(128) NOT NULL,
  `partNumber` varchar(32) NOT NULL,
  `eTag` varchar(32) NOT NULL,
  `partSize` bigint(32) NOT NULL,
  `partCRC` bigint(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  CONSTRAINT fkOssUploadPartsVOOssBucketVO FOREIGN KEY (ossBucketUuid) REFERENCES OssBucketVO (uuid) ON DELETE CASCADE,
  PRIMARY KEY (`id`),
  KEY `uploadId` (`uploadId`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Add name and description for Baremetal Resources
-- ----------------------------
ALTER TABLE `BaremetalPxeServerVO` ADD COLUMN `name` varchar(255) DEFAULT NULL COMMENT 'baremetal pxeserver name';
ALTER TABLE `BaremetalPxeServerVO` ADD COLUMN `description` varchar(2048) DEFAULT NULL COMMENT 'baremetal pxeserver description';
ALTER TABLE `BaremetalChassisVO` ADD COLUMN `name` varchar(255) DEFAULT NULL COMMENT 'baremetal chassis name';
ALTER TABLE `BaremetalChassisVO` ADD COLUMN `description` varchar(2048) DEFAULT NULL COMMENT 'baremetal chassis description';

CREATE TABLE `ConnectionRelationShipVO` (
  `uuid` varchar(32) NOT NULL UNIQUE,
  `name` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `relationShips` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `HybridConnectionRefVO` (
  `uuid` varchar(32) NOT NULL UNIQUE,
  `resourceType` varchar(32) NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `direction` varchar(16) NOT NULL,
  `connectionType` varchar(32) NOT NULL,
  `connectionUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  CONSTRAINT fkHybridConnectionRefVOConnectionRelationShipVO FOREIGN KEY (connectionUuid) REFERENCES ConnectionRelationShipVO (uuid) ON DELETE CASCADE,
  PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS AvailableIdentityZonesVO;

