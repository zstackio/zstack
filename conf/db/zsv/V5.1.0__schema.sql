-- Volume LUKS encryption flag (API opt-in + EncryptedResourceKeyRefVO binding)
-- VmInstanceEO.vmEncryption: VM sensitive system-tag encryption (consolePassword / sshkey / userdata)

ALTER TABLE `zstack`.`VolumeEO` ADD COLUMN `encrypted` tinyint(1) NOT NULL DEFAULT 0;
ALTER TABLE `zstack`.`VolumeSnapshotEO` ADD COLUMN `encrypted` tinyint(1) NOT NULL DEFAULT 0;
ALTER TABLE `zstack`.`VolumeBackupVO` ADD COLUMN `encrypted` tinyint(1) NOT NULL DEFAULT 0;
ALTER TABLE `zstack`.`VmInstanceEO` ADD COLUMN `vmEncryption` tinyint(1) NOT NULL DEFAULT 0;
ALTER TABLE `zstack`.`LunVO` ADD COLUMN `encrypted` tinyint(1) NOT NULL DEFAULT 0;
ALTER TABLE `zstack`.`ScsiLunVmInstanceRefVO` ADD COLUMN `encrypted` tinyint(1) NOT NULL DEFAULT 0;

UPDATE `zstack`.`VolumeBackupVO` vb
SET vb.`encrypted` = 1
WHERE EXISTS (
    SELECT 1
    FROM `zstack`.`EncryptedResourceKeyRefVO` keyRef
    WHERE keyRef.`resourceType` = 'VolumeBackupVO'
      AND keyRef.`resourceUuid` = vb.`uuid`
);

DROP VIEW IF EXISTS `zstack`.`VolumeVO`;
CREATE VIEW `zstack`.`VolumeVO` AS
SELECT uuid, name, description, primaryStorageUuid, vmInstanceUuid, diskOfferingUuid,
       rootImageUuid, installPath, type, status, size, actualSize, deviceId, format, state, createDate, lastOpDate,
       isShareable, volumeQos, lastVmInstanceUuid, lastDetachDate, lastAttachDate, protocol, encrypted
FROM `zstack`.`VolumeEO`
WHERE deleted IS NULL;

DROP VIEW IF EXISTS `zstack`.`VolumeSnapshotVO`;
CREATE VIEW `zstack`.`VolumeSnapshotVO` AS
SELECT uuid, name, description, type, volumeUuid, format, treeUuid, parentUuid,
       primaryStorageUuid, primaryStorageInstallPath, distance, size, latest,
       fullSnapshot, encrypted, volumeType, state, status, createDate, lastOpDate
FROM `zstack`.`VolumeSnapshotEO`
WHERE deleted IS NULL;

DROP VIEW IF EXISTS `zstack`.`ScsiLunVO`;
CREATE VIEW `zstack`.`ScsiLunVO` AS
SELECT uuid, name, wwid, vendor, model, wwn, serial, type, hctl, path, size,
       state, source, multipathDeviceUuid, encrypted, createDate, lastOpDate
FROM `zstack`.`LunVO`
WHERE source IN ('iSCSI', 'fiberChannel');

DROP VIEW IF EXISTS `zstack`.`VmInstanceVO`;
CREATE VIEW `zstack`.`VmInstanceVO` AS
SELECT uuid, name, description, zoneUuid, clusterUuid, imageUuid, hostUuid, internalId,
       lastHostUuid, instanceOfferingUuid, rootVolumeUuid, defaultL3NetworkUuid, type,
       hypervisorType, cpuNum, cpuSpeed, memorySize, reservedMemorySize, platform,
       guestOsType, allocatorStrategy, createDate, lastOpDate, state, architecture, vmEncryption
FROM `zstack`.`VmInstanceEO`
WHERE deleted IS NULL;

-- Host PKI

CREATE TABLE IF NOT EXISTS `zstack`.`PkiCaVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `scope` varchar(64) NOT NULL,
    `caType` varchar(32) NOT NULL,
    `subjectDn` varchar(512) DEFAULT NULL,
    `certChainPem` mediumtext NOT NULL,
    `encryptedPrivateKeyPem` mediumtext DEFAULT NULL,
    `serial` varchar(128) DEFAULT NULL,
    `fingerprint` varchar(128) DEFAULT NULL,
    `status` varchar(32) NOT NULL,
    `crlPem` mediumtext DEFAULT NULL,
    `notBefore` timestamp NULL DEFAULT NULL,
    `notAfter` timestamp NULL DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `ukPkiCaScopeType` (`scope`, `caType`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`HostCertificateVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `hostUuid` varchar(32) NOT NULL,
    `caUuid` varchar(32) NOT NULL,
    `certUsage` varchar(64) NOT NULL,
    `serial` varchar(128) DEFAULT NULL,
    `fingerprint` varchar(128) DEFAULT NULL,
    `sanSnapshot` varchar(2048) DEFAULT NULL,
    `status` varchar(32) NOT NULL,
    `notBefore` timestamp NULL DEFAULT NULL,
    `notAfter` timestamp NULL DEFAULT NULL,
    `lastInstallDate` timestamp NULL DEFAULT NULL,
    `lastError` text DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `ukHostCertificateHostUsage` (`hostUuid`, `certUsage`),
    KEY `idxHostCertificateCaUuid` (`caUuid`),
    CONSTRAINT `fkHostCertificateHost` FOREIGN KEY (`hostUuid`) REFERENCES `zstack`.`HostEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkHostCertificateCa` FOREIGN KEY (`caUuid`) REFERENCES `zstack`.`PkiCaVO` (`uuid`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Key provider / scanner alarm state (persists ACTIVE/INACTIVE across MN restart)

CREATE TABLE IF NOT EXISTS `zstack`.`ScannerAlarmStateVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `scannerName` varchar(255) NOT NULL,
    `alarmType` varchar(255) NOT NULL,
    `alarmKey` varchar(255) NOT NULL,
    `state` varchar(32) NOT NULL,
    `lastUpdateTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `ukScannerAlarmState` (`scannerName`, `alarmType`, `alarmKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
