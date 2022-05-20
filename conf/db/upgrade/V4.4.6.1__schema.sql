CREATE TABLE IF NOT EXISTS `zstack`.`SNSSmsPlatformVO`
(
    `uuid` varchar(32) NOT NULL UNIQUE,
    `username` varchar(32) NOT NULL,
    `password` varchar(32) NOT NULL,
    `serverIp` varchar(32) NOT NULL,
    `srcID` varchar(32) NOT NULL,
    `downlinkChannelNumber` varchar(32) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkSNSSmsPlatformVOSNSApplicationPlatformVO FOREIGN KEY (uuid) REFERENCES SNSApplicationPlatformVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
