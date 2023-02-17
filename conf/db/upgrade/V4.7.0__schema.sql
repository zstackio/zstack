CREATE TABLE IF NOT EXISTS `zstack`.`ExternalManagementNodeVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(128) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `hostName` varchar(255) NOT NULL,
    `port` int unsigned NOT NULL,
    `status` varchar(32) NOT NULL,
    `accessKeyID` VARCHAR(128) NOT NULL,
    `accessKeySecret` VARCHAR(128) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`MirrorCdpTaskVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(128) NOT NULL,
    `description` varchar(256) DEFAULT NULL,
    `externalManagementNodeUuid` varchar(32) NOT NULL,
    `peerCdpTaskUuid` varchar(32) NOT NULL,
    `mode` varchar(128) NOT NULL,
    `peerBackupStorageUuid` varchar(32) NOT NULL,
    `mirrorResourceUuid` varchar(128) DEFAULT NULL,
    `mirrorResourceType` varchar(255) DEFAULT NULL,
    `status` varchar(128) DEFAULT NULL,
    `state` varchar(255) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`VmTemplateVO` (
    `uuid` varchar(32) NOT NULL,
    `name` varchar(256) NOT NULL,
    `description` varchar(256) DEFAULT NULL,
    `type` varchar(256) DEFAULT NULL,
    `instanceOfferingUuid` varchar(32) DEFAULT NULL,
    `imageUuid` varchar(32) DEFAULT NULL,
    `memorySize` bigint(20) unsigned DEFAULT NULL,
    `cpuNum` int(10) unsigned DEFAULT NULL,
    `clusterUuid` varchar(32) DEFAULT NULL,
    `l3NetworkUuids` text DEFAULT NULL,
    `rootDiskOfferingUuid` varchar(32) DEFAULT NULL,
    `dataDiskOfferingUuids` text,
    `zoneUuid` varchar(32) DEFAULT NULL,
    `hostUuid` varchar(32) DEFAULT NULL,
    `rootVolumeSystemTags` varchar(2048) DEFAULT NULL,
    `dataVolumeSystemTags` varchar(2048) DEFAULT NULL,
    `primaryStorageUuidForRootVolume` varchar(32) DEFAULT NULL,
    `primaryStorageUuidForDataVolume` varchar(32) DEFAULT NULL,
    `defaultL3NetworkUuid` varchar(32) DEFAULT NULL,
    `strategy` text DEFAULT NULL,
    `systemTags` text DEFAULT NULL,
    `tagPatternUuids` text DEFAULT NULL,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`DisasterRecoveryVmTemplateVO` (
    `uuid` varchar(32) NOT NULL,
    `mirrorCdpTaskUuid` varchar(32) NOT NULL,
    `templateType` varchar(32) DEFAULT NULL,
    `failbackMode` varchar(32) DEFAULT NULL,
    `useExistingVolume` tinyint unsigned DEFAULT 0,
    `groupId` bigint(20) DEFAULT NULL,
    `originVmInstanceUuid` varchar(32) DEFAULT NULL,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uuid` (`uuid`),
    CONSTRAINT `fkDisasterRecoveryVmTempalateVOVmTemplateVO` FOREIGN KEY (`uuid`) REFERENCES `VmTemplateVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`MirrorCdpTaskRecoverRecordVO` (
    `uuid` varchar(32) NOT NULL,
    `mirrorCdpTaskUuid` varchar(32) NOT NULL,
    `resourceType` varchar(32) NOT NULL,
    `resourceUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uuid` (`uuid`),
    KEY `fkMirrorCdpTaskRecoverRecordVOResourceVO` (`resourceUuid`),
    KEY `fkMirrorCdpTaskRecoverRecordVOMirrorCdpTaskVO` (`mirrorCdpTaskUuid`),
    CONSTRAINT `fkMirrorCdpTaskRecoverRecordVOMirrorCdpTaskVO` FOREIGN KEY (`mirrorCdpTaskUuid`) REFERENCES `MirrorCdpTaskVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkMirrorCdpTaskRecoverRecordVOResourceVO` FOREIGN KEY (`resourceUuid`) REFERENCES `ResourceVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`MirrorCdpTaskScheduleJobVO` (
    `uuid` varchar(32) NOT NULL,
    `name` varchar(256) NOT NULL,
    `description` varchar(256) DEFAULT NULL,
    `status` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`MirrorCdpTaskScheduleJobTaskRefVO` (
    `uuid` varchar(32) NOT NULL,
    `mirrorCdpTaskUuid` varchar(32) NOT NULL,
    `scheduleJobUuid` varchar(32) NOT NULL,
    `parentTaskUuid` varchar(32) DEFAULT NULL,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uuid` (`uuid`),
    KEY `fkMirrorCdpTaskVOScheduleJobRefVO` (`mirrorCdpTaskUuid`),
    KEY `fkScheduleJobVOScheduleJobRefVO` (`scheduleJobUuid`),
    CONSTRAINT `fkMirrorCdpTaskVOScheduleJobRefVO` FOREIGN KEY (`mirrorCdpTaskUuid`) REFERENCES `MirrorCdpTaskVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkScheduleJobVOScheduleJobRefVO` FOREIGN KEY (`scheduleJobUuid`) REFERENCES `MirrorCdpTaskScheduleJobVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`TwinManagementNodeResourceMapVO` (
    `uuid` varchar(32) NOT NULL,
    `externalResourceUuid` varchar(32) NOT NULL,
    `resourceUuid` varchar(32) NOT NULL,
    `type` varchar(32) NOT NULL,
    `groupUuid` varchar(32) DEFAULT NULL,
    `externalManagementNodeUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
