ALTER TABLE `zstack`.`ConsoleProxyVO` ADD COLUMN `version` varchar(32) DEFAULT NULL;

DELIMITER $$
CREATE PROCEDURE migrateClockTrackSystemTagToGlobalConfig()
BEGIN
    DECLARE vmInstanceUuid VARCHAR(32);
    DECLARE clockTrackTag VARCHAR(32);
    DECLARE clockTrack VARCHAR(32);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR SELECT systemTag.tag, systemTag.resourceUuid FROM `zstack`.`SystemTagVO` systemTag
     where `tag` like 'clockTrack::%' and `resourceType`='VmInstanceVO';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO clockTrackTag, vmInstanceUuid;
        IF done THEN
            LEAVE read_loop;
        END IF;

        SET clockTrack = SUBSTRING_INDEX(clockTrackTag, '::', -1);
        INSERT INTO zstack.ResourceConfigVO (uuid, name, description, category, value, resourceUuid, resourceType, lastOpDate, createDate)
         values(ruuid, "vm.clock.track", "vm.clock.track", "vm", clockTrack, vmInstanceUuid, "VmInstanceVO", CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
    END LOOP;
    CLOSE cur;
    SELECT CURTIME();
END $$
DELIMITER ;

call migrateClockTrackSystemTagToGlobalConfig();
DROP PROCEDURE IF EXISTS migrateClockTrackSystemTagToGlobalConfig;

DELETE FROM `zstack`.`SystemTagVO` where `tag` like 'clockTrack::%' and `resourceType`='VmInstanceVO';

ALTER TABLE `zstack`.`SlbVmInstanceVO` DROP FOREIGN KEY `fkSlbVmInstanceVOSlbGroupVO`;
ALTER TABLE `zstack`.`SlbVmInstanceVO` DROP KEY `fkSlbVmInstanceVOSlbGroupVO`;
ALTER TABLE `zstack`.`SlbVmInstanceVO` MODIFY COLUMN `slbGroupUuid` varchar(32) DEFAULT NULL;
ALTER TABLE `zstack`.`SlbVmInstanceVO` ADD CONSTRAINT `fkSlbVmInstanceVOVmInstanceEO` FOREIGN KEY (`uuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE;
ALTER TABLE `zstack`.`SlbVmInstanceVO` ADD CONSTRAINT `fkSlbVmInstanceVOSlbGroupVO` FOREIGN KEY (`slbGroupUuid`) REFERENCES `SlbGroupVO` (`uuid`) ON DELETE SET NULL;

create table if not exists `zstack`.`HostAllocatedCPUVO` (
    `uuid` varchar(32) not null,
    `allocatedCPU` TEXT not null,
    constraint HostAllocatedCPUVO_pk
    primary key (`uuid`),
    constraint HostAllocatedCPUVO_HostEO_uuid_fk
    foreign key (`uuid`) references `zstack`.`HostEO` (`uuid`) on delete cascade
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table if not exists `zstack`.`VmInstanceNUMAVO` (
    `id` bigint not null auto_increment,
    `uuid` varchar(32) not null,
    `vNodeID` int not null,
    `vNodeCPUs` varchar(512) not null,
    `vNodeMemSize` bigint not null,
    `vNodeDistance` varchar(512) not null,
    `pNodeID` int not null,
    constraint VmInstanceNUMAVO_pk
    primary key (`id`),
    constraint VmInstanceNUMAVO_VmInstanceEO_uuid_fk
    foreign key (`uuid`) references `zstack`.`VmInstanceEO` (`uuid`) on delete cascade
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table if not exists `zstack`.`HostNUMATopologyVO` (
    `id` bigint not null auto_increment,
    `uuid` varchar(32) not null,
    `nodeID` int not null,
    `nodeCPUs` varchar(512) not null,
    `nodeMemSize` bigint not null,
    `nodeDistance` varchar(512) not null,
    constraint HostNumaTopologyVO_pk
    primary key (`id`),
    constraint HostNumaTopologyVO_HostEO_uuid_fk
    foreign key (`uuid`) references `zstack`.`HostEO` (`uuid`) on delete cascade
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
