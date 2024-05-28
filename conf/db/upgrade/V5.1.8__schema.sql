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