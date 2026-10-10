-- Memory optimization configuration, execution state, and durable receipts.
-- Repeated execution preserves existing records and stable system seeds.

CREATE TABLE IF NOT EXISTS `zstack`.`MemoryPolicyVO` (
    `uuid` VARCHAR(80) NOT NULL,
    `scope` VARCHAR(16) NOT NULL,
    `resourceUuid` VARCHAR(64) NOT NULL,
    `revision` BIGINT NOT NULL DEFAULT 0,
    `policy` LONGTEXT NOT NULL,
    `legacyPolicy` LONGTEXT DEFAULT NULL,
    `createDate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastOpDate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `memory_policy_scope_resource` (`scope`, `resourceUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`MemoryTaskVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `parentUuid` VARCHAR(32) DEFAULT NULL,
    `hostUuid` VARCHAR(32) DEFAULT NULL,
    `scope` VARCHAR(16) NOT NULL,
    `resourceUuid` VARCHAR(64) NOT NULL,
    `actorUuid` VARCHAR(80) NOT NULL,
    `requestKey` VARCHAR(160) DEFAULT NULL,
    `requestHash` VARCHAR(64) DEFAULT NULL,
    `reconcileOperationUuid` VARCHAR(32) DEFAULT NULL,
    `expectedInstanceGeneration` VARCHAR(128) DEFAULT NULL,
    `issuedPermitOperationUuid` VARCHAR(32) DEFAULT NULL,
    `issuedPermitDeadline` BIGINT DEFAULT NULL,
    `action` VARCHAR(32) NOT NULL,
    `status` VARCHAR(32) NOT NULL,
    `reason` VARCHAR(2048) DEFAULT NULL,
    `desiredRevision` BIGINT NOT NULL,
    `policy` LONGTEXT NOT NULL,
    `ownerManagementNodeUuid` VARCHAR(32) DEFAULT NULL,
    `targetSnapshotHash` VARCHAR(64) DEFAULT NULL,
    `expectedControlOperationUuid` VARCHAR(32) DEFAULT NULL,
    `policyPlanHash` VARCHAR(64) DEFAULT NULL,
    `createDate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastOpDate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `memory_task_request` (`requestKey`),
    KEY `memory_task_parent` (`parentUuid`),
    KEY `memory_task_host_status` (`hostUuid`, `status`),
    KEY `memory_task_status_created` (`status`, `createDate`),
    KEY `memory_task_created_uuid` (`createDate`, `uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`MemoryStateVO` (
    `hostUuid` VARCHAR(32) NOT NULL,
    `permitAuthorized` TINYINT(1) NOT NULL DEFAULT 0,
    `permitDeadline` BIGINT DEFAULT NULL,
    `controlOperationUuid` VARCHAR(32) DEFAULT NULL,
    `desiredRevision` BIGINT NOT NULL DEFAULT 0,
    `appliedRevision` BIGINT DEFAULT NULL,
    `status` VARCHAR(32) NOT NULL,
    `reason` VARCHAR(2048) DEFAULT NULL,
    `capabilities` TEXT DEFAULT NULL,
    `state` MEDIUMTEXT DEFAULT NULL,
    `lastSampleTime` BIGINT DEFAULT NULL,
    `activeTaskUuid` VARCHAR(32) DEFAULT NULL,
    `appliedPolicyHash` VARCHAR(64) DEFAULT NULL,
    `appliedPolicy` MEDIUMTEXT DEFAULT NULL,
    `policyTargetHash` VARCHAR(64) DEFAULT NULL,
    `policyPlanHash` VARCHAR(64) DEFAULT NULL,
    `policyPlanStatus` VARCHAR(16) DEFAULT NULL,
    `policyBlockedFields` TEXT DEFAULT NULL,
    `createDate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastOpDate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`hostUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`MemoryMigrationVO` (
    `vmUuid` VARCHAR(32) NOT NULL,
    `operationUuid` VARCHAR(32) NOT NULL,
    `sourceHostUuid` VARCHAR(32) NOT NULL,
    `targetHostUuid` VARCHAR(32) NOT NULL,
    `poolGeneration` VARCHAR(128) DEFAULT NULL,
    `sourceInstanceGeneration` VARCHAR(128) DEFAULT NULL,
    `targetInstanceGeneration` VARCHAR(128) DEFAULT NULL,
    `status` VARCHAR(32) NOT NULL,
    `reason` VARCHAR(2048) DEFAULT NULL,
    PRIMARY KEY (`vmUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`MemoryVmExclusionVO` (
    `vmUuid` VARCHAR(32) NOT NULL PRIMARY KEY,
    `sourceHostUuid` VARCHAR(32) NOT NULL,
    `targetHostUuid` VARCHAR(32) NOT NULL,
    `sourceRevisions` TEXT NOT NULL,
    `sourceInstanceGeneration` VARCHAR(128) DEFAULT NULL,
    `vmPolicyRevision` BIGINT NOT NULL,
    `retained` BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`MemoryTargetShardVO` (
    `uuid` VARCHAR(32) NOT NULL PRIMARY KEY,
    `requestKey` VARCHAR(160) NOT NULL,
    `scope` VARCHAR(16) NOT NULL,
    `resourceUuid` VARCHAR(64) NOT NULL,
    `shardIndex` INT NOT NULL,
    `shardCount` INT NOT NULL,
    `totalCount` BIGINT NOT NULL,
    `digest` VARCHAR(128) NOT NULL,
    `snapshotGeneration` VARCHAR(128) NOT NULL,
    `expectedRevision` BIGINT NOT NULL,
    `targets` MEDIUMTEXT NOT NULL,
    `requestHash` VARCHAR(128) NOT NULL,
    `expiresAt` BIGINT NOT NULL,
    UNIQUE KEY `memory_target_shard_request_index` (`requestKey`, `shardIndex`),
    KEY `memory_target_shard_expiry` (`expiresAt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`MemoryTaskFailureOutboxVO` (
    `taskUuid` VARCHAR(32) NOT NULL,
    `hostUuid` VARCHAR(32) NOT NULL,
    `action` VARCHAR(32) NOT NULL,
    `reason` VARCHAR(2048) DEFAULT NULL,
    `createDate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `leaseOwner` VARCHAR(255) DEFAULT NULL,
    `leaseUntil` BIGINT DEFAULT NULL,
    `attempts` INT NOT NULL DEFAULT 0,
    `delivered` BIT NOT NULL DEFAULT 0,
    `lastError` VARCHAR(512) DEFAULT NULL,
    `lastOpDate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`taskUuid`),
    KEY `idxMemoryTaskFailureOutboxDelivery` (`delivered`, `leaseUntil`, `createDate`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`MemoryTaskFailureReceiptVO` (
    `taskUuid` VARCHAR(32) NOT NULL,
    `subscriptionUuid` VARCHAR(32) NOT NULL,
    `dataUuid` VARCHAR(32) NOT NULL,
    `suppressed` BIT NOT NULL DEFAULT 0,
    `createDate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`taskUuid`, `subscriptionUuid`),
    UNIQUE KEY `ukMemoryTaskFailureReceiptDataUuid` (`dataUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`MemoryCloudBootstrapVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `status` VARCHAR(32) NOT NULL,
    `requestUuid` VARCHAR(32) NOT NULL,
    `taskUuid` VARCHAR(32) DEFAULT NULL,
    `targetPolicyHash` VARCHAR(64) DEFAULT NULL,
    `reason` VARCHAR(2048) DEFAULT NULL,
    `createDate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastOpDate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `memory_cloud_bootstrap_request` (`requestUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`MemoryQueryVersionVO` (
    `name` VARCHAR(16) NOT NULL,
    `revision` BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`MemoryStandardConfigMigrationVO` (
    `uuid` VARCHAR(64) NOT NULL,
    `status` VARCHAR(16) NOT NULL,
    `reason` VARCHAR(2048) NULL,
    `createDate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastOpDate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`MemoryTaskIdempotencyReceiptVO` (
    `taskUuid` VARCHAR(32) NOT NULL,
    `requestKey` VARCHAR(160) DEFAULT NULL,
    `requestHash` VARCHAR(64) DEFAULT NULL,
    `originalStatus` VARCHAR(32) NOT NULL,
    `deletedDate` DATETIME NOT NULL,
    PRIMARY KEY (`taskUuid`),
    UNIQUE KEY `memory_task_receipt_request` (`requestKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Seed only stable system/query rows. Bootstrap, exclusion, task, state,
-- migration and receipt tables intentionally start empty.
INSERT IGNORE INTO `zstack`.`MemoryPolicyVO`
    (`uuid`, `scope`, `resourceUuid`, `revision`, `policy`)
    VALUES ('Global:global', 'Global', 'global', 0, '{"schemaVersion":1}');

INSERT IGNORE INTO `zstack`.`MemoryQueryVersionVO` (`name`, `revision`)
    VALUES ('state', 0), ('task', 0);
