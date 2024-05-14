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