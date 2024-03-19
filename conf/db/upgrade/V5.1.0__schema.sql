CREATE TABLE `zstack`.`VpcSharedQosVO`
(
    `uuid`          varchar(32)  NOT NULL UNIQUE,
    `name`          varchar(255) NOT NULL,
    `description`   varchar(255)          DEFAULT NULL,
    `l3NetworkUuid` varchar(32)  NOT NULL,
    `vpcUuid`       varchar(32)           DEFAULT NULL,
    `bandwidth`     bigint unsigned,
    `lastOpDate`    timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate`    timestamp    NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT fkVpcSharedQosVOL3NetworkEO FOREIGN KEY (l3NetworkUuid) REFERENCES L3NetworkEO (uuid) ON DELETE CASCADE,
    CONSTRAINT fkVpcSharedQosVOApplianceVmVO FOREIGN KEY (vpcUuid) REFERENCES ApplianceVmVO (uuid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`VpcSharedQosRefVipVO`
(
    `id`            bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `sharedQosUuid` varchar(32) NOT NULL,
    `vipUuid`       varchar(32) NOT NULL,
    `lastOpDate`    timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate`    timestamp   NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`id`),
    CONSTRAINT fkVpcSharedQosRefVipVOVpcSharedQosVO FOREIGN KEY (sharedQosUuid) REFERENCES VpcSharedQosVO (uuid) ON DELETE CASCADE,
    CONSTRAINT fkVpcSharedQosRefVipVOVipVO FOREIGN KEY (vipUuid) REFERENCES VipVO (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

