CREATE TABLE `zstack`.`XDragonHostVO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'host uuid',
    `cpuNum` int unsigned NOT NULL DEFAULT 0,
    `cpuSockets` int unsigned NOT NULL DEFAULT 1,
    totalPhysicalMemory bigint unsigned NOT NULL DEFAULT 0,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkXDragonHostVOHostEO` FOREIGN KEY (`uuid`) REFERENCES `HostEO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
