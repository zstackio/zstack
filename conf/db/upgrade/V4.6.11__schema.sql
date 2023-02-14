ALTER TABLE `zstack`.`L2NetworkEO` MODIFY `vSwitchType` varchar(32) NOT NULL DEFAULT 'LinuxBridge' AFTER `type`;
ALTER TABLE `zstack`.`L2NetworkEO` ADD COLUMN `virtualNetworkId` int unsigned NOT NULL DEFAULT 0 AFTER `vSwitchType`;
UPDATE `zstack`.`L2NetworkEO` l2 INNER JOIN `zstack`.`L2VlanNetworkVO` vlan ON l2.uuid = vlan.uuid SET l2.virtualNetworkId = vlan.vlan;
UPDATE `zstack`.`L2NetworkEO` l2 INNER JOIN `zstack`.`VxlanNetworkVO` vxlan ON l2.uuid = vxlan.uuid SET l2.virtualNetworkId = vxlan.vni;
DROP VIEW IF EXISTS `zstack`.L2NetworkVO;
CREATE VIEW `zstack`.`L2NetworkVO` AS SELECT uuid, name, description, type, vSwitchType, virtualNetworkId, zoneUuid, physicalInterface, createDate, lastOpDate FROM `zstack`.`L2NetworkEO` WHERE deleted IS NULL;

insert into `zstack`.`LicenseHistoryVO` (`uuid`, `cpuNum`, `hostNum`, `vmNum`, `capacity`, `expiredDate`, `issuedDate`, `uploadDate`, `licenseType`, `userName`, `prodInfo`, `createDate`, `lastOpDate`, `hash`, `source`, `managementNodeUuid`, `mergedTo`)
    select `uuid`, `cpuNum`, `hostNum`, `vmNum`, `capacity`, `expiredDate`, `issuedDate`, `uploadDate`, 'AddOn' as `licenseType`, `userName`, 'hybrid' as `prodInfo`, `createDate`, `lastOpDate`, `hash`, `source`, `managementNodeUuid`, `mergedTo`
    from `zstack`.`LicenseHistoryVO`
    where `licenseType`='Hybrid';
update `zstack`.`LicenseHistoryVO` set `licenseType`='Paid' where `licenseType`='Hybrid';

CREATE TABLE IF NOT EXISTS `zstack`.`SSOTokenVO`(
    `uuid` varchar(32) not null unique,
    `clientUuid` varchar(32) DEFAULT NULL,
    `userUuid` varchar(32) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkSSOTokenVOClientVO` FOREIGN KEY (`clientUuid`) REFERENCES `SSOClientVO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`OAuth2TokenVO`(
    `uuid` varchar(32) not null unique,
    `accessToken` varchar(2048) not null,
    `idToken` varchar(2048) not null,
    `refreshToken` varchar(2048) not null,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkOAuth2TokenVOSSOTokenVO` FOREIGN KEY (`uuid`) REFERENCES `SSOTokenVO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

