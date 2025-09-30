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
