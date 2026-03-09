CREATE TABLE  `zstack`.`ImageStoreBackupStorageVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `hostname` varchar(255) NOT NULL UNIQUE,
    `username` varchar(255) NOT NULL,
    `password` varchar(255) NOT NULL,
    `sshPort` int unsigned NOT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE AccountVO modify column name varchar(255) NOT NULL;
ALTER TABLE UserVO modify column name varchar(255) NOT NULL;