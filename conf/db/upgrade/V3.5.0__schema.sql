ALTER TABLE VolumeSnapshotTreeEO ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT "Completed";
DROP VIEW IF EXISTS `zstack`.`VolumeSnapshotTreeVO`;
CREATE VIEW `zstack`.`VolumeSnapshotTreeVO` AS SELECT uuid, volumeUuid, current, status, createDate, lastOpDate FROM `zstack`.`VolumeSnapshotTreeEO` WHERE deleted IS NULL;
CREATE TABLE `zstack`.`VipNetworkServicesRefVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `serviceType` VARCHAR(32) NOT NULL,
    `vipUuid` VARCHAR(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    CONSTRAINT fkVipNetworkServicesRefVOVipVO FOREIGN KEY (vipUuid) REFERENCES VipVO (uuid) ON DELETE CASCADE,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO VipNetworkServicesRefVO (`uuid`, `serviceType`, `vipUuid`, `lastOpDate`, `createDate`) SELECT s.uuid, "LoadBalancer", s.vipUuid, s.createDate, s.createDate FROM LoadBalancerVO s;
INSERT INTO VipNetworkServicesRefVO (`uuid`, `serviceType`, `vipUuid`, `lastOpDate`, `createDate`) SELECT s.uuid, "PortForwarding", s.vipUuid, s.createDate, s.createDate  FROM PortForwardingRuleVO s;
INSERT INTO VipNetworkServicesRefVO (`uuid`, `serviceType`, `vipUuid`, `lastOpDate`, `createDate`) SELECT s.uuid, "IPsec", s.vipUuid, s.createDate, s.createDate  FROM IPsecConnectionVO s;
INSERT INTO VipNetworkServicesRefVO (`uuid`, `serviceType`, `vipUuid`, `lastOpDate`, `createDate`) SELECT s.uuid, "Eip", s.vipUuid, s.createDate, s.createDate  FROM EipVO s;
INSERT INTO VipNetworkServicesRefVO (`uuid`, `serviceType`, `vipUuid`, `lastOpDate`, `createDate`) SELECT s.uuid, "SNAT", s.uuid, current_timestamp(), current_timestamp()  FROM VirtualRouterVipVO s;
