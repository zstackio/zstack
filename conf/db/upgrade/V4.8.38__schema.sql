CALL ADD_COLUMN('LicenseHistoryVO', 'quotaType', 'varchar(64)', 1, 'None');
CALL ADD_COLUMN('HostCapacityVO', 'cpuCoreNum', 'int unsigned', 0, '0');

CREATE INDEX idx_schedType_createDate ON `zstack`.`VmSchedHistoryVO` (schedType, createDate);

UPDATE VolumeSnapshotVO AS sp, PrimaryStorageVO AS ps
SET sp.primaryStorageInstallPath = REPLACE(sp.primaryStorageInstallPath, '/dev/', 'sharedblock://')
WHERE sp.primaryStorageUuid = ps.uuid
  AND ps.type = 'SharedBlock'
  AND sp.volumeType = 'Memory'
  AND sp.primaryStorageInstallPath LIKE '/dev/%';

DELETE FROM `SystemTagVO`
WHERE `resourceUuid` IN (SELECT uuid FROM HostCapacityVO WHERE cpuSockets != 1)
  AND tag LIKE "cpuProcessorNum::%";
