CALL INSERT_COLUMN('AlarmRecordsVO', 'operatorAccountUuid', 'char(32)', 1, NULL, 'readStatus');
CALL INSERT_COLUMN('EventRecordsVO', 'operatorAccountUuid', 'char(32)', 1, NULL, 'readStatus');

-- Feature: Vm Dns Support | ZSV-7802

CREATE TABLE IF NOT EXISTS `zstack`.`VmDnsVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `vmInstanceUuid` char(32) NOT NULL,
    `vmNicUuid` char(32) DEFAULT NULL,
    `dns` varchar(255) NOT NULL,
    `ipVersion` int(10) unsigned DEFAULT 4,
    `lastOpDate` timestamp on update CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkVmDnsVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkVmDnsVOVmNicVO` FOREIGN KEY (`vmNicUuid`) REFERENCES `VmNicVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
