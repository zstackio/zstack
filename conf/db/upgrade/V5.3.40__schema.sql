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

DROP PROCEDURE IF EXISTS migrate_model_service_image_data;
DELIMITER $$
CREATE PROCEDURE migrate_model_service_image_data()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE service_uuid VARCHAR(32);
    DECLARE vm_image_uuid VARCHAR(32);
    DECLARE docker_image VARCHAR(255);
    DECLARE model_service_image_uuid VARCHAR(32);
    DECLARE cpu_arch VARCHAR(32);

    DECLARE vm_cursor CURSOR FOR
        SELECT ms.uuid, ms.vmImageUuid
        FROM ModelServiceVO ms
        WHERE ms.vmImageUuid IS NOT NULL;

    DECLARE docker_cursor CURSOR FOR
        SELECT ms.uuid, ms.dockerImage
        FROM ModelServiceVO ms
        WHERE ms.dockerImage IS NOT NULL;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN vm_cursor;
    vm_read_loop: LOOP
        FETCH vm_cursor INTO service_uuid, vm_image_uuid;
        IF done THEN
            SET done = FALSE;
            LEAVE vm_read_loop;
        END IF;

        SELECT IFNULL(img.architecture, 'x86_64') INTO cpu_arch
        FROM ImageVO img
        WHERE img.uuid = vm_image_uuid
        LIMIT 1;

        IF NOT EXISTS (SELECT 1 FROM ModelServiceImageVO WHERE modelServiceUuid = service_uuid AND vmImageUuid = vm_image_uuid) THEN
            SET model_service_image_uuid = REPLACE(UUID(),'-','');
            INSERT INTO ModelServiceImageVO (uuid, modelServiceUuid, cpuArchitecture, vmImageUuid, createDate, lastOpDate)
            VALUES (model_service_image_uuid, service_uuid, cpu_arch, vm_image_uuid, NOW(), NOW());
            INSERT INTO ResourceVO (uuid, resourceType, concreteResourceType)
            VALUES (model_service_image_uuid, 'ModelServiceImageVO', 'org.zstack.ai.entity.ModelServiceImageVO');
        END IF;
    END LOOP;
    CLOSE vm_cursor;

    OPEN docker_cursor;
    docker_read_loop: LOOP
        FETCH docker_cursor INTO service_uuid, docker_image;
        IF done THEN
            LEAVE docker_read_loop;
        END IF;

        SET cpu_arch = 'x86_64';

        IF NOT EXISTS (SELECT 1 FROM ModelServiceImageVO WHERE modelServiceUuid = service_uuid AND dockerImage = docker_image) THEN
            SET model_service_image_uuid = REPLACE(UUID(),'-','');
            INSERT INTO ModelServiceImageVO (uuid, modelServiceUuid, cpuArchitecture, dockerImage, createDate, lastOpDate)
            VALUES (model_service_image_uuid, service_uuid, cpu_arch, docker_image, NOW(), NOW());
            INSERT INTO ResourceVO (uuid, resourceType, concreteResourceType)
            VALUES (model_service_image_uuid, 'ModelServiceImageVO', 'org.zstack.ai.entity.ModelServiceImageVO');
        END IF;
    END LOOP;
    CLOSE docker_cursor;
END$$
DELIMITER ;

CALL migrate_model_service_image_data();
DROP PROCEDURE IF EXISTS migrate_model_service_image_data;

CALL DROP_COLUMN('ModelServiceVO', 'vmImageUuid');
CALL DROP_COLUMN('ModelServiceVO', 'dockerImage');

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
        SELECT ms.uuid, msi.vmImageUuid, img.architecture
        FROM ModelServiceVO ms
        JOIN ModelServiceImageVO msi ON ms.uuid = msi.modelServiceUuid
        JOIN ImageVO img ON msi.vmImageUuid = img.uuid
        WHERE msi.vmImageUuid IS NOT NULL;
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

CREATE TABLE IF NOT EXISTS `zstack`.`ModelServiceCpuArchitectureVO` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `modelServiceUuid` varchar(32) NOT NULL,
    `architecture` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`id`),
    CONSTRAINT `fkModelServiceCpuArchitectureVOModelServiceVO` FOREIGN KEY (`modelServiceUuid`) REFERENCES `ModelServiceVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ModelServiceGpuVendorVO` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `modelServiceUuid` varchar(32) NOT NULL,
    `gpuVendor` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`id`),
    CONSTRAINT `fkModelServiceGpuVendorVOModelServiceVO` FOREIGN KEY (`modelServiceUuid`) REFERENCES `ModelServiceVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ContainerImageVO` (
    `uuid` varchar(32) NOT NULL,
    `registryUrl` varchar(255) DEFAULT NULL,
    `endpointUuid` varchar(32) NOT NULL,
    `imageTag` varchar(64) DEFAULT NULL,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkContainerImageVOImageEO` FOREIGN KEY (`uuid`) REFERENCES `ImageEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkContainerImageVOContainerManagementEndpointVO` FOREIGN KEY (`endpointUuid`) REFERENCES `ContainerManagementEndpointVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;