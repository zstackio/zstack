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

ALTER TABLE `zstack`.`SNSApplicationEndpointVO` ADD COLUMN `connectionStatus` varchar(10) DEFAULT 'UP' COMMENT 'UP or DOWN';