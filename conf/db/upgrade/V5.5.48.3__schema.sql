CREATE TABLE IF NOT EXISTS `zstack`.`HostMemoryStateVO` (
    `hostUuid` VARCHAR(32) NOT NULL,
    `status` VARCHAR(32) NOT NULL,
    `featureState` VARCHAR(32) NOT NULL,
    `reason` VARCHAR(2048) DEFAULT NULL,
    `capabilities` TEXT DEFAULT NULL,
    `allowedActions` TEXT DEFAULT NULL,
    `uncertainRecovery` TEXT DEFAULT NULL,
    `activeJobUuid` VARCHAR(32) DEFAULT NULL,
    `lastObservedAt` BIGINT DEFAULT NULL,
    `createDate` TIMESTAMP NOT NULL DEFAULT '2000-01-01 00:00:00',
    `lastOpDate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`hostUuid`),
    CONSTRAINT `fkHostMemoryStateVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
