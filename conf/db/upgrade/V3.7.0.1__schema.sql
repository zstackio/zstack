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

CREATE TABLE IF NOT EXISTS `zstack`.`LuksEncryptedImageVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `encryptUuid` varchar(36) UNIQUE NOT NULL,
    `hashValue` varchar(32) NOT NULL,
    `bindingVmUuid` varchar(32) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
