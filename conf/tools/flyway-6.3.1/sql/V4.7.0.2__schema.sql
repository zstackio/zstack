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
