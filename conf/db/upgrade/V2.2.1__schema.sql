CREATE TABLE `UsbDeviceVO` (
  `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
  `name` varchar(2048) DEFAULT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `hostUuid` varchar(32) NOT NULL,
  `vmInstanceUuid` varchar(32) DEFAULT NULL,
  `state` varchar(32) NOT NULL,
  `busNum` varchar(32) NOT NULL,
  `devNum` varchar(32) NOT NULL,
  `idVendor` varchar(32) NOT NULL,
  `idProduct` varchar(32) NOT NULL,
  `iManufacturer` varchar(1024) DEFAULT NULL,
  `iProduct` varchar(1024) DEFAULT NULL,
  `iSerial` varchar(32) DEFAULT NULL,
  `usbVersion` varchar(32) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  CONSTRAINT fkUsbDeviceVOHostEO FOREIGN KEY (`hostUuid`) REFERENCES `zstack`.`HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# add Vip for virtual router pub ip for ZSTAC-4362
DELIMITER $$
CREATE PROCEDURE getIpRangeUuidForNetwork(IN networkUuid VARCHAR(32), IN gateway VARCHAR(32), OUT ipRangeUuid VARCHAR(32))
    BEGIN
        DECLARE done INT DEFAULT FALSE;
        DECLARE cur CURSOR FOR SELECT uuid from zstack.IpRangeVO ipr where ipr.l3NetworkUuid=networkUuid and ipr.gateway=gateway;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        OPEN cur;
        read_loop: LOOP
            fetch cur INTO ipRangeUuid;
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
CREATE PROCEDURE getUseIpUuid(IN ipRangeUuid VARCHAR(32), IN ip VARCHAR(255), OUT usedIpUuid VARCHAR(32))
    BEGIN
        DECLARE done INT DEFAULT FALSE;
        DECLARE cur CURSOR FOR SELECT uuid from zstack.UsedIpVO ipv  where ipv.ipRangeUuid=ipRangeUuid and ipv.ip=ip;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        OPEN cur;
        read_loop: LOOP
            fetch cur INTO usedIpUuid;
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
CREATE PROCEDURE getServiceProviderForNetwork(IN networkUuid VARCHAR(32), OUT serviceProvider VARCHAR(255))
    BEGIN
        DECLARE done INT DEFAULT FALSE;
        DECLARE cur CURSOR FOR SELECT sp.name FROM zstack.NetworkServiceProviderVO sp, zstack.NetworkServiceL3NetworkRefVO net WHERE net.networkServiceProviderUuid = sp.uuid and net.l3NetworkUuid=networkUuid and net.networkServiceType='SNAT';
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        OPEN cur;
        read_loop: LOOP
            fetch cur INTO serviceProvider;
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
CREATE PROCEDURE getVRouterGuestL3Network(IN vmInstanceUuid VARCHAR(32), IN pubL3Network VARCHAR(32),OUT GuestL3Network VARCHAR(32))
    BEGIN
        DECLARE done INT DEFAULT FALSE;
        DECLARE cur CURSOR FOR SELECT distinct l3NetworkUuid from zstack.VmNicVO nic where nic.vmInstanceUuid = vmInstanceUuid and nic.l3NetworkUuid != pubL3Network;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        OPEN cur;
        read_loop: LOOP
            fetch cur INTO GuestL3Network;
            SELECT vmInstanceUuid, pubL3Network, GuestL3Network;
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
CREATE PROCEDURE generateVipForVirtualRouterPubIP()
    BEGIN
        DECLARE name VARCHAR(255);
        DECLARE peerL3NetworkUuid VARCHAR(32);
        DECLARE publicNetworkUuid VARCHAR(32);
        DECLARE ipRangeUuid VARCHAR(32);
        DECLARE usedIpUuid VARCHAR(32);
        DECLARE uuid VARCHAR(32);
        DECLARE vmInstanceUuid VARCHAR(32);
        DECLARE ip VARCHAR(128);
        DECLARE gateway VARCHAR(128);
        DECLARE netmask VARCHAR(128);
        DECLARE serviceProvider VARCHAR(255);
        DECLARE done INT DEFAULT FALSE;
        DECLARE cur CURSOR FOR SELECT viv.name, viv.uuid, nic.l3NetworkUuid, nic.ip, nic.gateway, nic.netmask FROM  zstack.VmNicVO nic, zstack.VirtualRouterVmVO vrv, zstack.VmInstanceVO viv
                                      where viv.uuid = vrv.uuid and vrv.uuid=nic.vmInstanceUuid and nic.l3NetworkUuid=vrv.publicNetworkUuid;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO name, vmInstanceUuid, publicNetworkUuid, ip, gateway, netmask;
            IF done THEN
                LEAVE read_loop;
            END IF;

            SET peerL3NetworkUuid = NULL;
            SET serviceProvider = NULL;
            SET ipRangeUuid = NULL;
            SET usedIpUuid = NULL;
            SET uuid = REPLACE(UUID(), '-', '');
            SELECT name, vmInstanceUuid, publicNetworkUuid, ip, gateway, netmask;
            CALL getVRouterGuestL3Network(vmInstanceUuid, publicNetworkUuid, peerL3NetworkUuid);
            SELECT peerL3NetworkUuid;
            CALL getServiceProviderForNetwork(peerL3NetworkUuid, serviceProvider);
            SELECT serviceProvider;
            CALL getIpRangeUuidForNetwork(publicNetworkUuid, gateway, ipRangeUuid);
            SELECT ipRangeUuid;
            CALL getUseIpUuid(ipRangeUuid, ip, usedIpUuid);
            SELECT usedIpUuid;
            INSERT INTO zstack.VipVO (uuid, name, l3NetworkUuid, peerL3NetworkUuid, state, ip, gateway, netmask, useFor, serviceProvider, ipRangeUuid, usedIpUuid)
                                     values(uuid, CONCAT('Vip-', name), publicNetworkUuid, peerL3NetworkUuid, 'Enabled', ip, gateway, netmask, 'SNAT', serviceProvider, ipRangeUuid, usedIpUuid);

        END LOOP;
        CLOSE cur;
        # work around a bug of mysql : jira.mariadb.org/browse/MDEV-4602
        SELECT CURTIME();
    END $$
DELIMITER ;

CALL generateVipForVirtualRouterPubIP();
DROP PROCEDURE IF EXISTS generateVipForVirtualRouterPubIP;
DROP PROCEDURE IF EXISTS getServiceProviderForNetwork;
DROP PROCEDURE IF EXISTS getIpRangeUuidForNetwork;
DROP PROCEDURE IF EXISTS getUseIpUuid;
DROP PROCEDURE IF EXISTS getVRouterGuestL3Network;
