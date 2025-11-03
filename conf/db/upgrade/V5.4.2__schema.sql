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
    `clusterId` INT DEFAULT NULL,
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
