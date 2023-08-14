# ssh key pair 表, 记录 ssh 密钥对数据
CREATE TABLE IF NOT EXISTS `zstack`.SshKeyPairVO (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(128) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `publicKey` varchar(4096) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`uuid`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# 记录 ssh key pair 与 vm instance 对应关系
CREATE TABLE IF NOT EXISTS `zstack`.SshKeyPairRefVO (
    `id` int(11) NOT NULL UNIQUE AUTO_INCREMENT,
    `resourceUuid` varchar(32) NOT NULL,
    `sshKeyPairUuid` varchar(32) NOT NULL,
    `resourceType` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    CONSTRAINT `fkSshKeyPairRefVOVmInstanceEO` FOREIGN KEY (`resourceUuid`) REFERENCES `ResourceVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkSshKeyPairRefVOSshKey` FOREIGN KEY (`sshKeyPairUuid`) REFERENCES `SshKeyPairVO` (`uuid`) ON DELETE CASCADE,
    PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;