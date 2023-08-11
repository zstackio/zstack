CREATE TABLE IF NOT EXISTS `zstack`.`PluginDriverVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(64) NOT NULL,
    `type` varchar(64) NOT NULL,
    `vendor` varchar(64) NOT NULL,
    `features` varchar(1024) NOT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`SNSPluginEndpointVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `timeoutInSeconds` int NOT NULL,
    `properties` varchar(1024) NOT NULL,
    `pluginDriverUuid` varchar(32) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkPluginEndpointVOSNSApplicationEndpointVO FOREIGN KEY (uuid) REFERENCES SNSApplicationEndpointVO (uuid) ON DELETE CASCADE,
    CONSTRAINT fkPluginEndpointVOPluginDriverVO FOREIGN KEY (pluginDriverUuid) REFERENCES PluginDriverVO (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`PluginSecretResourcePoolVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `properties` varchar(1024) NOT NULL,
    `pluginDriverUuid` varchar(32) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkPluginSecretResourcePoolVOSecretResourcePoolVO FOREIGN KEY (uuid) REFERENCES SecretResourcePoolVO (uuid) ON DELETE CASCADE,
    CONSTRAINT fkPluginSecretResourcePoolVOPluginDriverVO FOREIGN KEY (pluginDriverUuid) REFERENCES PluginDriverVO (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;