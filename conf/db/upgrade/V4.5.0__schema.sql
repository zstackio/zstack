CREATE TABLE IF NOT EXISTS `zstack`.`AiSiNoSecretResourcePoolVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `managementIp` varchar(32) NOT NULL,
    `port` int unsigned NOT NULL,
    `route` varchar(32) NOT NULL,
    `clientID` varchar(32) NOT NULL,
    `clientSecrete` varchar(32) NOT NULL,
    `appId` varchar(8) NOT NULL,
    `keyNumSM2` varchar(8) NOT NULL,
    `keyNumSM4` varchar(8) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkAiSiNoSecretResourcePoolVOSecretResourcePoolVO FOREIGN KEY (uuid) REFERENCES SecretResourcePoolVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
ALTER TABLE SecretResourcePoolVO ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'Connected';
ALTER TABLE `zstack`.`TicketStatusHistoryVO` ADD COLUMN `flowName` VARCHAR(255) DEFAULT NULL;
ALTER TABLE `zstack`.`ArchiveTicketStatusHistoryVO` ADD COLUMN `flowName` VARCHAR(255) DEFAULT NULL;

CREATE TABLE IF NOT EXISTS `zstack`.`CephOsdGroupVO` (
    `uuid` varchar(32) NOT NULL,
    `primaryStorageUuid` varchar(32) NOT NULL,
    `osds` varchar(1024) NOT NULL,
    `availableCapacity` bigint(20) DEFAULT NULL,
    `availablePhysicalCapacity` bigint(20) unsigned NOT NULL DEFAULT 0,
    `totalPhysicalCapacity` bigint(20) unsigned NOT NULL DEFAULT 0,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    KEY `fkPrimaryStorageUuid` (`primaryStorageUuid`),
    CONSTRAINT `fkPrimaryStorageUuid` FOREIGN KEY (`primaryStorageUuid`) REFERENCES `PrimaryStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`CephPrimaryStoragePoolVO` ADD COLUMN `osdGroupUuid` VARCHAR(32) DEFAULT NULL;
ALTER TABLE `zstack`.`CephPrimaryStoragePoolVO` ADD CONSTRAINT fkCephPrimaryStoragePoolVOOsdGroupVO FOREIGN KEY (osdGroupUuid) REFERENCES CephOsdGroupVO (uuid) ON DELETE SET NULL;

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

CREATE TABLE IF NOT EXISTS `zstack`.`RegisterLicenseApplicationVO` (
    `appId` VARCHAR(32) NOT NULL UNIQUE,
    `licenseRequestCode` text NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`appId`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE `zstack`.`VolumeHostRefVO` (
    `volumeUuid` varchar(32) NOT NULL UNIQUE,
    `hostUuid` varchar(32) NOT NULL,
    `mountPath` varchar(512) NOT NULL,
    `device` varchar(512) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`volumeUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
ALTER TABLE `zstack`.`VolumeHostRefVO` ADD CONSTRAINT `fkVolumeHostRefVOHostEO`
FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE;

CREATE TABLE IF NOT EXISTS `zstack`.`SSOClientVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) DEFAULT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `clientType` varchar(255) NOT NULL,
    `loginType` varchar(255) NOT NULL,
    `loginMNUrl` varchar(255) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`OAuth2ClientVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `clientId` varchar(255) NOT NULL,
    `clientSecret` varchar(255),
    `authorizationUrl` varchar(255),
    `grantType` varchar(64) NOT NULL,
    `tokenUrl` varchar(255) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkOAuth2ClientVOSSOClientVO` FOREIGN KEY (`uuid`) REFERENCES `SSOClientVO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`CasClientVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `casServerLoginUrl` varchar(255) NOT NULL,
    `casServerUrlPrefix` varchar(255) NOT NULL,
    `serverName` varchar(255) NOT NULL,
    `state` varchar(128) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkCasClientVOSSOClientVO` FOREIGN KEY (`uuid`) REFERENCES `SSOClientVO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ThirdClientAccountRefVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `clientUuid` varchar(255) NOT NULL,
    `resourceUuid`  varchar(255) NOT NULL,
    `resourceType` varchar(255) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkThirdClientAccountRefVOSSOClientVO` FOREIGN KEY (`clientUuid`) REFERENCES `SSOClientVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`SSORedirectTemplateVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) DEFAULT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `clientUuid` varchar(255) NOT NULL,
    `redirectTemplate`  varchar(2048) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkSSORedirectTemplateClientVO` FOREIGN KEY (`clientUuid`) REFERENCES `SSOClientVO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`V2VConversionCacheVO` ADD COLUMN  `deviceAddress` varchar(128) DEFAULT NULL;
CREATE TABLE IF NOT EXISTS `zstack`.`ExternalManagementNodeVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(128) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `hostName` varchar(255) NOT NULL,
    `port` int unsigned NOT NULL,
    `status` varchar(32) NOT NULL,
    `accessKeyID` VARCHAR(128) NOT NULL,
    `accessKeySecret` VARCHAR(128) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`MirrorCdpTaskVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(128) NOT NULL,
    `peerExternalManagementNodeUuid` varchar(32) NOT NULL,
    `peerCdpTaskUuid` varchar(32) NOT NULL,
    `mode` varchar(128) NOT NULL,
    `peerHostName` varchar(128) NOT NULL,
    `mirrorResourceUuid` varchar(128) DEFAULT NULL,
    `mirrorResourceType` varchar(255) DEFAULT NULL,
    `status` varchar(128) DEFAULT NULL,
    `state` varchar(255) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`TwinManagementNodeResourceMapVO` (
    `uuid` varchar(32) NOT NULL,
    `externalResourceUuid` varchar(32) NOT NULL,
    `resourceUuid` varchar(32) NOT NULL,
    `type` varchar(32) NOT NULL,
    `externalManagementNodeUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;