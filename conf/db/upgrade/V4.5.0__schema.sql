ALTER TABLE `zstack`.`TicketStatusHistoryVO` ADD COLUMN `flowName` VARCHAR(255) DEFAULT NULL;
ALTER TABLE `zstack`.`ArchiveTicketStatusHistoryVO` ADD COLUMN `flowName` VARCHAR(255) DEFAULT NULL;

CREATE TABLE IF NOT EXISTS `zstack`.`ExternalManagementNodeVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(128) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `hostName` varchar(255) NOT NULL,
    `port` int unsigned NOT NULL,
    `status` varchar(32) NOT NULL,
    `accessKeyID` VARCHAR(128) NOT NULL,
    `accessKeySecret` VARCHAR(128) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;