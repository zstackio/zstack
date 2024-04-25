ALTER TABLE `zstack`.`OAuth2ClientVO` ADD COLUMN `scope` varchar(255) default NULL;

ALTER TABLE `zstack`.`OAuth2ClientVO` ADD COLUMN `identityProvider` varchar(16) default NULL;