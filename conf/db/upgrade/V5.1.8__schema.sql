ALTER TABLE `zstack`.`AuditsVO` ADD COLUMN `startTime` bigint(20);

CREATE INDEX idx_startTime ON AuditsVO (startTime);
CREATE INDEX id_id_resourceType ON AuditsVO (id, resourceType);
CREATE INDEX idx_id_resourceType_startTime ON AuditsVO (id, resourceType, startTime);

UPDATE AuditsVO set startTime = createTime WHERE startTime IS NULL;

CREATE TABLE IF NOT EXISTS `zstack`.`SanSecSecretResourcePoolVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `keyIndex` varchar(128) DEFAULT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkSanSecSecretResourcePoolVOSecretResourcePoolVO FOREIGN KEY (uuid) REFERENCES SecretResourcePoolVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`SanSecSecurityMachineVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `port` int unsigned NOT NULL,
    `password` varchar(255) DEFAULT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkSanSecurityMachineVOSecurityMachineVO FOREIGN KEY (uuid) REFERENCES SecurityMachineVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`FiSecSecretResourcePoolVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `keyNum` varchar(128) DEFAULT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkFiSecSecretResourcePoolVOSecretResourcePoolVO FOREIGN KEY (uuid) REFERENCES SecretResourcePoolVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`FiSecSecurityMachineVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `port` int unsigned NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkFiSecSecurityMachineVOSecurityMachineVO FOREIGN KEY (uuid) REFERENCES SecurityMachineVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`CSPSecretResourcePoolVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `managementIp` varchar(32) NOT NULL,
    `port` int unsigned NOT NULL,
    `appId` varchar(128) NOT NULL,
    `appKey` varchar(128) NOT NULL,
    `keyId` varchar(128) NOT NULL,
    `userId` varchar(128) DEFAULT NULL,
    `protocol` varchar(8) DEFAULT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkCSPSecretResourcePoolVOSecretResourcePoolVO FOREIGN KEY (uuid) REFERENCES SecretResourcePoolVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`SNSApplicationEndpointVO` ADD COLUMN `connectionStatus` varchar(10) DEFAULT 'UP' COMMENT 'UP or DOWN';

CREATE TABLE IF NOT EXISTS `zstack`.`SNSUniversalSmsEndpointVO`
(
    `uuid`               varchar(32)  NOT NULL UNIQUE,
    `smsAccessKeyId`     varchar(128) NOT NULL,
    `smsAccessKeySecret` varchar(128) NOT NULL,
    `supplier`           varchar(32)  NOT NULL,
    PRIMARY KEY (`uuid`),
    CONSTRAINT fkSNSUniversalSmsEndpointVOSNSApplicationEndpointVO FOREIGN KEY (uuid) REFERENCES SNSApplicationEndpointVO (uuid) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`SNSEmaySmsEndpointVO`
(
    `uuid`       varchar(32)  NOT NULL UNIQUE,
    `requestUrl` varchar(128) NOT NULL,
    PRIMARY KEY (`uuid`),
    CONSTRAINT fkSNSEmaySmsEndpointVOSNSApplicationEndpointVO FOREIGN KEY (uuid) REFERENCES SNSApplicationEndpointVO (uuid) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`SNSSmsEndpointVO` RENAME TO `zstack`.`SNSAliyunSmsEndpointVO`;
ALTER TABLE `zstack`.`SNSAliyunSmsEndpointVO` DROP FOREIGN KEY fkSNSSmsEndpointVOSNSApplicationEndpointVO;
ALTER TABLE `zstack`.`SNSAliyunSmsEndpointVO`
    ADD CONSTRAINT fkSNSAliyunSmsEndpointVOSNSApplicationEndpointVO FOREIGN KEY (uuid) REFERENCES `zstack`.`SNSApplicationEndpointVO` (uuid) ON DELETE CASCADE;
ALTER TABLE `zstack`.`SNSSmsReceiverVO` DROP FOREIGN KEY fkSNSSmsReceiverVOSNSSmsEndpointVO;
ALTER TABLE `zstack`.`SNSSmsReceiverVO`
    ADD CONSTRAINT fkSNSSmsReceiverVOSNSUniversalSmsEndpointVO FOREIGN KEY (endpointUuid) REFERENCES `zstack`.`SNSUniversalSmsEndpointVO` (uuid) ON DELETE CASCADE;

DROP PROCEDURE IF EXISTS UpgradeSNSAliyunSmsEndpointVO;
DELIMITER $$
CREATE PROCEDURE UpgradeSNSAliyunSmsEndpointVO()
BEGIN
    IF (SELECT COUNT(*) FROM SNSUniversalSmsEndpointVO u JOIN SNSAliyunSmsEndpointVO a ON u.uuid = a.uuid) = 0 THEN
        INSERT INTO SNSUniversalSmsEndpointVO (uuid, smsAccessKeyId, smsAccessKeySecret, supplier) SELECT uuid, '', '', 'Aliyun' FROM SNSAliyunSmsEndpointVO;
END IF;
END $$
DELIMITER ;
CALL UpgradeSNSAliyunSmsEndpointVO();

DROP PROCEDURE IF EXISTS check_and_insert_encrypt_metadata;
DELIMITER $$
CREATE PROCEDURE check_and_insert_encrypt_metadata()
BEGIN
    IF (select count(*) from GlobalConfigVO gconfig where gconfig.name = 'enable.password.encrypt' and gconfig.category = 'encrypt' and value != 'None') > 0 THEN
        UPDATE EncryptEntityMetadataVO SET state = 'NewAdded' WHERE entityName = 'IAM2VirtualIDAttributeVO' AND state = 'Encrypted';
        INSERT INTO EncryptEntityMetadataVO (entityName, columnName, state, lastOpDate, createDate) VALUES ('IAM2OrganizationAttributeVO', 'value', 'NeedDecrypt', NOW(), NOW());
        INSERT INTO EncryptEntityMetadataVO (entityName, columnName, state, lastOpDate, createDate) VALUES ('IAM2ProjectAttributeVO', 'value', 'NeedDecrypt', NOW(), NOW());
        INSERT INTO EncryptEntityMetadataVO (entityName, columnName, state, lastOpDate, createDate) VALUES ('IAM2VirtualIDAttributeVO', 'value', 'NeedDecrypt', NOW(), NOW());
        INSERT INTO EncryptEntityMetadataVO (entityName, columnName, state, lastOpDate, createDate) VALUES ('IAM2VirtualIDGroupAttributeVO', 'value', 'NeedDecrypt', NOW(), NOW());
    END IF;
END $$
DELIMITER ;
CALL check_and_insert_encrypt_metadata();

UPDATE SystemTagVO SET resourceType='SNSAliyunSmsEndpointVO' where resourceType='SNSSmsEndpointVO';


UPDATE IAM2VirtualIDAttributeVO attr JOIN IAM2VirtualIDVO vid ON attr.virtualIDUuid = vid.uuid SET attr.createDate = vid.createDate WHERE attr.createDate = '0000-00-00 00:00:00';
UPDATE IAM2VirtualIDAttributeVO attr JOIN IAM2VirtualIDVO vid ON attr.virtualIDUuid = vid.uuid SET attr.lastOpDate = vid.lastOpDate WHERE attr.lastOpDate = '0000-00-00 00:00:00';

UPDATE IAM2OrganizationAttributeVO attr JOIN IAM2OrganizationVO vid ON attr.organizationUuid = vid.uuid SET attr.createDate = vid.createDate WHERE attr.createDate = '0000-00-00 00:00:00';
UPDATE IAM2OrganizationAttributeVO attr JOIN IAM2OrganizationVO vid ON attr.organizationUuid = vid.uuid SET attr.lastOpDate = vid.lastOpDate WHERE attr.lastOpDate = '0000-00-00 00:00:00';

UPDATE IAM2ProjectAttributeVO attr JOIN IAM2ProjectVO vid ON attr.projectUuid = vid.uuid SET attr.createDate = vid.createDate WHERE attr.createDate = '0000-00-00 00:00:00';
UPDATE IAM2ProjectAttributeVO attr JOIN IAM2ProjectVO vid ON attr.projectUuid = vid.uuid SET attr.lastOpDate = vid.lastOpDate WHERE attr.lastOpDate = '0000-00-00 00:00:00';

UPDATE IAM2VirtualIDGroupAttributeVO attr JOIN IAM2VirtualIDGroupVO vid ON attr.groupUuid = vid.uuid SET attr.createDate = vid.createDate WHERE attr.createDate = '0000-00-00 00:00:00';
UPDATE IAM2VirtualIDGroupAttributeVO attr JOIN IAM2VirtualIDGroupVO vid ON attr.groupUuid = vid.uuid SET attr.lastOpDate = vid.lastOpDate WHERE attr.lastOpDate = '0000-00-00 00:00:00';

CREATE TABLE IF NOT EXISTS `zstack`.`ReservedIpRangeVO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
    `l3NetworkUuid` varchar(32) NOT NULL COMMENT 'l3 network uuid',
    `name` varchar(255) DEFAULT NULL COMMENT 'name',
    `description` varchar(2048) DEFAULT NULL COMMENT 'description',
    `ipVersion` int(10) unsigned DEFAULT 4 COMMENT 'ip range version',
    `startIp` varchar(64) NOT NULL COMMENT 'start ip',
    `endIp` varchar(64) NOT NULL COMMENT 'end ip',
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`PciDeviceVO` ADD `rev` varchar(32) DEFAULT '';

DELETE FROM `zstack`.`ResourceConfigVO` WHERE `category`='sharedblock' AND `name`='qcow2.allocation';

CREATE TABLE IF NOT EXISTS `zstack`.`GpuDeviceVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `serialNumber` varchar(255),
    `memory` bigint unsigned NULL DEFAULT 0,
    `power` bigint unsigned NULL DEFAULT 0,
    `isDriverLoaded` TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkGpuDeviceInfoVOPciDeviceVO` FOREIGN KEY (`uuid`) REFERENCES `PciDeviceVO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CALL ADD_COLUMN('PciDeviceVO', 'vendor', 'VARCHAR(128)', 1, NULL);
CALL ADD_COLUMN('PciDeviceVO', 'device', 'VARCHAR(128)', 1, NULL);
CALL ADD_COLUMN('PciDeviceSpecVO', 'vendor', 'VARCHAR(128)', 1, NULL);
CALL ADD_COLUMN('PciDeviceSpecVO', 'device', 'VARCHAR(128)', 1, NULL);
CALL ADD_COLUMN('MdevDeviceVO', 'vendor', 'VARCHAR(128)', 1, NULL);

DROP PROCEDURE IF EXISTS `MdevDeviceAddVendor`;
DELIMITER $$
CREATE PROCEDURE MdevDeviceAddVendor()
    BEGIN
        DECLARE vendor VARCHAR(128);
        DECLARE pciDeviceUuid VARCHAR(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE cur CURSOR FOR SELECT pci.uuid, pci.vendor FROM PciDeviceVO pci;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO vendor, pciDeviceUuid;
            IF done THEN
                LEAVE read_loop;
            END IF;
            UPDATE MdevDeviceVO SET vendor = vendor WHERE parentUuid = pciDeviceUuid;
        END LOOP;
        CLOSE cur;
        SELECT CURTIME();
    END $$
DELIMITER ;
call MdevDeviceAddVendor;
DROP PROCEDURE IF EXISTS `MdevDeviceAddVendor`;

CREATE TABLE IF NOT EXISTS `HostHwMonitorStatusVO`
(
    `uuid` varchar(32)  NOT NULL UNIQUE,
    `cpuStatus` varchar(32) NOT NULL,
    `memoryStatus` varchar(32) NOT NULL,
    `diskStatus` varchar(32) NOT NULL,
    `nicStatus` varchar(32) NOT NULL,
    `gpuStatus` varchar(32) NOT NULL,
    `powerSupplyStatus` varchar(32) NOT NULL,
    `fanStatus` varchar(32) NOT NULL,
    `raidStatus` varchar(32) NOT NULL,
    `temperatureStatus` varchar(32) NOT NULL,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkHostHwMonitorStatusVO` FOREIGN KEY (`uuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`BareMetal2ChassisPciDeviceVO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
    `chassisUuid` varchar(32) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `type` varchar(32) NOT NULL,
    `pciDeviceAddress` varchar(32) NOT NULL,
    `vendorId` varchar(64) NOT NULL,
    `deviceId` varchar(64) NOT NULL,
    `subvendorId` varchar(64) DEFAULT NULL,
    `subdeviceId` varchar(64) DEFAULT NULL,
    `iommuGroup` varchar(255) DEFAULT NULL,
    `name` varchar(255) NOT NULL,
    `vendor` varchar(128) DEFAULT NULL,
    `device` varchar(128) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkBareMetal2ChassisPciDeviceVOChassisVO` FOREIGN KEY (`chassisUuid`) REFERENCES `BareMetal2ChassisVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`BareMetal2ChassisGpuDeviceVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `serialNumber` varchar(255),
    `memory` varchar(255),
    `power` varchar(255),
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkBm2ChassisGpuDeviceVOBm2ChassisPciDeviceVO` FOREIGN KEY (`uuid`) REFERENCES `BareMetal2ChassisPciDeviceVO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP PROCEDURE IF EXISTS `CreateGpuDeviceVO`;
DELIMITER $$
CREATE PROCEDURE CreateGpuDeviceVO()
    BEGIN
        DECLARE uuid VARCHAR(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE cur CURSOR FOR SELECT pci.uuid FROM PciDeviceVO pci where pci.type in ('GPU_Video_Controller', 'GPU_3D_Controller');
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO uuid;
            IF done THEN
                LEAVE read_loop;
            END IF;
            insert into GpuDeviceVO (uuid) values (uuid);
        END LOOP;
        CLOSE cur;
        SELECT CURTIME();
    END $$
DELIMITER ;
call CreateGpuDeviceVO;
DROP PROCEDURE IF EXISTS `CreateGpuDeviceVO`;

DROP PROCEDURE IF EXISTS `addPciDeviceVendor`;
DELIMITER $$
CREATE PROCEDURE addPciDeviceVendor()
    BEGIN
        DECLARE pciUuid VARCHAR(32);
        DECLARE vendorId VARCHAR(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE cur CURSOR FOR SELECT pci.uuid, pci.vendorId FROM PciDeviceVO pci where pci.type in ('GPU_Video_Controller', 'GPU_3D_Controller');
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO pciUuid, vendorId;
            IF done THEN
                LEAVE read_loop;
            END IF;
            IF vendorId = '1d94' then
                update PciDeviceVO set vendor = 'Haiguang' where uuid = pciUuid;
            ELSEIF vendorId = '10de' then
                update PciDeviceVO set vendor = 'NVIDIA' where uuid = pciUuid;
            ELSEIF vendorId = '8086' then
                update PciDeviceVO set vendor = 'AMD' where uuid = pciUuid;
            END IF;
        END LOOP;
        CLOSE cur;
        SELECT CURTIME();
    END $$
DELIMITER ;
call addPciDeviceVendor;
DROP PROCEDURE IF EXISTS `addPciDeviceVendor`;
