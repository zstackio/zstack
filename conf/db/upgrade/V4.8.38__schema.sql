UPDATE VolumeSnapshotVO AS sp, PrimaryStorageVO AS ps
SET sp.primaryStorageInstallPath = REPLACE(sp.primaryStorageInstallPath, '/dev/', 'sharedblock://')
WHERE sp.primaryStorageUuid = ps.uuid
  AND ps.type = 'SharedBlock'
  AND sp.volumeType = 'Memory'
  AND sp.primaryStorageInstallPath LIKE '/dev/%';

DELETE FROM `SystemTagVO`
WHERE `resourceUuid` IN (SELECT uuid FROM HostCapacityVO WHERE cpuSockets != 1)
  AND tag LIKE "cpuProcessorNum::%";

ALTER TABLE SecurityGroupRuleVO MODIFY COLUMN `dstPortRange` varchar(1024) DEFAULT NULL;
ALTER TABLE SecurityGroupRuleVO MODIFY COLUMN `srcPortRange` varchar(1024) DEFAULT NULL;
