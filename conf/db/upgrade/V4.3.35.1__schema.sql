UPDATE `zstack`.`GlobalConfigVO` SET value="enable", defaultValue="enable" WHERE category="storageDevice" AND name="enable.multipath" AND value="true";
UPDATE `zstack`.`GlobalConfigVO` SET value="ignore", defaultValue="enable" WHERE category="storageDevice" AND name="enable.multipath" AND value="false";
ALTER TABLE `zstack`.`HostInitiatorRefVO` DROP FOREIGN KEY `fkHostInitiatorRefVOHostVo`;
ALTER TABLE `zstack`.`HostInitiatorRefVO` DROP INDEX hostUuid;
ALTER TABLE `zstack`.`HostInitiatorRefVO` ADD CONSTRAINT fkHostInitiatorRefVOHostVO FOREIGN KEY (hostUuid) REFERENCES `zstack`.`HostEO` (uuid) ON DELETE CASCADE;
ALTER TABLE `zstack`.`HostInitiatorRefVO` ADD COLUMN `primaryStorageUuid` varchar (32) DEFAULT NULL;
ALTER TABLE `zstack`.`HostInitiatorRefVO` ADD CONSTRAINT fkHostInitiatorRefVOPrimaryStorageVO FOREIGN KEY (primaryStorageUuid) REFERENCES `zstack`.`PrimaryStorageEO` (uuid) ON DELETE CASCADE;
