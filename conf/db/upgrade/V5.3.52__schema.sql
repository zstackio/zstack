CALL ADD_COLUMN('ModelServiceInstanceVO', 'name', 'VARCHAR(255)', 1, NULL);
CALL ADD_COLUMN('ModelServiceInstanceVO', 'namespace', 'VARCHAR(255)', 1, NULL);

-- Delete old vm records for pod and resync will be done after node started
DELETE FROM `ResourceVO` where resourceType = 'VmInstanceVO' and uuid in (SELECT uuid FROM `VmInstanceEO` where hypervisorType = 'Native');
DELETE FROM `VmInstanceEO` where hypervisorType = 'Native';

CREATE TABLE  `zstack`.`PodVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `status` varchar(64) NOT NULL,
     PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CALL ADD_COLUMN('GpuDeviceVO', 'gpuType', 'VARCHAR(255)', 1, NULL);
CALL ADD_COLUMN('GpuDeviceSpecVO', 'gpuType', 'VARCHAR(255)', 1, NULL);

DROP PROCEDURE IF EXISTS update_gpu_type_from_pci;

DELIMITER $$

CREATE PROCEDURE update_gpu_type_from_pci()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE pci_uuid_val VARCHAR(32);
    DECLARE pci_host_uuid_val VARCHAR(32);
    DECLARE pci_description_val VARCHAR(2048);
    DECLARE kvm_host_count INT;

    DECLARE cur CURSOR FOR
        SELECT pd.uuid, pd.hostUuid, pd.description
        FROM PciDeviceVO pd
        INNER JOIN GpuDeviceVO gd ON pd.uuid = gd.uuid;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;

    read_loop: LOOP
        FETCH cur INTO pci_uuid_val, pci_host_uuid_val, pci_description_val;

        IF done THEN
            LEAVE read_loop;
        END IF;

        SELECT COUNT(*) INTO kvm_host_count FROM KVMHostVO WHERE uuid = pci_host_uuid_val;

        IF kvm_host_count > 0 THEN
            UPDATE GpuDeviceVO
            SET gpuType = pci_description_val
            WHERE uuid = pci_uuid_val;
        END IF;

    END LOOP;

    SELECT CURTIME();

    CLOSE cur;
END$$

DELIMITER ;

CALL update_gpu_type_from_pci();

UPDATE ModelCenterVO m
LEFT JOIN L3NetworkEO l ON m.storageNetworkUuid = l.uuid
SET m.storageNetworkUuid = NULL
WHERE m.storageNetworkUuid IS NOT NULL AND l.uuid IS NULL;

UPDATE ModelCenterVO m
LEFT JOIN L3NetworkEO l ON m.serviceNetworkUuid = l.uuid
SET m.serviceNetworkUuid = NULL
WHERE m.serviceNetworkUuid IS NOT NULL AND l.uuid IS NULL;

ALTER TABLE ModelCenterVO
  MODIFY COLUMN storageNetworkUuid VARCHAR(32) NULL,
  MODIFY COLUMN serviceNetworkUuid VARCHAR(32) NULL;

CALL ADD_CONSTRAINT('ModelCenterVO', 'fkModelCenterVOStorageNetworkUuid', 'storageNetworkUuid', 'L3NetworkEO', 'uuid', 'SET NULL');
CALL ADD_CONSTRAINT('ModelCenterVO', 'fkModelCenterVOServiceNetworkUuid', 'serviceNetworkUuid', 'L3NetworkEO', 'uuid', 'SET NULL');
