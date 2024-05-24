CREATE TABLE IF NOT EXISTS `zstack`.`GpuDeviceVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `serialNumber` varchar(255),
    `memory` varchar(255),
    `power` varchar(255),
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkGpuDeviceInfoVOPciDeviceVO` FOREIGN KEY (`uuid`) REFERENCES `PciDeviceVO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CALL ADD_COLUMN('PciDeviceVO', 'vendor', 'VARCHAR(128)', 1, NULL);
CALL ADD_COLUMN('PciDeviceVO', 'device', 'VARCHAR(128)', 1, NULL);
CALL ADD_COLUMN('PciDeviceSpecVO', 'vendor', 'VARCHAR(128)', 1, NULL);
CALL ADD_COLUMN('PciDeviceSpecVO', 'device', 'VARCHAR(128)', 1, NULL);
CALL ADD_COLUMN('MdevDeviceVO', 'vendor', 'VARCHAR(128)', 1, NULL);

DROP PROCEDURE IF EXISTS `MdevDeviceAddVendor`;
DELIMITER $$
CREATE PROCEDURE MdevDeviceAddVendor()
    BEGIN
        DECLARE vendor VARCHAR(128);
        DECLARE pciDeviceUuid VARCHAR(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE cur CURSOR FOR SELECT pci.uuid, pci.vendor FROM PciDeviceVO pci;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO vendor, pciDeviceUuid;
            IF done THEN
                LEAVE read_loop;
            END IF;
            UPDATE MdevDeviceVO SET vendor = vendor WHERE parentUuid = pciDeviceUuid;
        END LOOP;
        CLOSE cur;
        SELECT CURTIME();
    END $$
DELIMITER ;
call MdevDeviceAddVendor;
DROP PROCEDURE IF EXISTS `MdevDeviceAddVendor`;
