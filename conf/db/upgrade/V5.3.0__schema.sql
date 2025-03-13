CREATE TABLE IF NOT EXISTS `zstack`.`HbaDeviceVO` (
    `uuid` varchar(32) not null unique,
    `hostUuid` varchar(32) default null,
    `name` varchar(255) default null,
    `hbaType`  varchar(64) default null,
    `createDate` timestamp not null default '0000-00-00 00:00:00',
    `lastOpDate` timestamp not null default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fkHBADeviceVOHostVO FOREIGN KEY (hostUuid) REFERENCES HostEO (uuid) ON DELETE CASCADE,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`FcHbaDeviceVO` (
    `uuid` varchar(32) not null unique,
    `portName` varchar(255) default null,
    `portState`  varchar(64) default null,
    `supportedSpeeds`  varchar(255) default null,
    `speed`  varchar(255) default null,
    `symbolicName`  varchar(255) default null,
    `supportedClasses`  varchar(255) default null,
    `nodeName` varchar(255) default null,
    CONSTRAINT fkFcHbaDeviceVO FOREIGN KEY (uuid) REFERENCES HbaDeviceVO (uuid) ON DELETE CASCADE,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

UPDATE `zstack`.`ImageEO` SET guestOsType = 'VyOS 1.1.7' WHERE architecture = 'x86_64' and guestOsType = 'Linux' and `system` = TRUE;
UPDATE `zstack`.`ImageEO` SET guestOsType = 'VyOS 1.2.0' WHERE architecture = 'aarch64' and guestOsType = 'Linux' and `system` = TRUE;
UPDATE `zstack`.`ImageEO` SET guestOsType = 'Kylin 10' WHERE architecture = 'loongarch64' and guestOsType = 'Linux' and `system` = TRUE;

UPDATE `zstack`.`VmInstanceEO` SET guestOsType = 'VyOS 1.1.7' WHERE architecture = 'x86_64' and guestOsType = 'Linux' and type = 'ApplianceVm';
UPDATE `zstack`.`VmInstanceEO` SET guestOsType = 'VyOS 1.2.0' WHERE architecture = 'aarch64' and guestOsType = 'Linux' and type = 'ApplianceVm';
UPDATE `zstack`.`VmInstanceEO` SET guestOsType = 'Kylin 10' WHERE architecture = 'loongarch64' and guestOsType = 'Linux' and type = 'ApplianceVm';

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
    VALUES (REPLACE(UUID(),'-',''),'StorageNetwork', TRUE, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
INSERT IGNORE INTO `zstack`.`HostNetworkLabelVO` (`uuid`, `serviceType`, `system`, `createDate`, `lastOpDate`)
    VALUES (REPLACE(UUID(),'-',''),'TenantNetwork', TRUE, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
INSERT IGNORE INTO `zstack`.`HostNetworkLabelVO` (`uuid`, `serviceType`, `system`, `createDate`, `lastOpDate`)
    VALUES (REPLACE(UUID(),'-',''),'BackupNetwork', TRUE, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
INSERT IGNORE INTO `zstack`.`HostNetworkLabelVO` (`uuid`, `serviceType`, `system`, `createDate`, `lastOpDate`)
    VALUES (REPLACE(UUID(),'-',''),'MigrationNetwork', TRUE, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

CREATE INDEX idx_schedType_createDate ON `zstack`.`VmSchedHistoryVO` (schedType, createDate);