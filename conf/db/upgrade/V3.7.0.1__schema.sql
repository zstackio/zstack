 CREATE TABLE  `zstack`.`FileVerificationVO` (
     `uuid` VARCHAR(32) NOT NULL UNIQUE,
     `path` varchar(256) NOT NULL,
     `node` varchar(32) NOT NULL,
     `hexType` varchar(32) NOT NULL,
     `digest` varchar(512) NOT NULL,
     `category` varchar(64) NOT NULL,
     `state` varchar(32) NOT NULL,
     `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
     `createDate` timestamp
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8;