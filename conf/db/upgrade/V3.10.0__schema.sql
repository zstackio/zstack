ALTER TABLE `zstack`.`LoadBalancerVO` DROP FOREIGN KEY `fkLoadBalancerVOVipVO`;
ALTER TABLE `zstack`.`LoadBalancerVO` ADD CONSTRAINT `fkLoadBalancerVOVipVO` FOREIGN KEY (`vipUuid`) REFERENCES `zstack`.`VipVO` (`uuid`) ON DELETE CASCADE;

ALTER TABLE `zstack`.`L3NetworkEO` MODIFY COLUMN `ipVersion` int(10) unsigned DEFAULT 0;

DELIMITER $$
CREATE PROCEDURE changeL3NetworkDefaultIpversion()
BEGIN
    DECLARE l3NetworkUuid VARCHAR(32);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR SELECT l3.uuid FROM `zstack`.`L3NetworkVO` l3 where uuid not in (select l3v.uuid from `zstack`.`L3NetworkVO` l3v, `zstack`.`IpRangeVO` ipr where ipr.l3NetworkUuid=l3v.uuid);
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO l3NetworkUuid;
        IF done THEN
            LEAVE read_loop;
        END IF;

        UPDATE zstack.L3NetworkEO set ipVersion = 0 where uuid = l3NetworkUuid;

    END LOOP;
    CLOSE cur;
END $$
DELIMITER ;

CALL changeL3NetworkDefaultIpversion();
DROP PROCEDURE IF EXISTS changeL3NetworkDefaultIpversion;