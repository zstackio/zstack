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
