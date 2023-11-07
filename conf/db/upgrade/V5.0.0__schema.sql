ALTER TABLE `SecretResourcePoolVO` ADD COLUMN `ability` varchar(256) NOT NULL DEFAULT 'All';

CREATE TABLE IF NOT EXISTS `zstack`.`JitSecurityMachineVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `port` int unsigned NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkJitSecurityMachineVOSecurityMachineVO FOREIGN KEY (uuid) REFERENCES SecurityMachineVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;