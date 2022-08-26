CREATE TABLE IF NOT EXISTS `zstack`.`SecurityLevelResourceRefVO` (
    `resourceUuid` VARCHAR(32) NOT NULL UNIQUE,
    `securityLevel` VARCHAR(12) NOT NULL,
    PRIMARY KEY (`resourceUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`RoleVO` ADD COLUMN `rootUuid` VARCHAR(32) DEFAULT NULL;

ALTER TABLE `zstack`.`TicketVO` ADD organizationUuid VARCHAR(32);
ALTER TABLE `zstack`.`TicketVO` ADD CONSTRAINT fkTicketVOOrganizationVO FOREIGN KEY (organizationUuid) REFERENCES IAM2OrganizationVO (uuid) ON DELETE CASCADE;

CREATE TABLE IF NOT EXISTS `zstack`.`IAM2ProjectResourceRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `resourceUuid` varchar(32) NOT NULL,
    `resourceType` varchar(128) NOT NULL,
    `projectUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkIAM2ProjectResourceRefVOResourceVO` FOREIGN KEY (`resourceUuid`) REFERENCES `ResourceVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkIAM2ProjectResourceRefVOIAM2ProjectVO` FOREIGN KEY (`projectUuid`) REFERENCES `IAM2ProjectVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;