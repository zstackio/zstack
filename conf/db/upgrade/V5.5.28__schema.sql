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

-- Host Model Cache control-plane state for VM/cloud-host model service deployments.
CREATE TABLE IF NOT EXISTS `zstack`.`AiHostModelCacheVO` (
    `uuid`              VARCHAR(32)   NOT NULL,
    `hostUuid`          VARCHAR(32)   NOT NULL,
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
    KEY `idxAiHostModelCacheVOModel` (`modelUuid`),
    KEY `idxAiHostModelCacheVOStatus` (`status`),
    CONSTRAINT `fkAiHostModelCacheVOHostEO`
        FOREIGN KEY (`hostUuid`) REFERENCES `zstack`.`HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`AiHostCacheStorageVO` (
    `uuid`                    VARCHAR(32)   NOT NULL,
    `hostUuid`                VARCHAR(32)   DEFAULT NULL,
    `sourceRoot`              VARCHAR(2048) DEFAULT NULL,
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
    UNIQUE KEY `ukAiHostCacheStorageVOHostRoot` (`hostUuid`, `sourceRoot`(255)),
    KEY `idxAiHostCacheStorageVOStatus` (`status`),
    CONSTRAINT `fkAiHostCacheStorageVOHostEO`
        FOREIGN KEY (`hostUuid`) REFERENCES `zstack`.`HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`AiHostModelCachePolicyVO` (
    `uuid`                 VARCHAR(32)   NOT NULL,
    `hostUuid`             VARCHAR(32)   DEFAULT NULL,
    `sourceRoot`           VARCHAR(2048) DEFAULT NULL,
    `enabled`              TINYINT(1)    DEFAULT NULL,
    `maxSizeBytes`         BIGINT        DEFAULT NULL,
    `highWatermarkPercent` INT           DEFAULT NULL,
    `lowWatermarkPercent`  INT           DEFAULT NULL,
    `disabledReason`       VARCHAR(1024) DEFAULT NULL,
    `createDate`           TIMESTAMP     NOT NULL DEFAULT '2000-01-01 00:00:00',
    `lastOpDate`           TIMESTAMP     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `ukAiHostModelCachePolicyVOHostRoot` (`hostUuid`, `sourceRoot`(255)),
    CONSTRAINT `fkAiHostModelCachePolicyVOHostEO`
        FOREIGN KEY (`hostUuid`) REFERENCES `zstack`.`HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

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
CALL CREATE_INDEX('VmModelMountVO', 'idxVmModelMountVOCacheUuid', 'cacheUuid');
CALL ADD_CONSTRAINT('VmModelMountVO', 'fkVmModelMountVOAiHostModelCacheVO', 'cacheUuid', 'AiHostModelCacheVO', 'uuid', 'SET NULL');
