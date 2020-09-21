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

DROP PROCEDURE IF EXISTS upgradeVirtualRouterToVpc;
DROP PROCEDURE IF EXISTS getVirtualRouterUuidForL3Network;
DROP PROCEDURE IF EXISTS addVpcDns;
DROP PROCEDURE IF EXISTS updateVirtualRouter;

DELIMITER $$
CREATE PROCEDURE getVirtualRouterUuidForL3Network(IN l3Uuid VARCHAR(32), OUT vrUuid VARCHAR(32))
BEGIN
    SELECT vmInstanceUuid INTO vrUuid from `zstack`.`VmNicVO` nic where nic.l3NetworkUuid=l3Uuid and nic.metaData in ('4', '5', '6', '7');
END $$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE addVpcDns(IN l3Uuid VARCHAR(32), IN vrUuid VARCHAR(32))
BEGIN
    DECLARE dnsAddress varchar(255);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR SELECT dns FROM L3NetworkDnsVO l3 where l3.l3NetworkUuid = l3Uuid;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO dnsAddress;
        IF done THEN
            LEAVE read_loop;
        END IF;
        INSERT INTO zstack.VpcRouterDnsVO (vpcRouterUuid, dns, createDate, lastOpDate) values (vrUuid, dnsAddress, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
    END LOOP;
    CLOSE cur;
END $$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE updateVirtualRouter(IN l3Uuid VARCHAR(32), IN vrUuid VARCHAR(32))
BEGIN
    IF vrUuid is not NULL THEN
        UPDATE zstack.ApplianceVmVO SET applianceVmType = 'vpcvrouter' WHERE uuid = vrUuid;
        INSERT INTO zstack.VpcRouterVmVO (uuid) VALUES (vrUuid);
        UPDATE zstack.ResourceVO set resourceType='VpcRouterVmVO' where uuid  = vrUuid;
        CALL addVpcDns(l3Uuid, vrUuid);
    END IF;
END $$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE upgradeVirtualRouterToVpc()
BEGIN
    DECLARE l3Uuid VARCHAR(32);
    DECLARE clusterType VARCHAR(32);
    DECLARE  networkServiceProviderUuid VARCHAR(255);
    DECLARE virtualRouterUuid VARCHAR(32);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR SELECT l3.uuid,cluster.type
                           FROM L3NetworkVO l3,L2NetworkClusterRefVO ref,ClusterVO cluster,NetworkServiceL3NetworkRefVO lnref
                           WHERE l3.uuid=lnref.l3NetworkUuid AND l3.l2NetworkUuid=ref.l2NetworkUuid AND ref.clusterUuid=cluster.uuid
                           AND l3.category = 'Private' AND l3.type = 'L3BasicNetwork' AND lnref.networkServiceType='SNAT';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    SELECT uuid INTO networkServiceProviderUuid FROM NetworkServiceProviderVO WHERE type="vrouter";
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO l3Uuid, clusterType;
        IF done THEN
            LEAVE read_loop;
        END IF;
        #update l3 network type to 'L3VpcNetwork'
        UPDATE zstack.L3NetworkEO SET type = 'L3VpcNetwork' WHERE uuid = l3Uuid;
        #delete DNS network service
        DELETE FROM zstack.NetworkServiceL3NetworkRefVO WHERE l3NetworkUuid = l3Uuid and networkServiceType = 'DNS';
        IF clusterType = "vmware" THEN
            INSERT INTO NetworkServiceL3NetworkRefVO (l3NetworkUuid, networkServiceProviderUuid, networkServiceType) VALUES (l3Uuid, networkServiceProviderUuid, "VRouterRoute");
            INSERT INTO NetworkServiceL3NetworkRefVO (l3NetworkUuid, networkServiceProviderUuid, networkServiceType) VALUES (l3Uuid, networkServiceProviderUuid, "VipQos");
        END IF;
        CALL getVirtualRouterUuidForL3Network(l3Uuid, virtualRouterUuid);
        CALL updateVirtualRouter(l3Uuid, virtualRouterUuid);

        DELETE FROM zstack.L3NetworkDnsVO WHERE l3NetworkUuid = l3Uuid;
    END LOOP;
    CLOSE cur;
END $$
DELIMITER ;

CALL upgradeVirtualRouterToVpc();
DROP PROCEDURE IF EXISTS upgradeVirtualRouterToVpc;
DROP PROCEDURE IF EXISTS getVirtualRouterUuidForL3Network;
DROP PROCEDURE IF EXISTS addVpcDns;
DROP PROCEDURE IF EXISTS updateVirtualRouter;

ALTER TABLE `zstack`.`VpcRouterVmVO` ADD COLUMN `generalVersion` varchar(32) DEFAULT NULL;
DROP PROCEDURE IF EXISTS insertVersionToVpcRouterVmVO;
DELIMITER $$
CREATE PROCEDURE insertVersionToVpcRouterVmVO()
BEGIN
    DECLARE vruuid varchar(255);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR select uuid from VpcRouterVmVO;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO vruuid;
        IF done THEN
            LEAVE read_loop;
        END IF;
        UPDATE zstack.VpcRouterVmVO SET generalVersion="3.10.0.0" WHERE uuid=vruuid;
    END LOOP;
    CLOSE cur;
END $$
DELIMITER ;
CALL insertVersionToVpcRouterVmVO();
DROP PROCEDURE IF EXISTS insertVersionToVpcRouterVmVO;