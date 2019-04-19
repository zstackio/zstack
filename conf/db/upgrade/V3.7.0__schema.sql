-- ----------------------------
--  For multicast router
-- ----------------------------
CREATE TABLE `MulticastRouterVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `description` VARCHAR(2048) DEFAULT NULL,
    `state` VARCHAR(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `MulticastRouterRendezvousPointVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `multicastRouterUuid` VARCHAR(32) NOT NULL,
    `rpAddress` VARCHAR(64) NOT NULL,
    `groupAddress` VARCHAR(64) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    CONSTRAINT fkMultiCastRouterRendezvousPointVOMulticastRouterVO FOREIGN KEY (multicastRouterUuid) REFERENCES MulticastRouterVO (uuid) ON DELETE CASCADE,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `MulticastRouterVpcVRouterRefVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `vpcRouterUuid` VARCHAR(32) NOT NULL,
    CONSTRAINT fkMulticastRouterVpcVRouterRefVOMulticastRouterVO FOREIGN KEY (uuid) REFERENCES MulticastRouterVO (uuid) ON DELETE CASCADE,
    CONSTRAINT fkMulticastRouterVpcVRouterRefVOVpcRouterVmVO FOREIGN KEY (vpcRouterUuid) REFERENCES VpcRouterVmVO (uuid) ON DELETE CASCADE,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;