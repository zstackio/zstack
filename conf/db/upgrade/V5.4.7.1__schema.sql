CREATE TABLE IF NOT EXISTS `zstack`.`VpcRouterDnsRecordVO` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `uuid` VARCHAR(32) NOT NULL,
    `vpcRouterUuid` VARCHAR(32) DEFAULT NULL,
    `vpcHaGroupUuid` VARCHAR(32) DEFAULT NULL,
    `type` VARCHAR(16) NOT NULL DEFAULT 'A',
    `domain` VARCHAR(255) NOT NULL,
    `ip` VARCHAR(255) NOT NULL,
    `createDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `ukVpcRouterDnsRecordVOuuid` (`uuid`),
    INDEX `idxVpcRouterDnsRecordVOvpcRouterUuid` (`vpcRouterUuid`),
    INDEX `idxVpcRouterDnsRecordVOvpcHaGroupUuid` (`vpcHaGroupUuid`),
    INDEX `idxVpcRouterDnsRecordVOtype` (`type`),
    INDEX `idxVpcRouterDnsRecordVOdomain` (`domain`),
    CONSTRAINT `fkVpcRouterDnsRecordVOVpcRouterVmVO` FOREIGN KEY (`vpcRouterUuid`) REFERENCES `zstack`.`VpcRouterVmVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkVpcRouterDnsRecordVOVpcHaGroupVO` FOREIGN KEY (`vpcHaGroupUuid`) REFERENCES `zstack`.`VpcHaGroupVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
