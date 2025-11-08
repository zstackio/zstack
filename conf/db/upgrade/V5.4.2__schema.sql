ALTER TABLE `zstack`.`MdevDeviceSpecVO` modify column name varchar(128) NOT NULL;
ALTER TABLE `zstack`.`MdevDeviceVO` modify column name varchar(128) NOT NULL;

CALL ADD_COLUMN('ModelVO', 'framework', 'varchar(255)', 1, NULL);
CALL ADD_COLUMN('ModelVO', 'versionSemver', 'varchar(255)', 1, NULL);
CALL ADD_COLUMN('ModelVO', 'isLatestVersion', 'tinyint(1)', 1, '0');
CALL ADD_COLUMN('ModelVO', 'artifactChecksum', 'varchar(255)', 1, NULL);
CALL ADD_COLUMN('ModelVO', 'artifactSizeBytes', 'bigint', 1, '0');
CALL ADD_COLUMN('ModelVO', 'architectureType', 'varchar(255)', 1, NULL);
CALL ADD_COLUMN('ModelVO', 'frameworkVersion', 'varchar(255)', 1, NULL);
CALL ADD_COLUMN('ModelVO', 'requiredAccelerator', 'varchar(255)', 1, NULL);
CALL ADD_COLUMN('ModelVO', 'quantizationType', 'varchar(255)', 1, NULL);

CALL RENAME_TABLE('ModelServiceImageVO', 'ModelServiceTemplateVO');

CALL ADD_COLUMN('ModelServiceTemplateVO', 'pythonVersionSemver', 'varchar(255)', 1, NULL);
CALL ADD_COLUMN('ModelServiceTemplateVO', 'cudaVersion', 'varchar(255)', 1, NULL);
CALL ADD_COLUMN('ModelServiceTemplateVO', 'cannVersion', 'varchar(255)', 1, NULL);
CALL ADD_COLUMN('ModelServiceTemplateVO', 'frameworkVersionSemver', 'varchar(255)', 1, NULL);
CALL ADD_COLUMN('ModelServiceTemplateVO', 'gpuVendor', 'varchar(255)', 1, NULL);

CALL ADD_COLUMN('PodVO', 'namespace', 'varchar(64)', 1, NULL);

CREATE TABLE IF NOT EXISTS `zstack`.`KubernetesServiceVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(64) NOT NULL,
    `description` varchar(255) DEFAULT NULL,
    `namespace` varchar(64) NOT NULL,
    `type` varchar(20) NOT NULL,
    `clusterIp` varchar(64) DEFAULT NULL,
    `externalIp` varchar(64) DEFAULT NULL,
    `ports` text,
    `endpointUuid` varchar(32) NOT NULL,
    `clusterId` INT DEFAULT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CALL ADD_COLUMN('PodVO', 'clusterId', 'INT', 1, NULL);

DELETE FROM `AccountResourceRefVO`
WHERE `concreteResourceType` = 'org.zstack.header.vm.VmInstanceVO'
  AND `resourceUuid` NOT IN (SELECT `uuid` FROM `VmInstanceVO`);

CALL DROP_COLUMN('ModelCenterCapacityVO', 'installationUsedCapacity');
CALL ADD_COLUMN('NativeClusterVO', 'status', 'varchar(32)', 1, NULL);

UPDATE `NativeClusterVO` SET `status` = 'Status_Cluster_Running' WHERE `status` IS NULL;

CREATE TABLE IF NOT EXISTS `zstack`.`ModelServiceGpuVendorSpecRefVO` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `refUuid` bigint UNSIGNED NOT NULL,
    `specUuid` varchar(32) NOT NULL,
    CONSTRAINT `pkModelServiceGpuVendorSpecRef` PRIMARY KEY (`id`),
    CONSTRAINT `ukModelServiceGpuVendorSpecRefRefSpec` UNIQUE (`refUuid`, `specUuid`),
    CONSTRAINT `fkModelServiceGpuVendorSpecRefRefUuid` FOREIGN KEY (`refUuid`)
            REFERENCES `ModelServiceGpuVendorVO`(`id`)
            ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE OR REPLACE VIEW PodGpuStatsVO AS
SELECT
    p.uuid AS podUuid,
    COUNT(g.uuid) AS gpuCount,
    COALESCE(CAST(ROUND(AVG(g.memory)) AS SIGNED), 0) AS avgAllocatedMb,
    COALESCE(CAST(SUM(g.memory) AS SIGNED), 0) AS totalGpuMemMb
FROM PodVO p
    LEFT JOIN PciDeviceVO pci ON pci.vmInstanceUuid = p.uuid
    LEFT JOIN GpuDeviceVO g ON g.uuid = pci.uuid
GROUP BY p.uuid;

CALL ADD_COLUMN('GpuDeviceVO', 'allocateStatus', 'varchar(32)', 1, NULL);

-- Upgrade GpuDeviceVO.allocateStatus based vmInstanceUuid
UPDATE GpuDeviceVO gpuDevice
JOIN PciDeviceVO pciDevice ON gpuDevice.uuid = pciDevice.uuid
SET gpuDevice.allocateStatus =
    CASE
        WHEN pciDevice.vmInstanceUuid IS NOT NULL THEN 'Allocated'
        ELSE 'Unallocated'
    END;

DELIMITER $$
CREATE PROCEDURE update_system_model_service_gpu_vendors()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE service_uuid VARCHAR(32);
    DECLARE model_name VARCHAR(255);
    DECLARE gpu_vendor_name VARCHAR(32);

    -- Cursor to iterate over the models
    DECLARE models_cursor CURSOR FOR
        SELECT name, vendor FROM system_models_to_update;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    -- Temporary table to hold the data
    CREATE TEMPORARY TABLE IF NOT EXISTS system_models_to_update (
        name VARCHAR(255),
        vendor VARCHAR(32)
    );

    -- Populate the temporary table
    INSERT INTO system_models_to_update (name, vendor) VALUES
    ('vLLM-0.7.2', 'NVIDIA'),
    ('vLLM-0.8.5', 'NVIDIA'),
    ('vLLM-0.9.2', 'NVIDIA'),
    ('vLLM-0.11.0', 'NVIDIA'),
    ('SGLang-0.4.9.post1', 'NVIDIA'),
    ('SGLang-0.5.2', 'NVIDIA'),
    ('Transformers-4.48.3', 'NVIDIA'),
    ('Transformers-4.56.2', 'NVIDIA'),
    ('sentence_transformers-3.1.1', 'NVIDIA'),
    ('Diffusers-0.30.0', 'NVIDIA'),
    ('Diffusers-0.35.1', 'NVIDIA'),
    ('MindIE-2.0.RC1-910B', 'HUAWEI'),
    ('MindIE-2.1.RC1-910B', 'HUAWEI'),
    ('MindIE-1.0.0-310P', 'HUAWEI'),
    ('vLLM-Ascend-0.11.0.rc0', 'HUAWEI'),
    ('vLLM-0.5.0-HYGON-Z100L', 'HAIGUANG'),
    ('vLLM-0.8.5-HYGON-K100-AI', 'HAIGUANG'),
    ('vLLM-0.9.2-HYGON-K100-AI', 'HAIGUANG'),
    ('vLLM-0.7.2-HYGON-K100-AI', 'HAIGUANG'),
    ('MinerU2.5-2509', 'NVIDIA');

    OPEN models_cursor;
    update_loop: LOOP
        FETCH models_cursor INTO model_name, gpu_vendor_name;
        IF done THEN
            LEAVE update_loop;
        END IF;

        -- Find the model service uuid, only for system services
        SELECT COUNT(*) INTO @cnt FROM `zstack`.`ModelServiceVO` WHERE name = model_name AND system = 1;
        IF @cnt > 0 THEN
            SELECT uuid INTO service_uuid FROM `zstack`.`ModelServiceVO` WHERE name = model_name AND system = 1 LIMIT 1;
        ELSE
            SET service_uuid = NULL;
        END IF;

        -- If the model service exists, ensure the GPU vendor is associated
        IF service_uuid IS NOT NULL THEN
            SELECT CONCAT('INFO: Processing model=', model_name, ', service_uuid=', service_uuid, ', desired_vendor=', gpu_vendor_name) AS msg;
            -- delete different gpu vendors
            DELETE FROM `zstack`.`ModelServiceGpuVendorVO`
                WHERE modelServiceUuid = service_uuid
                AND gpuVendor <> gpu_vendor_name;
            SELECT CONCAT('INFO: Deleted different GPU vendors for service_uuid=', service_uuid, ', deleted_rows=', ROW_COUNT()) AS msg;
            IF NOT EXISTS (SELECT 1 FROM `zstack`.`ModelServiceGpuVendorVO` WHERE modelServiceUuid = service_uuid AND gpuVendor = gpu_vendor_name) THEN
                    INSERT INTO `zstack`.`ModelServiceGpuVendorVO` (modelServiceUuid, gpuVendor, createDate, lastOpDate)
                    VALUES (service_uuid, gpu_vendor_name, NOW(), NOW());
                    SELECT CONCAT('INFO: Inserted GPU vendor=', gpu_vendor_name, ' for service_uuid=', service_uuid, ', inserted_rows=', ROW_COUNT()) AS msg;
            ELSE
                SELECT CONCAT('INFO: GPU vendor=', gpu_vendor_name, ' already exists for service_uuid=', service_uuid) AS msg;
            END IF;
        ELSE
            SELECT CONCAT('WARN: Service not found for model=', model_name) AS msg;
        END IF;

        SET service_uuid = NULL;

    END LOOP;
    CLOSE models_cursor;

    -- Clean up
    DROP TEMPORARY TABLE system_models_to_update;

END $$
DELIMITER ;

CALL update_system_model_service_gpu_vendors();
DROP PROCEDURE IF EXISTS update_system_model_service_gpu_vendors;

-- Delete unexpected ModelServiceTemplateVO entries of MindIE-1.0.0-310P
DELETE FROM `zstack`.`ModelServiceTemplateVO`
WHERE `modelServiceUuid` = 'fe4ed042ac074c55ba1e76921b175ba5' and `cpuArchitecture` = 'x86_64';

DELETE FROM `zstack`.`ModelServiceCpuArchitectureVO`
WHERE `modelServiceUuid` = 'fe4ed042ac074c55ba1e76921b175ba5' and `architecture` = 'x86_64';
