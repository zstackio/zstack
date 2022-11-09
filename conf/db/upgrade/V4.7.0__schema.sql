CREATE TABLE IF NOT EXISTS `zstack`.`HaiTaiSecretResourcePoolVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `managementIp` varchar(32) NOT NULL,
    `port` int unsigned NOT NULL,
    `realm` varchar(32) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkHaiTaiSecretResourcePoolVOSecretResourcePoolVO FOREIGN KEY (uuid) REFERENCES SecretResourcePoolVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`FileIntegrityVerificationVO` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `path` varchar(256) NOT NULL,
    `nodeType` varchar(16) NOT NULL,
    `nodeUuid` varchar(64) NOT NULL,
    `hexType` varchar(16) NOT NULL,
    `digest` varchar(256) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`id`),
    UNIQUE KEY `node` (`nodeUuid`,`nodeType`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
