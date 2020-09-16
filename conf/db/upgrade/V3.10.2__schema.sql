ALTER TABLE `zstack`.`ImageEO` ADD COLUMN architecture varchar(32) DEFAULT NULL;
DROP VIEW IF EXISTS `zstack`.`ImageVO`;
CREATE VIEW `zstack`.`ImageVO` AS SELECT uuid, name, description, status, state, size, actualSize, md5Sum, platform, type, format, url, `system`, mediaType, createDate, lastOpDate, guestOsType, architecture FROM `zstack`.`ImageEO` WHERE deleted IS NULL;

ALTER TABLE `zstack`.`ClusterEO` ADD COLUMN architecture varchar(32) DEFAULT NULL;
DROP VIEW IF EXISTS `zstack`.`ClusterVO`;
CREATE VIEW `zstack`.`ClusterVO` AS SELECT uuid, zoneUuid, name, type, description, state, hypervisorType, createDate, lastOpDate, managementNodeId, architecture FROM `zstack`.`ClusterEO` WHERE deleted IS NULL;
