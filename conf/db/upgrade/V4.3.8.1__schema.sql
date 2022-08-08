DROP PROCEDURE IF EXISTS syncSnatDefaultL3ConfigForVirtualRouter;
DELIMITER $$
CREATE PROCEDURE syncSnatDefaultL3ConfigForVirtualRouter()
BEGIN
    DECLARE virtualRouterUuid VARCHAR(32);
    DECLARE publicNetworkUuid VARCHAR(32);
    DECLARE defaultL3NetworkUuid VARCHAR(32);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR SELECT vss.vpcUuid, vss.l3NetworkUuid FROM `zstack`.`VpcSnatStateVO` vss;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO virtualRouterUuid, publicNetworkUuid;
        IF done THEN
            LEAVE read_loop;
        END IF;
        SELECT `defaultRouteL3NetworkUuid` INTO defaultL3NetworkUuid FROM `zstack`.`ApplianceVmVO` WHERE `uuid` = virtualRouterUuid;
        IF STRCMP(defaultL3NetworkUuid, publicNetworkUuid) THEN
            IF (select count(*) from `zstack`.`VpcSnatStateVO` where `l3NetworkUuid` = defaultL3NetworkUuid and `vpcUuid` = virtualRouterUuid) < 1 THEN
                UPDATE `zstack`.`VpcSnatStateVO` SET `l3NetworkUuid` = defaultL3NetworkUuid WHERE `vpcUuid` = virtualRouterUuid AND `l3NetworkUuid` = publicNetworkUuid;
            END IF;
        END IF;
    END LOOP;
    CLOSE cur;
    SELECT CURTIME();
END $$
DELIMITER ;
CALL syncSnatDefaultL3ConfigForVirtualRouter();
DROP PROCEDURE IF EXISTS syncSnatDefaultL3ConfigForVirtualRouter;

ALTER TABLE `zstack`.`VpcSnatStateVO` ADD CONSTRAINT uqVpcL3NetworkRefVO UNIQUE (vpcUuid, l3NetworkUuid);


DROP PROCEDURE IF EXISTS syncSnatDefaultL3ConfigForVpcHa;
DELIMITER $$
CREATE PROCEDURE syncSnatDefaultL3ConfigForVpcHa()
BEGIN
    DECLARE thisVpcHaRouterUuid VARCHAR(32);
    DECLARE vpcHaRouterDefaultPublicNetworkUuid VARCHAR(32);
    DECLARE defaultL3NetworkUuid VARCHAR(32);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR SELECT DISTINCT vgsr.vpcHaRouterUuid FROM `zstack`.`VpcHaGroupNetworkServiceRefVO` vgsr WHERE networkServiceName='SNAT';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO thisVpcHaRouterUuid;
        IF done THEN
            LEAVE read_loop;
        END IF;

        SELECT `networkServiceUuid`
        FROM `zstack`.`VpcHaGroupNetworkServiceRefVO`
        WHERE `vpcHaRouterUuid` = thisVpcHaRouterUuid
        AND `networkServiceName` = 'SNAT'
        LIMIT 1 INTO vpcHaRouterDefaultPublicNetworkUuid;

        SELECT `defaultRouteL3NetworkUuid`
        FROM `zstack`.`ApplianceVmVO`
        WHERE uuid IN (SELECT uuid FROM `zstack`.`VpcHaGroupApplianceVmRefVO` WHERE `vpcHaRouterUuid` = thisVpcHaRouterUuid)
        AND `haStatus` != 'NoHa'
        LIMIT 1 INTO defaultL3NetworkUuid;

        IF STRCMP(defaultL3NetworkUuid, vpcHaRouterDefaultPublicNetworkUuid) THEN
            UPDATE `zstack`.`VpcHaGroupNetworkServiceRefVO` SET `networkServiceUuid` = defaultL3NetworkUuid WHERE `vpcHaRouterUuid` = thisVpcHaRouterUuid AND `networkServiceName` = 'SNAT';
        END IF;
    END LOOP;
    CLOSE cur;
    SELECT CURTIME();
END $$
DELIMITER ;
CALL syncSnatDefaultL3ConfigForVpcHa();
DROP PROCEDURE IF EXISTS syncSnatDefaultL3ConfigForVpcHa;
