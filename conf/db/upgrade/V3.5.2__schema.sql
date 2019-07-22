ALTER TABLE `zstack`.`IAM2OrganizationVO` ADD COLUMN `srcType` varchar(32) DEFAULT NULL;
UPDATE `zstack`.`IAM2OrganizationVO` SET `srcType` = "ZStack" WHERE `uuid`
 NOT IN (SELECT `resourceUuid` FROM `LdapResourceRefVO` WHERE `resourceType`='IAM2OrganizationVO');
UPDATE `zstack`.`IAM2OrganizationVO` SET `srcType` = "Ldap" WHERE `uuid`
 IN (SELECT `resourceUuid` FROM `LdapResourceRefVO` WHERE `resourceType`='IAM2OrganizationVO');