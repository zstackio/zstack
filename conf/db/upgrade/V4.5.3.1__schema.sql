CREATE TABLE IF NOT EXISTS `zstack`.`HaiTaiSecretResourcePoolVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `managementIp` varchar(32) NOT NULL,
    `port` int unsigned NOT NULL,
    `realm` varchar(32) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkHaiTaiSecretResourcePoolVOSecretResourcePoolVO FOREIGN KEY (uuid) REFERENCES SecretResourcePoolVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;