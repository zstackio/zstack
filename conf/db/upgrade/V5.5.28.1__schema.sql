-- ZSTAC-86635: Persist ZNS controller connection candidates outside SystemTag.
ALTER TABLE `zstack`.`ZnsControllerVO`
    ADD COLUMN `ipOwnership` varchar(32) DEFAULT NULL,
    ADD COLUMN `vipEndpoint` varchar(255) DEFAULT NULL;

CREATE TABLE `zstack`.`ZnsControllerNodeEndpointVO` (
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
