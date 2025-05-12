CREATE TABLE  `zstack`.`L3NetworkSequenceNumberVO` (
    `id` int unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`L3NetworkEO` ADD COLUMN `internalId` INT(32) unsigned DEFAULT 0;
DROP VIEW IF EXISTS `zstack`.`L3NetworkVO`;
CREATE VIEW `zstack`.`L3NetworkVO` AS SELECT uuid, name, internalId, description, state, type, zoneUuid, l2NetworkUuid, `system`, dnsDomain, createDate, lastOpDate, category, ipVersion, enableIPAM, isolated FROM `zstack`.`L3NetworkEO` WHERE deleted IS NULL;