CREATE TABLE  `zstack`.`KeystoreVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `resourceUuid` varchar(32) NOT NULL,
    `resourceType` varchar(255) NOT NULL,
    `type` varchar(255) NOT NULL,
    `content` varchar(65535) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`AccountResourceRefVO` ADD UNIQUE INDEX(resourceUuid,resourceType);

ALTER TABLE `zstack`.`ConsoleProxyAgentVO` modify column description varchar(2048) DEFAULT NULL;
ALTER TABLE `zstack`.`ImageEO` modify column description varchar(2048) DEFAULT NULL COMMENT 'image description';
ALTER TABLE `zstack`.`InstanceOfferingEO` column description varchar(2048) DEFAULT NULL  COMMENT 'instance offering description';
ALTER TABLE `zstack`.`DiskOfferingEO` column description varchar(2048) DEFAULT NULL COMMENT 'disk offering description';
ALTER TABLE `zstack`.`VolumeEO` column description varchar(2048) DEFAULT NULL;
ALTER TABLE `zstack`.`VipVO` column description varchar(2048) DEFAULT NULL;
ALTER TABLE `zstack`.`GlobalConfigVO` column description varchar(2048) DEFAULT NULL;
ALTER TABLE `zstack`.`NetworkServiceProviderVO` column description varchar(2048) DEFAULT NULL;
ALTER TABLE `zstack`.`SecurityGroupVO` column description varchar(2048) DEFAULT NULL;