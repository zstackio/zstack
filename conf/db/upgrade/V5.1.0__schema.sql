ALTER TABLE `zstack`.`VmVfNicVO` ADD COLUMN `haState` varchar(32) NOT NULL DEFAULT "Disabled" AFTER `pciDeviceUuid`;
