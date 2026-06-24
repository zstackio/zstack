-- ZSTAC-75429: scope AI ModelCenter-derived resources by zone.
CALL ADD_COLUMN('ModelVO', 'zoneUuid', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('ModelServiceVO', 'zoneUuid', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('DatasetVO', 'zoneUuid', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('ModelServiceInstanceGroupVO', 'zoneUuid', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('ModelServiceInstanceVO', 'launchCommand', 'MEDIUMTEXT', 1, NULL);
CALL ADD_COLUMN('ZdfsVO', 'metaServerPort', 'INT', 0, 6379);

UPDATE `zstack`.`ModelCenterVO` mc
INNER JOIN (
    SELECT ms.`modelCenterUuid`, MIN(vm.`zoneUuid`) AS `zoneUuid`, COUNT(DISTINCT vm.`zoneUuid`) AS `zoneCount`
    FROM `zstack`.`ModelServiceVO` ms
    INNER JOIN `zstack`.`ModelServiceInstanceGroupVO` g ON g.`modelServiceUuid` = ms.`uuid`
    INNER JOIN `zstack`.`ModelServiceInstanceVO` i ON i.`modelServiceGroupUuid` = g.`uuid`
    INNER JOIN `zstack`.`VmInstanceVO` vm ON vm.`uuid` = i.`vmInstanceUuid`
    WHERE vm.`zoneUuid` IS NOT NULL
      AND ms.`modelCenterUuid` IS NOT NULL
    GROUP BY ms.`modelCenterUuid`
) inferred ON inferred.`modelCenterUuid` = mc.`uuid`
SET mc.`zoneUuid` = inferred.`zoneUuid`
WHERE mc.`zoneUuid` IS NULL
  AND inferred.`zoneCount` = 1;

UPDATE `zstack`.`ModelVO` m
INNER JOIN `zstack`.`ModelCenterVO` mc ON m.`modelCenterUuid` = mc.`uuid`
SET m.`zoneUuid` = mc.`zoneUuid`
WHERE m.`zoneUuid` IS NULL;

UPDATE `zstack`.`ModelServiceVO` ms
INNER JOIN `zstack`.`ModelCenterVO` mc ON ms.`modelCenterUuid` = mc.`uuid`
SET ms.`zoneUuid` = mc.`zoneUuid`
WHERE ms.`zoneUuid` IS NULL;

UPDATE `zstack`.`DatasetVO` d
INNER JOIN `zstack`.`ModelCenterVO` mc ON d.`modelCenterUuid` = mc.`uuid`
SET d.`zoneUuid` = mc.`zoneUuid`
WHERE d.`zoneUuid` IS NULL;

UPDATE `zstack`.`ModelServiceVO` ms
INNER JOIN (
    SELECT g.`modelServiceUuid`, MIN(vm.`zoneUuid`) AS `zoneUuid`, COUNT(DISTINCT vm.`zoneUuid`) AS `zoneCount`
    FROM `zstack`.`ModelServiceInstanceGroupVO` g
    INNER JOIN `zstack`.`ModelServiceInstanceVO` i ON i.`modelServiceGroupUuid` = g.`uuid`
    INNER JOIN `zstack`.`VmInstanceVO` vm ON vm.`uuid` = i.`vmInstanceUuid`
    WHERE vm.`zoneUuid` IS NOT NULL
      AND g.`modelServiceUuid` IS NOT NULL
    GROUP BY g.`modelServiceUuid`
) inferred ON inferred.`modelServiceUuid` = ms.`uuid`
SET ms.`zoneUuid` = inferred.`zoneUuid`
WHERE ms.`zoneUuid` IS NULL
  AND inferred.`zoneCount` = 1;

UPDATE `zstack`.`ModelVO` m
INNER JOIN (
    SELECT g.`modelUuid`, MIN(vm.`zoneUuid`) AS `zoneUuid`, COUNT(DISTINCT vm.`zoneUuid`) AS `zoneCount`
    FROM `zstack`.`ModelServiceInstanceGroupVO` g
    INNER JOIN `zstack`.`ModelServiceInstanceVO` i ON i.`modelServiceGroupUuid` = g.`uuid`
    INNER JOIN `zstack`.`VmInstanceVO` vm ON vm.`uuid` = i.`vmInstanceUuid`
    WHERE vm.`zoneUuid` IS NOT NULL
      AND g.`modelUuid` IS NOT NULL
    GROUP BY g.`modelUuid`
) inferred ON inferred.`modelUuid` = m.`uuid`
SET m.`zoneUuid` = inferred.`zoneUuid`
WHERE m.`zoneUuid` IS NULL
  AND inferred.`zoneCount` = 1;

UPDATE `zstack`.`ModelServiceInstanceGroupVO` g
LEFT JOIN `zstack`.`ModelServiceVO` ms ON g.`modelServiceUuid` = ms.`uuid`
LEFT JOIN `zstack`.`ModelVO` m ON g.`modelUuid` = m.`uuid`
SET g.`zoneUuid` = COALESCE(ms.`zoneUuid`, m.`zoneUuid`)
WHERE g.`zoneUuid` IS NULL;

UPDATE `zstack`.`ModelServiceInstanceGroupVO` g
INNER JOIN (
    SELECT i.`modelServiceGroupUuid`, MIN(vm.`zoneUuid`) AS `zoneUuid`, COUNT(DISTINCT vm.`zoneUuid`) AS `zoneCount`
    FROM `zstack`.`ModelServiceInstanceVO` i
    INNER JOIN `zstack`.`VmInstanceVO` vm ON vm.`uuid` = i.`vmInstanceUuid`
    WHERE vm.`zoneUuid` IS NOT NULL
    GROUP BY i.`modelServiceGroupUuid`
) inferred ON inferred.`modelServiceGroupUuid` = g.`uuid`
SET g.`zoneUuid` = inferred.`zoneUuid`
WHERE g.`zoneUuid` IS NULL
  AND inferred.`zoneCount` = 1;

CALL ADD_CONSTRAINT('ModelVO', 'fkModelVOZoneVO', 'zoneUuid', 'ZoneEO', 'uuid', 'RESTRICT');
CALL ADD_CONSTRAINT('ModelServiceVO', 'fkModelServiceVOZoneVO', 'zoneUuid', 'ZoneEO', 'uuid', 'RESTRICT');
CALL ADD_CONSTRAINT('DatasetVO', 'fkDatasetVOZoneVO', 'zoneUuid', 'ZoneEO', 'uuid', 'RESTRICT');
CALL ADD_CONSTRAINT('ModelServiceInstanceGroupVO', 'fkModelServiceInstanceGroupVOZoneVO', 'zoneUuid', 'ZoneEO', 'uuid', 'RESTRICT');

-- ZSTAC-84111: Persist Zaku health state on NativeClusterVO for query and manual recovery.
CALL ADD_COLUMN('NativeClusterVO', 'zakuHealthStatus', 'VARCHAR(32)', 1, 'Unknown');
UPDATE `zstack`.`NativeClusterVO` SET `zakuHealthStatus` = 'Unknown' WHERE `zakuHealthStatus` IS NULL;

-- ZSTAC-82189: distinguish AI inference image variants beyond CPU architecture.
CALL ADD_COLUMN('ModelServiceTemplateVO', 'name', 'VARCHAR(255)', 1, NULL);
CALL ADD_COLUMN('ModelServiceTemplateVO', 'acceleratorType', 'VARCHAR(255)', 1, NULL);
CALL ADD_COLUMN('ModelServiceTemplateVO', 'imageNamePattern', 'VARCHAR(2048)', 1, NULL);
CALL CREATE_INDEX('ModelServiceTemplateVO', 'idxModelServiceTemplateModelServiceUuid', 'modelServiceUuid');
CALL DELETE_INDEX('ModelServiceTemplateVO', 'ukModelServiceCpuArch');

-- AI host model cache persistence
CREATE TABLE IF NOT EXISTS `zstack`.`AiHostCacheStorageVO` (
    `uuid` varchar(32) NOT NULL,
    `hostUuid` varchar(32) NOT NULL,
    `sourceRoot` varchar(2048) NOT NULL,
    `physicalTotalBytes` bigint DEFAULT NULL,
    `physicalAvailableBytes` bigint DEFAULT NULL,
    `policyUsedBytes` bigint DEFAULT NULL,
    `unmanagedUsedBytesEstimate` bigint DEFAULT NULL,
    `policyReservedBytes` bigint DEFAULT NULL,
    `policyMaxSizeBytes` bigint DEFAULT NULL,
    `effectiveAvailableBytes` bigint DEFAULT NULL,
    `highWatermarkBytes` bigint DEFAULT NULL,
    `lowWatermarkBytes` bigint DEFAULT NULL,
    `status` varchar(32) DEFAULT NULL,
    `statusReason` varchar(1024) DEFAULT NULL,
    `lastSyncDate` timestamp NULL DEFAULT NULL,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `ukAiHostCacheStorageVOHostSourceRoot` (`hostUuid`, `sourceRoot`(512)),
    KEY `idxAiHostCacheStorageVOHostUuid` (`hostUuid`),
    CONSTRAINT `fkAiHostCacheStorageVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `zstack`.`HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`AiHostModelCachePolicyVO` (
    `uuid` varchar(32) NOT NULL,
    `hostUuid` varchar(32) NOT NULL,
    `sourceRoot` varchar(2048) NOT NULL,
    `enabled` tinyint(1) DEFAULT NULL,
    `maxSizeBytes` bigint DEFAULT NULL,
    `highWatermarkPercent` int DEFAULT NULL,
    `lowWatermarkPercent` int DEFAULT NULL,
    `disabledReason` varchar(1024) DEFAULT NULL,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `ukAiHostModelCachePolicyVOHostSourceRoot` (`hostUuid`, `sourceRoot`(512)),
    KEY `idxAiHostModelCachePolicyVOHostUuid` (`hostUuid`),
    CONSTRAINT `fkAiHostModelCachePolicyVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `zstack`.`HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`AiHostModelCacheVO` (
    `uuid` varchar(32) NOT NULL,
    `hostUuid` varchar(32) NOT NULL,
    `modelCenterUuid` varchar(32) DEFAULT NULL,
    `modelUuid` varchar(32) DEFAULT NULL,
    `sourceRoot` varchar(255) DEFAULT NULL,
    `sourcePath` varchar(2048) DEFAULT NULL,
    `sizeBytes` bigint DEFAULT NULL,
    `sourceMtime` bigint DEFAULT NULL,
    `checksum` varchar(255) DEFAULT NULL,
    `contentVersion` varchar(255) DEFAULT NULL,
    `identityHash` varchar(255) NOT NULL,
    `status` varchar(32) NOT NULL,
    `desiredRefCount` bigint NOT NULL DEFAULT 0,
    `runningRefCount` bigint NOT NULL DEFAULT 0,
    `reservationUuid` varchar(255) DEFAULT NULL,
    `waiterCount` int DEFAULT NULL,
    `lastAccessDate` timestamp NULL DEFAULT NULL,
    `lastSyncDate` timestamp NULL DEFAULT NULL,
    `failurePhase` varchar(32) DEFAULT NULL,
    `failureCode` varchar(32) DEFAULT NULL,
    `failureMessage` mediumtext DEFAULT NULL,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `ukAiHostModelCacheVOHostIdentityHash` (`hostUuid`, `identityHash`),
    KEY `idxAiHostModelCacheVOHostUuid` (`hostUuid`),
    KEY `idxAiHostModelCacheVOModelUuid` (`modelUuid`),
    KEY `idxAiHostModelCacheVOStatus` (`status`),
    CONSTRAINT `fkAiHostModelCacheVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `zstack`.`HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- AI model service business network support
CREATE TABLE IF NOT EXISTS `zstack`.`AIBusinessGatewayOfferingVO` (
    `uuid` varchar(32) NOT NULL,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `imageUuid` varchar(32) NOT NULL,
    `instanceOfferingUuid` varchar(32) NOT NULL,
    `managementNetworkUuid` varchar(32) NOT NULL,
    `businessNetworkUuid` varchar(32) NOT NULL,
    `developerAccessNetworkUuid` varchar(32) DEFAULT NULL,
    `agentPort` int NOT NULL,
    `listenerPort` int NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00',
    `lastOpDate` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    KEY `idxAIBusinessGatewayOfferingVOImageUuid` (`imageUuid`),
    KEY `idxAIBusinessGatewayOfferingVOInstanceOfferingUuid` (`instanceOfferingUuid`),
    KEY `idxAIBusinessGatewayOfferingVOManagementNetworkUuid` (`managementNetworkUuid`),
    KEY `idxAIBusinessGatewayOfferingVOBusinessNetworkUuid` (`businessNetworkUuid`),
    CONSTRAINT `fkAIBusinessGatewayOfferingVOImageEO` FOREIGN KEY (`imageUuid`) REFERENCES `zstack`.`ImageEO` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fkAIBusinessGatewayOfferingVOInstanceOfferingEO` FOREIGN KEY (`instanceOfferingUuid`) REFERENCES `zstack`.`InstanceOfferingEO` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fkAIBusinessGatewayOfferingVOManagementL3` FOREIGN KEY (`managementNetworkUuid`) REFERENCES `zstack`.`L3NetworkEO` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fkAIBusinessGatewayOfferingVOBusinessL3` FOREIGN KEY (`businessNetworkUuid`) REFERENCES `zstack`.`L3NetworkEO` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fkAIBusinessGatewayOfferingVODeveloperL3` FOREIGN KEY (`developerAccessNetworkUuid`) REFERENCES `zstack`.`L3NetworkEO` (`uuid`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`AIBusinessGatewayVO` (
    `uuid` varchar(32) NOT NULL,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `deploymentMode` varchar(32) NOT NULL,
    `applianceVmUuid` varchar(32) DEFAULT NULL,
    `offeringUuid` varchar(32) DEFAULT NULL,
    `zoneUuid` varchar(32) NOT NULL,
    `clusterUuid` varchar(32) DEFAULT NULL,
    `nativeClusterUuid` varchar(32) DEFAULT NULL,
    `modelServiceNetworkUuid` varchar(32) DEFAULT NULL,
    `developerAccessNetworkUuid` varchar(32) DEFAULT NULL,
    `vipUuid` varchar(32) DEFAULT NULL,
    `scheme` varchar(16) NOT NULL DEFAULT 'http',
    `address` varchar(255) NOT NULL,
    `port` int NOT NULL,
    `managementAddress` varchar(255) NOT NULL,
    `status` varchar(32) NOT NULL DEFAULT 'Disconnected',
    `desiredRuleVersion` varchar(64) DEFAULT NULL,
    `appliedRuleVersion` varchar(64) DEFAULT NULL,
    `version` varchar(64) DEFAULT NULL,
    `ruleSchemaVersion` varchar(32) DEFAULT 'v1',
    `createDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00',
    `lastOpDate` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    KEY `idxAIBusinessGatewayVOZoneUuid` (`zoneUuid`),
    KEY `idxAIBusinessGatewayVOClusterUuid` (`clusterUuid`),
    KEY `idxAIBusinessGatewayVONativeClusterUuid` (`nativeClusterUuid`),
    KEY `idxAIBusinessGatewayVOModelServiceNetworkUuid` (`modelServiceNetworkUuid`),
    CONSTRAINT `fkAIBusinessGatewayVOOffering` FOREIGN KEY (`offeringUuid`) REFERENCES `zstack`.`AIBusinessGatewayOfferingVO` (`uuid`) ON DELETE SET NULL,
    CONSTRAINT `fkAIBusinessGatewayVOZoneEO` FOREIGN KEY (`zoneUuid`) REFERENCES `zstack`.`ZoneEO` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fkAIBusinessGatewayVOClusterEO` FOREIGN KEY (`clusterUuid`) REFERENCES `zstack`.`ClusterEO` (`uuid`) ON DELETE SET NULL,
    CONSTRAINT `fkAIBusinessGatewayVOModelServiceL3` FOREIGN KEY (`modelServiceNetworkUuid`) REFERENCES `zstack`.`L3NetworkEO` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fkAIBusinessGatewayVODeveloperL3` FOREIGN KEY (`developerAccessNetworkUuid`) REFERENCES `zstack`.`L3NetworkEO` (`uuid`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ModelCenterBusinessNetworkProfileVO` (
    `uuid` varchar(32) NOT NULL,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `modelCenterUuid` varchar(32) NOT NULL,
    `zoneUuid` varchar(32) NOT NULL,
    `clusterUuid` varchar(32) DEFAULT NULL,
    `nativeClusterUuid` varchar(32) DEFAULT NULL,
    `backendType` varchar(32) NOT NULL,
    `modelServiceNetworkUuid` varchar(32) DEFAULT NULL,
    `developerAccessNetworkUuid` varchar(32) DEFAULT NULL,
    `storageNetworkUuid` varchar(32) DEFAULT NULL,
    `containerNetwork` varchar(255) DEFAULT NULL,
    `containerDeveloperAccessNetwork` varchar(255) DEFAULT NULL,
    `containerStorageNetwork` varchar(255) DEFAULT NULL,
    `businessGatewayUuid` varchar(32) NOT NULL,
    `developerAccessGatewayUuid` varchar(32) DEFAULT NULL,
    `defaultProfile` tinyint(1) NOT NULL DEFAULT 0,
    `status` varchar(32) NOT NULL DEFAULT 'Enabled',
    `createDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00',
    `lastOpDate` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `ukModelCenterBusinessNetworkProfileVOZoneName` (`zoneUuid`, `name`),
    KEY `idxModelCenterBusinessNetworkProfileVOModelCenterUuid` (`modelCenterUuid`),
    KEY `idxModelCenterBusinessNetworkProfileVOClusterUuid` (`clusterUuid`),
    KEY `idxModelCenterBusinessNetworkProfileVONativeClusterUuid` (`nativeClusterUuid`),
    KEY `idxModelCenterBusinessNetworkProfileVOBusinessGatewayUuid` (`businessGatewayUuid`),
    CONSTRAINT `fkModelCenterBusinessNetworkProfileVOModelCenter` FOREIGN KEY (`modelCenterUuid`) REFERENCES `zstack`.`ModelCenterVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkModelCenterBusinessNetworkProfileVOZoneEO` FOREIGN KEY (`zoneUuid`) REFERENCES `zstack`.`ZoneEO` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fkModelCenterBusinessNetworkProfileVOClusterEO` FOREIGN KEY (`clusterUuid`) REFERENCES `zstack`.`ClusterEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkModelCenterBusinessNetworkProfileVOModelServiceL3` FOREIGN KEY (`modelServiceNetworkUuid`) REFERENCES `zstack`.`L3NetworkEO` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fkModelCenterBusinessNetworkProfileVODeveloperL3` FOREIGN KEY (`developerAccessNetworkUuid`) REFERENCES `zstack`.`L3NetworkEO` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fkModelCenterBusinessNetworkProfileVOStorageL3` FOREIGN KEY (`storageNetworkUuid`) REFERENCES `zstack`.`L3NetworkEO` (`uuid`) ON DELETE SET NULL,
    CONSTRAINT `fkModelCenterBusinessNetworkProfileVOBusinessGateway` FOREIGN KEY (`businessGatewayUuid`) REFERENCES `zstack`.`AIBusinessGatewayVO` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fkModelCenterBusinessNetworkProfileVODeveloperGateway` FOREIGN KEY (`developerAccessGatewayUuid`) REFERENCES `zstack`.`AIBusinessGatewayVO` (`uuid`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CALL ADD_COLUMN('ModelServiceInstanceGroupVO', 'businessNetworkProfileUuid', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('ModelServiceInstanceGroupVO', 'businessGatewayUuid', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('ModelServiceInstanceGroupVO', 'developerAccessGatewayUuid', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('ModelServiceInstanceGroupVO', 'businessEndpoint', 'VARCHAR(2048)', 1, NULL);
CALL ADD_COLUMN('ModelServiceInstanceGroupVO', 'developerEndpoint', 'VARCHAR(2048)', 1, NULL);
CALL ADD_COLUMN('ModelServiceInstanceGroupVO', 'businessEndpointStatus', 'VARCHAR(32)', 1, NULL);

CALL ADD_CONSTRAINT('ModelServiceInstanceGroupVO', 'fkModelServiceInstanceGroupVOBusinessNetworkProfile',
                    'businessNetworkProfileUuid', 'ModelCenterBusinessNetworkProfileVO', 'uuid', 'SET NULL');
CALL ADD_CONSTRAINT('ModelServiceInstanceGroupVO', 'fkModelServiceInstanceGroupVOBusinessGateway',
                    'businessGatewayUuid', 'AIBusinessGatewayVO', 'uuid', 'SET NULL');
CALL ADD_CONSTRAINT('ModelServiceInstanceGroupVO', 'fkModelServiceInstanceGroupVODeveloperAccessGateway',
                    'developerAccessGatewayUuid', 'AIBusinessGatewayVO', 'uuid', 'SET NULL');
