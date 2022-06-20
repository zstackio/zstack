CREATE TABLE IF NOT EXISTS `zstack`.`IAM2RMSOrganizationRefVO` (
     `uuid` varchar(32) NOT NULL UNIQUE,
     `rmsOrganizationNo` varchar(255) NOT NULL,
     `organizationUuid` varchar(32),
     `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
     `createDate` timestamp,
     INDEX rmsOrganizationRefexOrgNo (rmsOrganizationNo),
     INDEX rmsOrganizationRefOrguuid (organizationUuid),
     PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`IAM2RMSOrganizationRefVO` ADD CONSTRAINT fkRMSOrgRefVOOrganizationVO FOREIGN KEY (organizationUuid) REFERENCES IAM2OrganizationVO (uuid) ON DELETE CASCADE;

CREATE TABLE IF NOT EXISTS `zstack`.`IAM2RMSUserRefVO` (
     `uuid` varchar(32) NOT NULL UNIQUE,
     `name` varchar(255) NOT NULL,
     `virtualIDUuid` varchar(32),
     `rmsOrgNo` varchar(255) NOT NULL,
     `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
     `createDate` timestamp,
     INDEX rmsVirtualidRefexvirtualID (virtualIDUuid),
     INDEX rmsName (name),
     PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`IAM2RMSUserRefVO` ADD CONSTRAINT fkRMSOrgRefVOVirtualIDVO FOREIGN KEY (virtualIDUuid) REFERENCES IAM2VirtualIDVO (uuid) ON DELETE CASCADE;
