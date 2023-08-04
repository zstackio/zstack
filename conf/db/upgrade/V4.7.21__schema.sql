CREATE TABLE IF NOT EXISTS `zstack`.`WebTerminalConnectionVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `username` varchar(255) NOT NULL,
    `password` varchar(255) NOT NULL,
    `protocol` varchar(64) NOT NULL,
    `port` int unsigned NOT NULL,
    `isSupportSftp` boolean NOT NULL DEFAULT FALSE,
    `sftpPort` int unsigned NOT NULL DEFAULT 0,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkWebTerminalConnectionVOVmInstanceEO` FOREIGN KEY (`uuid`) REFERENCES `VmInstanceEO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`HostWebTerminalUsedPortVO` (
	`id` bigint not null auto_increment,
    `hostUuid` varchar(32) NOT NULL,
    `vmInstanceUuid` varchar(32) NOT NULL,
    `protocol` varchar(64) NOT NULL,
    `destPort` int unsigned NOT NULL,
    `forwardPort` int unsigned NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	primary key (`id`),
    CONSTRAINT `fkHostWebTerminalUsedPortVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkHostWebTerminalUsedPortVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
