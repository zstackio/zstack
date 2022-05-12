CREATE TABLE IF NOT EXISTS `zstack`.`SecurityLevelResourceRefVO` (
    `resourceUuid` varchar(32) NOT NULL UNIQUE,
    `securityLevel` varchar(12),
    PRIMARY KEY  (`resourceUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
