DELIMITER $$
CREATE PROCEDURE getVirtualRouterUuidFromNicUuid(IN nicUuid VARCHAR(32), OUT virtualRouterUuid VARCHAR(32), OUT l3Type VARCHAR(128))
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR select nic1.vmInstanceUuid, l3.type from `zstack`.VmNicVO nic1, `zstack`.VmNicVO nic2, `zstack`.L3NetworkVO l3 where nic1.metaData in ('4', '5', '6', '7') and nic1.l3NetworkUuid = l3.uuid and nic1.l3NetworkUuid = nic2.l3NetworkUuid and nic2.uuid=nicUuid;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
        fetch cur INTO virtualRouterUuid, l3Type;
        IF done THEN
            LEAVE read_loop;
        END IF;
    END LOOP;
    CLOSE cur;
    # work around a bug of mysql : jira.mariadb.org/browse/MDEV-4602
    SELECT CURTIME();
END $$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE insertVirtualRouterEipRefVO()
BEGIN
    DECLARE eipUuid VARCHAR(32);
    DECLARE vipUuid VARCHAR(32);
    DECLARE nicUuid VARCHAR(32);
    DECLARE virtualRouterUuid VARCHAR(32);
    DECLARE l3Type VARCHAR(128);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR select eip.uuid, eip.vipUuid, eip.vmNicUuid from `zstack`.`EipVO` eip where eip.vmNicUuid is not NULL and eip.uuid not in (select eipUuid from `zstack`.`VirtualRouterEipRefVO`);
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO eipUuid, vipUuid, nicUuid;
        IF done THEN
            LEAVE read_loop;
        END IF;

        SET virtualRouterUuid = NULL;
        SET l3Type = NULL;
        SELECT nicUuid;
        CALL getVirtualRouterUuidFromNicUuid(nicUuid, virtualRouterUuid, l3Type);
        IF virtualRouterUuid IS NOT NULL AND l3Type = 'L3VpcNetwork' THEN
            INSERT IGNORE INTO `zstack`.VirtualRouterEipRefVO (eipUuid, virtualRouterVmUuid) values (eipUuid, virtualRouterUuid);
            INSERT IGNORE INTO `zstack`.VirtualRouterVipVO (uuid, virtualRouterVmUuid) values (vipUuid, virtualRouterUuid);
        END IF;

    END LOOP;
    CLOSE cur;
    SELECT CURTIME();
END $$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE insertVirtualRouterPortForwardingRuleRefVO()
BEGIN
    DECLARE pfUuid VARCHAR(32);
    DECLARE vipUuid VARCHAR(32);
    DECLARE nicUuid VARCHAR(32);
    DECLARE virtualRouterUuid VARCHAR(32);
    DECLARE l3Type VARCHAR(128);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR select pf.uuid, pf.vipUuid, pf.vmNicUuid from `zstack`.PortForwardingRuleVO pf where pf.vmNicUuid is not NULL and pf.uuid not in (select uuid from `zstack`.VirtualRouterPortForwardingRuleRefVO);
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO pfUuid, vipUuid, nicUuid;
        IF done THEN
            LEAVE read_loop;
        END IF;

        SET virtualRouterUuid = NULL;
        SET l3Type = NULL;
        SELECT nicUuid;
        CALL getVirtualRouterUuidFromNicUuid(nicUuid, virtualRouterUuid, l3Type);
        IF virtualRouterUuid IS NOT NULL AND l3Type = 'L3VpcNetwork' THEN
            INSERT IGNORE INTO `zstack`.VirtualRouterPortForwardingRuleRefVO (uuid, vipUuid, virtualRouterVmUuid) values (pfUuid, vipUuid, virtualRouterUuid);
            INSERT IGNORE INTO `zstack`.VirtualRouterVipVO (uuid, virtualRouterVmUuid) values (vipUuid, virtualRouterUuid);
        END IF;

    END LOOP;
    CLOSE cur;
    SELECT CURTIME();
END $$
DELIMITER ;

CALL insertVirtualRouterEipRefVO();
CALL insertVirtualRouterPortForwardingRuleRefVO();
update `zstack`.LoadBalancerListenerVmNicRefVO ref INNER JOIN `zstack`.VmNicVO nic on ref.vmNicUuid = nic.uuid INNER JOIN `zstack`.L3NetworkVO l3 on nic.l3NetworkUuid = l3.uuid set status = 'Active' where l3.type='L3VpcNetwork';
DROP PROCEDURE IF EXISTS insertVirtualRouterPortForwardingRuleRefVO;
DROP PROCEDURE IF EXISTS insertVirtualRouterEipRefVO;
DROP PROCEDURE IF EXISTS getVirtualRouterUuidFromNicUuid;