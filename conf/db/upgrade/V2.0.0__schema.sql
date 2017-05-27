ALTER TABLE VmInstanceEO ADD CONSTRAINT fkVmInstanceEOVolumeEO FOREIGN KEY (rootVolumeUuid) REFERENCES VolumeEO (uuid) ON DELETE SET NULL;

-- ----------------------------
--  Table structure for `BaremetalPxeServerVO`
-- ----------------------------
CREATE TABLE `BaremetalPxeServerVO` (
  `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
  `dhcpInterface` varchar(128) NOT NULL UNIQUE COMMENT 'pxe dhcp interface',
  `dhcpRangeBegin` varchar(32) DEFAULT NULL COMMENT 'dhcp range begin',
  `dhcpRangeEnd` varchar(32) DEFAULT NULL COMMENT 'dhcp range end',
  `dhcpRangeNetmask` varchar(32) DEFAULT NULL COMMENT 'dhcp range netmask',
  `status` varchar(32) DEFAULT NULL COMMENT 'pxe server status',
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp,
  PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `BaremetalChessisVO`
-- ----------------------------
CREATE TABLE `BaremetalChessisVO` (
  `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
  `ipmiAddress` varchar(32) NOT NULL UNIQUE COMMENT 'baremetal chessis ipmi address',
  `ipmiUsername` varchar(255) NOT NULL COMMENT 'baremetal chessis ipmi username',
  `ipmiPassword` varchar(255) NOT NULL COMMENT 'baremetal chessis ipmi password',
  `provisioned` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'is baremetal host provisioned already',
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp,
  PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `BaremetalHostCfgVO`
-- ----------------------------
CREATE TABLE `BaremetalHostCfgVO` (
  `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
  `chessisUuid` varchar(32) NOT NULL UNIQUE COMMENT 'baremetal chessis uuid',
  `password` varchar(255) DEFAULT NULL COMMENT 'host root password',
  `vnc` tinyint(1) unsigned NOT NULL DEFAULT 1 COMMENT 'start vnc or not',
  `unattended` tinyint(1) unsigned NOT NULL DEFAULT 1 COMMENT 'unattended installation process or not',
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp,
  PRIMARY KEY  (`uuid`),
  CONSTRAINT `fkBaremetalHostCfgVOBaremetalChessisVO` FOREIGN KEY (`chessisUuid`) REFERENCES `BaremetalChessisVO` (`uuid`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `BaremetalHostNicCfgVO`
-- ----------------------------
CREATE TABLE `BaremetalHostNicCfgVO` (
  `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
  `hostCfgUuid` varchar(32) NOT NULL COMMENT 'baremetal hostcfg uuid',
  `mac` varchar(32) NOT NULL UNIQUE COMMENT 'host nic mac',
  `ip` varchar(32) NOT NULL UNIQUE COMMENT 'host nic ip',
  `netmask` varchar(32) NOT NULL COMMENT 'host nic netmask',
  `gateway` varchar(32) DEFAULT NULL COMMENT 'host nic gateway',
  `dns` varchar(32) DEFAULT NULL COMMENT 'host nic dns',
  `pxe` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT 'as pxe client or not',
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp,
  PRIMARY KEY  (`uuid`),
  CONSTRAINT `fkBaremetalHostNicCfgVOBaremetalHostCfgVO` FOREIGN KEY (`hostCfgUuid`) REFERENCES `BaremetalHostCfgVO` (`uuid`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO ResourceVO (uuid, resourceType) SELECT t.uuid, "BaremetalPxeServerVO" FROM BaremetalPxeServerVO t;
INSERT INTO ResourceVO (uuid, resourceType) SELECT t.uuid, "BaremetalChessisVO" FROM BaremetalChessisVO t;
INSERT INTO ResourceVO (uuid, resourceType) SELECT t.uuid, "BaremetalHostCfgVO" FROM BaremetalHostCfgVO t;
INSERT INTO ResourceVO (uuid, resourceType) SELECT t.uuid, "BaremetalHostNicCfgVO" FROM BaremetalHostNicCfgVO t;
alter table OssBucketVO add description varchar(1024) DEFAULT NULL;

SET FOREIGN_KEY_CHECKS = 0;
CREATE TABLE `VpcUserVpnGatewayVO` (
	  `uuid` varchar(32) UNIQUE NOT NULL,
	  `accountName` varchar(128) NOT NULL,
	  `gatewayId` varchar(32) NOT NULL,
	  `dataCenterUuid` varchar(32) NOT NULL,
	  `type` varchar(32) NOT NULL,
	  `name` varchar(128) NOT NULL,
	  `ip` varchar(32) NOT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  UNIQUE KEY `ukVpcUserVpnGatewayVO` (`dataCenterUuid`,`accountName`,`gatewayId`) USING BTREE,
	  CONSTRAINT fkVpcUserVpnGatewayVODataCenterVO FOREIGN KEY (dataCenterUuid) REFERENCES DataCenterVO (uuid) ON DELETE RESTRICT,
	  CONSTRAINT fkVpcUserVpnGatewayVOAccountVO FOREIGN KEY (accountName) REFERENCES AccountVO (name) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VpcVpnConnectionVO` (
	  `uuid` varchar(32) UNIQUE NOT NULL,
	  `accountName` varchar(128) NOT NULL,
	  `connectionId` varchar(32) NOT NULL,
	  `userGatewayUuid` varchar(32) NOT NULL,
	  `vpnGatewayUuid` varchar(32) NOT NULL,
	  `type` varchar(32) NOT NULL,
	  `name` varchar(128) NOT NULL,
	  `localSubnet` varchar(64) NOT NULL,
	  `remoteSubnet` varchar(64) NOT NULL,
	  `ikeConfigUuid` varchar(32) NOT NULL,
	  `ipsecConfigUuid` varchar(32) NOT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  UNIQUE KEY `ukVpcVpnConnectionVO` (`connectionId`,`accountName`,`userGatewayUuid`) USING BTREE,
	  CONSTRAINT fkVpcVpnConnectionVOVpcUserVpnGatewayVO FOREIGN KEY (userGatewayUuid) REFERENCES VpcUserVpnGatewayVO (uuid) ON DELETE RESTRICT,
	  CONSTRAINT fkVpcVpnConnectionVOVpcVpnGatewayVO FOREIGN KEY (vpnGatewayUuid) REFERENCES VpcVpnGatewayVO (uuid) ON DELETE RESTRICT,
	  CONSTRAINT fkVpcVpnConnectionVOVpcVpnIkeConfigVO FOREIGN KEY (ikeConfigUuid) REFERENCES VpcVpnIkeConfigVO (uuid) ON DELETE RESTRICT,
	  CONSTRAINT fkVpcVpnConnectionVOVpcVpnIpSecConfigVO FOREIGN KEY (ipsecConfigUuid) REFERENCES VpcVpnIpSecConfigVO (uuid) ON DELETE RESTRICT,
	  CONSTRAINT fkVpcVpnConnectionVOAccountVO FOREIGN KEY (accountName) REFERENCES AccountVO (name) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VpcVpnGatewayVO` (
	  `uuid` varchar(32) UNIQUE NOT NULL,
	  `accountName` varchar(128) NOT NULL,
	  `gatewayId` varchar(32) NOT NULL,
	  `vSwitchUuid` varchar(32) NOT NULL,
	  `type` varchar(32) NOT NULL,
	  `name` varchar(128) NOT NULL,
	  `publicIp` varchar(32) NOT NULL,
	  `spec` varchar(32) NOT NULL,
	  `status` varchar(32) NOT NULL,
	  `businessStatus` varchar(32) NOT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `endDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  UNIQUE KEY `ukVpcVpnGatewayVO` (`vSwitchUuid`,`accountName`,`gatewayId`) USING BTREE,
	  CONSTRAINT fkVpcVpnGatewayVOEcsVSwitchVO FOREIGN KEY (vSwitchUuid) REFERENCES EcsVSwitchVO (uuid) ON DELETE RESTRICT,
	  CONSTRAINT fkVpcVpnGatewayVOAccountVO FOREIGN KEY (accountName) REFERENCES AccountVO (name) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



CREATE TABLE `VpcVpnIkeConfigVO` (
	  `uuid` varchar(32) UNIQUE NOT NULL,
	  `accountName` varchar(128) NOT NULL,
	  `name` varchar(128) NOT NULL,
	  `psk` varchar(32) NOT NULL,
	  `version` varchar(32) NOT NULL,
	  `mode` varchar(32) NOT NULL,
	  `encodeAlgorithm` varchar(32) NOT NULL,
	  `authAlgorithm` varchar(32) NOT NULL,
	  `pfs` varchar(32) NOT NULL,
	  `lifetime` bigint unsigned NOT NULL,
	  `localIp` varchar(32) NOT NULL,
	  `remoteIp` varchar(32) NOT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  UNIQUE KEY `ukVpcVpnIkeConfigVO` (`name`,`accountName`) USING BTREE,
	  CONSTRAINT fkVpcVpnIkeConfigVOAccountVO FOREIGN KEY (accountName) REFERENCES AccountVO (name) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VpcVpnIpSecConfigVO` (
	  `uuid` varchar(32) UNIQUE NOT NULL,
	  `accountName` varchar(128) NOT NULL,
	  `name` varchar(128) NOT NULL,
	  `encodeAlgorithm` varchar(32) NOT NULL,
	  `authAlgorithm` varchar(32) NOT NULL,
	  `pfs` varchar(32) NOT NULL,
	  `lifetime` bigint unsigned NOT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  UNIQUE KEY `ukVpcVpnIpSecConfigVO` (`name`,`accountName`) USING BTREE,
	  CONSTRAINT fkVpcVpnIpSecConfigVOAccountVO FOREIGN KEY (accountName) REFERENCES AccountVO (name) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO ResourceVO (uuid, resourceType) SELECT t.uuid, "VpcUserVpnGatewayVO" FROM VpcUserVpnGatewayVO t;
INSERT INTO ResourceVO (uuid, resourceType) SELECT t.uuid, "VpcVpnConnectionVO" FROM VpcVpnConnectionVO t;
INSERT INTO ResourceVO (uuid, resourceType) SELECT t.uuid, "VpcVpnGatewayVO" FROM VpcVpnGatewayVO t;
INSERT INTO ResourceVO (uuid, resourceType) SELECT t.uuid, "VpcVpnIkeConfigVO" FROM VpcVpnIkeConfigVO t;
INSERT INTO ResourceVO (uuid, resourceType) SELECT t.uuid, "VpcVpnIpSecConfigVO" FROM VpcVpnIpSecConfigVO t;
