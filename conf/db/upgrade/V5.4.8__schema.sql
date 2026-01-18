-- Add ExternalServiceConfiguration table
CREATE TABLE IF NOT EXISTS `zstack`.`ExternalServiceConfigurationVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `serviceType` varchar(32) NOT NULL,
    `configuration` text DEFAULT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;