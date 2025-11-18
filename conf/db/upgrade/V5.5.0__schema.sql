CALL ADD_COLUMN('VmVfNicVO', 'secondaryPciDeviceUuid', 'VARCHAR(32)', 1, NULL);
ALTER TABLE `zstack`.`VmVfNicVO` ADD CONSTRAINT `fkVmVfNicVOSecondaryPciDeviceVO` FOREIGN KEY (`secondaryPciDeviceUuid`) REFERENCES `zstack`.`PciDeviceVO` (`uuid`) ON DELETE SET NULL;

CALL ADD_COLUMN('LicenseAuthorizedCapacityVO', 'resourceInfo', 'text', 1, NULL);

CALL ADD_COLUMN('PciDeviceVO', 'dependentDevices', 'varchar(255)', 1, NULL);
