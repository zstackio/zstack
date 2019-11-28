SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `zstack`.`PriceTableVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` VARCHAR(256) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO ResourceVO (`uuid`, `resourceName`, `resourceType`) VALUES ("12a087c058cc45d5bf80a605f17c0083", "global_default", 'PriceTableVO');
INSERT INTO PriceTableVO (`uuid`, `name`, `lastOpDate`, `createDate`) VALUES ("12a087c058cc45d5bf80a605f17c0083", "global_default", CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

ALTER TABLE PriceVO ADD COLUMN tableUuid VARCHAR(32) DEFAULT NULL;
ALTER TABLE PriceVO ADD CONSTRAINT fkPriceVOPriceTableVO FOREIGN KEY (tableUuid) REFERENCES PriceTableVO (uuid) ON DELETE CASCADE;
UPDATE PriceVO set tableUuid = "12a087c058cc45d5bf80a605f17c0083";
ALTER TABLE PriceVO modify column tableUuid VARCHAR(32) NOT NULL;

ALTER TABLE PriceVO add column endDateInLong bigint unsigned DEFAULT NULL;

CREATE TABLE IF NOT EXISTS `zstack`.`AccountPriceTableRefVO` (
`tableUuid` varchar(32) NOT NULL,
`accountUuid` varchar(32) NOT NULL UNIQUE,
`lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
`createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
PRIMARY KEY (`tableUuid`,`accountUuid`),
CONSTRAINT `fkAccountPriceTableRefVOPriceTableVO` FOREIGN KEY (`tableUuid`) REFERENCES `PriceTableVO` (`uuid`) ON DELETE CASCADE,
CONSTRAINT `fkAccountPriceTableRefVOAccountVO` FOREIGN KEY (`accountUuid`) REFERENCES `AccountVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;