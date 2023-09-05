CREATE TABLE IF NOT EXISTS `zstack`.`ClusterDpmVO` (
    `uuid` varchar(32) not null unique,
    `name` varchar(256) not null,
    `clusterUuid` varchar(32) not null,
    `recycleCpuThreshold` double unsigned not null,
    `recycleMemoryThreshold` double unsigned not null,
    `wakeupCpuThreshold` double unsigned not null,
    `wakeupMemoryThreshold` double unsigned not null,
    `thresholdDurationInMinutes` int unsigned not null,
    `thresholdIntervalInMinutes` int unsigned not null,
    `effectiveStartTime` varchar(32) not null,
    `effectiveEndTime` varchar(32) not null,
    `enable` boolean not null default FALSE,
    `description` varchar(255) default null,
    `createDate` timestamp not null default '0000-00-00 00:00:00',
    `lastOpDate` timestamp not null default '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `clusterUuid` (`clusterUuid`),
    CONSTRAINT `fkClusterDpmVOClusterEO` FOREIGN KEY (`clusterUuid`) REFERENCES `ClusterEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ClusterDpmRecordsVO` (
    `uuid` varchar(32) not null unique,
    `dpmUuid` varchar(32) not null,
    `hostUuid` varchar(32) default null,
    `hostName` varchar(256) default null,
    `cpuThreshold` double unsigned default null,
    `memoryThreshold` double unsigned default null,
    `status`  varchar(255) not null,
    `operation`  varchar(255) not null,
    `reason`  varchar(255) default null,
    `errorDetail` MEDIUMTEXT default null,
    `createDate` timestamp not null default '0000-00-00 00:00:00',
    `lastOpDate` timestamp not null default '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    KEY `dpmUuid` (`dpmUuid`),
    CONSTRAINT `fkClusterDpmRecordsVOClusterDpmVO` FOREIGN KEY (`dpmUuid`) REFERENCES `ClusterDpmVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;