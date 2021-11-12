CREATE TABLE IF NOT EXISTS  `zstack`.`VpcSnatStateVO` (
                                                          `uuid` varchar(32) NOT NULL,
                                                          `vpcUuid` varchar(32) NOT NULL,
                                                          `l3NetworkUuid` varchar(32) NOT NULL,
                                                          `state` varchar(32) NOT NULL,
                                                          `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
                                                          `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
                                                          PRIMARY KEY (`uuid`),
                                                          UNIQUE KEY `uuid` (`uuid`) USING BTREE,
                                                          CONSTRAINT fkVpcNetworkServiceRefVOVirtualRouterVmVO FOREIGN KEY (vpcUuid) REFERENCES VirtualRouterVmVO (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP PROCEDURE IF EXISTS addSnatConfigForVirtualRouter;
DELIMITER $$
CREATE PROCEDURE addSnatConfigForVirtualRouter()
BEGIN
    DECLARE virtualRouterUuid VARCHAR(32);
    DECLARE publicNetworkUuid VARCHAR(32);
    DECLARE ruuid VARCHAR(32);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR SELECT vrv.uuid, vrv.publicNetworkUuid FROM `zstack`.`VirtualRouterVmVO` vrv WHERE vrv.uuid IN (SELECT uuid FROM `zstack`.`ApplianceVmVO` WHERE `haStatus` = 'NoHa');
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO virtualRouterUuid, publicNetworkUuid;
        IF done THEN
            LEAVE read_loop;
        END IF;
        SET ruuid = REPLACE(UUID(), '-', '');
        INSERT INTO `zstack`.`VpcSnatStateVO` (uuid, vpcUuid, l3NetworkUuid, state, createDate, lastOpDate)
        values(ruuid, virtualRouterUuid, publicNetworkUuid, 'enable', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
    END LOOP;
    CLOSE cur;
    DELETE FROM `zstack`.`VpcSnatStateVO` WHERE vpcUuid IN (
        SELECT resourceUuid FROM SystemTagVO WHERE tag = 'disabledService::SNAT'
    );
    SELECT CURTIME();
END $$
DELIMITER ;
CALL addSnatConfigForVirtualRouter();
DROP PROCEDURE IF EXISTS addSnatConfigForVirtualRouter;

DROP PROCEDURE IF EXISTS addSnatConfigForVpcHa;
DELIMITER $$
CREATE PROCEDURE addSnatConfigForVpcHa()
BEGIN
    DECLARE thisVpcHaRouterUuid VARCHAR(32);
    DECLARE vpcHaRouterDefaultPublicNetworkUuid VARCHAR(32);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR SELECT DISTINCT vgr.vpcHaRouterUuid FROM `zstack`.`VpcHaGroupApplianceVmRefVO` vgr ;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    DELETE FROM `zstack`.`VpcHaGroupNetworkServiceRefVO` WHERE networkServiceName = 'SNAT' AND networkServiceUuid != 'true';
    read_loop: LOOP
        FETCH cur INTO thisVpcHaRouterUuid;
        IF done THEN
            LEAVE read_loop;
        END IF;

        SELECT publicNetworkUuid
        FROM `zstack`.`VirtualRouterVmVO`
        WHERE uuid IN (SELECT uuid
                       FROM `zstack`.`ApplianceVmVO`
                       WHERE uuid IN (SELECT uuid
                                      FROM `zstack`.`VpcHaGroupApplianceVmRefVO`
                                      WHERE `vpcHaRouterUuid` = thisVpcHaRouterUuid)
                         AND `haStatus` != 'NoHa')
        LIMIT 1
        INTO vpcHaRouterDefaultPublicNetworkUuid;

        INSERT INTO `zstack`.`VpcHaGroupNetworkServiceRefVO` (vpcHaRouterUuid, networkServiceName, networkServiceUuid, lastOpDate, createDate)
        values(thisVpcHaRouterUuid, 'SNAT', vpcHaRouterDefaultPublicNetworkUuid, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
    END LOOP;
    CLOSE cur;
    DELETE FROM `zstack`.`VpcHaGroupNetworkServiceRefVO` WHERE networkServiceName = 'SNAT' AND vpcHaRouterUuid IN
                                                                                               (SELECT a.vpcHaRouterUuid FROM
                                                                                                   (SELECT vpcHaRouterUuid FROM `zstack`.`VpcHaGroupNetworkServiceRefVO` WHERE networkServiceName = 'SNAT' AND networkServiceUuid = 'true') a);
    SELECT CURTIME();
END $$
DELIMITER ;
CALL addSnatConfigForVpcHa();
DROP PROCEDURE IF EXISTS addSnatConfigForVpcHa;