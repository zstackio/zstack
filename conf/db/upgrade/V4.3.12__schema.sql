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

create table if not exists `zstack`.`HostAllocatedCpuVO` (
    `id` bigint not null auto_increment,
    `hostUuid` varchar(32) not null,
    `allocatedCPU` smallint not null,
    constraint HostAllocatedCpuVO_pk
    primary key (`id`),
    constraint HostAllocatedCpuVO_UniqueIndex_HostUuid_CPUID
    unique (hostUuid, allocatedCPU),
    constraint HostAllocatedCpuVO_HostEO_uuid_fk
    foreign key (`hostUuid`) references `zstack`.`HostEO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table if not exists `zstack`.`VmInstanceNumaNodeVO` (
    `id` bigint not null auto_increment,
    `vmUuid` varchar(32) not null,
    `vNodeID` int not null,
    `vNodeCPUs` varchar(512) not null,
    `vNodeMemSize` bigint not null,
    `vNodeDistance` varchar(512) not null,
    `pNodeID` int not null,
    constraint VmInstanceNumaNodeVO_pk
    primary key (`id`),
    constraint VmInstanceNumaNodeVO_VmInstanceEO_uuid_fk
    foreign key (`vmUuid`) references `zstack`.`VmInstanceEO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table if not exists `zstack`.`HostNumaNodeVO` (
    `id` bigint not null auto_increment,
    `hostUuid` varchar(32) not null,
    `nodeID` int not null,
    `nodeCPUs` varchar(512) not null,
    `nodeMemSize` bigint not null,
    `nodeDistance` varchar(512) not null,
    constraint HostNumaNodeVO_pk
    primary key (`id`),
    constraint HostNumaNodeVO_HostEO_uuid_fk
    foreign key (`hostUuid`) references `zstack`.`HostEO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`PrimaryStorageHostRefVO` ADD UNIQUE INDEX(`primaryStorageUuid`, `hostUuid`);
