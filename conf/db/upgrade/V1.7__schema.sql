CREATE TABLE  `zstack`.`KeystoreVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `resourceUuid` varchar(32) NOT NULL,
    `resourceType` varchar(255) NOT NULL,
    `type` varchar(255) NOT NULL,
    `content` varchar(65535) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;