-- Add ExternalServiceConfiguration table
CREATE TABLE IF NOT EXISTS `zstack`.`ExternalServiceConfigurationVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `serviceType` varchar(32) NOT NULL,
    `configuration` text DEFAULT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ResNotifySubscriptionVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `name` VARCHAR(255) DEFAULT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `resourceTypes` TEXT NOT NULL,
    `eventTypes` VARCHAR(256) DEFAULT NULL,
    `type` VARCHAR(32) NOT NULL DEFAULT 'WEBHOOK',
    `state` VARCHAR(32) NOT NULL DEFAULT 'Enabled',
    `lastOpDate` TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` TIMESTAMP,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ResNotifyWebhookRefVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `webhookUrl` TEXT NOT NULL,
    `secret` VARCHAR(256) DEFAULT NULL,
    `customHeaders` TEXT,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fk_ResNotifyWebhookRefVO_ResNotifySubscriptionVO` 
        FOREIGN KEY (`uuid`) REFERENCES `ResNotifySubscriptionVO`(`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ExternalTenantResourceRefVO` (
    `id`            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `source`        VARCHAR(64)  NOT NULL COMMENT 'source service identifier (zcf, svcX, ...)',
    `tenantId`      VARCHAR(128) NOT NULL COMMENT 'external tenant identifier',
    `userId`        VARCHAR(128) DEFAULT NULL COMMENT 'external user identifier (optional)',
    `resourceUuid`  VARCHAR(32)  NOT NULL COMMENT 'resource UUID',
    `resourceType`  VARCHAR(256) NOT NULL COMMENT 'resource type (VO SimpleName)',
    `accountUuid`   VARCHAR(32)  NOT NULL COMMENT 'associated ZStack Account',
    `createDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_source_tenant_user (`source`, `tenantId`, `userId`),
    INDEX idx_source_tenant_resource (`source`, `tenantId`, `resourceUuid`),
    INDEX idx_resource (`resourceUuid`),
    UNIQUE KEY uk_resource_source_tenant (`resourceUuid`, `source`, `tenantId`),
    CONSTRAINT fk_ext_tenant_resource FOREIGN KEY (`resourceUuid`)
        REFERENCES `ResourceVO`(`uuid`) ON DELETE CASCADE,
    CONSTRAINT fk_ext_tenant_account FOREIGN KEY (`accountUuid`)
        REFERENCES `AccountVO`(`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
