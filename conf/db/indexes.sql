
# Index for table AccountResourceRefVO

CREATE INDEX idxAccountResourceRefVOresourceUuid ON AccountResourceRefVO (resourceUuid);
CREATE INDEX idxAccountResourceRefVOresourceType ON AccountResourceRefVO (resourceType);

# Index for table AccountVO

CREATE INDEX idxAccountVOname ON AccountVO (name);

# Index for table ApplianceVmFirewallRuleVO

CREATE INDEX idxApplianceVmFirewallRuleVOprotocol ON ApplianceVmFirewallRuleVO (protocol);
CREATE INDEX idxApplianceVmFirewallRuleVOstartPort ON ApplianceVmFirewallRuleVO (startPort);
CREATE INDEX idxApplianceVmFirewallRuleVOendPort ON ApplianceVmFirewallRuleVO (endPort);
CREATE INDEX idxApplianceVmFirewallRuleVOallowCidr ON ApplianceVmFirewallRuleVO (allowCidr);
CREATE INDEX idxApplianceVmFirewallRuleVOsourceIp ON ApplianceVmFirewallRuleVO (sourceIp);
CREATE INDEX idxApplianceVmFirewallRuleVOdestIp ON ApplianceVmFirewallRuleVO (destIp);
CREATE INDEX idxApplianceVmFirewallRuleVOidentity ON ApplianceVmFirewallRuleVO (identity);

# Index for table BackupStorageEO

CREATE INDEX idxBackupStorageEOname ON BackupStorageEO (name);

# Index for table ClusterEO

CREATE INDEX idxClusterEOname ON ClusterEO (name);

# Index for table DiskOfferingEO

CREATE INDEX idxDiskOfferingEOname ON DiskOfferingEO (name);

# Index for table EipVO

CREATE INDEX idxEipVOname ON EipVO (name);

# Index for table HostCapacityVO

CREATE INDEX idxHostCapacityVOtotalMemory ON HostCapacityVO (totalMemory);
CREATE INDEX idxHostCapacityVOtotalCpu ON HostCapacityVO (totalCpu);
CREATE INDEX idxHostCapacityVOavailableMemory ON HostCapacityVO (availableMemory);
CREATE INDEX idxHostCapacityVOavailableCpu ON HostCapacityVO (availableCpu);

# Index for table HostEO

CREATE INDEX idxHostEOuuid ON HostEO (uuid);

# Index for table ImageEO

CREATE INDEX idxImageEOname ON ImageEO (name);

# Index for table InstanceOfferingEO

CREATE INDEX idxInstanceOfferingEOname ON InstanceOfferingEO (name);

# Index for table IpRangeEO

CREATE INDEX idxIpRangeEOname ON IpRangeEO (name);
CREATE INDEX idxIpRangeEOstartIp ON IpRangeEO (startIp);
CREATE INDEX idxIpRangeEOendIp ON IpRangeEO (endIp);
CREATE INDEX idxIpRangeEOnetmask ON IpRangeEO (netmask);
CREATE INDEX idxIpRangeEOgateway ON IpRangeEO (gateway);

# Index for table L2NetworkEO

CREATE INDEX idxL2NetworkEOname ON L2NetworkEO (name);

# Index for table L3NetworkEO

CREATE INDEX idxL3NetworkEOname ON L3NetworkEO (name);

# Index for table NetworkServiceProviderVO

CREATE INDEX idxNetworkServiceProviderVOname ON NetworkServiceProviderVO (name);

# Index for table PortForwardingRuleVO

CREATE INDEX idxPortForwardingRuleVOname ON PortForwardingRuleVO (name);
CREATE INDEX idxPortForwardingRuleVOvipPortStart ON PortForwardingRuleVO (vipPortStart);
CREATE INDEX idxPortForwardingRuleVOvipPortEnd ON PortForwardingRuleVO (vipPortEnd);
CREATE INDEX idxPortForwardingRuleVOprivatePortStart ON PortForwardingRuleVO (privatePortStart);
CREATE INDEX idxPortForwardingRuleVOprivatePortEnd ON PortForwardingRuleVO (privatePortEnd);

# Index for table PrimaryStorageCapacityVO

CREATE INDEX idxPrimaryStorageCapacityVOtotalCapacity ON PrimaryStorageCapacityVO (totalCapacity);
CREATE INDEX idxPrimaryStorageCapacityVOavailableCapacity ON PrimaryStorageCapacityVO (availableCapacity);

# Index for table SecurityGroupVO

CREATE INDEX idxSecurityGroupVOname ON SecurityGroupVO (name);

# Index for table SystemTagVO

CREATE INDEX idxSystemTagVOresourceUuid ON SystemTagVO (resourceUuid);
CREATE INDEX idxSystemTagVOresourceType ON SystemTagVO (resourceType);
CREATE INDEX idxSystemTagVOtag ON SystemTagVO (tag(128));
CREATE INDEX idxSystemTagVOtype ON SystemTagVO (type);

# Index for table UsedIpVO

CREATE INDEX idxUsedIpVOip ON UsedIpVO (ip);
CREATE INDEX idxUsedIpVOipInLong ON UsedIpVO (ipInLong);

# Index for table UserTagVO

CREATE INDEX idxUserTagVOresourceUuid ON UserTagVO (resourceUuid);
CREATE INDEX idxUserTagVOresourceType ON UserTagVO (resourceType);
CREATE INDEX idxUserTagVOtag ON UserTagVO (tag(128));
CREATE INDEX idxUserTagVOtype ON UserTagVO (type);

# Index for table VipVO

CREATE INDEX idxVipVOname ON VipVO (name);
CREATE INDEX idxVipVOip ON VipVO (ip);

# Index for table VmInstanceEO

CREATE INDEX idxVmInstanceEOname ON VmInstanceEO (name(128));

# Index for table VmNicVO

CREATE INDEX idxVmNicVOip ON VmNicVO (ip);
CREATE INDEX idxVmNicVOmac ON VmNicVO (mac);

# Index for table VolumeEO

CREATE INDEX idxVolumeEOname ON VolumeEO (name);

# Index for table VolumeSnapshotEO

CREATE INDEX idxVolumeSnapshotEOname ON VolumeSnapshotEO (name);

# Index for table ZoneEO

CREATE INDEX idxZoneEOname ON ZoneEO (name);
