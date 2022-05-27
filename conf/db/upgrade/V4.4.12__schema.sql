UPDATE `zstack`.`VmInstanceVO` t1, `zstack`.`HostVO` t2 set t1.`architecture` = t2.`architecture` where t1.`type` = 'ApplianceVm' and t1.`hostUuid` = t2.`uuid`;
UPDATE `zstack`.`VmInstanceVO` t1, `zstack`.`HostVO` t2 set t1.`architecture` = t2.`architecture` where t1.`type` = 'ApplianceVm' and t1.`architecture` IS NULL and t1.`lastHostUuid` = t2.`uuid`;