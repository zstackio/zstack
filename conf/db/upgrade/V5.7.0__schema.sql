-- IB devices retain the existing PCI UUID, VM binding and account references.
-- PCI classification arrives from a host synchronization, never from upgrade-time guesses.
CREATE TABLE IF NOT EXISTS `zstack`.`IbDeviceVO` (
    `uuid` varchar(32) NOT NULL,
    `displayName` varchar(255) DEFAULT NULL,
    `allocateStatus` varchar(32) NOT NULL DEFAULT 'Unknown',
    `bindingSource` varchar(32) NOT NULL DEFAULT 'Unknown',
    `collectionStatus` varchar(32) NOT NULL DEFAULT 'Unknown',
    `lastCollectedAt` timestamp NULL DEFAULT NULL,
    `reservedAt` timestamp NULL DEFAULT NULL,
    `collectionError` text DEFAULT NULL,
    `hostDriver` varchar(255) DEFAULT NULL,
    `hostRecoveryError` mediumtext DEFAULT NULL,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkIbDeviceVOPciDeviceVO` FOREIGN KEY (`uuid`)
        REFERENCES `zstack`.`PciDeviceVO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Preserve recovery evidence when this script is applied to an existing IB table.
CALL ADD_COLUMN('IbDeviceVO', 'hostDriver', 'varchar(255)', 1, NULL);
CALL ADD_COLUMN('IbDeviceVO', 'hostRecoveryError', 'mediumtext', 1, NULL);

-- Existing reservations have no reliable start time; leave them unknown.
CALL ADD_COLUMN('IbDeviceVO', 'reservedAt', 'timestamp NULL', 1, NULL);

START TRANSACTION;

-- JsonLabel stores these string values directly, without JSON quoting.
UPDATE `zstack`.`IbDeviceVO` ib
JOIN `zstack`.`JsonLabelVO` label ON label.labelKey = CONCAT('ib.hostDriver.', ib.uuid)
SET ib.hostDriver = label.labelValue
WHERE ib.hostDriver IS NULL
  AND CHAR_LENGTH(label.labelValue) BETWEEN 1 AND 255
  AND TRIM(label.labelValue) <> ''
  AND label.labelValue NOT IN ('vfio-pci', 'pci-stub', 'uio_pci_generic', 'igb_uio');

-- Preserve both reasons if a partially migrated installation already has a different failure.
UPDATE `zstack`.`IbDeviceVO` ib
JOIN `zstack`.`JsonLabelVO` label ON label.labelKey = CONCAT('ib.hostRecoveryError.', ib.uuid)
SET ib.hostRecoveryError = CASE
    WHEN label.labelValue IS NULL THEN COALESCE(ib.hostRecoveryError,
        'Host driver recovery failed; legacy record has no error details')
    WHEN ib.hostRecoveryError IS NULL THEN label.labelValue
    WHEN BINARY ib.hostRecoveryError = BINARY label.labelValue THEN ib.hostRecoveryError
    ELSE CONCAT(ib.hostRecoveryError, '\n', label.labelValue)
END;

-- Recovery labels for deleted or reclassified devices must not outlive the extension.
DELETE FROM `zstack`.`JsonLabelVO`
WHERE labelKey = CONCAT('ib.hostDriver.', resourceUuid)
   OR labelKey = CONCAT('ib.hostRecoveryError.', resourceUuid);

COMMIT;
