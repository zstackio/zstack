CREATE TABLE IF NOT EXISTS `zstack`.`OpsLogVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `resourceUuid` VARCHAR(32) NOT NULL,
    `managementNodeUuid` VARCHAR(32),
    `topic` VARCHAR(32) NOT NULL,
    `state` VARCHAR(32) NOT NULL,
    `opName` VARCHAR(32) NOT NULL,
    `opData` TEXT        DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX `idxOpsLogVOresourceUuid` (`resourceUuid`),
    INDEX `idxOpsLogVOtopic` (`topic`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
