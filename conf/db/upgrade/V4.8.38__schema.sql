CALL ADD_COLUMN('LicenseHistoryVO', 'quotaType', 'varchar(64)', 1, 'None');
CALL ADD_COLUMN('HostCapacityVO', 'cpuCoreNum', 'int unsigned', 0, '0');

CREATE INDEX idx_schedType_createDate ON `zstack`.`VmSchedHistoryVO` (schedType, createDate);
