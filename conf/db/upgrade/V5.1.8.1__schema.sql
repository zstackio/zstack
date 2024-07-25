CREATE TABLE IF NOT EXISTS `zstack`.`ModelCenterVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(128) DEFAULT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `url` varchar(2048) DEFAULT NULL,
    `parameters` varchar(128) DEFAULT NULL,
    `managementIp` varchar(128) NOT NULL,
    `managementPort` int(16) not NULL,
    `storageNetworkUuid` varchar(32) DEFAULT NULL,
    `serviceNetworkUuid` varchar(32) DEFAULT NULL,
    `containerRegistry` varchar(2048) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ModelVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) DEFAULT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `modelCenterUuid` varchar(32) NOT NULL,
    `parameters` mediumtext DEFAULT NULL,
    `installPath` varchar(2048) DEFAULT NULL,
    `introduction` mediumtext DEFAULT NULL,
    `logo` mediumtext DEFAULT NULL,
    `version` varchar(255) DEFAULT NULL,
    `vendor` varchar(255) DEFAULT NULL,
    `type` varchar(32) NOT NULL,
    `size` bigint(20) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkModelVOModelCenterVO FOREIGN KEY (modelCenterUuid) REFERENCES ModelCenterVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ModelServiceVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) DEFAULT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `type` varchar(32) NOT NULL,
    `yaml` mediumtext NOT NULL,
    `requestCpu` int(10) NOT NULL,
    `requestMemory` bigint(20) NOT NULL,
    `framework` varchar(255) DEFAULT 'Other',
    `condaVersion` varchar(32) DEFAULT NULL,
    `dockerImage` varchar(255) DEFAULT NULL,
    `gpuComputeCapability` varchar(32) DEFAULT NULL,
    `system` tinyint(1) DEFAULT 0,
    `modelCenterUuid` varchar(32) NOT NULL,
    `vmImageUuid` varchar(32) DEFAULT NULL,
    `installPath` varchar(512) NOT NULL,
    `startCommand` varchar(1024) NOT NULL,
    `pythonVersion` varchar(32) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ModelServiceRefVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `modelUuid` varchar(32) NOT NULL,
    `modelServiceUuid` varchar(32) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkModelRefVO FOREIGN KEY (modelUuid) REFERENCES ModelVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT fkModelServiceRefVO FOREIGN KEY (modelServiceUuid) REFERENCES ModelServiceVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`ModelServiceInstanceGroupVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `modelServiceUuid` varchar(32) NOT NULL,
    `modelUuid` varchar(32) NOT NULL,
    `name` varchar(255) DEFAULT NULL,
    `status` varchar(255) NOT NULL,
    `type` varchar(128) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    CONSTRAINT fkModelServiceInstanceGroupVOModelServiceModelServiceVO FOREIGN KEY (modelServiceUuid) REFERENCES ModelServiceVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ModelServiceInstanceVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `description` varchar(2048) DEFAULT NULL,
    `yaml` mediumtext DEFAULT NULL,
    `status` varchar(255) NOT NULL,
    `url` varchar(2048) NOT NULL,
    `modelServiceGroupUuid` varchar(32) NOT NULL,
    `vmInstanceUuid` varchar(32) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkModelServiceInstanceVOVmInstanceVO FOREIGN KEY (vmInstanceUuid) REFERENCES VmInstanceEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--CREATE TABLE  `zstack`.`VmModelServiceInstanceVO` (
--    `uuid` varchar(32) NOT NULL UNIQUE,
--    `modelServiceInstanceGroupUuid` varchar(32) DEFAULT NULL,
--    PRIMARY KEY  (`uuid`)
--) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`SSOServerTokenVO`(
    `uuid` varchar(32) not null unique,
    `accessToken` text DEFAULT NULL,
    `idToken` text DEFAULT NULL,
    `refreshToken` text DEFAULT NULL,
    `userUuid` varchar(32) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ContainerManagementVmVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `managementIp` varchar(255) DEFAULT NULL,
    `vendor` varchar(64) DEFAULT NULL,
    `managementPort` int unsigned DEFAULT NULL,
    `vmInstanceUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`DatasetVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NULL,
    `url` varchar(2048) NULL,
    `installPath` varchar(2048) NULL,
    `description` varchar(2048) NULL,
    `modelCenterUuid` varchar(32) NOT NULL,
    `size` bigint NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  CONSTRAINT fkDatasetVOModelCenterVO FOREIGN KEY (modelCenterUuid) REFERENCES ModelCenterVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
