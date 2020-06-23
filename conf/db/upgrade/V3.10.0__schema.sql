
CREATE TABLE IF NOT EXISTS `zstack`.`SNSMicrosoftTeamsEndpointVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `url` varchar(1024) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkSNSMicrosoftTeamsEndpointVOSNSApplicationEndpointVO FOREIGN KEY (uuid) REFERENCES SNSApplicationEndpointVO (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



