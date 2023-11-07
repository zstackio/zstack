CREATE TABLE IF NOT EXISTS `zstack`.`DeAnSecretResourcePoolVO` (
    `uuid` varchar(64) NOT NULL UNIQUE,
    `protocol` varchar(64) NOT NULL,
    `managementIp` varchar(64) NOT NULL,
    `port` int unsigned NOT NULL,
    `appId` varchar(64) NOT NULL,
    `sm4Key` varchar(64) NOT NULL,
    `sm3HmacKey` varchar(64) NOT NULL,
    `signCert` text NOT NULL,
    `tsaCert` text NOT NULL,
    `keyNum` varchar(64) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkDeAnSecretResourcePoolVOSecretResourcePoolVO FOREIGN KEY (uuid) REFERENCES SecretResourcePoolVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `SecretResourcePoolVO` ADD COLUMN `ability` varchar(256) NOT NULL DEFAULT 'All';

CREATE TABLE IF NOT EXISTS `zstack`.`JitSecurityMachineVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `port` int unsigned NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkJitSecurityMachineVOSecurityMachineVO FOREIGN KEY (uuid) REFERENCES SecurityMachineVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;