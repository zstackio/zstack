CREATE TABLE IF NOT EXISTS `zstack`.`HostKernelInterfaceVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(255) DEFAULT NULL,
    `hostUuid` varchar(32) NOT NULL,
    `l2NetworkUuid` varchar(32) NOT NULL,
    `l3NetworkUuid` varchar(32) DEFAULT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkHostKernelInterfaceVOHostVO` FOREIGN KEY (`hostUuid`) REFERENCES HostEO (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkHostKernelInterfaceVOL2NetworkVO` FOREIGN KEY (`l2NetworkUuid`) REFERENCES L2NetworkEO (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkHostKernelInterfaceVOL3NetworkVO` FOREIGN KEY (`l3NetworkUuid`) REFERENCES L3NetworkEO (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`HostKernelInterfaceUsedIpVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `hostKernelInterfaceUuid` varchar(32) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkHostKernelInterfaceUsedIpVOHostKernelInterfaceVO` FOREIGN KEY (`hostKernelInterfaceUuid`) REFERENCES HostKernelInterfaceVO (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`HostKernelInterfaceTrafficTypeVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `hostKernelInterfaceUuid` varchar(32) NOT NULL,
    `trafficType` varchar(128) NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkHostKernelInterfaceTrafficTypeVOHostKernelInterfaceVO` FOREIGN KEY (`hostKernelInterfaceUuid`) REFERENCES HostKernelInterfaceVO (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
