CREATE TABLE IF NOT EXISTS `zstack`.`SecurityLevelResourceRefVO` (
    `resourceUuid` varchar(32) NOT NULL UNIQUE,
    `securityLevel` varchar(12),
    PRIMARY KEY  (`resourceUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ClusterIAM2ProjectRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `projectUuid` varchar(32) NOT NULL,
    `clusterUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`id`),
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`TicketVO` ADD organizationUuid VARCHAR(32);
ALTER TABLE `zstack`.`TicketVO` ADD CONSTRAINT fkTicketVOOrganizationVO FOREIGN KEY (organizationUuid) REFERENCES IAM2OrganizationVO (uuid) ON DELETE CASCADE;