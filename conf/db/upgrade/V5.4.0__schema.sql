CREATE TABLE IF NOT EXISTS `zstack`.`PhysicalSwitchVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `ip` varchar(64) NOT NULL,
    `mac` varchar(32) NOT NULL,
    `mode` varchar(128) NOT NULL,
    `softwareVersion` varchar(128) NOT NULL,
    `sdnControllerUuid` varchar(32) DEFAULT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`PhysicalSwitchPortVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `ethTrunkName` varchar(255) DEFAULT NULL,
    `portType` varchar(64) NOT NULL,
    `peerInterfaceUuid` varchar(32) DEFAULT NULL,
    `switchUuid` varchar(32) NOT NULL,
    `sdnControllerUuid` varchar(32) DEFAULT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkPhysicalSwitchPortVOHostNetworkInterfaceVO` FOREIGN KEY (`peerInterfaceUuid`) REFERENCES `HostNetworkInterfaceVO` (`uuid`) ON DELETE SET NULL,
    CONSTRAINT `fkPhysicalSwitchPortVOPhysicalSwitchVO` FOREIGN KEY (`switchUuid`) REFERENCES `PhysicalSwitchVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`HuaweiIMasterFabricVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `sdnControllerUuid` varchar(32) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `state` varchar(32) NOT NULL DEFAULT "Enabled",
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkHuaweiIMasterFabricVOSdnControllerVO` FOREIGN KEY (`sdnControllerUuid`) REFERENCES `SdnControllerVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`HuaweiIMasterTenantVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `sdnControllerUuid` varchar(32) NOT NULL,
    `state` varchar(32) NOT NULL DEFAULT "Enabled",
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkHuaweiIMasterTenantVOSdnControllerVO` FOREIGN KEY (`sdnControllerUuid`) REFERENCES `SdnControllerVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`HuaweiIMasterVpcVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `tenantId` varchar(32) NOT NULL,
    `fabricId` varchar(2048) NOT NULL,
    `sdnControllerUuid` varchar(32) NOT NULL,
    `isVpcDeployed` BOOLEAN NOT NULL DEFAULT TRUE,
    `state` varchar(32) NOT NULL DEFAULT "Enabled",
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkHuaweiIMasterVpcVOTenantVO` FOREIGN KEY (`tenantId`) REFERENCES `HuaweiIMasterTenantVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkHuaweiIMasterVpcVOSdnControllerVO` FOREIGN KEY (`sdnControllerUuid`) REFERENCES `SdnControllerVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`HuaweiIMasterVRouterVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `logicalNetworkId` varchar(32) NOT NULL,
    `tenantId` varchar(32) NOT NULL,
    `fabricUuid` varchar(32) NOT NULL,
    `sdnControllerUuid` varchar(32) NOT NULL,
    `state` varchar(32) NOT NULL DEFAULT "Enabled",
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkHuaweiIMasterVRouterVOLogicalNetworkVO` FOREIGN KEY (`logicalNetworkId`) REFERENCES `HuaweiIMasterVpcVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkHuaweiIMasterVRouterVOTenantVO` FOREIGN KEY (`tenantId`) REFERENCES `HuaweiIMasterTenantVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkHuaweiIMasterVRouterVOHuaweiIMasterFabricVO` FOREIGN KEY (`fabricUuid`) REFERENCES `HuaweiIMasterFabricVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkHuaweiIMasterVRouterVOSdnControllerVO` FOREIGN KEY (`sdnControllerUuid`) REFERENCES `SdnControllerVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  IF NOT EXISTS `HardwareL2VxlanNetworkVO` (
  `uuid` varchar(32) NOT NULL UNIQUE,
  `vlan` int unsigned NOT NULL,
  PRIMARY KEY  (`uuid`),
  CONSTRAINT fkHardwareL2VxlanNetworkVOL2NetworkEO FOREIGN KEY (uuid) REFERENCES L2NetworkEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`HuaweiIMasterSdnControllerVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkHuaweiIMasterSdnControllerVOSdnControllerVO FOREIGN KEY (uuid) REFERENCES SdnControllerVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS  `zstack`.`HuaweiIMasterTenantFabricRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `tenantUuid` varchar(32) NOT NULL,
    `fabricUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`),
    UNIQUE KEY `uk_tenant_fabric` (`tenantUuid`, `fabricUuid`),
    CONSTRAINT fkHuaweiIMasterTenantFabricRefVOHuaweiIMasterFabricVO FOREIGN KEY (fabricUuid) REFERENCES HuaweiIMasterFabricVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT fkHuaweiIMasterTenantFabricRefVOHuaweiIMasterTenantVO FOREIGN KEY (tenantUuid) REFERENCES HuaweiIMasterTenantVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CALL ADD_COLUMN('HardwareL2VxlanNetworkPoolVO', 'startVlan', 'int unsigned', 1, NULL);
CALL ADD_COLUMN('HardwareL2VxlanNetworkPoolVO', 'endVlan', 'int unsigned', 1, NULL);
CALL ADD_COLUMN('SdnControllerVO', 'vendorVersion', 'VARCHAR(1024)', 0, 'V1');