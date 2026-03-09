ALTER TABLE `zstack`.`HostNumaNodeVO` DROP FOREIGN KEY `HostNumaNodeVO_HostEO_uuid_fk`;
ALTER TABLE `zstack`.`HostNumaNodeVO` ADD CONSTRAINT `HostNumaNodeVO_HostEO_uuid_fk` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE ;

ALTER TABLE `zstack`.`VmInstanceNumaNodeVO` DROP FOREIGN KEY `VmInstanceNumaNodeVO_VmInstanceEO_uuid_fk`;
ALTER TABLE `zstack`.`VmInstanceNumaNodeVO` ADD CONSTRAINT `VmInstanceNumaNodeVO_VmInstanceEO_uuid_fk` FOREIGN KEY (`vmUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE ;

ALTER TABLE `zstack`.`HostAllocatedCpuVO` DROP FOREIGN KEY `HostAllocatedCpuVO_HostEO_uuid_fk`;
ALTER TABLE `zstack`.`HostAllocatedCpuVO` ADD CONSTRAINT `HostAllocatedCpuVO_HostEO_uuid_fk` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE ;