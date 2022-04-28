CREATE TABLE IF NOT EXISTS `zstack`.`SharedBlockCapacityVO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'shared block uuid',
    `totalCapacity` bigint unsigned NOT NULL COMMENT 'total capacity of lun in bytes',
    `availableCapacity` bigint unsigned NOT NULL COMMENT 'available capacity of lun in bytes',
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
