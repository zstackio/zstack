CREATE TABLE IF NOT EXISTS `zstack`.`ModelServiceImageVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `modelServiceUuid` varchar(32) NOT NULL,
    `cpuArchitecture` varchar(32) NOT NULL,
    `vmImageUuid` varchar(32) NULL,
    `dockerImage` varchar(255) NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkModelServiceImageVOModelServiceVO` FOREIGN KEY (`modelServiceUuid`) REFERENCES `ModelServiceVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CALL ADD_COLUMN('ModelServiceVO', 'gpuVendors', 'varchar(255)', 1, 'NULL');
CALL ADD_COLUMN('ModelServiceVO', 'cpuArchitectures', 'varchar(255)', 1, 'NULL');

DROP PROCEDURE IF EXISTS update_model_service_cpu_arch;
DELIMITER $$
CREATE PROCEDURE update_model_service_cpu_arch()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE service_uuid VARCHAR(32);
    DECLARE vm_image_uuid VARCHAR(32);
    DECLARE cpu_arch VARCHAR(32);
    DECLARE model_service_image_uuid VARCHAR(32);
    DECLARE img_cursor CURSOR FOR
        SELECT ms.uuid, ms.vmImageUuid, img.architecture
        FROM ModelServiceVO ms
        JOIN ImageVO img ON ms.vmImageUuid = img.uuid
        WHERE ms.vmImageUuid IS NOT NULL;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN img_cursor;

    read_loop: LOOP
        FETCH img_cursor INTO service_uuid, vm_image_uuid, cpu_arch;
        IF done THEN
            LEAVE read_loop;
        END IF;

        UPDATE ModelServiceVO
        SET cpuArchitectures = cpu_arch
        WHERE uuid = service_uuid;

        IF NOT EXISTS (SELECT 1 FROM ModelServiceImageVO WHERE modelServiceUuid = service_uuid AND cpuArchitecture = cpu_arch) THEN
            SET model_service_image_uuid = REPLACE(UUID(),'-','');
            INSERT INTO ModelServiceImageVO (uuid, modelServiceUuid, cpuArchitecture, vmImageUuid, createDate, lastOpDate)
            VALUES (model_service_image_uuid, service_uuid, cpu_arch, vm_image_uuid, NOW(), NOW());
            INSERT INTO ResourceVO (uuid, resourceType, concreteResourceType) values (model_service_image_uuid, 'ModelServiceImageVO', 'org.zstack.ai.entity.ModelServiceImageVO');
        END IF;
    END LOOP;

    CLOSE img_cursor;
END$$
DELIMITER ;

CALL update_model_service_cpu_arch();
DROP PROCEDURE IF EXISTS update_model_service_cpu_arch;

CREATE TABLE IF NOT EXISTS `zstack`.`ContainerBackupStorageVO` (
    `uuid` varchar(32) NOT NULL,
    `endpointUuid` varchar(32) NOT NULL,
    `id` bigint(20) unsigned DEFAULT NULL,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkContainerBackupStorageVOBackupStorageEO` FOREIGN KEY (`uuid`) REFERENCES `BackupStorageEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkContainerBackupStorageVOContainerManagementEndpointVO` FOREIGN KEY (`endpointUuid`) REFERENCES `ContainerManagementEndpointVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;