CREATE TABLE IF NOT EXISTS `zstack`.`HostNetworkLabelVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `serviceType` varchar(255) NOT NULL,
    `system` boolean NOT NULL DEFAULT TRUE,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT IGNORE INTO `zstack`.`HostNetworkLabelVO` (`uuid`, `serviceType`, `system`, `createDate`, `lastOpDate`)
    VALUES (REPLACE(UUID(),'-',''),'ManagementNetwork', TRUE, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
INSERT IGNORE INTO `zstack`.`HostNetworkLabelVO` (`uuid`, `serviceType`, `system`, `createDate`, `lastOpDate`)
    VALUES (REPLACE(UUID(),'-',''),'TenantNetwork', TRUE, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
INSERT IGNORE INTO `zstack`.`HostNetworkLabelVO` (`uuid`, `serviceType`, `system`, `createDate`, `lastOpDate`)
    VALUES (REPLACE(UUID(),'-',''),'StorageNetwork', TRUE, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
INSERT IGNORE INTO `zstack`.`HostNetworkLabelVO` (`uuid`, `serviceType`, `system`, `createDate`, `lastOpDate`)
    VALUES (REPLACE(UUID(),'-',''),'BackupNetwork', TRUE, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
INSERT IGNORE INTO `zstack`.`HostNetworkLabelVO` (`uuid`, `serviceType`, `system`, `createDate`, `lastOpDate`)
    VALUES (REPLACE(UUID(),'-',''),'MigrationNetwork', TRUE, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
