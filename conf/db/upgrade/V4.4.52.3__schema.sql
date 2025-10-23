CREATE TABLE IF NOT EXISTS `zstack`.`ResourceResponsibleVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `resourceUuid` varchar(32) NOT NULL,
    `responsibleType` varchar(256) NOT NULL,
    `responsibleUuid` varchar(32) NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkResourceUuid` FOREIGN KEY (`resourceUuid`) REFERENCES `ResourceVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkResponsibleUuid` FOREIGN KEY (`responsibleUuid`) REFERENCES `ResourceVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`FileVerificationRecordsVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `fileVerificationUuid` varchar(32) NOT NULL UNIQUE,
    `path` varchar(256) NOT NULL,
    `node` varchar(32) NOT NULL,
    `currentDigest` varchar(32) DEFAULT NULL,
    `targetDigest` varchar(256) DEFAULT NULL,
    `reason` varchar(256) DEFAULT NULL,
    `recoverFlag` tinyint(1) unsigned NOT NULL DEFAULT 0,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `fkFileVerificationUuid` FOREIGN KEY (`fileVerificationUuid`) REFERENCES `FileVerificationVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`PortMirrorSessionVO` ADD COLUMN `dstEndPointType` varchar(32) NOT NULL DEFAULT 'VmNic';