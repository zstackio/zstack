alter table AliyunProxyVpcVO modify vpcName varchar(128) not null;

alter table AliyunProxyVpcVO
    drop foreign key fkAliyunProxyVpcVOVmInstanceEO;
alter table AliyunProxyVpcVO
    add constraint fkAliyunProxyVpcVOVmInstanceEO foreign key (vRouterUuid) references VmInstanceEO (uuid);

alter table AliyunProxyVSwitchVO
    drop foreign key fkAliyunProxyVSwitchVOAliyunProxyVpcVO;
alter table AliyunProxyVSwitchVO
    add constraint fkAliyunProxyVSwitchVOAliyunProxyVpcVO foreign key (aliyunProxyVpcUuid) references AliyunProxyVpcVO (uuid);
alter table AliyunProxyVSwitchVO
    drop foreign key fkAliyunProxyVSwitchVOL3NetworkEO;
alter table AliyunProxyVSwitchVO
    add constraint fkAliyunProxyVSwitchVOL3NetworkEO foreign key (vpcL3NetworkUuid) references L3NetworkEO (uuid);

CREATE TABLE IF NOT EXISTS `zstack`.`MetricDataHttpReceiverVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(256) NOT NULL,
    `url` varchar(256) NOT NULL,
    `state` VARCHAR(128) NOT NUll,
    `description` VARCHAR(2048) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`MetricTemplateVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `receiverUuid` varchar(32) NOT NULL,
    `template` varchar(4096) NOT NULL,
    `namespace` varchar(64) NOT NULL,
    `metricName` varchar(128) NOT NULL,
    `labelsJsonStr` varchar(256) DEFAULT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkMetricTemplateVOMetricDataHttpReceiverVO` FOREIGN KEY (`receiverUuid`) REFERENCES `zstack`.`MetricDataHttpReceiverVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;