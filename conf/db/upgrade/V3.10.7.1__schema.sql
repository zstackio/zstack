CREATE TABLE IF NOT EXISTS `zstack`.`IAM2ExternalOrganizationRefVO` (
     `uuid` varchar(32) NOT NULL UNIQUE,
     `externalOrganizationID` varchar(255) NOT NULL,
     `organizationUuid` varchar(32),
     `externalOrganizationInfo` text,
     `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
     `createDate` timestamp,
     INDEX externalOrganizationRefexOrgID (externalOrganizationID),
     INDEX externalOrganizationRefOrguuid (organizationUuid),
     PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`IAM2ExternalOrganizationRefVO` ADD CONSTRAINT fkExternalOrgRefVOOrganizationVO FOREIGN KEY (organizationUuid) REFERENCES IAM2OrganizationVO (uuid) ON DELETE CASCADE;

CREATE TABLE IF NOT EXISTS `zstack`.`IAM2ExternalVirtualIDRefVO` (
     `uuid` varchar(32) NOT NULL UNIQUE,
     `externalVirtualID` varchar(255) NOT NULL,
     `virtualIDUuid` varchar(32),
     `externalOrganizationID` varchar(255) NOT NULL,
     `externalVirtualIDInfo` text,
     `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
     `createDate` timestamp,
     INDEX externalVirtualidRefexvirtualID (virtualIDUuid),
     INDEX externalVirtualidRefvirtualiduuid (virtualIDUuid),
     PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`IAM2ExternalVirtualIDRefVO` ADD CONSTRAINT fkExternalOrgRefVOVirtualIDVO FOREIGN KEY (virtualIDUuid) REFERENCES IAM2VirtualIDVO (uuid) ON DELETE CASCADE;
