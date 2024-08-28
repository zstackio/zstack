CREATE TABLE IF NOT EXISTS `zstack`.`SNSSmsPlatformVO`
(
    `uuid` varchar(32) NOT NULL UNIQUE,
    `node1Ip` varchar(32) NOT NULL,
    `node2Ip` varchar(32) NOT NULL,
    `username1` varchar(32) NOT NULL,
    `username2` varchar(32) NOT NULL,
    `password1` varchar(32) NOT NULL,
    `password2` varchar(32) NOT NULL,
    `serverIp` varchar(32) NOT NULL,
    `srcID` varchar(32) NOT NULL,
    `downlinkChannelNumber1` varchar(32) NOT NULL,
    `downlinkChannelNumber2` varchar(32) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkSNSSmsPlatformVOSNSApplicationPlatformVO FOREIGN KEY (uuid) REFERENCES SNSApplicationPlatformVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
