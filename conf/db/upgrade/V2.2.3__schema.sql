
# New feature: affinity group -- add 2 tables: AffinityGroupVO, AffinityGroupUsageVO
CREATE TABLE IF NOT EXISTS `AffinityGroupVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE COMMENT 'uuid',
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `policy` VARCHAR(255) NOT NULL,
    `version` VARCHAR(255) NOT NULL,
    `type` VARCHAR(255) NOT NULL,
    `appliance` VARCHAR(255) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `AffinityGroupUsageVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE COMMENT 'uuid',
    `affinityGroupUuid` VARCHAR(32) NOT NULL,
    `resourceType` VARCHAR(255) NOT NULL,
    `resourceUuid` VARCHAR(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkAffinityGroupUsageVOcreateAffinityGroupVO` FOREIGN KEY (`affinityGroupUuid`) REFERENCES `zstack`.`AffinityGroupVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DELIMITER $$
CREATE PROCEDURE insertApplianceVmAffinityGroup()
    BEGIN
        DECLARE applianceVmAffinityGroupUuid VARCHAR(32);
        SET applianceVmAffinityGroupUuid = REPLACE(UUID(), '-', '');
        INSERT INTO zstack.AffinityGroupVO (uuid, name, description, policy, version, type, appliance, lastOpDate, createDate)
            values(applianceVmAffinityGroupUuid, 'zstack.affinity.group.for.virtual.router', 'zstack.affinity.group.for.virtual.router', 'ANTIAFFINITYSOFT', '1.0', 'HOST', 'VROUTER', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
        INSERT INTO zstack.ResourceVO(uuid, resourceName, resourceType) values(applianceVmAffinityGroupUuid, 'zstack.affinity.group.for.virtual.router', 'AffinityGroupVO');
    END $$
DELIMITER ;

call insertApplianceVmAffinityGroup();
DROP PROCEDURE IF EXISTS insertApplianceVmAffinityGroup;

SET FOREIGN_KEY_CHECKS = 0;
ALTER TABLE AliyunSnapshotVO DROP FOREIGN KEY fkAliyunSnapshotVOAliyunDiskVO;
ALTER TABLE AliyunSnapshotVO ADD CONSTRAINT fkAliyunSnapshotVOAliyunDiskVO FOREIGN KEY (diskUuid) REFERENCES AliyunDiskVO (uuid) ON DELETE SET NULL;
SET FOREIGN_KEY_CHECKS = 1;

DELIMITER $$
CREATE PROCEDURE cleanDeprecatedGlobalConfig()
    BEGIN
        DECLARE config_value VARCHAR(32) DEFAULT 'vnc';
        SELECT `value` into config_value FROM `zstack`.`GlobalConfigVO` WHERE `name`='vm.consoleMode' and `category`='kvm';
        UPDATE `zstack`.`GlobalConfigVO` SET `value`=config_value WHERE `name`='vm.consoleMode' and `category`='mevoco';
        DELETE FROM `zstack`.`GlobalConfigVO` WHERE `name`='vm.consoleMode' and `category`='kvm';
    END $$
DELIMITER ;

call cleanDeprecatedGlobalConfig();
DROP PROCEDURE IF EXISTS cleanDeprecatedGlobalConfig;

ALTER TABLE `UserTagVO` MODIFY `tag` TEXT NOT NULL;
ALTER TABLE `SystemTagVO` MODIFY `tag` TEXT NOT NULL;



