CREATE TABLE IF NOT EXISTS `zstack`.`ReservedIpRangeVO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
    `l3NetworkUuid` varchar(32) NOT NULL COMMENT 'l3 network uuid',
    `name` varchar(255) DEFAULT NULL COMMENT 'name',
    `description` varchar(2048) DEFAULT NULL COMMENT 'description',
    `ipVersion` int(10) unsigned DEFAULT 4 COMMENT 'ip range version',
    `startIp` varchar(64) NOT NULL COMMENT 'start ip',
    `endIp` varchar(64) NOT NULL COMMENT 'end ip',
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;