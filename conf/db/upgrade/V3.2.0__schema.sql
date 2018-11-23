CREATE TABLE `AccessKeyVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `accountUuid` VARCHAR(32) NOT NULL,
    `userUuid` VARCHAR(32) NOT NULL,
    `AccessKeyID` VARCHAR(128) NOT NULL,
    `AccessKeySecret` VARCHAR(128) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `state` varchar(64) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
