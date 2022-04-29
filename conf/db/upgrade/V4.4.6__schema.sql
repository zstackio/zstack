CREATE TABLE IF NOT EXISTS `zstack`.`SharedBlockCapacityVO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'sharedBlock uuid',
    `totalCapacity` bigint unsigned NOT NULL DEFAULT 0 COMMENT 'total capacity of sharedBlock in bytes',
    `availableCapacity` bigint unsigned NOT NULL DEFAULT 0 COMMENT 'available capacity of sharedBlock in bytes',
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkSharedBlockCapacityVOSharedBlockVO` FOREIGN KEY (`uuid`) REFERENCES `zstack`.`SharedBlockVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;