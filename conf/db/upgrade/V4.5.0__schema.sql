ALTER TABLE `zstack`.`TicketStatusHistoryVO` ADD COLUMN `flowName` VARCHAR(255) DEFAULT NULL;
ALTER TABLE `zstack`.`ArchiveTicketStatusHistoryVO` ADD COLUMN `flowName` VARCHAR(255) DEFAULT NULL;
CREATE TABLE IF NOT EXISTS `zstack`.`RegisterLicenseApplicationVO` (
    `appId` VARCHAR(32) NOT NULL UNIQUE,
    `licenseRequestCode` text NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`appId`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;