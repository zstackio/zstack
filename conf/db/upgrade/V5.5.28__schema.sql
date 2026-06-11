-- Do not filter by architecture here. The upgrade preserves previous Windows VM behavior across all architectures;
-- current kvmagent consumption is still gated by host CPU architecture at start time.
INSERT INTO `zstack`.`ResourceConfigVO` (`uuid`, `name`, `description`, `category`, `value`, `resourceUuid`, `resourceType`, `lastOpDate`, `createDate`)
SELECT REPLACE(UUID(), '-', ''), 'vm.cpu.hardwareVirtualization', 'enable or disable hardware virtualization feature in Windows guest cpuid',
       'kvm', 'true', vm.`uuid`, 'VmInstanceVO', NOW(), NOW()
FROM `zstack`.`VmInstanceVO` vm
WHERE (vm.`platform` IN ('Windows', 'WindowsVirtio')
    OR LOWER(IFNULL(vm.`guestOsType`, '')) LIKE '%windows%')
  AND NOT EXISTS (
      SELECT 1
      FROM `zstack`.`ResourceConfigVO` rc
      WHERE rc.`resourceUuid` = vm.`uuid`
        AND rc.`category` = 'kvm'
        AND rc.`name` = 'vm.cpu.hardwareVirtualization'
  );
