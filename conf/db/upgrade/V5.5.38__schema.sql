CALL ADD_COLUMN('SNSSnmpPlatformVO', 'version', 'VARCHAR(32)', 0, 'v1');
CALL ADD_COLUMN('SNSSnmpPlatformVO', 'community', 'TEXT', 1, NULL);
CALL ADD_COLUMN('SNSSnmpPlatformVO', 'userName', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('SNSSnmpPlatformVO', 'authEnabled', 'tinyint(1)', 0, 0);
CALL ADD_COLUMN('SNSSnmpPlatformVO', 'authAlgorithm', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('SNSSnmpPlatformVO', 'authPassword', 'TEXT', 1, NULL);
CALL ADD_COLUMN('SNSSnmpPlatformVO', 'privacyEnabled', 'tinyint(1)', 0, 0);
CALL ADD_COLUMN('SNSSnmpPlatformVO', 'privacyAlgorithm', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('SNSSnmpPlatformVO', 'privacyPassword', 'TEXT', 1, NULL);
CALL ADD_COLUMN('SNSSnmpPlatformVO', 'configRevision', 'BIGINT UNSIGNED', 0, 0);

CREATE TABLE IF NOT EXISTS `zstack`.`SnmpEngineVO` (
    `uuid` varchar(32) NOT NULL,
    `engineId` varchar(64) NOT NULL UNIQUE,
    `engineBoots` int unsigned NOT NULL DEFAULT 1,
    `engineStartTime` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `ownerManagementNodeUuid` varchar(32) DEFAULT NULL,
    `ownerEpochUuid` varchar(32) DEFAULT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`AICapacityReservationVO` (
    `uuid` varchar(32) NOT NULL,
    `accountUuid` varchar(32) NOT NULL,
    `quotaName` varchar(255) NOT NULL,
    `reservedBytes` bigint NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    KEY `idx_ai_capacity_reservation_account_quota` (`accountUuid`, `quotaName`),
    CONSTRAINT `fkAICapacityReservationVOAccountVO`
        FOREIGN KEY (`accountUuid`) REFERENCES `zstack`.`AccountVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- AI host-model-cache / business-network / template schema ported from
-- feature-5.5.32-aios commit 30a51aa3ec (V5.5.32__schema.sql) plus
-- 0eaaf669a5 (INSERT line) and the ZSTAC-87753 hostUuid-nullable ALTER.
-- 5.5.38 never runs V5.5.32; this is the Flyway file that applies.

-- =====================================================================

-- Phase 1: tables and columns
-- ============================================================================

-- ZSTAC-82189: distinguish AI inference image variants beyond CPU architecture.
CALL ADD_COLUMN('ModelServiceTemplateVO', 'name', 'VARCHAR(255)', 1, NULL);
CALL ADD_COLUMN('ModelServiceTemplateVO', 'acceleratorType', 'VARCHAR(255)', 1, NULL);
CALL ADD_COLUMN('ModelServiceTemplateVO', 'imageNamePattern', 'VARCHAR(2048)', 1, NULL);

-- Host Model Cache control-plane state for VM/cloud-host model service deployments.
CREATE TABLE IF NOT EXISTS `zstack`.`AiHostModelCacheVO` (
    `uuid`              VARCHAR(32)   NOT NULL,
    `hostUuid`          VARCHAR(32)   NOT NULL,
    `primaryStorageUuid` VARCHAR(32)  DEFAULT NULL,
    `primaryStorageName` VARCHAR(255) DEFAULT NULL,
    `modelCenterUuid`   VARCHAR(32)   DEFAULT NULL,
    `modelUuid`         VARCHAR(32)   DEFAULT NULL,
    `sourceRoot`        VARCHAR(2048) DEFAULT NULL,
    `sourcePath`        VARCHAR(2048) DEFAULT NULL,
    `sizeBytes`         BIGINT        DEFAULT NULL,
    `sourceMtime`       BIGINT        DEFAULT NULL,
    `checksum`          VARCHAR(255)  DEFAULT NULL,
    `contentVersion`    VARCHAR(255)  DEFAULT NULL,
    `identityHash`      VARCHAR(255)  NOT NULL,
    `status`            VARCHAR(32)   NOT NULL,
    `desiredRefCount`   BIGINT        NOT NULL DEFAULT 0,
    `runningRefCount`   BIGINT        NOT NULL DEFAULT 0,
    `reservationUuid`   VARCHAR(32)   DEFAULT NULL,
    `waiterCount`       INT           DEFAULT NULL,
    `lastAccessDate`    TIMESTAMP     NULL DEFAULT NULL,
    `lastSyncDate`      TIMESTAMP     NULL DEFAULT NULL,
    `failurePhase`      VARCHAR(64)   DEFAULT NULL,
    `failureCode`       VARCHAR(64)   DEFAULT NULL,
    `failureMessage`    MEDIUMTEXT    DEFAULT NULL,
    `createDate`        TIMESTAMP     NOT NULL DEFAULT '2000-01-01 00:00:00',
    `lastOpDate`        TIMESTAMP     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `ukAiHostModelCacheVOHostIdentity` (`hostUuid`, `identityHash`),
    KEY `idxAiHostModelCacheVOHostRoot` (`hostUuid`, `sourceRoot`(255)),
    KEY `idxAiHostModelCacheVOPrimaryStorage` (`primaryStorageUuid`),
    KEY `idxAiHostModelCacheVOModel` (`modelUuid`),
    KEY `idxAiHostModelCacheVOStatus` (`status`),
    CONSTRAINT `fkAiHostModelCacheVOHostEO`
        FOREIGN KEY (`hostUuid`) REFERENCES `zstack`.`HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Drift repair for old AIOS environments whose table predates these columns.
CALL ADD_COLUMN('AiHostModelCacheVO', 'primaryStorageUuid', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('AiHostModelCacheVO', 'primaryStorageName', 'VARCHAR(255)', 1, NULL);

CREATE TABLE IF NOT EXISTS `zstack`.`AiHostCacheStorageVO` (
    `uuid`                    VARCHAR(32)   NOT NULL,
    `hostUuid`                VARCHAR(32)   NOT NULL,
    `primaryStorageUuid`      VARCHAR(32)   DEFAULT NULL,
    `primaryStorageName`      VARCHAR(255)  DEFAULT NULL,
    `sourceRoot`              VARCHAR(2048) NOT NULL,
    `sourceRootIdentity`      VARCHAR(64)   NOT NULL,
    `physicalTotalBytes`      BIGINT        DEFAULT NULL,
    `physicalAvailableBytes`  BIGINT        DEFAULT NULL,
    `policyUsedBytes`         BIGINT        DEFAULT NULL,
    `unmanagedUsedBytesEstimate` BIGINT     DEFAULT NULL,
    `policyReservedBytes`     BIGINT        DEFAULT NULL,
    `policyMaxSizeBytes`      BIGINT        DEFAULT NULL,
    `effectiveAvailableBytes` BIGINT        DEFAULT NULL,
    `highWatermarkBytes`      BIGINT        DEFAULT NULL,
    `lowWatermarkBytes`       BIGINT        DEFAULT NULL,
    `status`                  VARCHAR(32)   DEFAULT NULL,
    `statusReason`            VARCHAR(1024) DEFAULT NULL,
    `lastSyncDate`            TIMESTAMP     NULL DEFAULT NULL,
    `createDate`              TIMESTAMP     NOT NULL DEFAULT '2000-01-01 00:00:00',
    `lastOpDate`              TIMESTAMP     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `ukAiHostCacheStorageVOHostRootIdentity` (`hostUuid`, `sourceRootIdentity`),
    KEY `idxAiHostCacheStorageVOPrimaryStorage` (`primaryStorageUuid`),
    KEY `idxAiHostCacheStorageVOStatus` (`status`),
    CONSTRAINT `fkAiHostCacheStorageVOHostEO`
        FOREIGN KEY (`hostUuid`) REFERENCES `zstack`.`HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CALL ADD_COLUMN('AiHostCacheStorageVO', 'sourceRootIdentity', 'VARCHAR(64)', 1, NULL);
CALL ADD_COLUMN('AiHostCacheStorageVO', 'primaryStorageUuid', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('AiHostCacheStorageVO', 'primaryStorageName', 'VARCHAR(255)', 1, NULL);

CREATE TABLE IF NOT EXISTS `zstack`.`AiHostModelCachePolicyVO` (
    `uuid`                 VARCHAR(32)   NOT NULL,
    `hostUuid`             VARCHAR(32)   NOT NULL,
    `primaryStorageUuid`   VARCHAR(32)   DEFAULT NULL,
    `primaryStorageName`   VARCHAR(255)  DEFAULT NULL,
    `sourceRoot`           VARCHAR(2048) NOT NULL,
    `sourceRootIdentity`   VARCHAR(64)   NOT NULL,
    `enabled`              TINYINT(1)    DEFAULT NULL,
    `maxSizeBytes`         BIGINT        DEFAULT NULL,
    `highWatermarkPercent` INT           DEFAULT NULL,
    `lowWatermarkPercent`  INT           DEFAULT NULL,
    `disabledReason`       VARCHAR(1024) DEFAULT NULL,
    `createDate`           TIMESTAMP     NOT NULL DEFAULT '2000-01-01 00:00:00',
    `lastOpDate`           TIMESTAMP     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `ukAiHostModelCachePolicyVOHostRootIdentity` (`hostUuid`, `sourceRootIdentity`),
    KEY `idxAiHostModelCachePolicyVOPrimaryStorage` (`primaryStorageUuid`),
    CONSTRAINT `fkAiHostModelCachePolicyVOHostEO`
        FOREIGN KEY (`hostUuid`) REFERENCES `zstack`.`HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CALL ADD_COLUMN('AiHostModelCachePolicyVO', 'sourceRootIdentity', 'VARCHAR(64)', 1, NULL);
CALL ADD_COLUMN('AiHostModelCachePolicyVO', 'primaryStorageUuid', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('AiHostModelCachePolicyVO', 'primaryStorageName', 'VARCHAR(255)', 1, NULL);

CREATE TABLE IF NOT EXISTS `zstack`.`AiHostModelCacheReservationVO` (
    `uuid`              VARCHAR(32)   NOT NULL,
    `hostUuid`          VARCHAR(32)   NOT NULL,
    `sourceRoot`        VARCHAR(2048) DEFAULT NULL,
    `modelUuid`         VARCHAR(32)   DEFAULT NULL,
    `modelCenterUuid`   VARCHAR(32)   DEFAULT NULL,
    `ownerType`         VARCHAR(32)   NOT NULL,
    `ownerResourceUuid` VARCHAR(32)   NOT NULL,
    `reservedBytes`     BIGINT        NOT NULL,
    `status`            VARCHAR(32)   NOT NULL,
    `expiredDate`       TIMESTAMP     NULL DEFAULT NULL,
    `createDate`        TIMESTAMP     NOT NULL DEFAULT '2000-01-01 00:00:00',
    `lastOpDate`        TIMESTAMP     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    KEY `idxAiHostModelCacheReservationVOHostRoot` (`hostUuid`, `sourceRoot`(255)),
    KEY `idxAiHostModelCacheReservationVOOwner` (`ownerType`, `ownerResourceUuid`),
    KEY `idxAiHostModelCacheReservationVOStatus` (`status`),
    CONSTRAINT `fkAiHostModelCacheReservationVOHostEO`
        FOREIGN KEY (`hostUuid`) REFERENCES `zstack`.`HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CALL ADD_COLUMN('VmModelMountVO', 'cacheUuid', 'VARCHAR(32)', 1, NULL);

-- AI model service business network support.
CREATE TABLE IF NOT EXISTS `zstack`.`AIBusinessGatewayOfferingVO` (
    `uuid` varchar(32) NOT NULL,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `imageUuid` varchar(32) DEFAULT NULL,
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

-- Converge drift: the earliest AIOS 5.5.28 DDL created imageUuid NOT NULL.
ALTER TABLE `zstack`.`AIBusinessGatewayOfferingVO`
    MODIFY COLUMN `imageUuid` varchar(32) DEFAULT NULL;

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
    `agentStatus` varchar(32) DEFAULT 'Unknown',
    `dataPlaneStatus` varchar(32) DEFAULT 'Unknown',
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
    CONSTRAINT `fkAIBusinessGatewayVOZoneEO` FOREIGN KEY (`zoneUuid`) REFERENCES `zstack`.`ZoneEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkAIBusinessGatewayVOClusterEO` FOREIGN KEY (`clusterUuid`) REFERENCES `zstack`.`ClusterEO` (`uuid`) ON DELETE SET NULL,
    CONSTRAINT `fkAIBusinessGatewayVOModelServiceL3` FOREIGN KEY (`modelServiceNetworkUuid`) REFERENCES `zstack`.`L3NetworkEO` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fkAIBusinessGatewayVODeveloperL3` FOREIGN KEY (`developerAccessNetworkUuid`) REFERENCES `zstack`.`L3NetworkEO` (`uuid`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Component status columns added in AIOS 5.5.28.2; guard for drifted tables.
CALL ADD_COLUMN('AIBusinessGatewayVO', 'agentStatus', 'VARCHAR(32)', 1, 'Unknown');
CALL ADD_COLUMN('AIBusinessGatewayVO', 'dataPlaneStatus', 'VARCHAR(32)', 1, 'Unknown');

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
    `businessGatewayUuid` varchar(32) DEFAULT NULL,
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
    CONSTRAINT `fkModelCenterBusinessNetworkProfileVOZoneEO` FOREIGN KEY (`zoneUuid`) REFERENCES `zstack`.`ZoneEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkModelCenterBusinessNetworkProfileVOClusterEO` FOREIGN KEY (`clusterUuid`) REFERENCES `zstack`.`ClusterEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkModelCenterBusinessNetworkProfileVOModelServiceL3` FOREIGN KEY (`modelServiceNetworkUuid`) REFERENCES `zstack`.`L3NetworkEO` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fkModelCenterBusinessNetworkProfileVODeveloperL3` FOREIGN KEY (`developerAccessNetworkUuid`) REFERENCES `zstack`.`L3NetworkEO` (`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fkModelCenterBusinessNetworkProfileVOStorageL3` FOREIGN KEY (`storageNetworkUuid`) REFERENCES `zstack`.`L3NetworkEO` (`uuid`) ON DELETE SET NULL,
    CONSTRAINT `fkModelCenterBusinessNetworkProfileVOBusinessGateway` FOREIGN KEY (`businessGatewayUuid`) REFERENCES `zstack`.`AIBusinessGatewayVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkModelCenterBusinessNetworkProfileVODeveloperGateway` FOREIGN KEY (`developerAccessGatewayUuid`) REFERENCES `zstack`.`AIBusinessGatewayVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Converge drift: the earliest AIOS 5.5.28 DDL created businessGatewayUuid NOT NULL.
ALTER TABLE `zstack`.`ModelCenterBusinessNetworkProfileVO`
    MODIFY COLUMN `businessGatewayUuid` varchar(32) DEFAULT NULL;

CALL ADD_COLUMN('ModelServiceInstanceGroupVO', 'businessNetworkProfileUuid', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('ModelServiceInstanceGroupVO', 'businessGatewayUuid', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('ModelServiceInstanceGroupVO', 'developerAccessGatewayUuid', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('ModelServiceInstanceGroupVO', 'businessEndpoint', 'VARCHAR(2048)', 1, NULL);
CALL ADD_COLUMN('ModelServiceInstanceGroupVO', 'developerEndpoint', 'VARCHAR(2048)', 1, NULL);
CALL ADD_COLUMN('ModelServiceInstanceGroupVO', 'businessEndpointStatus', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('ModelServiceInstanceGroupVO', 'managementEndpoint', 'VARCHAR(2048)', 1, NULL);

-- ============================================================================
-- Phase 2: data backfill
-- ============================================================================

UPDATE `zstack`.`AiHostCacheStorageVO`
SET `sourceRootIdentity` = SHA2(IFNULL(`sourceRoot`, ''), 256)
WHERE `sourceRootIdentity` IS NULL OR `sourceRootIdentity` = '';

UPDATE `zstack`.`AiHostModelCachePolicyVO`
SET `sourceRootIdentity` = SHA2(IFNULL(`sourceRoot`, ''), 256)
WHERE `sourceRootIdentity` IS NULL OR `sourceRootIdentity` = '';

-- Generate a default AI business gateway instance offering and one offering per
-- Model Center service network for environments that predate the business
-- network feature. Deterministic UUIDs plus INSERT IGNORE / NOT EXISTS keep the
-- backfill single-shot on both upgrade paths.
SET @ai_gateway_instance_offering_uuid = MD5('zstack-ai-business-gateway-instance-offering');
SET @admin_account_uuid = '36c27e8ff05c4780bf6d2fa65700f22e';

INSERT IGNORE INTO `zstack`.`ResourceVO`
    (`uuid`, `resourceName`, `resourceType`, `concreteResourceType`)
SELECT @ai_gateway_instance_offering_uuid,
       'AI 网关计算规格',
       'InstanceOfferingVO',
       'org.zstack.header.configuration.InstanceOfferingVO'
FROM `zstack`.`ModelCenterVO`
WHERE `serviceNetworkUuid` IS NOT NULL
LIMIT 1;

INSERT IGNORE INTO `zstack`.`InstanceOfferingEO`
    (`uuid`, `name`, `description`, `cpuNum`, `cpuSpeed`, `memorySize`, `reservedMemorySize`,
     `allocatorStrategy`, `sortKey`, `state`, `type`, `duration`, `createDate`, `lastOpDate`, `deleted`)
SELECT @ai_gateway_instance_offering_uuid,
       'AI 网关计算规格',
       '由 5.5.38 升级生成，供 AI 网关云主机使用',
       1,
       0,
       1073741824,
       0,
       'LeastVmPreferredHostAllocatorStrategy',
       0,
       'Enabled',
       'UserVm',
       'Permanent',
       CURRENT_TIMESTAMP(),
       CURRENT_TIMESTAMP(),
       NULL
FROM `zstack`.`ModelCenterVO`
WHERE `serviceNetworkUuid` IS NOT NULL
LIMIT 1;

INSERT INTO `zstack`.`AccountResourceRefVO` (`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `concreteResourceType`,
     `permission`, `isShared`, `createDate`, `lastOpDate`)
SELECT @admin_account_uuid,
       @admin_account_uuid,
       @ai_gateway_instance_offering_uuid,
       'InstanceOfferingVO',
       'org.zstack.header.configuration.InstanceOfferingVO',
       2,
       0,
       CURRENT_TIMESTAMP(),
       CURRENT_TIMESTAMP()
FROM `zstack`.`InstanceOfferingEO`
WHERE `uuid` = @ai_gateway_instance_offering_uuid
  AND NOT EXISTS (
      SELECT 1
      FROM `zstack`.`AccountResourceRefVO`
      WHERE `resourceUuid` = @ai_gateway_instance_offering_uuid
  );

INSERT IGNORE INTO `zstack`.`ResourceVO`
    (`uuid`, `resourceName`, `resourceType`, `concreteResourceType`)
SELECT MD5(CONCAT('zstack-ai-business-gateway-offering-', `mc`.`serviceNetworkUuid`)),
       LEFT(CONCAT('AI 网关规格-', COALESCE(NULLIF(MIN(`mc`.`name`), ''), LEFT(`mc`.`serviceNetworkUuid`, 8))), 255),
       'AIBusinessGatewayOfferingVO',
       'org.zstack.ai.entity.AIBusinessGatewayOfferingVO'
FROM `zstack`.`ModelCenterVO` `mc`
JOIN `zstack`.`L3NetworkEO` `l3` ON `l3`.`uuid` = `mc`.`serviceNetworkUuid`
LEFT JOIN `zstack`.`AIBusinessGatewayOfferingVO` `offering`
       ON `offering`.`managementNetworkUuid` = `mc`.`serviceNetworkUuid`
WHERE `offering`.`uuid` IS NULL
GROUP BY `mc`.`serviceNetworkUuid`;

INSERT IGNORE INTO `zstack`.`AIBusinessGatewayOfferingVO`
    (`uuid`, `name`, `description`, `imageUuid`, `instanceOfferingUuid`, `managementNetworkUuid`,
     `businessNetworkUuid`, `developerAccessNetworkUuid`, `agentPort`, `listenerPort`, `createDate`, `lastOpDate`)
SELECT MD5(CONCAT('zstack-ai-business-gateway-offering-', `mc`.`serviceNetworkUuid`)),
       LEFT(CONCAT('AI 网关规格-', COALESCE(NULLIF(MIN(`mc`.`name`), ''), LEFT(`mc`.`serviceNetworkUuid`, 8))), 255),
       '由 5.5.38 升级从 Model Center 网络配置生成，请关联 AI 网关镜像后使用',
       NULL,
       @ai_gateway_instance_offering_uuid,
       `mc`.`serviceNetworkUuid`,
       `mc`.`serviceNetworkUuid`,
       `mc`.`serviceNetworkUuid`,
       7777,
       80,
       CURRENT_TIMESTAMP(),
       CURRENT_TIMESTAMP()
FROM `zstack`.`ModelCenterVO` `mc`
JOIN `zstack`.`L3NetworkEO` `l3` ON `l3`.`uuid` = `mc`.`serviceNetworkUuid`
LEFT JOIN `zstack`.`AIBusinessGatewayOfferingVO` `offering`
       ON `offering`.`managementNetworkUuid` = `mc`.`serviceNetworkUuid`
WHERE `offering`.`uuid` IS NULL
GROUP BY `mc`.`serviceNetworkUuid`;

-- ============================================================================
-- Phase 3: indexes and constraints
-- ============================================================================

CALL CREATE_INDEX('ModelServiceTemplateVO', 'idxModelServiceTemplateModelServiceUuid', 'modelServiceUuid');
CALL DELETE_INDEX('ModelServiceTemplateVO', 'ukModelServiceCpuArch');

CALL CREATE_INDEX('AiHostModelCacheVO', 'idxAiHostModelCacheVOPrimaryStorage', 'primaryStorageUuid');
CALL CREATE_INDEX('AiHostCacheStorageVO', 'idxAiHostCacheStorageVOPrimaryStorage', 'primaryStorageUuid');
CALL CREATE_INDEX('AiHostModelCachePolicyVO', 'idxAiHostModelCachePolicyVOPrimaryStorage', 'primaryStorageUuid');

ALTER TABLE `zstack`.`AiHostCacheStorageVO` MODIFY COLUMN `sourceRootIdentity` VARCHAR(64) NOT NULL;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.statistics
                     WHERE table_schema = 'zstack'
                       AND table_name = 'AiHostCacheStorageVO'
                       AND index_name = 'ukAiHostCacheStorageVOHostRootIdentity');
SET @sql = IF(@index_exists = 0,
              'ALTER TABLE `zstack`.`AiHostCacheStorageVO` ADD UNIQUE KEY `ukAiHostCacheStorageVOHostRootIdentity` (`hostUuid`, `sourceRootIdentity`)',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
CALL DELETE_INDEX('AiHostCacheStorageVO', 'ukAiHostCacheStorageVOHostRoot');

ALTER TABLE `zstack`.`AiHostModelCachePolicyVO` MODIFY COLUMN `sourceRootIdentity` VARCHAR(64) NOT NULL;
CALL DELETE_INDEX('AiHostModelCachePolicyVO', 'ukAiHostModelCachePolicyVOHostRoot');
SET @index_exists = (SELECT COUNT(*) FROM information_schema.statistics
                     WHERE table_schema = 'zstack'
                       AND table_name = 'AiHostModelCachePolicyVO'
                       AND index_name = 'ukAiHostModelCachePolicyVOHostRootIdentity');
SET @sql = IF(@index_exists = 0,
              'ALTER TABLE `zstack`.`AiHostModelCachePolicyVO` ADD UNIQUE KEY `ukAiHostModelCachePolicyVOHostRootIdentity` (`hostUuid`, `sourceRootIdentity`)',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ZSTAC-87753: primary-storage cache policies are resource-level and shared by
-- all hosts; shared rows carry hostUuid NULL, so the column must be nullable.
ALTER TABLE `zstack`.`AiHostModelCachePolicyVO` MODIFY COLUMN `hostUuid` VARCHAR(32) NULL;

CALL CREATE_INDEX('VmModelMountVO', 'idxVmModelMountVOCacheUuid', 'cacheUuid');
CALL ADD_CONSTRAINT('VmModelMountVO', 'fkVmModelMountVOAiHostModelCacheVO', 'cacheUuid', 'AiHostModelCacheVO', 'uuid', 'SET NULL');

CALL ADD_CONSTRAINT('ModelServiceInstanceGroupVO', 'fkModelServiceInstanceGroupVOBusinessNetworkProfile',
                    'businessNetworkProfileUuid', 'ModelCenterBusinessNetworkProfileVO', 'uuid', 'SET NULL');
CALL ADD_CONSTRAINT('ModelServiceInstanceGroupVO', 'fkModelServiceInstanceGroupVOBusinessGateway',
                    'businessGatewayUuid', 'AIBusinessGatewayVO', 'uuid', 'SET NULL');
CALL ADD_CONSTRAINT('ModelServiceInstanceGroupVO', 'fkModelServiceInstanceGroupVODeveloperAccessGateway',
                    'developerAccessGatewayUuid', 'AIBusinessGatewayVO', 'uuid', 'SET NULL');

-- ZNS segment binding state and controller inventory

CALL ADD_COLUMN('ZnsControllerVO', 'zcfAccessKeyId', 'VARCHAR(64)', 1, NULL);
CALL ADD_COLUMN('ZnsControllerVO', 'zcfAccessKeySecret', 'TEXT', 1, NULL);

CREATE TABLE IF NOT EXISTS `zstack`.`ZnsControllerStateVO` (
    `sdnControllerUuid` varchar(32) NOT NULL,
    `activeScanToken` varchar(255) DEFAULT NULL,
    `activeScanStartedAt` timestamp NULL DEFAULT NULL,
    `previousCompleteScanToken` varchar(255) DEFAULT NULL,
    `previousCompleteScanDigest` varchar(64) DEFAULT NULL,
    `previousCompleteScanTotal` int DEFAULT NULL,
    `migrationState` varchar(32) NOT NULL DEFAULT 'NotStarted',
    `compatibilityMode` varchar(32) NOT NULL DEFAULT 'LEGACY',
    `compatibilityEpoch` bigint NOT NULL DEFAULT 0,
    `remoteCompatibilityEpoch` bigint DEFAULT NULL,
    `capabilityDigest` varchar(64) DEFAULT NULL,
    `migrationDigest` varchar(64) DEFAULT NULL,
    `pendingContractEpoch` bigint DEFAULT NULL,
    `pendingContractRequestDigest` varchar(64) DEFAULT NULL,
    `lastSuccessfulScanDate` timestamp NULL DEFAULT NULL,
    `lastErrorCode` varchar(128) DEFAULT NULL,
    `lastErrorDetails` text DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`sdnControllerUuid`),
    CONSTRAINT `fkZnsControllerStateVOSdnControllerVO`
        FOREIGN KEY (`sdnControllerUuid`) REFERENCES `zstack`.`SdnControllerVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ZnsControllerCapabilityVO` (
    `uuid` varchar(32) NOT NULL,
    `controllerUuid` varchar(32) NOT NULL,
    `capabilityName` varchar(64) NOT NULL,
    `selectedContract` varchar(32) DEFAULT NULL,
    `observedState` varchar(32) NOT NULL DEFAULT 'NOT_ACTIVATED',
    `effectiveState` varchar(32) NOT NULL DEFAULT 'NOT_ACTIVATED',
    `observedEpoch` bigint NOT NULL DEFAULT 0,
    `supportedContracts` text DEFAULT NULL,
    `servingClusterSupported` tinyint(1) DEFAULT NULL,
    `servingClusterDigest` varchar(128) DEFAULT NULL,
    `cloudClusterSupported` tinyint(1) DEFAULT NULL,
    `cloudClusterDigest` varchar(128) DEFAULT NULL,
    `pendingTransitionUuid` varchar(255) DEFAULT NULL,
    `pendingExpectedEpoch` bigint DEFAULT NULL,
    `pendingSelectedContract` varchar(32) DEFAULT NULL,
    `pendingTargetState` varchar(32) DEFAULT NULL,
    `pendingRequestDigest` varchar(64) DEFAULT NULL,
    `lastErrorCode` varchar(128) DEFAULT NULL,
    `lastErrorDetails` text DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `ukZnsControllerCapabilityVOControllerName` (`controllerUuid`, `capabilityName`),
    CONSTRAINT `fkZnsControllerCapabilityVOSdnControllerVO`
        FOREIGN KEY (`controllerUuid`) REFERENCES `zstack`.`SdnControllerVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `chk_zns_controller_capability_state`
        CHECK (`observedState` IN ('NOT_ACTIVATED', 'MIGRATING_READ_ONLY', 'ACTIVE', 'INCOMPATIBLE_READ_ONLY')
            AND `effectiveState` IN ('NOT_ACTIVATED', 'MIGRATING_READ_ONLY', 'ACTIVE', 'INCOMPATIBLE_READ_ONLY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ZnsSegmentInventoryVO` (
    `uuid` varchar(32) NOT NULL,
    `sdnControllerUuid` varchar(32) NOT NULL,
    `znsSegmentUuid` varchar(36) NOT NULL,
    `name` varchar(255) NOT NULL,
    `transportZoneUuid` varchar(36) NOT NULL,
    `availability` varchar(32) NOT NULL,
    `reasonCode` varchar(128) DEFAULT NULL,
    `lastSeenToken` varchar(36) NOT NULL,
    `lastSeenDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00',
    `createDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uk_zns_seg_inv_controller_segment` (`sdnControllerUuid`, `znsSegmentUuid`),
    KEY `idx_zns_seg_inv_candidate` (`sdnControllerUuid`, `transportZoneUuid`, `availability`),
    CONSTRAINT `fkZnsSegmentInventoryVOSdnControllerVO`
        FOREIGN KEY (`sdnControllerUuid`) REFERENCES `zstack`.`SdnControllerVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ZnsSegmentRefVO` (
    `uuid` varchar(32) NOT NULL,
    `sdnControllerUuid` varchar(32) NOT NULL,
    `znsSegmentUuid` varchar(36) DEFAULT NULL,
    `zoneUuid` varchar(32) NOT NULL,
    `l2NetworkUuid` varchar(32) NOT NULL,
    `accountUuid` varchar(32) NOT NULL,
    `l3NetworkUuid` varchar(32) DEFAULT NULL,
    `znsTenantRouterUuid` varchar(36) DEFAULT NULL,
    `state` varchar(32) NOT NULL,
    `operationUuid` varchar(36) DEFAULT NULL,
    `operationStep` varchar(32) DEFAULT NULL,
    `operationDigest` varchar(64) DEFAULT NULL,
    `currentConfigVersion` bigint DEFAULT NULL,
    `appliedConfigVersion` bigint DEFAULT NULL,
    `lastErrorCode` varchar(128) DEFAULT NULL,
    `lastErrorDetails` text DEFAULT NULL,
    `createDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uk_zns_seg_ref_controller_segment`
        (`sdnControllerUuid`, `znsSegmentUuid`),
    UNIQUE KEY `uk_zns_seg_ref_l2` (`l2NetworkUuid`),
    KEY `idx_zns_seg_ref_account` (`accountUuid`),
    KEY `idx_zns_seg_ref_state` (`sdnControllerUuid`, `state`),
    CONSTRAINT `fkZnsSegmentRefVOSdnControllerVO`
        FOREIGN KEY (`sdnControllerUuid`) REFERENCES `zstack`.`SdnControllerVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `chk_zns_seg_ref_segment_state`
        CHECK (`znsSegmentUuid` IS NOT NULL OR `state` = 'MigrationFailed')
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
