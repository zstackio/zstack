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

ALTER TABLE `zstack`.`LoadBalancerListenerVO` ADD COLUMN `securityPolicyType` varchar(255);

DROP PROCEDURE IF EXISTS updateLoadBalancerListenerVO;
DELIMITER $$
CREATE PROCEDURE updateLoadBalancerListenerVO()
BEGIN
    DECLARE uuid VARCHAR(32);
    DECLARE protocol VARCHAR(64);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR SELECT lbl.uuid,lbl.protocol FROM `zstack`.`LoadBalancerListenerVO` lbl;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;
    update_loop: LOOP
        FETCH cur INTO uuid,protocol;
        IF done THEN
            LEAVE update_loop;
        END IF;

        IF protocol = "https" THEN UPDATE `zstack`.`LoadBalancerListenerVO` lbl SET lbl.securityPolicyType = "tls_cipher_policy_default" WHERE lbl.uuid = uuid;
        END IF;
    END LOOP;
    CLOSE cur;

END $$
DELIMITER ;

CALL updateLoadBalancerListenerVO();
DROP PROCEDURE IF EXISTS updateLoadBalancerListenerVO;