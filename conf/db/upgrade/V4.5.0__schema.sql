CREATE TABLE IF NOT EXISTS `zstack`.`VxlanHostMappingVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `vxlanUuid` varchar(32) NOT NULL,
    `hostUuid` varchar(32) NOT NULL,
    `vlanId` int,
    `physicalInterface` varchar(32),
    `createDate`   timestamp    NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate`   timestamp    NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkVxlanHostMappingVOVxlanNetworkVO` FOREIGN KEY (`vxlanUuid`) REFERENCES `VxlanNetworkVO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `fkVxlanHostMappingVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`VxlanClusterMappingVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `vxlanUuid` varchar(32) NOT NULL,
    `clusterUuid` varchar(32) NOT NULL,
    `vlanId` int,
    `physicalInterface` varchar(32),
    `createDate`   timestamp    NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate`   timestamp    NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkVxlanClusterMappingVOVxlanNetworkVO` FOREIGN KEY (`vxlanUuid`) REFERENCES `VxlanNetworkVO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `fkVxlanClusterMappingVOClusterEO` FOREIGN KEY (`clusterUuid`) REFERENCES `ClusterEO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;