CREATE TABLE  `zstack`.`LdapResourceRefVO` (
     `uuid` varchar(32) NOT NULL UNIQUE,
     `ldapUid` varchar(255) NOT NULL,
     `ldapGlobalUuid` varchar(255),
     `ldapServerUuid` varchar(255) NOT NULL,
     `resourceUuid`  varchar(32) NOT NULL,
     `resourceType` varchar(255) NOT NULL,
     `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
     `createDate` timestamp,
     PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`LdapResourceRefVO` ADD CONSTRAINT fkLdapResourceRefVOLdapServerVO FOREIGN KEY (ldapServerUuid) REFERENCES LdapServerVO (uuid) ON DELETE CASCADE;
ALTER TABLE `zstack`.`LdapResourceRefVO` ADD UNIQUE INDEX(ldapUid,ldapServerUuid,resourceUuid,resourceType);
ALTER TABLE `zstack`.`LdapServerVO` ADD COLUMN `scope` varchar(255) NOT NULL;
UPDATE `zstack`.`LdapServerVO` SET `scope` = "account" WHERE `scope` IS NULL;

ALTER TABLE `zstack`.`IAM2VirtualIDVO` ADD COLUMN `type` varchar(32) DEFAULT NULL;
UPDATE `zstack`.`IAM2VirtualIDVO` SET `type` = "ZStack" WHERE `type` IS NULL;