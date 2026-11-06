CREATE TABLE IF NOT EXISTS `zstack`.`NetProbeTaskVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `hostUuid` varchar(32) NOT NULL,
    `type` varchar(16) NOT NULL,
    `target` varchar(253) DEFAULT NULL,
    `port` int DEFAULT NULL,
    `url` varchar(2048) DEFAULT NULL,
    `intervalSeconds` int NOT NULL,
    `timeoutSeconds` int NOT NULL,
    `nextRunAt` datetime NOT NULL,
    `lastStatus` varchar(32) NOT NULL,
    `lastLatencyMs` double DEFAULT NULL,
    `lastRunAt` datetime DEFAULT NULL,
    `lastError` varchar(1024) DEFAULT NULL,
    `createDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    INDEX `idxNetProbeTaskVOnextRunAt` (`nextRunAt`),
    CONSTRAINT `fkNetProbeTaskVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`NetProbeResultVO` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT,
    `taskUuid` varchar(32) NOT NULL,
    `runAt` datetime NOT NULL,
    `success` tinyint(1) NOT NULL,
    `latencyMs` double DEFAULT NULL,
    `lossPercent` double DEFAULT NULL,
    `httpStatus` int DEFAULT NULL,
    `error` varchar(1024) DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX `idxNetProbeResultVOtaskUuidrunAt` (`taskUuid`, `runAt`),
    CONSTRAINT `fkNetProbeResultVONetProbeTaskVO` FOREIGN KEY (`taskUuid`) REFERENCES `NetProbeTaskVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
