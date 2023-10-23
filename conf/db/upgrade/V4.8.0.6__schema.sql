ALTER TABLE `zstack`.`L2NetworkEO` ADD COLUMN `isolated` boolean NOT NULL DEFAULT FALSE AFTER `virtualNetworkId`;
ALTER TABLE `zstack`.`L2NetworkEO` ADD COLUMN `pvlan` varchar(128) DEFAULT NULL AFTER `virtualNetworkId`;
DROP VIEW IF EXISTS `zstack`.L2NetworkVO;
CREATE VIEW `zstack`.`L2NetworkVO` AS SELECT uuid, name, description, type, vSwitchType, virtualNetworkId, zoneUuid, physicalInterface, isolated, pvlan, createDate, lastOpDate FROM `zstack`.`L2NetworkEO` WHERE deleted IS NULL;

ALTER TABLE `zstack`.`L3NetworkEO` ADD COLUMN `isolated` boolean NOT NULL DEFAULT FALSE AFTER `enableIPAM`;
DROP VIEW IF EXISTS `zstack`.`L3NetworkVO`;
CREATE VIEW `zstack`.`L3NetworkVO` AS SELECT uuid, name, description, state, type, zoneUuid, l2NetworkUuid, system, dnsDomain, createDate, lastOpDate, category, ipVersion, enableIPAM, isolated FROM `zstack`.`L3NetworkEO` WHERE deleted IS NULL;