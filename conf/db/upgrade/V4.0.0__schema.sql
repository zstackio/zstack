DROP PROCEDURE IF EXISTS checkFlatLoadBalancerExist;
DELIMITER $$
CREATE PROCEDURE checkFlatLoadBalancerExist()
BEGIN
    if((select count(*) from LoadBalancerListenerVmNicRefVO ref, VmNicVO nic
    where ref.vmNicUuid=nic.uuid
    and nic.l3NetworkUuid not in (SELECT uuid FROM L3NetworkEO l3
    LEFT JOIN NetworkServiceL3NetworkRefVO ref on l3.uuid = ref.l3NetworkUuid WHERE ref.networkServiceType='SNAT')) > 0) THEN
        SIGNAL SQLSTATE "45000"
            SET MESSAGE_TEXT = "VirtualRouter are not supported this version";
    END IF;
END$$
DELIMITER ;
CALL checkFlatLoadBalancerExist();
DROP PROCEDURE IF EXISTS checkFlatLoadBalancerExist;

DROP PROCEDURE IF EXISTS checkVirtualhostsExist;
DELIMITER $$
CREATE PROCEDURE checkVirtualhostsExist()
BEGIN
    if((SELECT count(*) from ApplianceVmVO where applianceVmType = "VirtualRouter") > 0) THEN
        SIGNAL SQLSTATE "45000"
            SET MESSAGE_TEXT = "VirtualRouter are not supported this version";
    END IF;
END$$
DELIMITER ;
CALL checkVirtualhostsExist();
DROP PROCEDURE IF EXISTS checkVirtualhostsExist;

ALTER TABLE `zstack`.`VpcRouterVmVO` ADD COLUMN `generalVersion` varchar(32) DEFAULT NULL;