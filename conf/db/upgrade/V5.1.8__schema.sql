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