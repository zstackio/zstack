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
