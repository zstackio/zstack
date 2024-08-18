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
    `containerNetwork` varchar(2048) DEFAULT NULL,
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
    `size` bigint(20) DEFAULT 0,
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
    `modelServiceUuid` varchar(32) DEFAULT NULL,
    `modelUuid` varchar(32) DEFAULT NULL,
    `name` varchar(255) DEFAULT NULL,
    `status` varchar(255) NOT NULL,
    `modelServiceType` varchar(62) NOT NULL,
    `type` varchar(128) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    CONSTRAINT fkModelServiceInstanceGroupVOModelServiceModelServiceVO FOREIGN KEY (modelServiceUuid) REFERENCES ModelServiceVO (uuid) ON DELETE SET NULL,
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
    CONSTRAINT fkModelServiceInstanceVOVmInstanceVO FOREIGN KEY (vmInstanceUuid) REFERENCES VmInstanceEO (uuid) ON DELETE SET NULL
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
    `name` varchar(255) DEFAULT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `managementIp` varchar(255) DEFAULT NULL,
    `vendor` varchar(64) DEFAULT NULL,
    `managementPort` int unsigned DEFAULT NULL,
    `vmInstanceUuid` varchar(32) NOT NULL,
    `accessKeyId` VARCHAR(128) NOT NULL,
    `accessKeySecret` VARCHAR(128) NOT NULL,
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
    `system` tinyint(1) DEFAULT 0,
    `size` bigint NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  CONSTRAINT fkDatasetVOModelCenterVO FOREIGN KEY (modelCenterUuid) REFERENCES ModelCenterVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ModelServiceGroupDatasetRefVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `datasetUuid` varchar(32) NOT NULL,
    `modelServiceInstanceGroupUuid` varchar(32) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkDatasetRefVO FOREIGN KEY (datasetUuid) REFERENCES DatasetVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT fkModelServiceInstanceGroupVORefVO FOREIGN KEY (modelServiceInstanceGroupUuid) REFERENCES ModelServiceInstanceGroupVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ModelServiceGroupModelServiceRefVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `modelServiceInstanceGroupUuid` varchar(32) NOT NULL,
    `dependModelServiceInstanceGroupUuid` varchar(32) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkModelServiceGroupModelServiceRefVOModelServicePrimary FOREIGN KEY (dependModelServiceInstanceGroupUuid) REFERENCES ModelServiceInstanceGroupVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT fkModelServiceGroupModelServiceRefVOModelServiceDepend FOREIGN KEY (modelServiceInstanceGroupUuid) REFERENCES ModelServiceInstanceGroupVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ModelEvalServiceInstanceGroupVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `temperature` FLOAT NULL,
    `topK` INT NULL,
    `topP` FLOAT NULL,
    `maxLength` INT NULL,
    `maxNewTokens` INT NULL,
    `repetitionPenalty` FLOAT NULL,
    `limits` INT,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`UserProxyConfigVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `proxyType` varchar(255) NULL,
    `proxyHost` varchar(255) NULL,
    `proxyPort` int NULL,
    `proxyUsername` varchar(255) NULL,
    `proxyPassword` varchar(255) NULL,
    `isEnabled` boolean NULL,
    `proxyProtocolVersion` varchar(255) NULL,
    `useSsl` boolean NULL,
    `noProxy` varchar(255) NULL,
    `createDate` timestamp NULL,
    `lastOpDate` timestamp NULL,
  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`UserProxyConfigResourceRefVO` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `resourceUuid` varchar(32) NOT NULL,
    `proxyUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`id`),
    UNIQUE KEY `id` (`id`),
    KEY `fkUserProxyConfigResourceRefVOResourceVO` (`resourceUuid`),
    KEY `fkUserProxyConfigResourceRefVOUserProxyConfigVO` (`proxyUuid`),
    CONSTRAINT `fUserProxyConfigResourceRefVO` FOREIGN KEY (`resourceUuid`) REFERENCES `ResourceVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkUserProxyConfigResourceRefVO1` FOREIGN KEY (`proxyUuid`) REFERENCES `UserProxyConfigVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ModelEvaluationTaskVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) DEFAULT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `percentage` int(3) DEFAULT 0,
    `status` varchar(64) NOT NULL,
    `modelServiceGroupUuid` varchar(32) NOT NULL,
    `evaluatedServiceGroupUuid` varchar(32) NOT NULL,
    `datasetUuid` varchar(32) NOT NULL,
    `limits` int(3) DEFAULT 0,
    `opaque` mediumtext DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`TrainedModelRecordVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `modelUuid` varchar(32) NOT NULL,
    `sourceModelUuid` varchar(32) DEFAULT NULL,
    `modelServiceInstanceGroupUuid` varchar(32) NOT NULL,
    `datasetUuid` varchar(32) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;