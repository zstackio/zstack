CREATE TABLE IF NOT EXISTS `zstack`.`QuotaReservationVO` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT,
    `accountUuid` varchar(32) NOT NULL,
    `quotaName` varchar(255) NOT NULL,
    `amount` bigint NOT NULL,
    `operationUuid` varchar(32) NOT NULL,
    `resourceUuid` varchar(32) DEFAULT NULL,
    `createDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `ukQuotaReservationVOOperationQuota` (`operationUuid`, `quotaName`),
    KEY `idxQuotaReservationVOAccountUuid` (`accountUuid`),
    KEY `idxQuotaReservationVOQuotaName` (`quotaName`),
    KEY `idxQuotaReservationVOOperationUuid` (`operationUuid`),
    KEY `idxQuotaReservationVOResourceUuid` (`resourceUuid`),
    CONSTRAINT `fkQuotaReservationVOAccountVO` FOREIGN KEY (`accountUuid`) REFERENCES `zstack`.`AccountVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
