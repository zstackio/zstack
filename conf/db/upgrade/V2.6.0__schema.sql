SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE AccountResourceRefVO ADD COLUMN `concreteResourceType` varchar(512) NOT NULL;
UPDATE AccountResourceRefVO set concreteResourceType = 'org.zstack.network.l2.vxlan.vxlanNetwork', resourceType = 'L2NetworkVO' WHERE resourceType = 'VxlanNetworkVO';
ALTER TABLE ResourceVO ADD COLUMN `concreteResourceType` varchar(512) NOT NULL;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE `TwoFactorAuthenticationSecretVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `secret` VARCHAR(2048) NOT NULL,
    `resourceUuid` VARCHAR(32) NOT NULL,
    `resourceType` VARCHAR(256) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `CaptchaVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `captcha` text NOT NULL,
    `verifyCode` VARCHAR(32) NOT NULL,
    `targetResourceIdentity` VARCHAR(256) NOT NULL,
    `attempts` int(10) unsigned DEFAULT 0,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `AliyunNasPrimaryStorageMountPointVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `hostUuid` varchar(32) NOT NULL,
    `primaryStorageUuid` varchar(32) NOT NULL,
    `mountUrl` varchar(512) NOT NULL,
    `mountPath` varchar(512) NOT NULL,
    `lastErrInfo` varchar(1024) DEFAULT NULL,
    `checkTimes` bigint unsigned NOT NULL,
    `errorTimes` bigint unsigned DEFAULT 0,
    `lastNormalDistance` bigint unsigned DEFAULT 0,
    `status` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`id`),
    INDEX `idxMountPointVOhostUuid` (`hostUuid`),
    CONSTRAINT `fkMountPointVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES HostEO (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkMountPointVOPrimaryStorageEO` FOREIGN KEY (`primaryStorageUuid`) REFERENCES PrimaryStorageEO (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
