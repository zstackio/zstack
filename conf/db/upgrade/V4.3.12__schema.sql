ALTER TABLE `zstack`.`ConsoleProxyVO` ADD COLUMN `version` varchar(32) DEFAULT NULL;

ALTER TABLE `zstack`.`SlbVmInstanceVO` DROP FOREIGN KEY `fkSlbVmInstanceVOSlbGroupVO`;
ALTER TABLE `zstack`.`SlbVmInstanceVO` DROP KEY `fkSlbVmInstanceVOSlbGroupVO`;
ALTER TABLE `zstack`.`SlbVmInstanceVO` MODIFY COLUMN `slbGroupUuid` varchar(32) DEFAULT NULL;
ALTER TABLE `zstack`.`SlbVmInstanceVO` ADD CONSTRAINT `fkSlbVmInstanceVOVmInstanceEO` FOREIGN KEY (`uuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE;
ALTER TABLE `zstack`.`SlbVmInstanceVO` ADD CONSTRAINT `fkSlbVmInstanceVOSlbGroupVO` FOREIGN KEY (`slbGroupUuid`) REFERENCES `SlbGroupVO` (`uuid`) ON DELETE SET NULL;