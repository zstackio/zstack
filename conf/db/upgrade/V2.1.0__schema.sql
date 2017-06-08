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
alter table `ConsoleProxyAgentVO` add `consoleProxyOverriddenIp` varchar(255) NOT NULL;
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
    `status` varchar(255),
    `state` varchar(255),
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkSchedulerJobSchedulerTriggerRefVOSchedulerJobVO` FOREIGN KEY (`schedulerJobUuid`) REFERENCES `SchedulerJobVO` (`uuid`),
    CONSTRAINT `fkSchedulerJobSchedulerTriggerRefVOSchedulerTriggerVO` FOREIGN KEY (`schedulerTriggerUuid`) REFERENCES `SchedulerTriggerVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

