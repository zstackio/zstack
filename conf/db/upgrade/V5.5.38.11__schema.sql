CREATE TABLE IF NOT EXISTS `zstack`.`FineTuneRunVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `modelServiceInstanceGroupUuid` VARCHAR(32) NOT NULL,
    `generation` INT NOT NULL,
    `runId` VARCHAR(64) NOT NULL,
    `modelUuid` VARCHAR(32) DEFAULT NULL,
    `modelVersion` VARCHAR(255) DEFAULT NULL,
    `datasetUuids` TEXT,
    `state` VARCHAR(16) NOT NULL,
    `phase` VARCHAR(32) DEFAULT NULL,
    `acceptedAt` TIMESTAMP NULL DEFAULT NULL,
    `finishedAt` TIMESTAMP NULL DEFAULT NULL,
    `lastReportedAt` TIMESTAMP(3) NULL DEFAULT NULL,
    `exitCode` INT DEFAULT NULL,
    `error` TEXT,
    `nodes` MEDIUMTEXT,
    `createDate` TIMESTAMP NOT NULL DEFAULT '2000-01-01 00:00:00',
    `lastOpDate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uk_fine_tune_run_group_generation_id` (`modelServiceInstanceGroupUuid`, `generation`, `runId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CALL ADD_COLUMN('DatasetVO', 'version', 'INT', 0, 1);
CALL ADD_COLUMN('FineTuneRunVO', 'datasetVersions', 'TEXT', 1, NULL);
