DELIMITER |

DROP TRIGGER IF EXISTS `zstack`.VmInstanceVOInsertTrigger;
CREATE TRIGGER `zstack`.VmInstanceVOInsertTrigger AFTER INSERT ON `zstack`.`VmInstanceVO`
FOR EACH ROW BEGIN
	INSERT INTO InsertVO (voName, uuid) VALUES ('VmInstanceVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.VmInstanceVOUpdateTrigger;
CREATE TRIGGER `zstack`.VmInstanceVOUpdateTrigger AFTER UPDATE ON `zstack`.`VmInstanceVO`
FOR EACH ROW BEGIN
	INSERT INTO UpdateVO (voName, uuid) VALUES ('VmInstanceVO', new.uuid);
	INSERT INTO UpdateVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('VmInstanceVO', new.uuid, 'VirtualRouterVmVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.VmInstanceVODeleteTrigger;
CREATE TRIGGER `zstack`.VmInstanceVODeleteTrigger AFTER DELETE ON `zstack`.`VmInstanceVO`
FOR EACH ROW BEGIN
	INSERT INTO DeleteVO (voName, uuid) VALUES ('VmInstanceVO', old.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.VmNicVOInsertTrigger;
CREATE TRIGGER `zstack`.VmNicVOInsertTrigger AFTER INSERT ON `zstack`.`VmNicVO`
FOR EACH ROW BEGIN
	INSERT INTO InsertVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('VmNicVO', new.uuid, 'VirtualRouterVmVO', new.vmInstanceUuid);
	INSERT INTO InsertVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('VmNicVO', new.uuid, 'VmInstanceVO', new.vmInstanceUuid);
END|

DROP TRIGGER IF EXISTS `zstack`.VmNicVOUpdateTrigger;
CREATE TRIGGER `zstack`.VmNicVOUpdateTrigger AFTER UPDATE ON `zstack`.`VmNicVO`
FOR EACH ROW BEGIN
	INSERT INTO UpdateVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('VmNicVO', new.uuid, 'VirtualRouterVmVO', new.vmInstanceUuid);
	INSERT INTO UpdateVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('VmNicVO', new.uuid, 'VmInstanceVO', new.vmInstanceUuid);
END|

DROP TRIGGER IF EXISTS `zstack`.VmNicVODeleteTrigger;
CREATE TRIGGER `zstack`.VmNicVODeleteTrigger AFTER DELETE ON `zstack`.`VmNicVO`
FOR EACH ROW BEGIN
	INSERT INTO DeleteVO (voName, uuid) VALUES ('VmNicVO', old.uuid);
	INSERT INTO DeleteVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('VmNicVO', old.uuid, 'VirtualRouterVmVO', old.vmInstanceUuid);
	INSERT INTO DeleteVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('VmNicVO', old.uuid, 'VmInstanceVO', old.vmInstanceUuid);
END|

DROP TRIGGER IF EXISTS `zstack`.ImageVOInsertTrigger;
CREATE TRIGGER `zstack`.ImageVOInsertTrigger AFTER INSERT ON `zstack`.`ImageVO`
FOR EACH ROW BEGIN
	INSERT INTO InsertVO (voName, uuid) VALUES ('ImageVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.ImageVOUpdateTrigger;
CREATE TRIGGER `zstack`.ImageVOUpdateTrigger AFTER UPDATE ON `zstack`.`ImageVO`
FOR EACH ROW BEGIN
	INSERT INTO UpdateVO (voName, uuid) VALUES ('ImageVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.ImageVODeleteTrigger;
CREATE TRIGGER `zstack`.ImageVODeleteTrigger AFTER DELETE ON `zstack`.`ImageVO`
FOR EACH ROW BEGIN
	INSERT INTO DeleteVO (voName, uuid) VALUES ('ImageVO', old.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.VolumeVOInsertTrigger;
CREATE TRIGGER `zstack`.VolumeVOInsertTrigger AFTER INSERT ON `zstack`.`VolumeVO`
FOR EACH ROW BEGIN
	INSERT INTO InsertVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('VolumeVO', new.uuid, 'VirtualRouterVmVO', new.vmInstanceUuid);
	INSERT INTO InsertVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('VolumeVO', new.uuid, 'VmInstanceVO', new.vmInstanceUuid);
END|

DROP TRIGGER IF EXISTS `zstack`.VolumeVOUpdateTrigger;
CREATE TRIGGER `zstack`.VolumeVOUpdateTrigger AFTER UPDATE ON `zstack`.`VolumeVO`
FOR EACH ROW BEGIN
	INSERT INTO UpdateVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('VolumeVO', new.uuid, 'VirtualRouterVmVO', new.vmInstanceUuid);
	INSERT INTO UpdateVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('VolumeVO', new.uuid, 'VmInstanceVO', new.vmInstanceUuid);
END|

DROP TRIGGER IF EXISTS `zstack`.VolumeVODeleteTrigger;
CREATE TRIGGER `zstack`.VolumeVODeleteTrigger AFTER DELETE ON `zstack`.`VolumeVO`
FOR EACH ROW BEGIN
	INSERT INTO DeleteVO (voName, uuid) VALUES ('VolumeVO', old.uuid);
	INSERT INTO DeleteVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('VolumeVO', old.uuid, 'VirtualRouterVmVO', old.vmInstanceUuid);
	INSERT INTO DeleteVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('VolumeVO', old.uuid, 'VmInstanceVO', old.vmInstanceUuid);
END|

DROP TRIGGER IF EXISTS `zstack`.InstanceOfferingVOInsertTrigger;
CREATE TRIGGER `zstack`.InstanceOfferingVOInsertTrigger AFTER INSERT ON `zstack`.`InstanceOfferingVO`
FOR EACH ROW BEGIN
	INSERT INTO InsertVO (voName, uuid) VALUES ('InstanceOfferingVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.InstanceOfferingVOUpdateTrigger;
CREATE TRIGGER `zstack`.InstanceOfferingVOUpdateTrigger AFTER UPDATE ON `zstack`.`InstanceOfferingVO`
FOR EACH ROW BEGIN
	INSERT INTO UpdateVO (voName, uuid) VALUES ('InstanceOfferingVO', new.uuid);
	INSERT INTO UpdateVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('InstanceOfferingVO', new.uuid, 'VirtualRouterOfferingVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.InstanceOfferingVODeleteTrigger;
CREATE TRIGGER `zstack`.InstanceOfferingVODeleteTrigger AFTER DELETE ON `zstack`.`InstanceOfferingVO`
FOR EACH ROW BEGIN
	INSERT INTO DeleteVO (voName, uuid) VALUES ('InstanceOfferingVO', old.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.DiskOfferingVOInsertTrigger;
CREATE TRIGGER `zstack`.DiskOfferingVOInsertTrigger AFTER INSERT ON `zstack`.`DiskOfferingVO`
FOR EACH ROW BEGIN
	INSERT INTO InsertVO (voName, uuid) VALUES ('DiskOfferingVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.DiskOfferingVOUpdateTrigger;
CREATE TRIGGER `zstack`.DiskOfferingVOUpdateTrigger AFTER UPDATE ON `zstack`.`DiskOfferingVO`
FOR EACH ROW BEGIN
	INSERT INTO UpdateVO (voName, uuid) VALUES ('DiskOfferingVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.DiskOfferingVODeleteTrigger;
CREATE TRIGGER `zstack`.DiskOfferingVODeleteTrigger AFTER DELETE ON `zstack`.`DiskOfferingVO`
FOR EACH ROW BEGIN
	INSERT INTO DeleteVO (voName, uuid) VALUES ('DiskOfferingVO', old.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.PrimaryStorageVOInsertTrigger;
CREATE TRIGGER `zstack`.PrimaryStorageVOInsertTrigger AFTER INSERT ON `zstack`.`PrimaryStorageVO`
FOR EACH ROW BEGIN
	INSERT INTO InsertVO (voName, uuid) VALUES ('PrimaryStorageVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.PrimaryStorageVOUpdateTrigger;
CREATE TRIGGER `zstack`.PrimaryStorageVOUpdateTrigger AFTER UPDATE ON `zstack`.`PrimaryStorageVO`
FOR EACH ROW BEGIN
	INSERT INTO UpdateVO (voName, uuid) VALUES ('PrimaryStorageVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.PrimaryStorageVODeleteTrigger;
CREATE TRIGGER `zstack`.PrimaryStorageVODeleteTrigger AFTER DELETE ON `zstack`.`PrimaryStorageVO`
FOR EACH ROW BEGIN
	INSERT INTO DeleteVO (voName, uuid) VALUES ('PrimaryStorageVO', old.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.PrimaryStorageClusterRefVOInsertTrigger;
CREATE TRIGGER `zstack`.PrimaryStorageClusterRefVOInsertTrigger AFTER INSERT ON `zstack`.`PrimaryStorageClusterRefVO`
FOR EACH ROW BEGIN
	INSERT INTO InsertVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('PrimaryStorageClusterRefVO', new.id, 'PrimaryStorageVO', new.primaryStorageUuid);
END|

DROP TRIGGER IF EXISTS `zstack`.PrimaryStorageClusterRefVOUpdateTrigger;
CREATE TRIGGER `zstack`.PrimaryStorageClusterRefVOUpdateTrigger AFTER UPDATE ON `zstack`.`PrimaryStorageClusterRefVO`
FOR EACH ROW BEGIN
	INSERT INTO UpdateVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('PrimaryStorageClusterRefVO', new.id, 'PrimaryStorageVO', new.primaryStorageUuid);
END|

DROP TRIGGER IF EXISTS `zstack`.PrimaryStorageClusterRefVODeleteTrigger;
CREATE TRIGGER `zstack`.PrimaryStorageClusterRefVODeleteTrigger AFTER DELETE ON `zstack`.`PrimaryStorageClusterRefVO`
FOR EACH ROW BEGIN
	INSERT INTO DeleteVO (voName, uuid) VALUES ('PrimaryStorageClusterRefVO', old.id);
	INSERT INTO DeleteVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('PrimaryStorageClusterRefVO', old.id, 'PrimaryStorageVO', old.primaryStorageUuid);
END|

DROP TRIGGER IF EXISTS `zstack`.BackupStorageZoneRefVOInsertTrigger;
CREATE TRIGGER `zstack`.BackupStorageZoneRefVOInsertTrigger AFTER INSERT ON `zstack`.`BackupStorageZoneRefVO`
FOR EACH ROW BEGIN
	INSERT INTO InsertVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('BackupStorageZoneRefVO', new.id, 'SftpBackupStorageVO', new.backupStorageUuid);
	INSERT INTO InsertVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('BackupStorageZoneRefVO', new.id, 'BackupStorageVO', new.backupStorageUuid);
END|

DROP TRIGGER IF EXISTS `zstack`.BackupStorageZoneRefVOUpdateTrigger;
CREATE TRIGGER `zstack`.BackupStorageZoneRefVOUpdateTrigger AFTER UPDATE ON `zstack`.`BackupStorageZoneRefVO`
FOR EACH ROW BEGIN
	INSERT INTO UpdateVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('BackupStorageZoneRefVO', new.id, 'SftpBackupStorageVO', new.backupStorageUuid);
	INSERT INTO UpdateVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('BackupStorageZoneRefVO', new.id, 'BackupStorageVO', new.backupStorageUuid);
END|

DROP TRIGGER IF EXISTS `zstack`.BackupStorageZoneRefVODeleteTrigger;
CREATE TRIGGER `zstack`.BackupStorageZoneRefVODeleteTrigger AFTER DELETE ON `zstack`.`BackupStorageZoneRefVO`
FOR EACH ROW BEGIN
	INSERT INTO DeleteVO (voName, uuid) VALUES ('BackupStorageZoneRefVO', old.id);
	INSERT INTO DeleteVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('BackupStorageZoneRefVO', old.id, 'SftpBackupStorageVO', old.backupStorageUuid);
	INSERT INTO DeleteVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('BackupStorageZoneRefVO', old.id, 'BackupStorageVO', old.backupStorageUuid);
END|

DROP TRIGGER IF EXISTS `zstack`.BackupStorageVOInsertTrigger;
CREATE TRIGGER `zstack`.BackupStorageVOInsertTrigger AFTER INSERT ON `zstack`.`BackupStorageVO`
FOR EACH ROW BEGIN
	INSERT INTO InsertVO (voName, uuid) VALUES ('BackupStorageVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.BackupStorageVOUpdateTrigger;
CREATE TRIGGER `zstack`.BackupStorageVOUpdateTrigger AFTER UPDATE ON `zstack`.`BackupStorageVO`
FOR EACH ROW BEGIN
	INSERT INTO UpdateVO (voName, uuid) VALUES ('BackupStorageVO', new.uuid);
	INSERT INTO UpdateVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('BackupStorageVO', new.uuid, 'SftpBackupStorageVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.BackupStorageVODeleteTrigger;
CREATE TRIGGER `zstack`.BackupStorageVODeleteTrigger AFTER DELETE ON `zstack`.`BackupStorageVO`
FOR EACH ROW BEGIN
	INSERT INTO DeleteVO (voName, uuid) VALUES ('BackupStorageVO', old.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.L3NetworkVOInsertTrigger;
CREATE TRIGGER `zstack`.L3NetworkVOInsertTrigger AFTER INSERT ON `zstack`.`L3NetworkVO`
FOR EACH ROW BEGIN
	INSERT INTO InsertVO (voName, uuid) VALUES ('L3NetworkVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.L3NetworkVOUpdateTrigger;
CREATE TRIGGER `zstack`.L3NetworkVOUpdateTrigger AFTER UPDATE ON `zstack`.`L3NetworkVO`
FOR EACH ROW BEGIN
	INSERT INTO UpdateVO (voName, uuid) VALUES ('L3NetworkVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.L3NetworkVODeleteTrigger;
CREATE TRIGGER `zstack`.L3NetworkVODeleteTrigger AFTER DELETE ON `zstack`.`L3NetworkVO`
FOR EACH ROW BEGIN
	INSERT INTO DeleteVO (voName, uuid) VALUES ('L3NetworkVO', old.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.L3NetworkDnsVOInsertTrigger;
CREATE TRIGGER `zstack`.L3NetworkDnsVOInsertTrigger AFTER INSERT ON `zstack`.`L3NetworkDnsVO`
FOR EACH ROW BEGIN
	INSERT INTO InsertVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('L3NetworkDnsVO', new.id, 'L3NetworkDnsVO', new.l3NetworkUuid);
END|

DROP TRIGGER IF EXISTS `zstack`.L3NetworkDnsVOUpdateTrigger;
CREATE TRIGGER `zstack`.L3NetworkDnsVOUpdateTrigger AFTER UPDATE ON `zstack`.`L3NetworkDnsVO`
FOR EACH ROW BEGIN
	INSERT INTO UpdateVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('L3NetworkDnsVO', new.id, 'L3NetworkDnsVO', new.l3NetworkUuid);
END|

DROP TRIGGER IF EXISTS `zstack`.L3NetworkDnsVODeleteTrigger;
CREATE TRIGGER `zstack`.L3NetworkDnsVODeleteTrigger AFTER DELETE ON `zstack`.`L3NetworkDnsVO`
FOR EACH ROW BEGIN
	INSERT INTO DeleteVO (voName, uuid) VALUES ('L3NetworkDnsVO', old.id);
	INSERT INTO DeleteVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('L3NetworkDnsVO', old.id, 'L3NetworkDnsVO', old.l3NetworkUuid);
END|

DROP TRIGGER IF EXISTS `zstack`.NetworkServiceProviderL2NetworkRefVOInsertTrigger;
CREATE TRIGGER `zstack`.NetworkServiceProviderL2NetworkRefVOInsertTrigger AFTER INSERT ON `zstack`.`NetworkServiceProviderL2NetworkRefVO`
FOR EACH ROW BEGIN
	INSERT INTO InsertVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('NetworkServiceProviderL2NetworkRefVO', new.id, 'NetworkServiceProviderVO', new.networkServiceProviderUuid);
END|

DROP TRIGGER IF EXISTS `zstack`.NetworkServiceProviderL2NetworkRefVOUpdateTrigger;
CREATE TRIGGER `zstack`.NetworkServiceProviderL2NetworkRefVOUpdateTrigger AFTER UPDATE ON `zstack`.`NetworkServiceProviderL2NetworkRefVO`
FOR EACH ROW BEGIN
	INSERT INTO UpdateVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('NetworkServiceProviderL2NetworkRefVO', new.id, 'NetworkServiceProviderVO', new.networkServiceProviderUuid);
END|

DROP TRIGGER IF EXISTS `zstack`.NetworkServiceProviderL2NetworkRefVODeleteTrigger;
CREATE TRIGGER `zstack`.NetworkServiceProviderL2NetworkRefVODeleteTrigger AFTER DELETE ON `zstack`.`NetworkServiceProviderL2NetworkRefVO`
FOR EACH ROW BEGIN
	INSERT INTO DeleteVO (voName, uuid) VALUES ('NetworkServiceProviderL2NetworkRefVO', old.id);
	INSERT INTO DeleteVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('NetworkServiceProviderL2NetworkRefVO', old.id, 'NetworkServiceProviderVO', old.networkServiceProviderUuid);
END|

DROP TRIGGER IF EXISTS `zstack`.L2NetworkVOInsertTrigger;
CREATE TRIGGER `zstack`.L2NetworkVOInsertTrigger AFTER INSERT ON `zstack`.`L2NetworkVO`
FOR EACH ROW BEGIN
	INSERT INTO InsertVO (voName, uuid) VALUES ('L2NetworkVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.L2NetworkVOUpdateTrigger;
CREATE TRIGGER `zstack`.L2NetworkVOUpdateTrigger AFTER UPDATE ON `zstack`.`L2NetworkVO`
FOR EACH ROW BEGIN
	INSERT INTO UpdateVO (voName, uuid) VALUES ('L2NetworkVO', new.uuid);
	INSERT INTO UpdateVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('L2NetworkVO', new.uuid, 'L2VlanNetworkVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.L2NetworkVODeleteTrigger;
CREATE TRIGGER `zstack`.L2NetworkVODeleteTrigger AFTER DELETE ON `zstack`.`L2NetworkVO`
FOR EACH ROW BEGIN
	INSERT INTO DeleteVO (voName, uuid) VALUES ('L2NetworkVO', old.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.NetworkServiceProviderVOInsertTrigger;
CREATE TRIGGER `zstack`.NetworkServiceProviderVOInsertTrigger AFTER INSERT ON `zstack`.`NetworkServiceProviderVO`
FOR EACH ROW BEGIN
	INSERT INTO InsertVO (voName, uuid) VALUES ('NetworkServiceProviderVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.NetworkServiceProviderVOUpdateTrigger;
CREATE TRIGGER `zstack`.NetworkServiceProviderVOUpdateTrigger AFTER UPDATE ON `zstack`.`NetworkServiceProviderVO`
FOR EACH ROW BEGIN
	INSERT INTO UpdateVO (voName, uuid) VALUES ('NetworkServiceProviderVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.NetworkServiceProviderVODeleteTrigger;
CREATE TRIGGER `zstack`.NetworkServiceProviderVODeleteTrigger AFTER DELETE ON `zstack`.`NetworkServiceProviderVO`
FOR EACH ROW BEGIN
	INSERT INTO DeleteVO (voName, uuid) VALUES ('NetworkServiceProviderVO', old.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.NetworkServiceL3NetworkRefVOInsertTrigger;
CREATE TRIGGER `zstack`.NetworkServiceL3NetworkRefVOInsertTrigger AFTER INSERT ON `zstack`.`NetworkServiceL3NetworkRefVO`
FOR EACH ROW BEGIN
	INSERT INTO InsertVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('NetworkServiceL3NetworkRefVO', new.id, 'L3NetworkVO', new.l3NetworkUuid);
END|

DROP TRIGGER IF EXISTS `zstack`.NetworkServiceL3NetworkRefVOUpdateTrigger;
CREATE TRIGGER `zstack`.NetworkServiceL3NetworkRefVOUpdateTrigger AFTER UPDATE ON `zstack`.`NetworkServiceL3NetworkRefVO`
FOR EACH ROW BEGIN
	INSERT INTO UpdateVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('NetworkServiceL3NetworkRefVO', new.id, 'L3NetworkVO', new.l3NetworkUuid);
END|

DROP TRIGGER IF EXISTS `zstack`.NetworkServiceL3NetworkRefVODeleteTrigger;
CREATE TRIGGER `zstack`.NetworkServiceL3NetworkRefVODeleteTrigger AFTER DELETE ON `zstack`.`NetworkServiceL3NetworkRefVO`
FOR EACH ROW BEGIN
	INSERT INTO DeleteVO (voName, uuid) VALUES ('NetworkServiceL3NetworkRefVO', old.id);
	INSERT INTO DeleteVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('NetworkServiceL3NetworkRefVO', old.id, 'L3NetworkVO', old.l3NetworkUuid);
END|

DROP TRIGGER IF EXISTS `zstack`.L2NetworkClusterRefVOInsertTrigger;
CREATE TRIGGER `zstack`.L2NetworkClusterRefVOInsertTrigger AFTER INSERT ON `zstack`.`L2NetworkClusterRefVO`
FOR EACH ROW BEGIN
	INSERT INTO InsertVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('L2NetworkClusterRefVO', new.id, 'L2VlanNetworkVO', new.l2NetworkUuid);
	INSERT INTO InsertVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('L2NetworkClusterRefVO', new.id, 'L2NetworkVO', new.l2NetworkUuid);
END|

DROP TRIGGER IF EXISTS `zstack`.L2NetworkClusterRefVOUpdateTrigger;
CREATE TRIGGER `zstack`.L2NetworkClusterRefVOUpdateTrigger AFTER UPDATE ON `zstack`.`L2NetworkClusterRefVO`
FOR EACH ROW BEGIN
	INSERT INTO UpdateVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('L2NetworkClusterRefVO', new.id, 'L2VlanNetworkVO', new.l2NetworkUuid);
	INSERT INTO UpdateVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('L2NetworkClusterRefVO', new.id, 'L2NetworkVO', new.l2NetworkUuid);
END|

DROP TRIGGER IF EXISTS `zstack`.L2NetworkClusterRefVODeleteTrigger;
CREATE TRIGGER `zstack`.L2NetworkClusterRefVODeleteTrigger AFTER DELETE ON `zstack`.`L2NetworkClusterRefVO`
FOR EACH ROW BEGIN
	INSERT INTO DeleteVO (voName, uuid) VALUES ('L2NetworkClusterRefVO', old.id);
	INSERT INTO DeleteVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('L2NetworkClusterRefVO', old.id, 'L2VlanNetworkVO', old.l2NetworkUuid);
	INSERT INTO DeleteVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('L2NetworkClusterRefVO', old.id, 'L2NetworkVO', old.l2NetworkUuid);
END|

DROP TRIGGER IF EXISTS `zstack`.IpRangeVOInsertTrigger;
CREATE TRIGGER `zstack`.IpRangeVOInsertTrigger AFTER INSERT ON `zstack`.`IpRangeVO`
FOR EACH ROW BEGIN
	INSERT INTO InsertVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('IpRangeVO', new.uuid, 'L3NetworkVO', new.l3NetworkUuid);
END|

DROP TRIGGER IF EXISTS `zstack`.IpRangeVOUpdateTrigger;
CREATE TRIGGER `zstack`.IpRangeVOUpdateTrigger AFTER UPDATE ON `zstack`.`IpRangeVO`
FOR EACH ROW BEGIN
	INSERT INTO UpdateVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('IpRangeVO', new.uuid, 'L3NetworkVO', new.l3NetworkUuid);
END|

DROP TRIGGER IF EXISTS `zstack`.IpRangeVODeleteTrigger;
CREATE TRIGGER `zstack`.IpRangeVODeleteTrigger AFTER DELETE ON `zstack`.`IpRangeVO`
FOR EACH ROW BEGIN
	INSERT INTO DeleteVO (voName, uuid) VALUES ('IpRangeVO', old.uuid);
	INSERT INTO DeleteVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('IpRangeVO', old.uuid, 'L3NetworkVO', old.l3NetworkUuid);
END|

DROP TRIGGER IF EXISTS `zstack`.L2VlanNetworkVOInsertTrigger;
CREATE TRIGGER `zstack`.L2VlanNetworkVOInsertTrigger AFTER INSERT ON `zstack`.`L2VlanNetworkVO`
FOR EACH ROW BEGIN
	INSERT INTO InsertVO (voName, uuid) VALUES ('L2VlanNetworkVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.L2VlanNetworkVOUpdateTrigger;
CREATE TRIGGER `zstack`.L2VlanNetworkVOUpdateTrigger AFTER UPDATE ON `zstack`.`L2VlanNetworkVO`
FOR EACH ROW BEGIN
	INSERT INTO UpdateVO (voName, uuid) VALUES ('L2VlanNetworkVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.L2VlanNetworkVODeleteTrigger;
CREATE TRIGGER `zstack`.L2VlanNetworkVODeleteTrigger AFTER DELETE ON `zstack`.`L2VlanNetworkVO`
FOR EACH ROW BEGIN
	INSERT INTO DeleteVO (voName, uuid) VALUES ('L2VlanNetworkVO', old.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.ClusterVOInsertTrigger;
CREATE TRIGGER `zstack`.ClusterVOInsertTrigger AFTER INSERT ON `zstack`.`ClusterVO`
FOR EACH ROW BEGIN
	INSERT INTO InsertVO (voName, uuid) VALUES ('ClusterVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.ClusterVOUpdateTrigger;
CREATE TRIGGER `zstack`.ClusterVOUpdateTrigger AFTER UPDATE ON `zstack`.`ClusterVO`
FOR EACH ROW BEGIN
	INSERT INTO UpdateVO (voName, uuid) VALUES ('ClusterVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.ClusterVODeleteTrigger;
CREATE TRIGGER `zstack`.ClusterVODeleteTrigger AFTER DELETE ON `zstack`.`ClusterVO`
FOR EACH ROW BEGIN
	INSERT INTO DeleteVO (voName, uuid) VALUES ('ClusterVO', old.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.UserGroupUserRefVOInsertTrigger;
CREATE TRIGGER `zstack`.UserGroupUserRefVOInsertTrigger AFTER INSERT ON `zstack`.`UserGroupUserRefVO`
FOR EACH ROW BEGIN
	INSERT INTO InsertVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('UserGroupUserRefVO', new.id, 'UserVO', new.userUuid);
END|

DROP TRIGGER IF EXISTS `zstack`.UserGroupUserRefVOUpdateTrigger;
CREATE TRIGGER `zstack`.UserGroupUserRefVOUpdateTrigger AFTER UPDATE ON `zstack`.`UserGroupUserRefVO`
FOR EACH ROW BEGIN
	INSERT INTO UpdateVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('UserGroupUserRefVO', new.id, 'UserVO', new.userUuid);
END|

DROP TRIGGER IF EXISTS `zstack`.UserGroupUserRefVODeleteTrigger;
CREATE TRIGGER `zstack`.UserGroupUserRefVODeleteTrigger AFTER DELETE ON `zstack`.`UserGroupUserRefVO`
FOR EACH ROW BEGIN
	INSERT INTO DeleteVO (voName, uuid) VALUES ('UserGroupUserRefVO', old.id);
	INSERT INTO DeleteVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('UserGroupUserRefVO', old.id, 'UserVO', old.userUuid);
END|

DROP TRIGGER IF EXISTS `zstack`.UserGroupVOInsertTrigger;
CREATE TRIGGER `zstack`.UserGroupVOInsertTrigger AFTER INSERT ON `zstack`.`UserGroupVO`
FOR EACH ROW BEGIN
	INSERT INTO InsertVO (voName, uuid) VALUES ('UserGroupVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.UserGroupVOUpdateTrigger;
CREATE TRIGGER `zstack`.UserGroupVOUpdateTrigger AFTER UPDATE ON `zstack`.`UserGroupVO`
FOR EACH ROW BEGIN
	INSERT INTO UpdateVO (voName, uuid) VALUES ('UserGroupVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.UserGroupVODeleteTrigger;
CREATE TRIGGER `zstack`.UserGroupVODeleteTrigger AFTER DELETE ON `zstack`.`UserGroupVO`
FOR EACH ROW BEGIN
	INSERT INTO DeleteVO (voName, uuid) VALUES ('UserGroupVO', old.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.UserPolicyRefVOInsertTrigger;
CREATE TRIGGER `zstack`.UserPolicyRefVOInsertTrigger AFTER INSERT ON `zstack`.`UserPolicyRefVO`
FOR EACH ROW BEGIN
	INSERT INTO InsertVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('UserPolicyRefVO', new.id, 'UserVO', new.userUuid);
END|

DROP TRIGGER IF EXISTS `zstack`.UserPolicyRefVOUpdateTrigger;
CREATE TRIGGER `zstack`.UserPolicyRefVOUpdateTrigger AFTER UPDATE ON `zstack`.`UserPolicyRefVO`
FOR EACH ROW BEGIN
	INSERT INTO UpdateVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('UserPolicyRefVO', new.id, 'UserVO', new.userUuid);
END|

DROP TRIGGER IF EXISTS `zstack`.UserPolicyRefVODeleteTrigger;
CREATE TRIGGER `zstack`.UserPolicyRefVODeleteTrigger AFTER DELETE ON `zstack`.`UserPolicyRefVO`
FOR EACH ROW BEGIN
	INSERT INTO DeleteVO (voName, uuid) VALUES ('UserPolicyRefVO', old.id);
	INSERT INTO DeleteVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('UserPolicyRefVO', old.id, 'UserVO', old.userUuid);
END|

DROP TRIGGER IF EXISTS `zstack`.AccountVOInsertTrigger;
CREATE TRIGGER `zstack`.AccountVOInsertTrigger AFTER INSERT ON `zstack`.`AccountVO`
FOR EACH ROW BEGIN
	INSERT INTO InsertVO (voName, uuid) VALUES ('AccountVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.AccountVOUpdateTrigger;
CREATE TRIGGER `zstack`.AccountVOUpdateTrigger AFTER UPDATE ON `zstack`.`AccountVO`
FOR EACH ROW BEGIN
	INSERT INTO UpdateVO (voName, uuid) VALUES ('AccountVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.AccountVODeleteTrigger;
CREATE TRIGGER `zstack`.AccountVODeleteTrigger AFTER DELETE ON `zstack`.`AccountVO`
FOR EACH ROW BEGIN
	INSERT INTO DeleteVO (voName, uuid) VALUES ('AccountVO', old.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.UserGroupPolicyRefVOInsertTrigger;
CREATE TRIGGER `zstack`.UserGroupPolicyRefVOInsertTrigger AFTER INSERT ON `zstack`.`UserGroupPolicyRefVO`
FOR EACH ROW BEGIN
	INSERT INTO InsertVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('UserGroupPolicyRefVO', new.id, 'UserGroupVO', new.groupUuid);
END|

DROP TRIGGER IF EXISTS `zstack`.UserGroupPolicyRefVOUpdateTrigger;
CREATE TRIGGER `zstack`.UserGroupPolicyRefVOUpdateTrigger AFTER UPDATE ON `zstack`.`UserGroupPolicyRefVO`
FOR EACH ROW BEGIN
	INSERT INTO UpdateVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('UserGroupPolicyRefVO', new.id, 'UserGroupVO', new.groupUuid);
END|

DROP TRIGGER IF EXISTS `zstack`.UserGroupPolicyRefVODeleteTrigger;
CREATE TRIGGER `zstack`.UserGroupPolicyRefVODeleteTrigger AFTER DELETE ON `zstack`.`UserGroupPolicyRefVO`
FOR EACH ROW BEGIN
	INSERT INTO DeleteVO (voName, uuid) VALUES ('UserGroupPolicyRefVO', old.id);
	INSERT INTO DeleteVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('UserGroupPolicyRefVO', old.id, 'UserGroupVO', old.groupUuid);
END|

DROP TRIGGER IF EXISTS `zstack`.PolicyVOInsertTrigger;
CREATE TRIGGER `zstack`.PolicyVOInsertTrigger AFTER INSERT ON `zstack`.`PolicyVO`
FOR EACH ROW BEGIN
	INSERT INTO InsertVO (voName, uuid) VALUES ('PolicyVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.PolicyVOUpdateTrigger;
CREATE TRIGGER `zstack`.PolicyVOUpdateTrigger AFTER UPDATE ON `zstack`.`PolicyVO`
FOR EACH ROW BEGIN
	INSERT INTO UpdateVO (voName, uuid) VALUES ('PolicyVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.PolicyVODeleteTrigger;
CREATE TRIGGER `zstack`.PolicyVODeleteTrigger AFTER DELETE ON `zstack`.`PolicyVO`
FOR EACH ROW BEGIN
	INSERT INTO DeleteVO (voName, uuid) VALUES ('PolicyVO', old.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.UserVOInsertTrigger;
CREATE TRIGGER `zstack`.UserVOInsertTrigger AFTER INSERT ON `zstack`.`UserVO`
FOR EACH ROW BEGIN
	INSERT INTO InsertVO (voName, uuid) VALUES ('UserVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.UserVOUpdateTrigger;
CREATE TRIGGER `zstack`.UserVOUpdateTrigger AFTER UPDATE ON `zstack`.`UserVO`
FOR EACH ROW BEGIN
	INSERT INTO UpdateVO (voName, uuid) VALUES ('UserVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.UserVODeleteTrigger;
CREATE TRIGGER `zstack`.UserVODeleteTrigger AFTER DELETE ON `zstack`.`UserVO`
FOR EACH ROW BEGIN
	INSERT INTO DeleteVO (voName, uuid) VALUES ('UserVO', old.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.ZoneVOInsertTrigger;
CREATE TRIGGER `zstack`.ZoneVOInsertTrigger AFTER INSERT ON `zstack`.`ZoneVO`
FOR EACH ROW BEGIN
	INSERT INTO InsertVO (voName, uuid) VALUES ('ZoneVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.ZoneVOUpdateTrigger;
CREATE TRIGGER `zstack`.ZoneVOUpdateTrigger AFTER UPDATE ON `zstack`.`ZoneVO`
FOR EACH ROW BEGIN
	INSERT INTO UpdateVO (voName, uuid) VALUES ('ZoneVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.ZoneVODeleteTrigger;
CREATE TRIGGER `zstack`.ZoneVODeleteTrigger AFTER DELETE ON `zstack`.`ZoneVO`
FOR EACH ROW BEGIN
	INSERT INTO DeleteVO (voName, uuid) VALUES ('ZoneVO', old.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.HostVOInsertTrigger;
CREATE TRIGGER `zstack`.HostVOInsertTrigger AFTER INSERT ON `zstack`.`HostVO`
FOR EACH ROW BEGIN
	INSERT INTO InsertVO (voName, uuid) VALUES ('HostVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.HostVOUpdateTrigger;
CREATE TRIGGER `zstack`.HostVOUpdateTrigger AFTER UPDATE ON `zstack`.`HostVO`
FOR EACH ROW BEGIN
	INSERT INTO UpdateVO (voName, uuid) VALUES ('HostVO', new.uuid);
	INSERT INTO UpdateVO (voName, uuid, foreignVOName, foreignVOUuid) VALUES ('HostVO', new.uuid, 'SimulatorHostVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.HostVODeleteTrigger;
CREATE TRIGGER `zstack`.HostVODeleteTrigger AFTER DELETE ON `zstack`.`HostVO`
FOR EACH ROW BEGIN
	INSERT INTO DeleteVO (voName, uuid) VALUES ('HostVO', old.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.SimulatorHostVOInsertTrigger;
CREATE TRIGGER `zstack`.SimulatorHostVOInsertTrigger AFTER INSERT ON `zstack`.`SimulatorHostVO`
FOR EACH ROW BEGIN
	INSERT INTO InsertVO (voName, uuid) VALUES ('SimulatorHostVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.SimulatorHostVOUpdateTrigger;
CREATE TRIGGER `zstack`.SimulatorHostVOUpdateTrigger AFTER UPDATE ON `zstack`.`SimulatorHostVO`
FOR EACH ROW BEGIN
	INSERT INTO UpdateVO (voName, uuid) VALUES ('SimulatorHostVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.SimulatorHostVODeleteTrigger;
CREATE TRIGGER `zstack`.SimulatorHostVODeleteTrigger AFTER DELETE ON `zstack`.`SimulatorHostVO`
FOR EACH ROW BEGIN
	INSERT INTO DeleteVO (voName, uuid) VALUES ('SimulatorHostVO', old.uuid);
	INSERT INTO DeleteVO (voName, uuid, foreignVOToDeleteName, foreignVOToDeleteUuid) VALUES ('SimulatorHostVO', old.uuid, 'SimulatorHostVO', old.uuid);
	INSERT INTO DeleteVO (voName, uuid, foreignVOToDeleteName, foreignVOToDeleteUuid) VALUES ('SimulatorHostVO', old.uuid, 'HostVO', old.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.SftpBackupStorageVOInsertTrigger;
CREATE TRIGGER `zstack`.SftpBackupStorageVOInsertTrigger AFTER INSERT ON `zstack`.`SftpBackupStorageVO`
FOR EACH ROW BEGIN
	INSERT INTO InsertVO (voName, uuid) VALUES ('SftpBackupStorageVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.SftpBackupStorageVOUpdateTrigger;
CREATE TRIGGER `zstack`.SftpBackupStorageVOUpdateTrigger AFTER UPDATE ON `zstack`.`SftpBackupStorageVO`
FOR EACH ROW BEGIN
	INSERT INTO UpdateVO (voName, uuid) VALUES ('SftpBackupStorageVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.SftpBackupStorageVODeleteTrigger;
CREATE TRIGGER `zstack`.SftpBackupStorageVODeleteTrigger AFTER DELETE ON `zstack`.`SftpBackupStorageVO`
FOR EACH ROW BEGIN
	INSERT INTO DeleteVO (voName, uuid) VALUES ('SftpBackupStorageVO', old.uuid);
	INSERT INTO DeleteVO (voName, uuid, foreignVOToDeleteName, foreignVOToDeleteUuid) VALUES ('SftpBackupStorageVO', old.uuid, 'SftpBackupStorageVO', old.uuid);
	INSERT INTO DeleteVO (voName, uuid, foreignVOToDeleteName, foreignVOToDeleteUuid) VALUES ('SftpBackupStorageVO', old.uuid, 'BackupStorageVO', old.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.VirtualRouterVmVOInsertTrigger;
CREATE TRIGGER `zstack`.VirtualRouterVmVOInsertTrigger AFTER INSERT ON `zstack`.`VirtualRouterVmVO`
FOR EACH ROW BEGIN
	INSERT INTO InsertVO (voName, uuid) VALUES ('VirtualRouterVmVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.VirtualRouterVmVOUpdateTrigger;
CREATE TRIGGER `zstack`.VirtualRouterVmVOUpdateTrigger AFTER UPDATE ON `zstack`.`VirtualRouterVmVO`
FOR EACH ROW BEGIN
	INSERT INTO UpdateVO (voName, uuid) VALUES ('VirtualRouterVmVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.VirtualRouterVmVODeleteTrigger;
CREATE TRIGGER `zstack`.VirtualRouterVmVODeleteTrigger AFTER DELETE ON `zstack`.`VirtualRouterVmVO`
FOR EACH ROW BEGIN
	INSERT INTO DeleteVO (voName, uuid) VALUES ('VirtualRouterVmVO', old.uuid);
	INSERT INTO DeleteVO (voName, uuid, foreignVOToDeleteName, foreignVOToDeleteUuid) VALUES ('VirtualRouterVmVO', old.uuid, 'VirtualRouterVmVO', old.uuid);
	INSERT INTO DeleteVO (voName, uuid, foreignVOToDeleteName, foreignVOToDeleteUuid) VALUES ('VirtualRouterVmVO', old.uuid, 'VmInstanceVO', old.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.VirtualRouterOfferingVOInsertTrigger;
CREATE TRIGGER `zstack`.VirtualRouterOfferingVOInsertTrigger AFTER INSERT ON `zstack`.`VirtualRouterOfferingVO`
FOR EACH ROW BEGIN
	INSERT INTO InsertVO (voName, uuid) VALUES ('VirtualRouterOfferingVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.VirtualRouterOfferingVOUpdateTrigger;
CREATE TRIGGER `zstack`.VirtualRouterOfferingVOUpdateTrigger AFTER UPDATE ON `zstack`.`VirtualRouterOfferingVO`
FOR EACH ROW BEGIN
	INSERT INTO UpdateVO (voName, uuid) VALUES ('VirtualRouterOfferingVO', new.uuid);
END|

DROP TRIGGER IF EXISTS `zstack`.VirtualRouterOfferingVODeleteTrigger;
CREATE TRIGGER `zstack`.VirtualRouterOfferingVODeleteTrigger AFTER DELETE ON `zstack`.`VirtualRouterOfferingVO`
FOR EACH ROW BEGIN
	INSERT INTO DeleteVO (voName, uuid) VALUES ('VirtualRouterOfferingVO', old.uuid);
END|
