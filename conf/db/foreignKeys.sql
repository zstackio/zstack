
# Foreign keys for table ApplianceVmFirewallRuleVO

ALTER TABLE ApplianceVmFirewallRuleVO ADD CONSTRAINT fkApplianceVmFirewallRuleVOL3NetworkEO FOREIGN KEY (l3NetworkUuid) REFERENCES L3NetworkEO (uuid) ON DELETE CASCADE;
ALTER TABLE ApplianceVmFirewallRuleVO ADD CONSTRAINT fkApplianceVmFirewallRuleVOVmInstanceEO FOREIGN KEY (applianceVmUuid) REFERENCES VmInstanceEO (uuid) ON DELETE CASCADE;

# Foreign keys for table ApplianceVmVO

ALTER TABLE ApplianceVmVO ADD CONSTRAINT fkApplianceVmVOL3NetworkEO FOREIGN KEY (managementNetworkUuid) REFERENCES L3NetworkEO (uuid) ON DELETE RESTRICT;
ALTER TABLE ApplianceVmVO ADD CONSTRAINT fkApplianceVmVOL3NetworkEO1 FOREIGN KEY (defaultRouteL3NetworkUuid) REFERENCES L3NetworkEO (uuid) ;
ALTER TABLE ApplianceVmVO ADD CONSTRAINT fkApplianceVmVOVmInstanceEO FOREIGN KEY (uuid) REFERENCES VmInstanceEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;

# Foreign keys for table BackupStorageZoneRefVO

ALTER TABLE BackupStorageZoneRefVO ADD CONSTRAINT fkBackupStorageZoneRefVOBackupStorageEO FOREIGN KEY (backupStorageUuid) REFERENCES BackupStorageEO (uuid) ON DELETE CASCADE;
ALTER TABLE BackupStorageZoneRefVO ADD CONSTRAINT fkBackupStorageZoneRefVOZoneEO FOREIGN KEY (zoneUuid) REFERENCES ZoneEO (uuid) ON DELETE CASCADE;

# Foreign keys for table ClusterEO

ALTER TABLE ClusterEO ADD CONSTRAINT fkClusterEOZoneEO FOREIGN KEY (zoneUuid) REFERENCES ZoneEO (uuid) ON DELETE RESTRICT;

# Foreign keys for table ConsoleProxyVO

ALTER TABLE ConsoleProxyVO ADD CONSTRAINT fkConsoleProxyVOVmInstanceEO FOREIGN KEY (vmInstanceUuid) REFERENCES VmInstanceEO (uuid) ON DELETE CASCADE;

# Foreign keys for table EipVO

ALTER TABLE EipVO ADD CONSTRAINT fkEipVOVipVO FOREIGN KEY (vipUuid) REFERENCES VipVO (uuid) ON DELETE CASCADE;
ALTER TABLE EipVO ADD CONSTRAINT fkEipVOVmNicVO FOREIGN KEY (vmNicUuid) REFERENCES VmNicVO (uuid) ON DELETE SET NULL;

# Foreign keys for table HostCapacityVO

ALTER TABLE HostCapacityVO ADD CONSTRAINT fkHostCapacityVOHostEO FOREIGN KEY (uuid) REFERENCES HostEO (uuid) ON DELETE CASCADE;

# Foreign keys for table HostEO

ALTER TABLE HostEO ADD CONSTRAINT fkHostEOClusterEO FOREIGN KEY (clusterUuid) REFERENCES ClusterEO (uuid) ON DELETE RESTRICT;
ALTER TABLE HostEO ADD CONSTRAINT fkHostEOZoneEO FOREIGN KEY (zoneUuid) REFERENCES ZoneEO (uuid) ON DELETE RESTRICT;

# Foreign keys for table ImageBackupStorageRefVO

ALTER TABLE ImageBackupStorageRefVO ADD CONSTRAINT fkImageBackupStorageRefVOBackupStorageEO FOREIGN KEY (backupStorageUuid) REFERENCES BackupStorageEO (uuid) ON DELETE CASCADE;
ALTER TABLE ImageBackupStorageRefVO ADD CONSTRAINT fkImageBackupStorageRefVOImageEO FOREIGN KEY (imageUuid) REFERENCES ImageEO (uuid) ON DELETE CASCADE;

# Foreign keys for table ImageCacheVO

ALTER TABLE ImageCacheVO ADD CONSTRAINT fkImageCacheVOImageEO FOREIGN KEY (imageUuid) REFERENCES ImageEO (uuid) ON DELETE SET NULL;
ALTER TABLE ImageCacheVO ADD CONSTRAINT fkImageCacheVOPrimaryStorageEO FOREIGN KEY (primaryStorageUuid) REFERENCES PrimaryStorageEO (uuid) ON DELETE CASCADE;

# Foreign keys for table IpRangeEO

ALTER TABLE IpRangeEO ADD CONSTRAINT fkIpRangeEOL3NetworkEO FOREIGN KEY (l3NetworkUuid) REFERENCES L3NetworkEO (uuid) ON DELETE CASCADE;

# Foreign keys for table IscsiFileSystemBackendPrimaryStorageVO

ALTER TABLE IscsiFileSystemBackendPrimaryStorageVO ADD CONSTRAINT fkIscsiFileSystemBackendPrimaryStorageVOPrimaryStorageEO FOREIGN KEY (uuid) REFERENCES PrimaryStorageEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;

# Foreign keys for table IscsiIsoVO

ALTER TABLE IscsiIsoVO ADD CONSTRAINT fkIscsiIsoVOPrimaryStorageEO FOREIGN KEY (primaryStorageUuid) REFERENCES PrimaryStorageEO (uuid) ON DELETE CASCADE;
ALTER TABLE IscsiIsoVO ADD CONSTRAINT fkIscsiIsoVOVmInstanceEO FOREIGN KEY (vmInstanceUuid) REFERENCES VmInstanceEO (uuid) ON DELETE SET NULL;

# Foreign keys for table JobQueueEntryVO

ALTER TABLE JobQueueEntryVO ADD CONSTRAINT fkJobQueueEntryVOJobQueueVO FOREIGN KEY (jobQueueId) REFERENCES JobQueueVO (id) ON DELETE CASCADE;
ALTER TABLE JobQueueEntryVO ADD CONSTRAINT fkJobQueueEntryVOManagementNodeVO FOREIGN KEY (issuerManagementNodeId) REFERENCES ManagementNodeVO (uuid) ON DELETE SET NULL;

# Foreign keys for table JobQueueVO

ALTER TABLE JobQueueVO ADD CONSTRAINT fkJobQueueVOManagementNodeVO FOREIGN KEY (workerManagementNodeId) REFERENCES ManagementNodeVO (uuid) ON DELETE SET NULL;

# Foreign keys for table KVMHostVO

ALTER TABLE KVMHostVO ADD CONSTRAINT fkKVMHostVOHostEO FOREIGN KEY (uuid) REFERENCES HostEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;

# Foreign keys for table KeyValueVO

ALTER TABLE KeyValueVO ADD CONSTRAINT fkKeyValueVOKeyValueBinaryVO FOREIGN KEY (uuid) REFERENCES KeyValueBinaryVO (uuid) ON DELETE CASCADE;

# Foreign keys for table L2NetworkClusterRefVO

ALTER TABLE L2NetworkClusterRefVO ADD CONSTRAINT fkL2NetworkClusterRefVOClusterEO FOREIGN KEY (clusterUuid) REFERENCES ClusterEO (uuid) ON DELETE CASCADE;
ALTER TABLE L2NetworkClusterRefVO ADD CONSTRAINT fkL2NetworkClusterRefVOL2NetworkEO FOREIGN KEY (l2NetworkUuid) REFERENCES L2NetworkEO (uuid) ON DELETE CASCADE;

# Foreign keys for table L2NetworkEO

ALTER TABLE L2NetworkEO ADD CONSTRAINT fkL2NetworkEOZoneEO FOREIGN KEY (zoneUuid) REFERENCES ZoneEO (uuid) ON DELETE RESTRICT;

# Foreign keys for table L2VlanNetworkVO

ALTER TABLE L2VlanNetworkVO ADD CONSTRAINT fkL2VlanNetworkVOL2NetworkEO FOREIGN KEY (uuid) REFERENCES L2NetworkEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;

# Foreign keys for table L3NetworkDnsVO

ALTER TABLE L3NetworkDnsVO ADD CONSTRAINT fkL3NetworkDnsVOL3NetworkEO FOREIGN KEY (l3NetworkUuid) REFERENCES L3NetworkEO (uuid) ON DELETE CASCADE;

# Foreign keys for table L3NetworkEO

ALTER TABLE L3NetworkEO ADD CONSTRAINT fkL3NetworkEOL2NetworkEO FOREIGN KEY (l2NetworkUuid) REFERENCES L2NetworkEO (uuid) ON DELETE RESTRICT;
ALTER TABLE L3NetworkEO ADD CONSTRAINT fkL3NetworkEOZoneEO FOREIGN KEY (zoneUuid) REFERENCES ZoneEO (uuid) ON DELETE RESTRICT;

# Foreign keys for table NetworkServiceL3NetworkRefVO

ALTER TABLE NetworkServiceL3NetworkRefVO ADD CONSTRAINT fkNetworkServiceL3NetworkRefVOL3NetworkEO FOREIGN KEY (l3NetworkUuid) REFERENCES L3NetworkEO (uuid) ON DELETE CASCADE;
ALTER TABLE NetworkServiceL3NetworkRefVO ADD CONSTRAINT fkNetworkServiceL3NetworkRefVONetworkServiceProviderVO FOREIGN KEY (networkServiceProviderUuid) REFERENCES NetworkServiceProviderVO (uuid) ON DELETE CASCADE;

# Foreign keys for table NetworkServiceProviderL2NetworkRefVO

ALTER TABLE NetworkServiceProviderL2NetworkRefVO ADD CONSTRAINT fkNetworkServiceProviderL2NetworkRefVOL2NetworkEO FOREIGN KEY (l2NetworkUuid) REFERENCES L2NetworkEO (uuid) ON DELETE CASCADE;
ALTER TABLE NetworkServiceProviderL2NetworkRefVO ADD CONSTRAINT fkNetworkServiceProviderL2NetworkRefVONetworkServiceProviderVO FOREIGN KEY (networkServiceProviderUuid) REFERENCES NetworkServiceProviderVO (uuid) ON DELETE CASCADE;

# Foreign keys for table NetworkServiceTypeVO

ALTER TABLE NetworkServiceTypeVO ADD CONSTRAINT fkNetworkServiceTypeVONetworkServiceProviderVO FOREIGN KEY (networkServiceProviderUuid) REFERENCES NetworkServiceProviderVO (uuid) ON DELETE CASCADE;

# Foreign keys for table PortForwardingRuleVO

ALTER TABLE PortForwardingRuleVO ADD CONSTRAINT fkPortForwardingRuleVOVipVO FOREIGN KEY (vipUuid) REFERENCES VipVO (uuid) ON DELETE RESTRICT;
ALTER TABLE PortForwardingRuleVO ADD CONSTRAINT fkPortForwardingRuleVOVmNicVO FOREIGN KEY (vmNicUuid) REFERENCES VmNicVO (uuid) ON DELETE SET NULL;

# Foreign keys for table PrimaryStorageCapacityVO

ALTER TABLE PrimaryStorageCapacityVO ADD CONSTRAINT fkPrimaryStorageCapacityVOPrimaryStorageEO FOREIGN KEY (uuid) REFERENCES PrimaryStorageEO (uuid) ON DELETE CASCADE;

# Foreign keys for table PrimaryStorageClusterRefVO

ALTER TABLE PrimaryStorageClusterRefVO ADD CONSTRAINT fkPrimaryStorageClusterRefVOClusterEO FOREIGN KEY (clusterUuid) REFERENCES ClusterEO (uuid) ON DELETE CASCADE;
ALTER TABLE PrimaryStorageClusterRefVO ADD CONSTRAINT fkPrimaryStorageClusterRefVOPrimaryStorageEO FOREIGN KEY (primaryStorageUuid) REFERENCES PrimaryStorageEO (uuid) ON DELETE CASCADE;

# Foreign keys for table PrimaryStorageEO

ALTER TABLE PrimaryStorageEO ADD CONSTRAINT fkPrimaryStorageEOZoneEO FOREIGN KEY (zoneUuid) REFERENCES ZoneEO (uuid) ON DELETE RESTRICT;

# Foreign keys for table SecurityGroupFailureHostVO

ALTER TABLE SecurityGroupFailureHostVO ADD CONSTRAINT fkSecurityGroupFailureHostVOHostEO FOREIGN KEY (hostUuid) REFERENCES HostEO (uuid) ON DELETE CASCADE;
ALTER TABLE SecurityGroupFailureHostVO ADD CONSTRAINT fkSecurityGroupFailureHostVOManagementNodeVO FOREIGN KEY (managementNodeId) REFERENCES ManagementNodeVO (uuid) ON DELETE SET NULL;

# Foreign keys for table SecurityGroupL3NetworkRefVO

ALTER TABLE SecurityGroupL3NetworkRefVO ADD CONSTRAINT fkSecurityGroupL3NetworkRefVOL3NetworkEO FOREIGN KEY (l3NetworkUuid) REFERENCES L3NetworkEO (uuid) ON DELETE CASCADE;
ALTER TABLE SecurityGroupL3NetworkRefVO ADD CONSTRAINT fkSecurityGroupL3NetworkRefVOSecurityGroupVO FOREIGN KEY (securityGroupUuid) REFERENCES SecurityGroupVO (uuid) ON DELETE CASCADE;

# Foreign keys for table SecurityGroupRuleVO

ALTER TABLE SecurityGroupRuleVO ADD CONSTRAINT fkSecurityGroupRuleVOSecurityGroupVO FOREIGN KEY (securityGroupUuid) REFERENCES SecurityGroupVO (uuid) ON DELETE CASCADE;

# Foreign keys for table SftpBackupStorageVO

ALTER TABLE SftpBackupStorageVO ADD CONSTRAINT fkSftpBackupStorageVOBackupStorageEO FOREIGN KEY (uuid) REFERENCES BackupStorageEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;

# Foreign keys for table SimulatorHostVO

ALTER TABLE SimulatorHostVO ADD CONSTRAINT fkSimulatorHostVOHostEO FOREIGN KEY (uuid) REFERENCES HostEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;

# Foreign keys for table UsedIpVO

ALTER TABLE UsedIpVO ADD CONSTRAINT fkUsedIpVOIpRangeEO FOREIGN KEY (ipRangeUuid) REFERENCES IpRangeEO (uuid) ON DELETE CASCADE;
ALTER TABLE UsedIpVO ADD CONSTRAINT fkUsedIpVOL3NetworkEO FOREIGN KEY (l3NetworkUuid) REFERENCES L3NetworkEO (uuid) ON DELETE CASCADE;

# Foreign keys for table VipVO

ALTER TABLE VipVO ADD CONSTRAINT fkVipVOIpRangeEO FOREIGN KEY (ipRangeUuid) REFERENCES IpRangeEO (uuid) ON DELETE CASCADE;
ALTER TABLE VipVO ADD CONSTRAINT fkVipVOL3NetworkEO FOREIGN KEY (l3NetworkUuid) REFERENCES L3NetworkEO (uuid) ON DELETE CASCADE;
ALTER TABLE VipVO ADD CONSTRAINT fkVipVOL3NetworkEO1 FOREIGN KEY (peerL3NetworkUuid) REFERENCES L3NetworkEO (uuid) ON DELETE CASCADE;

# Foreign keys for table VirtualRouterBootstrapIsoVO

ALTER TABLE VirtualRouterBootstrapIsoVO ADD CONSTRAINT fkVirtualRouterBootstrapIsoVOVmInstanceEO FOREIGN KEY (virtualRouterUuid) REFERENCES VmInstanceEO (uuid) ON DELETE CASCADE;

# Foreign keys for table VirtualRouterEipRefVO

ALTER TABLE VirtualRouterEipRefVO ADD CONSTRAINT fkVirtualRouterEipRefVOEipVO FOREIGN KEY (eipUuid) REFERENCES EipVO (uuid) ON DELETE RESTRICT;
ALTER TABLE VirtualRouterEipRefVO ADD CONSTRAINT fkVirtualRouterEipRefVOVmInstanceEO FOREIGN KEY (virtualRouterVmUuid) REFERENCES VmInstanceEO (uuid) ON DELETE CASCADE;

# Foreign keys for table VirtualRouterOfferingVO

ALTER TABLE VirtualRouterOfferingVO ADD CONSTRAINT fkVirtualRouterOfferingVOImageEO FOREIGN KEY (imageUuid) REFERENCES ImageEO (uuid) ON DELETE CASCADE;
ALTER TABLE VirtualRouterOfferingVO ADD CONSTRAINT fkVirtualRouterOfferingVOInstanceOfferingEO FOREIGN KEY (uuid) REFERENCES InstanceOfferingEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;
ALTER TABLE VirtualRouterOfferingVO ADD CONSTRAINT fkVirtualRouterOfferingVOL3NetworkEO FOREIGN KEY (managementNetworkUuid) REFERENCES L3NetworkEO (uuid) ON DELETE CASCADE;
ALTER TABLE VirtualRouterOfferingVO ADD CONSTRAINT fkVirtualRouterOfferingVOL3NetworkEO1 FOREIGN KEY (publicNetworkUuid) REFERENCES L3NetworkEO (uuid) ON DELETE CASCADE;
ALTER TABLE VirtualRouterOfferingVO ADD CONSTRAINT fkVirtualRouterOfferingVOZoneEO FOREIGN KEY (zoneUuid) REFERENCES ZoneEO (uuid) ON DELETE CASCADE;

# Foreign keys for table VirtualRouterPortForwardingRuleRefVO

ALTER TABLE VirtualRouterPortForwardingRuleRefVO ADD CONSTRAINT fkVirtualRouterPortForwardingRuleRefVOVipVO FOREIGN KEY (vipUuid) REFERENCES VipVO (uuid) ON DELETE CASCADE;
ALTER TABLE VirtualRouterPortForwardingRuleRefVO ADD CONSTRAINT fkVirtualRouterPortForwardingRuleRefVOVmInstanceEO FOREIGN KEY (virtualRouterVmUuid) REFERENCES VmInstanceEO (uuid) ON DELETE CASCADE;

# Foreign keys for table VirtualRouterVipVO

ALTER TABLE VirtualRouterVipVO ADD CONSTRAINT fkVirtualRouterVipVOVipVO FOREIGN KEY (uuid) REFERENCES VipVO (uuid) ON DELETE RESTRICT;
ALTER TABLE VirtualRouterVipVO ADD CONSTRAINT fkVirtualRouterVipVOVmInstanceEO FOREIGN KEY (virtualRouterVmUuid) REFERENCES VmInstanceEO (uuid) ON DELETE CASCADE;

# Foreign keys for table VirtualRouterVmVO

ALTER TABLE VirtualRouterVmVO ADD CONSTRAINT fkVirtualRouterVmVOVmInstanceEO FOREIGN KEY (uuid) REFERENCES VmInstanceEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;

# Foreign keys for table VmInstanceEO

ALTER TABLE VmInstanceEO ADD CONSTRAINT fkVmInstanceEOClusterEO FOREIGN KEY (clusterUuid) REFERENCES ClusterEO (uuid) ON DELETE SET NULL;
ALTER TABLE VmInstanceEO ADD CONSTRAINT fkVmInstanceEOHostEO FOREIGN KEY (hostUuid) REFERENCES HostEO (uuid) ON DELETE SET NULL;
ALTER TABLE VmInstanceEO ADD CONSTRAINT fkVmInstanceEOHostEO1 FOREIGN KEY (lastHostUuid) REFERENCES HostEO (uuid) ON DELETE SET NULL;
ALTER TABLE VmInstanceEO ADD CONSTRAINT fkVmInstanceEOImageEO FOREIGN KEY (imageUuid) REFERENCES ImageEO (uuid) ON DELETE RESTRICT;
ALTER TABLE VmInstanceEO ADD CONSTRAINT fkVmInstanceEOInstanceOfferingEO FOREIGN KEY (instanceOfferingUuid) REFERENCES InstanceOfferingEO (uuid) ON DELETE RESTRICT;
ALTER TABLE VmInstanceEO ADD CONSTRAINT fkVmInstanceEOZoneEO FOREIGN KEY (zoneUuid) REFERENCES ZoneEO (uuid) ON DELETE SET NULL;

# Foreign keys for table VmNicSecurityGroupRefVO

ALTER TABLE VmNicSecurityGroupRefVO ADD CONSTRAINT fkVmNicSecurityGroupRefVOSecurityGroupVO FOREIGN KEY (securityGroupUuid) REFERENCES SecurityGroupVO (uuid) ON DELETE CASCADE;
ALTER TABLE VmNicSecurityGroupRefVO ADD CONSTRAINT fkVmNicSecurityGroupRefVOVmInstanceEO FOREIGN KEY (vmInstanceUuid) REFERENCES VmInstanceEO (uuid) ON DELETE CASCADE;
ALTER TABLE VmNicSecurityGroupRefVO ADD CONSTRAINT fkVmNicSecurityGroupRefVOVmNicVO FOREIGN KEY (vmNicUuid) REFERENCES VmNicVO (uuid) ON DELETE CASCADE;

# Foreign keys for table VmNicVO

ALTER TABLE VmNicVO ADD CONSTRAINT fkVmNicVOL3NetworkEO FOREIGN KEY (l3NetworkUuid) REFERENCES L3NetworkEO (uuid) ON DELETE SET NULL;
ALTER TABLE VmNicVO ADD CONSTRAINT fkVmNicVOUsedIpVO FOREIGN KEY (usedIpUuid) REFERENCES UsedIpVO (uuid) ON DELETE SET NULL;
ALTER TABLE VmNicVO ADD CONSTRAINT fkVmNicVOVmInstanceEO FOREIGN KEY (vmInstanceUuid) REFERENCES VmInstanceEO (uuid) ON DELETE CASCADE;

# Foreign keys for table VolumeEO

ALTER TABLE VolumeEO ADD CONSTRAINT fkVolumeEODiskOfferingEO FOREIGN KEY (diskOfferingUuid) REFERENCES DiskOfferingEO (uuid) ON DELETE RESTRICT;
ALTER TABLE VolumeEO ADD CONSTRAINT fkVolumeEOImageEO FOREIGN KEY (rootImageUuid) REFERENCES ImageEO (uuid) ON DELETE SET NULL;
ALTER TABLE VolumeEO ADD CONSTRAINT fkVolumeEOPrimaryStorageEO FOREIGN KEY (primaryStorageUuid) REFERENCES PrimaryStorageEO (uuid) ON DELETE CASCADE;
ALTER TABLE VolumeEO ADD CONSTRAINT fkVolumeEOVmInstanceEO FOREIGN KEY (vmInstanceUuid) REFERENCES VmInstanceEO (uuid) ON DELETE CASCADE;

# Foreign keys for table VolumeSnapshotBackupStorageRefVO

ALTER TABLE VolumeSnapshotBackupStorageRefVO ADD CONSTRAINT fkVolumeSnapshotBackupStorageRefVOBackupStorageEO FOREIGN KEY (backupStorageUuid) REFERENCES BackupStorageEO (uuid) ON DELETE CASCADE;
ALTER TABLE VolumeSnapshotBackupStorageRefVO ADD CONSTRAINT fkVolumeSnapshotBackupStorageRefVOVolumeSnapshotEO FOREIGN KEY (volumeSnapshotUuid) REFERENCES VolumeSnapshotEO (uuid) ON DELETE CASCADE;

# Foreign keys for table VolumeSnapshotEO

ALTER TABLE VolumeSnapshotEO ADD CONSTRAINT fkVolumeSnapshotEOPrimaryStorageEO FOREIGN KEY (primaryStorageUuid) REFERENCES PrimaryStorageEO (uuid) ON DELETE SET NULL;
ALTER TABLE VolumeSnapshotEO ADD CONSTRAINT fkVolumeSnapshotEOVolumeEO FOREIGN KEY (volumeUuid) REFERENCES VolumeEO (uuid) ON DELETE SET NULL;
ALTER TABLE VolumeSnapshotEO ADD CONSTRAINT fkVolumeSnapshotEOVolumeSnapshotEO FOREIGN KEY (parentUuid) REFERENCES VolumeSnapshotEO (uuid) ON DELETE SET NULL;
ALTER TABLE VolumeSnapshotEO ADD CONSTRAINT fkVolumeSnapshotEOVolumeSnapshotTreeEO FOREIGN KEY (treeUuid) REFERENCES VolumeSnapshotTreeEO (uuid) ON DELETE CASCADE;

# Foreign keys for table VolumeSnapshotTreeEO

ALTER TABLE VolumeSnapshotTreeEO ADD CONSTRAINT fkVolumeSnapshotTreeEOVolumeEO FOREIGN KEY (volumeUuid) REFERENCES VolumeEO (uuid) ON DELETE SET NULL;
