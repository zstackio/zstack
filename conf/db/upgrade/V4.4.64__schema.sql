UPDATE ResourceConfigVO SET createDate = CURRENT_TIMESTAMP where name='iam2.force.enable.securityGroup' and createDate='0000-00-00 00:00:00';

DROP PROCEDURE IF EXISTS AddFkPciDeviceVOVmInstanceEO;
DELIMITER $$
CREATE PROCEDURE AddFkPciDeviceVOVmInstanceEO()
BEGIN
    IF (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_TYPE = 'FOREIGN KEY' AND TABLE_NAME = 'PciDeviceVO' AND CONSTRAINT_NAME = 'fkPciDeviceVOVmInstanceEO') = 0 THEN
ALTER TABLE PciDeviceVO
    ADD CONSTRAINT fkPciDeviceVOVmInstanceEO FOREIGN KEY (vmInstanceUuid) REFERENCES VmInstanceEO(uuid) ON DELETE SET NULL;
END IF;
END $$
DELIMITER ;
CALL AddFkPciDeviceVOVmInstanceEO();

ALTER TABLE SecretResourcePoolVO ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'Connected';
