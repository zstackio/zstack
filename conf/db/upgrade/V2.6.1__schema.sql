alter table VmNicVO modify vmInstanceUuid varchar(32) NULL COMMENT 'vm instance uuid';
alter table VmNicVO modify internalName varchar(128) NULL;

CREATE TABLE `AliyunProxyVpcVO` (
  `uuid` VARCHAR(32) NOT NULL,
  `status` VARCHAR(32) NOT NULL,
  `vpcName` VARCHAR(32) DEFAULT NULL,
  `cidrBlock` VARCHAR(128) NOT NULL,
  `vRouterUuid` VARCHAR(32) NOT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `isDefault` tinyint(1) unsigned DEFAULT 0,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkAliyunProxyVpcVOVmInstanceEO` FOREIGN KEY (`vRouterUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AliyunProxyVSwitchVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `vpcL3NetworkUuid` VARCHAR(32) NOT NULL,
    `aliyunProxyVpcUuid` VARCHAR(32) NOT NULL,
    `status` VARCHAR(32) NOT NULL,
    `isDefault` tinyint(1) unsigned DEFAULT 0,
    CONSTRAINT `fkAliyunProxyVSwitchVOAliyunProxyVpcVO` FOREIGN KEY (`aliyunProxyVpcUuid`) REFERENCES `AliyunProxyVpcVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkAliyunProxyVSwitchVOL3NetworkEO` FOREIGN KEY (`vpcL3NetworkUuid`) REFERENCES `L3NetworkEO` (`uuid`) ON DELETE CASCADE
)  ENGINE=InnoDB DEFAULT CHARSET=utf8;