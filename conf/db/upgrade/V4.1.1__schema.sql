ALTER TABLE `zstack`.`PciDeviceVO` ADD `chooser` varchar(32) DEFAULT 'None';
UPDATE PciDeviceVO SET chooser='None' WHERE vmInstanceUuid IS NULL;
UPDATE PciDeviceVO SET chooser='Device' WHERE vmInstanceUuid IS NOT NULL;

ALTER TABLE `zstack`.`MdevDeviceVO` ADD `chooser` varchar(32) DEFAULT 'None';
UPDATE MdevDeviceVO SET chooser='None' WHERE vmInstanceUuid IS NULL;
UPDATE MdevDeviceVO SET chooser='Device' WHERE vmInstanceUuid IS NOT NULL;

UPDATE VmInstancePciSpecDeviceRefVO AS ref LEFT JOIN PciDeviceVO AS pci
    ON ref.pciDeviceUuid = pci.uuid
    SET chooser='Spec'
    WHERE ref.vmInstanceUuid = pci.vmInstanceUuid;
UPDATE VmInstanceMdevSpecDeviceRefVO AS ref LEFT JOIN MdevDeviceVO AS mdev
    ON ref.mdevDeviceUuid = mdev.uuid
    SET chooser='Spec'
    WHERE ref.vmInstanceUuid = mdev.vmInstanceUuid;
DROP TABLE IF EXISTS VmInstancePciSpecDeviceRefVO;
DROP TABLE IF EXISTS VmInstanceMdevSpecDeviceRefVO;

DELETE FROM SystemTagVO WHERE tag LIKE 'pciDevice::%';
DELETE FROM SystemTagVO WHERE tag LIKE 'mdevDevice::%';
