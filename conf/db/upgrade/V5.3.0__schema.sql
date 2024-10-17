UPDATE `zstack`.`ImageEO` SET guestOsType = 'VyOS 1.1.7' WHERE architecture = 'x86_64' and guestOsType = 'Linux' and system = TRUE;
UPDATE `zstack`.`ImageEO` SET guestOsType = 'VyOS 1.2.0' WHERE architecture = 'aarch64' and guestOsType = 'Linux' and system = TRUE;
UPDATE `zstack`.`ImageEO` SET guestOsType = 'Kylin 10' WHERE architecture = 'loongarch64' and guestOsType = 'Linux' and system = TRUE;

UPDATE `zstack`.`VmInstanceEO` SET guestOsType = 'VyOS 1.1.7' WHERE architecture = 'x86_64' and guestOsType = 'Linux' and type = 'ApplianceVm';
UPDATE `zstack`.`VmInstanceEO` SET guestOsType = 'VyOS 1.2.0' WHERE architecture = 'aarch64' and guestOsType = 'Linux' and type = 'ApplianceVm';
UPDATE `zstack`.`VmInstanceEO` SET guestOsType = 'Kylin 10' WHERE architecture = 'loongarch64' and guestOsType = 'Linux' and type = 'ApplianceVm';