
CREATE VIEW `zstack`.`VmInstanceVO` AS SELECT uuid, name, description, zoneUuid, clusterUuid, imageUuid, hostUuid, internalId, lastHostUuid, instanceOfferingUuid, rootVolumeUuid, defaultL3NetworkUuid, type, hypervisorType, cpuNum, cpuSpeed, memorySize, platform, allocatorStrategy, createDate, lastOpDate, state FROM `zstack`.`VmInstanceEO` WHERE deleted IS NULL;

CREATE VIEW `zstack`.`ImageVO` AS SELECT uuid, name, description, status, state, size, md5Sum, platform, type, format, url, system, mediaType, createDate, lastOpDate, guestOsType FROM `zstack`.`ImageEO` WHERE deleted IS NULL;

CREATE VIEW `zstack`.`VolumeVO` AS SELECT uuid, name, description, primaryStorageUuid, vmInstanceUuid, diskOfferingUuid, rootImageUuid, installPath, type, status, size, deviceId, format, state, createDate, lastOpDate FROM `zstack`.`VolumeEO` WHERE deleted IS NULL;

CREATE VIEW `zstack`.`InstanceOfferingVO` AS SELECT uuid, name, description, cpuNum, cpuSpeed, memorySize, allocatorStrategy, sortKey, state, createDate, lastOpDate, type, duration FROM `zstack`.`InstanceOfferingEO` WHERE deleted IS NULL;

CREATE VIEW `zstack`.`DiskOfferingVO` AS SELECT uuid, name, description, diskSize, sortKey, type, state, createDate, lastOpDate, allocatorStrategy FROM `zstack`.`DiskOfferingEO` WHERE deleted IS NULL;

CREATE VIEW `zstack`.`PrimaryStorageVO` AS SELECT uuid, zoneUuid, name, url, description, type, mountPath, state, status, createDate, lastOpDate FROM `zstack`.`PrimaryStorageEO` WHERE deleted IS NULL;

CREATE VIEW `zstack`.`VolumeSnapshotVO` AS SELECT uuid, name, description, type, volumeUuid, format, treeUuid, parentUuid, primaryStorageUuid, primaryStorageInstallPath, distance, size, latest, fullSnapshot, volumeType, state, status, createDate, lastOpDate FROM `zstack`.`VolumeSnapshotEO` WHERE deleted IS NULL;

CREATE VIEW `zstack`.`VolumeSnapshotTreeVO` AS SELECT uuid, volumeUuid, current, createDate, lastOpDate FROM `zstack`.`VolumeSnapshotTreeEO` WHERE deleted IS NULL;

CREATE VIEW `zstack`.`BackupStorageVO` AS SELECT uuid, name, url, description, totalCapacity, availableCapacity, type, state, status, createDate, lastOpDate FROM `zstack`.`BackupStorageEO` WHERE deleted IS NULL;

CREATE VIEW `zstack`.`L3NetworkVO` AS SELECT uuid, name, description, state, type, zoneUuid, l2NetworkUuid, system, dnsDomain, createDate, lastOpDate FROM `zstack`.`L3NetworkEO` WHERE deleted IS NULL;

CREATE VIEW `zstack`.`IpRangeVO` AS SELECT uuid, l3NetworkUuid, name, description, startIp, endIp, netmask, gateway, networkCidr, createDate, lastOpDate FROM `zstack`.`IpRangeEO` WHERE deleted IS NULL;

CREATE VIEW `zstack`.`L2NetworkVO` AS SELECT uuid, name, description, type, zoneUuid, physicalInterface, createDate, lastOpDate FROM `zstack`.`L2NetworkEO` WHERE deleted IS NULL;

CREATE VIEW `zstack`.`ClusterVO` AS SELECT uuid, zoneUuid, name, type, description, state, hypervisorType, createDate, lastOpDate, managementNodeId FROM `zstack`.`ClusterEO` WHERE deleted IS NULL;

CREATE VIEW `zstack`.`ZoneVO` AS SELECT uuid, name, type, description, state, createDate, lastOpDate FROM `zstack`.`ZoneEO` WHERE deleted IS NULL;

CREATE VIEW `zstack`.`HostVO` AS SELECT uuid, zoneUuid, clusterUuid, name, description, managementIp, hypervisorType, state, status, createDate, lastOpDate FROM `zstack`.`HostEO` WHERE deleted IS NULL;
