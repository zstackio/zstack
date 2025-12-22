-- Fix UsedIpVO gateway and netmask from empty strings
DROP PROCEDURE IF EXISTS fixUsedIpGatewayAndNetmask;

DELIMITER $$

CREATE PROCEDURE fixUsedIpGatewayAndNetmask()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_usedIpUuid VARCHAR(32);
    DECLARE v_ipRangeUuid VARCHAR(32);
    DECLARE v_gateway VARCHAR(64);
    DECLARE v_netmask VARCHAR(64);
    
    -- Cursor for UsedIpVO records with empty gateway or netmask
    DECLARE cur CURSOR FOR 
        SELECT uuid, ipRangeUuid 
        FROM UsedIpVO 
        WHERE (gateway = '' OR netmask = '' OR gateway IS NULL OR netmask IS NULL)
          AND ipRangeUuid IS NOT NULL;
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    OPEN cur;
    
    read_loop: LOOP
        FETCH cur INTO v_usedIpUuid, v_ipRangeUuid;
        
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        -- Get gateway and netmask from IpRangeVO
        SELECT gateway, netmask 
        INTO v_gateway, v_netmask
        FROM IpRangeVO 
        WHERE uuid = v_ipRangeUuid;
        
        -- Update UsedIpVO with correct gateway and netmask
        IF v_gateway IS NOT NULL AND v_netmask IS NOT NULL THEN
            UPDATE UsedIpVO 
            SET gateway = v_gateway, netmask = v_netmask
            WHERE uuid = v_usedIpUuid;
        END IF;
        
    END LOOP;
    
    CLOSE cur;
    
END$$

DELIMITER ;

-- Execute the procedure
CALL fixUsedIpGatewayAndNetmask();

-- Drop the procedure after use
DROP PROCEDURE IF EXISTS fixUsedIpGatewayAndNetmask;


-- Update ModelServiceVO framework values
DROP PROCEDURE IF EXISTS updateModelServiceFramework;

DELIMITER $$

CREATE PROCEDURE updateModelServiceFramework()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;

    -- Check if ModelServiceVO table and framework column exist
    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ModelServiceVO'
      AND COLUMN_NAME = 'framework';

    IF v_table_exists > 0 THEN
        -- Update llama.cpp to LlamaCpp
        UPDATE ModelServiceVO
        SET framework = 'LlamaCpp'
        WHERE framework = 'llama.cpp';

        -- Update sentence_transformers to SentenceTransformers
        UPDATE ModelServiceVO
        SET framework = 'SentenceTransformers'
        WHERE framework = 'sentence_transformers';
    END IF;
END$$

DELIMITER ;

-- Execute the procedure
CALL updateModelServiceFramework();

-- Drop the procedure after use
DROP PROCEDURE IF EXISTS updateModelServiceFramework;

UPDATE ModelVO SET architectureType = 'xtts-v2' WHERE uuid = '39280569b4e0490fb581f6ab98e76400';
UPDATE ModelVO SET architectureType = 'sdxl-turbo' WHERE uuid = '6a720c01935f4f9ea09f81bde722ee42';

CALL ADD_COLUMN('ModelServiceVO', 'containerArgs', 'TEXT', 1, NULL);
CALL ADD_COLUMN('ModelServiceVO', 'containerCommand', 'TEXT', 1, NULL);

-- Upgrade gpuVendor field in ModelServiceGpuVendorVO
Update ModelServiceGpuVendorVO set gpuVendor = 'Huawei' where gpuVendor = 'HUAWEI';
Update ModelServiceGpuVendorVO set gpuVendor = 'Haiguang' where gpuVendor = 'HAIGUANG';
Update ModelServiceGpuVendorVO set gpuVendor = 'TianShu' where gpuVendor = 'TIANSHU';
Update ModelServiceGpuVendorVO set gpuVendor = 'Intel' where gpuVendor = 'INTEL';

CALL ADD_COLUMN('GpuDeviceVO', 'isolated', 'TINYINT(1)', 1, NULL);
CALL ADD_COLUMN('GpuDeviceSpecVO', 'isolated', 'TINYINT(1)', 1, NULL);

DELIMITER $$

DROP PROCEDURE IF EXISTS fix_missing_architecture_records$$

CREATE PROCEDURE fix_missing_architecture_records()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_model_service_uuid VARCHAR(32);
    DECLARE v_architecture VARCHAR(32);
    DECLARE v_exists INT;

    -- Cursor to find all model services that have templates but missing architecture records
    DECLARE service_cursor CURSOR FOR
        SELECT DISTINCT mst.modelServiceUuid, mst.cpuArchitecture
        FROM ModelServiceTemplateVO mst
        WHERE NOT EXISTS (
            SELECT 1
            FROM ModelServiceCpuArchitectureVO msca
            WHERE msca.modelServiceUuid = mst.modelServiceUuid
            AND msca.architecture = mst.cpuArchitecture
        )
        ORDER BY mst.modelServiceUuid, mst.cpuArchitecture;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    -- Start transaction
    START TRANSACTION;

    -- Create a temporary table to store statistics
    CREATE TEMPORARY TABLE IF NOT EXISTS fix_stats (
            total_services INT DEFAULT 0,
            total_records_added INT DEFAULT 0
        );

    INSERT INTO fix_stats VALUES (0, 0);

    -- Open cursor and process each missing architecture record
    OPEN service_cursor;

    read_loop: LOOP
        FETCH service_cursor INTO v_model_service_uuid, v_architecture;

        IF done THEN
            LEAVE read_loop;
        END IF;

        -- Double check if the record doesn't exist (to avoid duplicates)
        SELECT COUNT(*) INTO v_exists
        FROM ModelServiceCpuArchitectureVO
        WHERE modelServiceUuid = v_model_service_uuid
          AND architecture = v_architecture;

        IF v_exists = 0 THEN
            -- Insert the missing architecture record
            INSERT INTO ModelServiceCpuArchitectureVO (modelServiceUuid, architecture, lastOpDate, createDate)
            VALUES (
                v_model_service_uuid,
                v_architecture,
                CURRENT_TIMESTAMP(3),
                CURRENT_TIMESTAMP(3)
            );

            -- Update statistics
            UPDATE fix_stats SET total_records_added = total_records_added + 1;

            -- Log the fix
            SELECT CONCAT('Fixed: Added architecture record [', v_architecture, '] for model service [', v_model_service_uuid, ']') AS log_message;
        END IF;

    END LOOP;

    CLOSE service_cursor;

    -- Count total affected services
    UPDATE fix_stats SET total_services = (
        SELECT COUNT(DISTINCT modelServiceUuid)
        FROM ModelServiceCpuArchitectureVO
        WHERE createDate >= (SELECT MIN(createDate) FROM (SELECT createDate FROM ModelServiceCpuArchitectureVO ORDER BY createDate DESC LIMIT 1000) AS recent)
    );

    -- Display summary
    SELECT
        total_records_added AS 'Total Architecture Records Added',
        (SELECT COUNT(DISTINCT modelServiceUuid) FROM ModelServiceTemplateVO) AS 'Total Model Services with Templates',
        (SELECT COUNT(DISTINCT modelServiceUuid) FROM ModelServiceCpuArchitectureVO) AS 'Total Model Services with Architecture Records'
    FROM fix_stats;

    -- Cleanup
    DROP TEMPORARY TABLE IF EXISTS fix_stats;

        -- Commit transaction
    COMMIT;

    SELECT 'Fix completed successfully!' AS status;
END$$

DELIMITER ;

CALL fix_missing_architecture_records();

-- Add new gpu constraint fields
CALL ADD_COLUMN('ModelVO', 'recommendedGpuNum', 'VARCHAR(256)', 1, NULL);
CALL ADD_COLUMN('ModelVO', 'gpuConstraintDescription', 'VARCHAR(512)', 1, NULL);

DELIMITER $$

CREATE PROCEDURE update_system_model_service_templates()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE model_name VARCHAR(255);
    DECLARE gpu_vendor_name VARCHAR(32);
    DECLARE py_version VARCHAR(255);
    DECLARE cuda_version VARCHAR(255);
    DECLARE cann_version VARCHAR(255);
    DECLARE fw_version VARCHAR(255);
    DECLARE service_uuid VARCHAR(32);

    DECLARE templates_cursor CURSOR FOR
        SELECT name, vendor, pythonVersion, cudaVersion, cannVersion, frameworkVersion FROM system_models_templates_to_update;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    CREATE TEMPORARY TABLE IF NOT EXISTS system_models_templates_to_update (
        name VARCHAR(255),
        vendor VARCHAR(32),
        pythonVersion VARCHAR(255),
        cudaVersion VARCHAR(255),
        cannVersion VARCHAR(255),
        frameworkVersion VARCHAR(255)
    );

    INSERT INTO system_models_templates_to_update (name, vendor, pythonVersion, cudaVersion, cannVersion, frameworkVersion) VALUES
     ('vLLM-0.7.2', 'NVIDIA', '3.10', '12.5', NULL, '0.7.2'),
     ('vLLM-0.8.5', 'NVIDIA', '3.10', '12.5', NULL, '0.8.5'),
     ('vLLM-0.9.2', 'NVIDIA', '3.10', '12.5', NULL, '0.9.2'),
     ('vLLM-0.11.0', 'NVIDIA', '3.10', '12.5', NULL, '0.11.0'),
     ('SGLang-0.4.9.post1', 'NVIDIA', '3.10', '12.5', NULL, '0.4.9.post1'),
     ('SGLang-0.5.2', 'NVIDIA', '3.10', '12.5', NULL, '0.5.2'),
     ('SGLang-0.5.4', 'NVIDIA', '3.10', '12.5', NULL, '0.5.4'),
     ('Transformers-4.48.3', 'NVIDIA', '3.10', '12.5', NULL, '4.48.3'),
     ('Transformers-4.57.1', 'NVIDIA', '3.10', '12.5', NULL, '4.57.1'),
     ('Transformers-4.56.2', 'NVIDIA', '3.10', '12.5', NULL, '4.56.2'),
     ('sentence_transformers-3.1.1', 'NVIDIA', '3.10', '12.5', NULL, '3.1.1'),
     ('Diffusers-0.30.0', 'NVIDIA', '3.10', '12.5', NULL, '0.30.0'),
     ('Diffusers-0.35.1', 'NVIDIA', '3.10', '12.5', NULL, '0.35.1'),
     ('MindIE-2.0.RC1-910B', 'Huawei', '3.9', NULL, '8.0', '2.0.RC1'),
     ('MindIE-2.1.RC1-910B', 'Huawei', '3.9', NULL, '8.0', '2.1.RC1'),
     ('MindIE-2.1.RC2-910B', 'Huawei', '3.9', NULL, '8.0', '2.1.RC2'),
     ('MindIE-1.0.0-310P', 'Huawei', '3.9', NULL, '7.0', '1.0.0'),
     ('vLLM-Ascend-0.11.0.rc0', 'Huawei', '3.9', NULL, '8.0', '0.11.0.rc0'),
     ('vLLM-0.5.0-HYGON-Z100L', 'Haiguang', '3.10', NULL, NULL, '0.5.0'),
     ('vLLM-0.8.5-HYGON-K100-AI', 'Haiguang', '3.10', NULL, NULL, '0.8.5'),
     ('vLLM-0.9.2-HYGON-K100-AI', 'Haiguang', '3.10', NULL, NULL, '0.9.2'),
     ('vLLM-0.7.2-HYGON-K100-AI', 'Haiguang', '3.10', NULL, NULL, '0.7.2'),
     ('MinerU2.5-2509', 'NVIDIA', '3.10', '12.5', NULL, '2.5');

    OPEN templates_cursor;
    update_template_loop: LOOP
        FETCH templates_cursor INTO model_name, gpu_vendor_name, py_version, cuda_version, cann_version, fw_version;
        IF done THEN
            LEAVE update_template_loop;
        END IF;

        SELECT COUNT(*) INTO @cnt FROM `zstack`.`ModelServiceVO` WHERE name = model_name AND system = 1;
        IF @cnt > 0 THEN
            SELECT uuid INTO service_uuid FROM `zstack`.`ModelServiceVO` WHERE name = model_name AND system = 1 LIMIT 1;
        ELSE
            SET service_uuid = NULL;
        END IF;

        IF service_uuid IS NOT NULL THEN
            SELECT COUNT(*) INTO @tmpl_cnt FROM `zstack`.`ModelServiceTemplateVO` WHERE `modelServiceUuid` = service_uuid;
            IF @tmpl_cnt > 0 THEN
                UPDATE `zstack`.`ModelServiceTemplateVO` SET
                    `pythonVersionSemver` = py_version,
                    `cudaVersion` = cuda_version,
                    `cannVersion` = cann_version,
                    `frameworkVersionSemver` = fw_version,
                    `gpuVendor` = gpu_vendor_name
                WHERE `modelServiceUuid` = service_uuid;
                SELECT CONCAT('INFO: Updated ModelServiceTemplateVO for service_uuid=', service_uuid, ', updated_rows=', ROW_COUNT()) AS msg;
            ELSE
                INSERT INTO `zstack`.`ModelServiceTemplateVO` (`uuid`, `modelServiceUuid`, `cpuArchitecture`, `pythonVersionSemver`, `cudaVersion`, `cannVersion`, `frameworkVersionSemver`, `gpuVendor`, `createDate`, `lastOpDate`)
                    VALUES (REPLACE(UUID(),'-',''), service_uuid, 'x86_64', py_version, cuda_version, cann_version, fw_version, gpu_vendor_name, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
                SELECT CONCAT('INFO: Inserted ModelServiceTemplateVO for service_uuid=', service_uuid, ', inserted_rows=', ROW_COUNT()) AS msg;
            END IF;

            -- Ensure ModelServiceGpuVendorVO record exists with the corresponding gpuVendor for this modelServiceUuid
            SELECT COUNT(*) INTO @gv_cnt FROM `zstack`.`ModelServiceGpuVendorVO`
                WHERE `modelServiceUuid` = service_uuid AND `gpuVendor` = gpu_vendor_name;
            IF @gv_cnt = 0 THEN
                INSERT INTO `zstack`.`ModelServiceGpuVendorVO` (`modelServiceUuid`, `gpuVendor`)
                VALUES (service_uuid, gpu_vendor_name);
                SELECT CONCAT('INFO: Inserted ModelServiceGpuVendorVO for service_uuid=', service_uuid, ', gpuVendor=', gpu_vendor_name) AS msg;
            ELSE
                SELECT CONCAT('INFO: ModelServiceGpuVendorVO exists for service_uuid=', service_uuid, ', gpuVendor=', gpu_vendor_name) AS msg;
            END IF;
        ELSE
            SELECT CONCAT('WARN: Service not found for model=', model_name, ', skipping template and gpuVendor update.') AS msg;
        END IF;

        SET service_uuid = NULL;
    END LOOP;
    CLOSE templates_cursor;

    DROP TEMPORARY TABLE IF EXISTS system_models_templates_to_update;
END $$
DELIMITER ;

CALL update_system_model_service_templates();
DROP PROCEDURE IF EXISTS update_system_model_service_templates;

Update ModelServiceTemplateVO set gpuVendor = 'Huawei' where gpuVendor = 'HUAWEI';
Update ModelServiceTemplateVO set gpuVendor = 'Haiguang' where gpuVendor = 'HAIGUANG';
Update ModelServiceTemplateVO set gpuVendor = 'TianShu' where gpuVendor = 'TIANSHU';
Update ModelServiceTemplateVO set gpuVendor = 'Intel' where gpuVendor = 'INTEL';

CALL ADD_COLUMN('GpuDeviceVO', 'gpuStatus', 'varchar(16)', 1, NULL);

UPDATE `zstack`.`GpuDeviceVO` SET `gpuStatus`='NOMINAL' WHERE `gpuStatus` IS NULL;

-- Add supportMetrics column to ModelServiceInstanceGroupVO
DROP PROCEDURE IF EXISTS addModelServiceInstanceGroupSupportMetricsColumn;
DELIMITER $$
CREATE PROCEDURE addModelServiceInstanceGroupSupportMetricsColumn()
BEGIN
    DECLARE columnExists BOOLEAN DEFAULT FALSE;

    SELECT COUNT(*) INTO columnExists
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_NAME = 'ModelServiceInstanceGroupVO'
    AND COLUMN_NAME = 'supportMetrics'
    AND TABLE_SCHEMA = 'zstack';

    IF columnExists = FALSE THEN
        ALTER TABLE `zstack`.`ModelServiceInstanceGroupVO` ADD COLUMN `supportMetrics` TEXT DEFAULT NULL;
    END IF;
END $$
DELIMITER ;

CALL addModelServiceInstanceGroupSupportMetricsColumn();
DROP PROCEDURE IF EXISTS addModelServiceInstanceGroupSupportMetricsColumn;
