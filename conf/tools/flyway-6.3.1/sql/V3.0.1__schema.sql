CREATE TABLE `V2VConversionHostVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `name` varchar(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `type` VARCHAR(255) NOT NULL,
    `hostUuid` varchar(32) NOT NULL,
    `storagePath`varchar(2048) NOT NULL,
    `state` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uuid` (`uuid`),
    CONSTRAINT fkV2VConversionHostVOHostEO FOREIGN KEY (`hostUuid`) REFERENCES HostEO (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`VmNicVO` DROP INDEX `mac`;
ALTER TABLE `zstack`.`VmNicVO` ADD COLUMN `hypervisorType` varchar(64) DEFAULT NULL;
ALTER TABLE `zstack`.`VmNicVO` ADD CONSTRAINT `ukVmNicVO` UNIQUE (`hypervisorType`,`mac`);
UPDATE `zstack`.`VmNicVO` nic INNER JOIN `zstack`.`VmInstanceVO` vm ON nic.vmInstanceUuid = vm.uuid SET nic.hypervisorType = vm.hypervisorType WHERE nic.vmInstanceUuid IS NOT NULL;

ALTER TABLE `zstack`.`BaremetalImageCacheVO` MODIFY `md5sum` varchar(255) DEFAULT NULL;
