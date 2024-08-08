CREATE TABLE IF NOT EXISTS `zstack`.`GuestVmScriptEO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `name` VARCHAR(256) NOT NULL,
    `description` VARCHAR(256),
    `platform` VARCHAR(255) NOT NULL,
    `scriptContent` MEDIUMTEXT,
    `renderParams` MEDIUMTEXT,
    `scriptType` VARCHAR(32) NOT NULL,
    `scriptTimeout` INT UNSIGNED NOT NULL,
    `version` INT UNSIGNED NOT NULL,
    `deleted` VARCHAR(255) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP VIEW IF EXISTS `zstack`.`GuestVmScriptVO`;
CREATE VIEW `zstack`.`GuestVmScriptVO` AS SELECT uuid, name, description, platform, scriptContent, renderParams, scriptType, scriptTimeout, version, createDate, lastOpDate FROM `zstack`.`GuestVmScriptEO` WHERE deleted IS NULL;

CREATE TABLE IF NOT EXISTS `zstack`.`GuestVmScriptExecutedRecordVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `recordName` VARCHAR(255) NOT NULL,
    `scriptUuid` VARCHAR(32) NOT NULL,
    `scriptTimeout` INT UNSIGNED NOT NULL,
    `status` VARCHAR(256) NOT NULL,
    `version` INT UNSIGNED NOT NULL,
    `Executor` VARCHAR(256) NOT NULL ,
    `ExecutionCount` INT UNSIGNED NOT NULL,
    `scriptContent` MEDIUMTEXT,
    `renderParams` MEDIUMTEXT,
    `startTime` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00',
    `endTime` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    INDEX `idxScriptUuid` (`scriptUuid`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`GuestVmScriptExecutedRecordDetailVO` (
    `recordUuid` VARCHAR(32) NOT NULL,
    `vmInstanceUuid` VARCHAR(32) NOT NULL,
    `vmName` VARCHAR(255) NOT NULL,
    `status` VARCHAR(128) NOT NULL,
    `exitCode` INT UNSIGNED,
    `stdout` MEDIUMTEXT,
    `errCause` MEDIUMTEXT,
    `stderr` MEDIUMTEXT,
    `startTime` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00',
    `endTime` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`recordUuid`, `vmInstanceUuid`),
    CONSTRAINT `fkGuestVmScriptExecutedRecordDetailVOScriptExecutedRecordVO` FOREIGN KEY (`recordUuid`) REFERENCES `GuestVmScriptExecutedRecordVO` (`uuid`) ON DELETE CASCADE
)ENGINE=InnoDB DEFAULT CHARSET=utf8;