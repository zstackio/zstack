CALL ADD_COLUMN('ModelServiceInstanceVO', 'name', 'VARCHAR(255)', 1, NULL);
CALL ADD_COLUMN('ModelServiceInstanceVO', 'namespace', 'VARCHAR(255)', 1, NULL);

-- Delete old vm records for pod and resync will be done after node started
DELETE FROM `VmInstanceEO` where hypervisorType = 'Native';

CREATE TABLE  `zstack`.`PodVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `status` varchar(64) NOT NULL,
     PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CALL ADD_COLUMN('GpuDeviceVO', 'gpuType', 'VARCHAR(255)', 1, NULL);
CALL ADD_COLUMN('GpuDeviceSpecVO', 'gpuType', 'VARCHAR(255)', 1, NULL);
