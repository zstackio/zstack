CREATE TABLE IF NOT EXISTS `zstack`.`AiSiNoSecretResourcePoolVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `managementIp` varchar(32) NOT NULL,
    `port` int unsigned NOT NULL,
    `route` varchar(32) NOT NULL,
    `clientID` varchar(32) NOT NULL,
    `clientSecrete` varchar(32) NOT NULL,
    `appId` varchar(8) NOT NULL,
    `keyNumSM2` varchar(8) NOT NULL,
    `keyNumSM4` varchar(8) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkAiSiNoSecretResourcePoolVOSecretResourcePoolVO FOREIGN KEY (uuid) REFERENCES SecretResourcePoolVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
ALTER TABLE SecretResourcePoolVO ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'Connected';
ALTER TABLE `zstack`.`TicketStatusHistoryVO` ADD COLUMN `flowName` VARCHAR(255) DEFAULT NULL;
ALTER TABLE `zstack`.`ArchiveTicketStatusHistoryVO` ADD COLUMN `flowName` VARCHAR(255) DEFAULT NULL;