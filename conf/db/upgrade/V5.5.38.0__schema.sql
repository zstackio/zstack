-- ZSTAC-86635: Persist ZNS controller connection candidates outside SystemTag.
CALL ADD_COLUMN('ZnsControllerVO', 'ipOwnership', 'varchar(32)', 1, NULL);
CALL ADD_COLUMN('ZnsControllerVO', 'vipEndpoint', 'varchar(255)', 1, NULL);

CREATE TABLE IF NOT EXISTS `zstack`.`ZnsControllerNodeEndpointVO` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT,
    `znsControllerUuid` varchar(32) NOT NULL,
    `endpoint` varchar(255) NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_zns_controller_endpoint` (`znsControllerUuid`, `endpoint`),
    KEY `idx_zns_controller_endpoint_controller` (`znsControllerUuid`),
    CONSTRAINT `fkZnsControllerNodeEndpointVOSdnControllerVO`
        FOREIGN KEY (`znsControllerUuid`) REFERENCES `zstack`.`SdnControllerVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
