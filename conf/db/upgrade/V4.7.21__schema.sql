CREATE TABLE `NvmeServerVO` (
  `uuid` VARCHAR(32) NOT NULL,
  `name` VARCHAR(256) NOT NULL,
  `ip` VARCHAR(64) NOT NULL,
  `port` int unsigned DEFAULT 4420,
  `transport` VARCHAR(32) NOT NULL,
  `state` VARCHAR(32) NOT NULL,
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`)
)  ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `NvmeServerClusterRefVO` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  `clusterUuid` VARCHAR(32) NOT NULL,
  `nvmeServerUuid` VARCHAR(32) NOT NULL,
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  CONSTRAINT `fkNvmeServerClusterRefVONvmeServerVO` FOREIGN KEY (`nvmeServerUuid`) REFERENCES NvmeServerVO (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkNvmeServerClusterRefVOClusterEO` FOREIGN KEY (`clusterUuid`) REFERENCES ClusterEO (`uuid`) ON DELETE CASCADE
)  ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`NvmeTargetVO` ADD COLUMN `nvmeServerUuid` varchar(32);
ALTER TABLE `zstack`.`NvmeTargetVO` ADD CONSTRAINT `fkNvmeTargetVONvmeServerVO` FOREIGN KEY (nvmeServerUuid) REFERENCES NvmeServerVO(uuid) ON DELETE SET NULL;

ALTER TABLE `zstack`.`VmNicSecurityGroupRefVO`
ADD COLUMN `priority` INT DEFAULT -1 AFTER `securityGroupUuid`;

ALTER TABLE `zstack`.`SecurityGroupRuleVO`
ADD COLUMN `srcPortRange` varchar(255) DEFAULT NULL AFTER `protocol`,
ADD COLUMN `dstPortRange` varchar(255) DEFAULT NULL AFTER `protocol`,
ADD COLUMN `srcIpRange` varchar(1024) DEFAULT NULL AFTER `protocol`,
ADD COLUMN `dstIpRange` varchar(1024) DEFAULT NULL AFTER `protocol`,
ADD COLUMN `description` varchar(255) DEFAULT NULL AFTER `protocol`,
ADD COLUMN `action` varchar(32) NOT NULL DEFAULT 'ACCEPT' AFTER `protocol`,
ADD COLUMN `priority` INT DEFAULT -1 AFTER `protocol`;


CREATE TABLE IF NOT EXISTS `zstack`.`VmNicSecurityPolicyVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `vmNicUuid` varchar(32) NOT NULL,
    `ingressPolicy` varchar(32) NOT NULL DEFAULT 'DENY',
    `egressPolicy` varchar(32) NOT NULL DEFAULT 'ALLOW',
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkVmNicSecurityPolicyVOVmNicVO` FOREIGN KEY (vmNicUuid) REFERENCES VmNicVO (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`OAuth2ClientVO` ADD COLUMN `userinfoUrl` varchar(256) DEFAULT NULL;
ALTER TABLE `zstack`.`SSOClientVO` ADD COLUMN `redirectUrl` varchar(256) DEFAULT NULL;
ALTER TABLE `zstack`.`OAuth2ClientVO` ADD COLUMN `logoutUrl` varchar(256) DEFAULT NULL;
ALTER TABLE `zstack`.`OAuth2TokenVO` MODIFY COLUMN `accessToken` text DEFAULT NULL;
ALTER TABLE `zstack`.`OAuth2TokenVO` MODIFY COLUMN `idToken` text DEFAULT NULL;
ALTER TABLE `zstack`.`OAuth2TokenVO` MODIFY COLUMN `refreshToken` text DEFAULT NULL;
CALL CREATE_INDEX('SSOTokenVO', 'idxSSOTokenVOUserUuid', 'userUuid');

