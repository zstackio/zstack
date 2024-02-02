CREATE TABLE IF NOT EXISTS `zstack`.`CSPSecretResourcePoolVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `managementIp` varchar(32) NOT NULL,
    `port` int unsigned NOT NULL,
    `appId` varchar(128) NOT NULL,
    `appKey` varchar(128) NOT NULL,
    `keyId` varchar(128) NOT NULL,
    `userId` varchar(128) DEFAULT NULL,
    `protocol` varchar(8) DEFAULT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkCSPSecretResourcePoolVOSecretResourcePoolVO FOREIGN KEY (uuid) REFERENCES SecretResourcePoolVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;