CREATE TABLE  `zstack`.`SchedulerVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `targetResourceUuid` varchar(32) NOT NULL,
    `schedulerName` varchar(255) NOT NULL,
    `schedulerDescription` varchar(2048) DEFAULT NULL,
    `schedulerType` varchar(255) NOT NULL,
    `schedulerInterval` int unsigned DEFAULT NULL,
    `repeatCount` int unsigned DEFAULT NULL,
    `cronScheduler` varchar(255),
    `jobName` varchar(255),
    `jobGroup` varchar(255),
    `triggerName` varchar(255),
    `triggerGroup` varchar(255),
    `jobClassName` varchar(255),
    `jobData` varchar(65535),
    `status` varchar(255),
    `managementNodeUuid` varchar(32) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
    `startDate` timestamp,
    `stopDate` timestamp,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
ALTER TABLE SftpBackupStorageVO change column port sshPort int unsigned DEFAULT 22;
ALTER TABLE SchedulerVO ADD CONSTRAINT fkSchedulerVOManagementNodeVO FOREIGN KEY (managementNodeUuid) REFERENCES ManagementNodeVO (uuid) ON DELETE SET NULL;
