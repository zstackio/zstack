CREATE TABLE IF NOT EXISTS `zstack`.`SlbGroupConfigTaskVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `slbGroupUuid` varchar(32) NOT NULL UNIQUE,
    `configVersion` bigint unsigned DEFAULT 0,
    `taskName` varchar(32) DEFAULT NULL,
    `taskData` text NOT NULL,
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkSlbGroupConfigTaskVOSlbGroupVO` FOREIGN KEY (`slbGroupUuid`) REFERENCES `SlbGroupVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`SlbVmInstanceConfigTaskVO` (
    `vmInstanceUuid` varchar(32) NOT NULL UNIQUE,
    `configVersion` bigint unsigned DEFAULT 0,
    `taskName` varchar(32) NOT NULL,
    `taskData` text NOT NULL,
    `lastFailedReason` varchar(1024) NOT NULL,
    `retryNumber` bigint unsigned DEFAULT 0,
    `status` varchar(32) NOT NULL,
    PRIMARY KEY  (`vmInstanceUuid`),
    CONSTRAINT `fkSlbVmInstanceConfigTaskVOSlbVmInstanceVO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `SlbVmInstanceVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;