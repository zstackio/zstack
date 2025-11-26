ALTER TABLE `zstack`.`MdevDeviceSpecVO` modify column name varchar(128) NOT NULL;
ALTER TABLE `zstack`.`MdevDeviceVO` modify column name varchar(128) NOT NULL;

CALL ADD_COLUMN('ModelVO', 'framework', 'varchar(255)', 1, NULL);
CALL ADD_COLUMN('ModelVO', 'versionSemver', 'varchar(255)', 1, NULL);
CALL ADD_COLUMN('ModelVO', 'isLatestVersion', 'tinyint(1)', 1, '0');
CALL ADD_COLUMN('ModelVO', 'artifactChecksum', 'varchar(255)', 1, NULL);
CALL ADD_COLUMN('ModelVO', 'artifactSizeBytes', 'bigint', 1, '0');
CALL ADD_COLUMN('ModelVO', 'architectureType', 'varchar(255)', 1, NULL);
CALL ADD_COLUMN('ModelVO', 'frameworkVersion', 'varchar(255)', 1, NULL);
CALL ADD_COLUMN('ModelVO', 'requiredAccelerator', 'varchar(255)', 1, NULL);
CALL ADD_COLUMN('ModelVO', 'quantizationType', 'varchar(255)', 1, NULL);

CALL RENAME_TABLE('ModelServiceImageVO', 'ModelServiceTemplateVO');

CALL ADD_COLUMN('ModelServiceTemplateVO', 'pythonVersionSemver', 'varchar(255)', 1, NULL);
CALL ADD_COLUMN('ModelServiceTemplateVO', 'cudaVersion', 'varchar(255)', 1, NULL);
CALL ADD_COLUMN('ModelServiceTemplateVO', 'cannVersion', 'varchar(255)', 1, NULL);
CALL ADD_COLUMN('ModelServiceTemplateVO', 'frameworkVersionSemver', 'varchar(255)', 1, NULL);
CALL ADD_COLUMN('ModelServiceTemplateVO', 'gpuVendor', 'varchar(255)', 1, NULL);

CALL ADD_COLUMN('PodVO', 'namespace', 'varchar(64)', 1, NULL);

CREATE TABLE IF NOT EXISTS `zstack`.`KubernetesServiceVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(64) NOT NULL,
    `description` varchar(255) DEFAULT NULL,
    `namespace` varchar(64) NOT NULL,
    `type` varchar(20) NOT NULL,
    `clusterIp` varchar(64) DEFAULT NULL,
    `externalIp` varchar(64) DEFAULT NULL,
    `ports` text,
    `endpointUuid` varchar(32) NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CALL ADD_COLUMN('PodVO', 'clusterId', 'INT', 1, NULL);

DELETE FROM `AccountResourceRefVO`
WHERE `concreteResourceType` = 'org.zstack.header.vm.VmInstanceVO'
  AND `resourceUuid` NOT IN (SELECT `uuid` FROM `VmInstanceVO`);

CALL DROP_COLUMN('ModelCenterCapacityVO', 'installationUsedCapacity');
CALL ADD_COLUMN('NativeClusterVO', 'status', 'varchar(32)', 1, NULL);

UPDATE `NativeClusterVO` SET `status` = 'Status_Cluster_Running' WHERE `status` IS NULL;

CREATE TABLE IF NOT EXISTS `zstack`.`ModelServiceGpuVendorSpecRefVO` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `refUuid` bigint UNSIGNED NOT NULL,
    `specUuid` varchar(32) NOT NULL,
    CONSTRAINT `pkModelServiceGpuVendorSpecRef` PRIMARY KEY (`id`),
    CONSTRAINT `ukModelServiceGpuVendorSpecRefRefSpec` UNIQUE (`refUuid`, `specUuid`),
    CONSTRAINT `fkModelServiceGpuVendorSpecRefRefUuid` FOREIGN KEY (`refUuid`)
            REFERENCES `ModelServiceGpuVendorVO`(`id`)
            ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`NfvInstOfferingVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `managementNetworkUuid` varchar(32) NOT NULL,
    `imageUuid` varchar(32) NOT NULL,
    `zoneUuid` varchar(32) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkNfvInstOfferingVOL3NetworkEO FOREIGN KEY (managementNetworkUuid) REFERENCES `zstack`.`L3NetworkEO` (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`NfvInstGroupVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `nfvInstOfferingUuid` VARCHAR(32) DEFAULT NULL,
    `instType` VARCHAR(64) NOT NULL,
    `funcType` VARCHAR(64) NOT NULL,
    `configVersion` int unsigned DEFAULT 0,
    `netOsDistro` VARCHAR(128) DEFAULT NULL,
    `baseOsDistro` VARCHAR(128) DEFAULT NULL,
    `status` VARCHAR(32) DEFAULT 'Initializing',
    `statusDetail` VARCHAR(255) DEFAULT NULL,
    `operationMode` VARCHAR(32) DEFAULT 'Normal',
    `vipUuid` VARCHAR(32) DEFAULT NULL,
    `ipv6VipUuid` VARCHAR(32) DEFAULT NULL,
    `primaryStorageUuid` VARCHAR(32) DEFAULT NULL,
    `primaryStoragePoolUuid` VARCHAR(32) DEFAULT NULL,
    `clusterUuid` VARCHAR(32) DEFAULT NULL,
    `zoneUuid` VARCHAR(32) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkNfvInstGroupVONfvInstOfferingVO` FOREIGN KEY (`nfvInstOfferingUuid`) REFERENCES `zstack`.`NfvInstOfferingVO` (uuid) ON DELETE SET NULL,
    CONSTRAINT `fkNfvInstGroupVOPrimaryStorageEO` FOREIGN KEY (`primaryStorageUuid`) REFERENCES `zstack`.`PrimaryStorageEO` (uuid) ON DELETE SET NULL,
    CONSTRAINT `fkNfvInstGroupVOClusterEO` FOREIGN KEY (`clusterUuid`) REFERENCES `zstack`.`ClusterEO` (uuid) ON DELETE SET NULL,
    CONSTRAINT `fkNfvInstGroupVOZoneEO` FOREIGN KEY (`zoneUuid`) REFERENCES `zstack`.`ZoneEO` (uuid) ON DELETE SET NULL,
    KEY `idx_nfv_inst_group_status_mode` (`status`, `operationMode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`NfvInstGroupMonitorIpVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `nfvInstGroupUuid` varchar(32) NOT NULL,
    `monitorIp` varchar(255) NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
     PRIMARY KEY (`id`),
     CONSTRAINT fkNfvInstGroupMonitorIpVONfvInstGroupVO FOREIGN KEY (nfvInstGroupUuid) REFERENCES NfvInstGroupVO (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`NfvInstGroupNetworkServiceRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `nfvInstGroupUuid` varchar(32) NOT NULL,
    `networkServiceName` VARCHAR(255) NOT NULL,
    `networkServiceUuid` varchar(32) NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT fkNfvInstGroupNetworkServiceRefVONfvInstGroupVO FOREIGN KEY (nfvInstGroupUuid) REFERENCES NfvInstGroupVO (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`NfvInstGroupL3NetworkRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `nfvInstGroupUuid` varchar(32) NOT NULL,
    `networkServiceUuid` VARCHAR(32) NOT NULL,
    `l3NetworkUuid` varchar(32) NOT NULL,
    `type` varchar(255) NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT fkNfvInstGroupL3NetworkRefVONfvInstGroupVO FOREIGN KEY (nfvInstGroupUuid) REFERENCES NfvInstGroupVO (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`NfvInstVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `nfvInstGroupUuid` varchar(32) NOT NULL,
    `configVersion` int unsigned NOT NULL DEFAULT 0,
    `netOsDistro` VARCHAR(128) NOT NULL,
    `baseOsDistro` VARCHAR(128) NOT NULL,
    `clusterStatus` VARCHAR(32) DEFAULT 'Unknown',
    `statusDetail` VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkNfvInstVONfvInstGroupVO FOREIGN KEY (nfvInstGroupUuid) REFERENCES NfvInstGroupVO (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`NfvInstMetaDataVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `agentVersion` varchar(32) DEFAULT NULL,
    `netOsVersion` varchar(32) DEFAULT NULL,
    `baseOsVersion` varchar(32) DEFAULT NULL,
    `kernelVersion` varchar(256) DEFAULT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkNfvInstMetadataVONfvInstVO` FOREIGN KEY (`uuid`) REFERENCES `NfvInstVO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`NfvInstGroupConfigTaskVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `nfvInstGroupUuid` varchar(32) NOT NULL,
    `configVersion` int unsigned DEFAULT 0,
    `serviceUuid` varchar(32) NOT NULL,
    `taskName` VARCHAR(255) NOT NULL,
    `path` VARCHAR(255) NOT NULL,
    `taskData` text DEFAULT NULL,
    `checkStatus` BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (`id`),
    CONSTRAINT fkNfvInstGroupConfigTaskVONfvInstGroupVO FOREIGN KEY (nfvInstGroupUuid) REFERENCES NfvInstGroupVO (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CALL ADD_COLUMN('OvnControllerVmInstanceVO', 'nbClusterStatus', 'VARCHAR(32)', 1, 'Unknown');
CALL ADD_COLUMN('OvnControllerVmInstanceVO', 'sbClusterStatus', 'VARCHAR(32)', 1, 'Unknown');

ALTER TABLE OvnControllerVmOfferingVO DROP FOREIGN KEY fkOvnControllerVmOfferingVOL3NetworkEO;
CALL DROP_COLUMN('OvnControllerVmOfferingVO', 'managementNetworkUuid');
CALL DROP_COLUMN('OvnControllerVmOfferingVO', 'imageUuid');
CALL DROP_COLUMN('OvnControllerVmOfferingVO', 'zoneUuid');
