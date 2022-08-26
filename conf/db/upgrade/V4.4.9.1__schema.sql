CREATE TABLE IF NOT EXISTS `zstack`.`SecurityLevelResourceRefVO` (
    `resourceUuid` VARCHAR(32) NOT NULL UNIQUE,
    `securityLevel` VARCHAR(12) NOT NULL,
    PRIMARY KEY (`resourceUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`RoleVO` ADD COLUMN `rootUuid` VARCHAR(32) DEFAULT NULL;

ALTER TABLE `zstack`.`TicketVO` ADD organizationUuid VARCHAR(32);
ALTER TABLE `zstack`.`TicketVO` ADD CONSTRAINT fkTicketVOOrganizationVO FOREIGN KEY (organizationUuid) REFERENCES IAM2OrganizationVO (uuid) ON DELETE CASCADE;

CREATE TABLE IF NOT EXISTS `zstack`.`ClusterIAM2ProjectRefVO` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `projectUuid` varchar(32) NOT NULL,
    `clusterUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`id`),
    UNIQUE KEY `projectUuid` (`projectUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`IAM2TicketFlowCollectionVO` ADD COLUMN organizationUuid VARCHAR(32) DEFAULT NULL;
ALTER TABLE `zstack`.`IAM2TicketFlowCollectionVO` ADD CONSTRAINT fkIAM2TicketFlowCollectionVOIAM2OrganizationVO FOREIGN KEY (organizationUuid) REFERENCES IAM2OrganizationVO (uuid) ON DELETE CASCADE;
ALTER TABLE `zstack`.`IAM2TicketFlowCollectionVO` MODIFY COLUMN projectUuid VARCHAR(32) DEFAULT NULL;