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
