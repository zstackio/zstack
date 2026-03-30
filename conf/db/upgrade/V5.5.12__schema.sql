-- ZSTAC-75319: Add normalizedModelName column for GPU spec dedup
CALL ADD_COLUMN('GpuDeviceSpecVO', 'normalizedModelName', 'VARCHAR(255)', 1, NULL);
CALL CREATE_INDEX('GpuDeviceSpecVO', 'idx_gpu_spec_normalized_model', 'normalizedModelName');

-- Add totalScore and endTime columns to ModelEvaluationTaskVO for ZQL sorting support
-- Previously these values were only stored inside the opaque JSON TEXT field,
-- making them invisible to ZQL ORDER BY queries.
CALL ADD_COLUMN('ModelEvaluationTaskVO', 'totalScore', 'DOUBLE', 1, NULL);
CALL ADD_COLUMN('ModelEvaluationTaskVO', 'endTime', 'DATETIME', 1, NULL);

-- Add indexes to support efficient sorting
CALL CREATE_INDEX('ModelEvaluationTaskVO', 'idx_ModelEvaluationTaskVO_totalScore', 'totalScore');
CALL CREATE_INDEX('ModelEvaluationTaskVO', 'idx_ModelEvaluationTaskVO_endTime', 'endTime');

-- Backfill totalScore from opaque JSON for existing completed tasks
-- Uses Json_getKeyValue defined in beforeMigrate.sql for MySQL 5.5+ compatibility
UPDATE `zstack`.`ModelEvaluationTaskVO`
SET `totalScore` = CAST(Json_getKeyValue(`opaque`, 'total_score') AS DECIMAL(20,6))
WHERE `opaque` IS NOT NULL
  AND `totalScore` IS NULL
  AND Json_getKeyValue(`opaque`, 'total_score') IS NOT NULL;

-- Backfill endTime from opaque JSON for existing completed/failed tasks
-- end_time format from Python agent: "MMM dd, yyyy hh:mm:ss a" (e.g. "Jan 01, 2025 10:30:00 AM")
UPDATE `zstack`.`ModelEvaluationTaskVO`
SET `endTime` = STR_TO_DATE(
    Json_getKeyValue(`opaque`, 'end_time'),
    '%b %d, %Y %h:%i:%s %p'
)
WHERE `opaque` IS NOT NULL
  AND `endTime` IS NULL
  AND Json_getKeyValue(`opaque`, 'end_time') IS NOT NULL
  AND Json_getKeyValue(`opaque`, 'end_time') != '';

-- dGPU (TensorFusion) support tables

CREATE TABLE IF NOT EXISTS `zstack`.`DGpuProfileVO` (
    `uuid`        VARCHAR(32)      NOT NULL,
    `gpuSpecUuid` VARCHAR(32)      NOT NULL,
    `memorySize`  BIGINT UNSIGNED  NOT NULL,
    `shmemSize`   BIGINT UNSIGNED  NOT NULL DEFAULT 268435456,
    `createDate`  TIMESTAMP        NOT NULL,
    `lastOpDate`  TIMESTAMP        NOT NULL,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uk_dgpu_profile` (`gpuSpecUuid`, `memorySize`),
    CONSTRAINT `fk_dgpu_profile_spec`
        FOREIGN KEY (`gpuSpecUuid`) REFERENCES `zstack`.`GpuDeviceSpecVO`(`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`DGpuDeviceVO` (
    `uuid`             VARCHAR(32)      NOT NULL,
    `name`             VARCHAR(255)     NOT NULL,
    `parentGpuUuid`    VARCHAR(32)      NOT NULL,
    `gpuSpecUuid`      VARCHAR(32)      NOT NULL,
    `hostUuid`         VARCHAR(32)      NOT NULL,
    `vmInstanceUuid`   VARCHAR(32)      DEFAULT NULL,
    `allocatedMemory`  BIGINT UNSIGNED  NOT NULL,
    `shmemSize`        BIGINT UNSIGNED  NOT NULL DEFAULT 268435456,
    `smPercentLimit`   INT              NOT NULL DEFAULT 0,
    `protocol`         VARCHAR(16)      NOT NULL DEFAULT 'shmem',
    `status`           VARCHAR(32)      NOT NULL,
    `vendorId`         VARCHAR(64)      DEFAULT NULL,
    `vendor`           VARCHAR(255)     DEFAULT NULL,
    `createDate`       TIMESTAMP        NOT NULL,
    `lastOpDate`       TIMESTAMP        NOT NULL,
    PRIMARY KEY (`uuid`),
    INDEX `idx_dgpu_device_parent` (`parentGpuUuid`),
    INDEX `idx_dgpu_device_spec`   (`gpuSpecUuid`),
    INDEX `idx_dgpu_device_host`   (`hostUuid`),
    INDEX `idx_dgpu_device_vm`     (`vmInstanceUuid`),
    CONSTRAINT `fk_dgpu_device_parent`
        FOREIGN KEY (`parentGpuUuid`) REFERENCES `zstack`.`PciDeviceVO`(`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fk_dgpu_device_spec`
        FOREIGN KEY (`gpuSpecUuid`) REFERENCES `zstack`.`GpuDeviceSpecVO`(`uuid`) ON DELETE RESTRICT,
    CONSTRAINT `fk_dgpu_device_host`
        FOREIGN KEY (`hostUuid`) REFERENCES `zstack`.`HostEO`(`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fk_dgpu_device_vm`
        FOREIGN KEY (`vmInstanceUuid`) REFERENCES `zstack`.`VmInstanceEO`(`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`VmInstanceDGpuStrategyVO` (
    `id`               BIGINT          NOT NULL AUTO_INCREMENT,
    `vmInstanceUuid`   VARCHAR(32)     NOT NULL,
    `gpuSpecUuid`      VARCHAR(32)     NOT NULL,
    `memorySize`       BIGINT UNSIGNED NOT NULL,
    `shmemSize`        BIGINT UNSIGNED NOT NULL DEFAULT 268435456,
    `gpuDeviceUuid`    VARCHAR(32)     DEFAULT NULL,
    `chooser`          VARCHAR(16)     NOT NULL,
    `autoDetachOnStop` TINYINT(1)      NOT NULL DEFAULT 1,
    `createDate`       TIMESTAMP       NOT NULL,
    `lastOpDate`       TIMESTAMP       NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_vm_dgpu_strategy`       (`vmInstanceUuid`),
    INDEX      `idx_vm_dgpu_strategy_spec` (`gpuSpecUuid`),
    CONSTRAINT `fk_vm_dgpu_strategy_vm`
        FOREIGN KEY (`vmInstanceUuid`) REFERENCES `zstack`.`VmInstanceEO`(`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fk_vm_dgpu_strategy_spec`
        FOREIGN KEY (`gpuSpecUuid`) REFERENCES `zstack`.`GpuDeviceSpecVO`(`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fk_vm_dgpu_strategy_device`
        FOREIGN KEY (`gpuDeviceUuid`) REFERENCES `zstack`.`PciDeviceVO`(`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ZSTAC-83157: VM model mount feature
CREATE TABLE IF NOT EXISTS `zstack`.`VmModelMountVO` (
    `uuid`            VARCHAR(32)   NOT NULL,
    `vmInstanceUuid`  VARCHAR(32)   NOT NULL,
    `modelUuid`       VARCHAR(32)   NOT NULL,
    `modelName`       VARCHAR(256)  DEFAULT NULL,
    `mountPath`       VARCHAR(512)  NOT NULL,
    `sourcePath`      VARCHAR(1024) NOT NULL,
    `status`          VARCHAR(32)   NOT NULL,
    `hostUuid`        VARCHAR(32)   DEFAULT NULL,
    `accountUuid`     VARCHAR(32)   DEFAULT NULL,
    `createDate`      TIMESTAMP     NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate`      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uk_vm_mountpath` (`vmInstanceUuid`, `mountPath`(255)),
    UNIQUE KEY `uk_vm_model` (`vmInstanceUuid`, `modelUuid`),
    CONSTRAINT `fk_vm_model_mount_vm`
        FOREIGN KEY (`vmInstanceUuid`) REFERENCES `zstack`.`VmInstanceEO`(`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fk_vm_model_mount_model`
        FOREIGN KEY (`modelUuid`) REFERENCES `zstack`.`ModelVO`(`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
