ALTER TABLE `zstack`.`IpRangeEO` ADD COLUMN `ipVersion` int(10) unsigned NOT NULL DEFAULT 4;
ALTER TABLE `zstack`.`IpRangeEO` ADD COLUMN `addressMode` varchar(64) DEFAULT NULL;
ALTER TABLE `zstack`.`IpRangeEO` ADD COLUMN `prefixLen` int(10) unsigned DEFAULT NULL;
ALTER VIEW `zstack`.`IpRangeVO` AS SELECT uuid, l3NetworkUuid, name, description, startIp, endIp, netmask, gateway, networkCidr, createDate, lastOpDate, ipVersion, addressMode, prefixLen FROM `zstack`.`IpRangeEO` WHERE deleted IS NULL;

ALTER TABLE `zstack`.`UsedIpVO` ADD COLUMN `ipVersion` int(10) unsigned NOT NULL DEFAULT 4;
ALTER TABLE `zstack`.`UsedIpVO` ADD COLUMN `vmNicUuid` varchar(32) DEFAULT NULL;
ALTER TABLE `zstack`.`UsedIpVO` ADD CONSTRAINT fkUsedIpVOVmNicVO FOREIGN KEY (vmNicUuid) REFERENCES VmNicVO (uuid) ON DELETE SET NULL;


ALTER TABLE `zstack`.`L3NetworkEO` ADD COLUMN `ipVersion` int(10) unsigned NOT NULL DEFAULT 4;
ALTER VIEW `zstack`.`L3NetworkVO` AS SELECT uuid, name, description, state, type, zoneUuid, l2NetworkUuid, system, dnsDomain, createDate, lastOpDate, category, ipVersion FROM `zstack`.`L3NetworkEO` WHERE deleted IS NULL;

ALTER TABLE `zstack`.`VmNicVO` ADD COLUMN `ipVersion` int(10) unsigned NOT NULL DEFAULT 4;