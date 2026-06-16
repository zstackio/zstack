CREATE TABLE IF NOT EXISTS `zstack`.`ExternalPrimaryStorageHostProtocolRefVO` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `hostUuid` varchar(32) NOT NULL,
    `primaryStorageUuid` varchar(32) NOT NULL,
    `protocol` varchar(32) NOT NULL,
    `status` varchar(32) NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `ukExternalPrimaryStorageHostProtocolRefVO` (`primaryStorageUuid`, `hostUuid`, `protocol`),
    CONSTRAINT `fkExternalPrimaryStorageHostProtocolRefVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `zstack`.`HostEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkExternalPrimaryStorageHostProtocolRefVOPrimaryStorageEO` FOREIGN KEY (`primaryStorageUuid`) REFERENCES `zstack`.`PrimaryStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CALL DROP_COLUMN('ExternalPrimaryStorageHostRefVO', 'protocol');
