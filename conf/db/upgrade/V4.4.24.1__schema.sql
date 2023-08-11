CREATE TABLE IF NOT EXISTS `zstack`.`ExternalBackupStorageVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `identity` varchar(255) NOT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

