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
  AND Json_getKeyValue(`opaque`, 'total_score') IS NOT NULL
  AND Json_getKeyValue(`opaque`, 'total_score') != '';

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

-- PCI virtualization capability metadata

CREATE TABLE IF NOT EXISTS `zstack`.`PciDeviceVirtCapabilityVO` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `pciDeviceUuid` VARCHAR(32)     NOT NULL,
    `capability`    VARCHAR(32)     NOT NULL,
    `createDate`    TIMESTAMP       NOT NULL,
    `lastOpDate`    TIMESTAMP       NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pci_device_virt_capability` (`pciDeviceUuid`, `capability`),
    KEY `idx_pci_device_virt_capability_pci` (`pciDeviceUuid`),
    CONSTRAINT `fk_pci_device_virt_capability_pci`
        FOREIGN KEY (`pciDeviceUuid`) REFERENCES `zstack`.`PciDeviceVO`(`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CALL ADD_COLUMN('PciDeviceVO', 'virtState', 'varchar(32)', 1, NULL);

UPDATE `zstack`.`PciDeviceVO`
SET `virtState` =
    CASE
        WHEN `virtStatus` IN ('SRIOV_VIRTUALIZABLE', 'VFIO_MDEV_VIRTUALIZABLE', 'TENSORFUSION_VIRTUALIZABLE') THEN 'VIRTUALIZABLE'
        WHEN `virtStatus` IN ('SRIOV_VIRTUALIZED', 'VFIO_MDEV_VIRTUALIZED', 'VIRTUALIZED_BYPASS_ZSTACK',
                              'HAMI_VIRTUALIZED', 'TENSORFUSION_VIRTUALIZED') THEN 'VIRTUALIZED'
        WHEN `virtStatus` = 'SRIOV_VIRTUAL' THEN 'VIRTUAL'
        ELSE 'UNVIRTUALIZABLE'
    END
WHERE `virtState` IS NULL;

INSERT IGNORE INTO `zstack`.`PciDeviceVirtCapabilityVO`
    (`pciDeviceUuid`, `capability`, `createDate`, `lastOpDate`)
SELECT `uuid`, 'SRIOV', NOW(), NOW()
FROM `zstack`.`PciDeviceVO`
WHERE `virtStatus` IN ('SRIOV_VIRTUALIZABLE', 'SRIOV_VIRTUALIZED');

INSERT IGNORE INTO `zstack`.`PciDeviceVirtCapabilityVO`
    (`pciDeviceUuid`, `capability`, `createDate`, `lastOpDate`)
SELECT `uuid`, 'VFIO_MDEV', NOW(), NOW()
FROM `zstack`.`PciDeviceVO`
WHERE `virtStatus` IN ('VFIO_MDEV_VIRTUALIZABLE', 'VFIO_MDEV_VIRTUALIZED', 'VIRTUALIZED_BYPASS_ZSTACK');

INSERT IGNORE INTO `zstack`.`PciDeviceVirtCapabilityVO`
    (`pciDeviceUuid`, `capability`, `createDate`, `lastOpDate`)
SELECT `uuid`, 'TENSORFUSION', NOW(), NOW()
FROM `zstack`.`PciDeviceVO`
WHERE `virtStatus` IN ('TENSORFUSION_VIRTUALIZABLE', 'TENSORFUSION_VIRTUALIZED');

INSERT IGNORE INTO `zstack`.`PciDeviceVirtCapabilityVO`
    (`pciDeviceUuid`, `capability`, `createDate`, `lastOpDate`)
SELECT `uuid`, 'HAMI', NOW(), NOW()
FROM `zstack`.`PciDeviceVO`
WHERE `virtStatus` = 'HAMI_VIRTUALIZED';

CALL ADD_COLUMN('PciDeviceVO', 'virtMode', 'varchar(32)', 1, NULL);

UPDATE `zstack`.`PciDeviceVO`
SET `virtMode` =
    CASE
        WHEN `virtStatus` IN ('SRIOV_VIRTUALIZED') THEN 'SRIOV'
        WHEN `virtStatus` = 'SRIOV_VIRTUAL' THEN 'SRIOV'
        WHEN `virtStatus` IN ('VFIO_MDEV_VIRTUALIZED', 'VIRTUALIZED_BYPASS_ZSTACK') THEN 'VFIO_MDEV'
        WHEN `virtStatus` = 'TENSORFUSION_VIRTUALIZED' THEN 'TENSORFUSION'
        WHEN `virtStatus` = 'HAMI_VIRTUALIZED' THEN 'HAMI'
        ELSE `virtMode`
    END
WHERE `virtStatus` IN (
    'SRIOV_VIRTUALIZED', 'SRIOV_VIRTUAL',
    'VFIO_MDEV_VIRTUALIZED', 'VIRTUALIZED_BYPASS_ZSTACK',
    'TENSORFUSION_VIRTUALIZED', 'HAMI_VIRTUALIZED'
);

CALL ADD_COLUMN('GpuDeviceVO', 'mode', 'varchar(32)', 1, NULL);
CALL CREATE_INDEX('GpuDeviceVO', 'idx_gpu_device_mode', 'mode');

UPDATE `zstack`.`GpuDeviceVO` g
INNER JOIN `zstack`.`PciDeviceVO` p ON g.`uuid` = p.`uuid`
SET g.`mode` = CASE
    WHEN p.`virtState` = 'VIRTUALIZED' AND p.`virtMode` = 'TENSORFUSION' THEN 'DGPU'
    WHEN p.`virtState` = 'VIRTUALIZED' AND p.`virtMode` IN ('VFIO_MDEV', 'SRIOV') THEN 'VGPU'
    ELSE 'PCI'
END;

UPDATE `zstack`.`GpuDeviceVO` g
INNER JOIN `zstack`.`PciDeviceVO` p ON g.`uuid` = p.`uuid`
SET g.`allocateStatus` = CASE
    WHEN p.`vmInstanceUuid` IS NOT NULL THEN 'Allocated'
    WHEN p.`virtState` = 'VIRTUALIZED' AND p.`virtMode` IS NOT NULL THEN 'Unallocatable'
    ELSE 'Unallocated'
END;

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
