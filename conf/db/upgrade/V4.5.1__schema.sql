CREATE TABLE IF NOT EXISTS `zstack`.`SNSPluginEndpointVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `pluginType` varchar(64) NOT NULL,
    `pluginProductName` varchar(64) NOT NULL,
    `pluginProductKey` varchar(64) NOT NULL,
    `timeoutInSeconds` int NOT NULL,
    `properties` varchar(1024) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkPluginEndpointVOSNSApplicationEndpointVO FOREIGN KEY (uuid) REFERENCES SNSApplicationEndpointVO (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;