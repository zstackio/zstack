-- V6.0.0.4: Fix GpuDeviceSpecVO resourceType in account/sharing/project ref tables
-- ZSTAC-69623: GpuDeviceSpecVO was stored as "PciDeviceSpecVO" due to @BaseResource inheritance.
-- Update existing records so GPU specs have their own resourceType for proper tenant isolation.

DELIMITER $$
DROP PROCEDURE IF EXISTS fix_gpu_spec_resource_type$$
CREATE PROCEDURE fix_gpu_spec_resource_type()
BEGIN
    -- Fix AccountResourceRefVO: GPU specs should have resourceType = 'GpuDeviceSpecVO'
    UPDATE AccountResourceRefVO
    SET resourceType = 'GpuDeviceSpecVO'
    WHERE resourceType = 'PciDeviceSpecVO'
      AND resourceUuid IN (SELECT uuid FROM GpuDeviceSpecVO);

    -- Fix SharedResourceVO: same correction for shared GPU specs
    IF EXISTS (SELECT 1 FROM information_schema.TABLES
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'SharedResourceVO') THEN
        UPDATE SharedResourceVO
        SET resourceType = 'GpuDeviceSpecVO'
        WHERE resourceType = 'PciDeviceSpecVO'
          AND resourceUuid IN (SELECT uuid FROM GpuDeviceSpecVO);
    END IF;

    -- Fix IAM2ProjectResourceRefVO: same correction for project-bound GPU specs
    IF EXISTS (SELECT 1 FROM information_schema.TABLES
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'IAM2ProjectResourceRefVO') THEN
        UPDATE IAM2ProjectResourceRefVO
        SET resourceType = 'GpuDeviceSpecVO'
        WHERE resourceType = 'PciDeviceSpecVO'
          AND resourceUuid IN (SELECT uuid FROM GpuDeviceSpecVO);
    END IF;
END$$
DELIMITER ;

CALL fix_gpu_spec_resource_type();
DROP PROCEDURE IF EXISTS fix_gpu_spec_resource_type;
