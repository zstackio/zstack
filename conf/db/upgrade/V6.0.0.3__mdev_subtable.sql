-- V6.0.0.3: Convert MdevDeviceVO from standalone to PciDeviceVO subtable

DELIMITER $$
DROP PROCEDURE IF EXISTS migrate_mdev_subtable$$
CREATE PROCEDURE migrate_mdev_subtable()
BEGIN
    -- Only proceed if old MdevDeviceVO exists and still has `hostUuid` column
    -- (the new subtable only has uuid/mdevSpecUuid/mttyUuid/mdevDeviceAddress).
    -- Cannot use fkMdevDeviceVOPciDeviceVO as guard because the old table also has
    -- a constraint with that exact name (parentUuid -> PciDeviceVO).
    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'MdevDeviceVO'
               AND COLUMN_NAME = 'hostUuid') THEN

        -- Step 1: Migrate MdevDeviceVO shared fields into PciDeviceVO
        INSERT INTO `PciDeviceVO` (
            `uuid`, `name`, `description`,
            `hostUuid`, `parentUuid`, `vmInstanceUuid`,
            `type`, `state`, `status`, `virtStatus`, `chooser`,
            `vendor`,
            `vendorId`, `deviceId`, `subvendorId`, `subdeviceId`,
            `pciDeviceAddress`,
            `createDate`, `lastOpDate`
        )
        SELECT
            m.`uuid`, m.`name`, m.`description`,
            m.`hostUuid`, m.`parentUuid`, m.`vmInstanceUuid`,
            m.`type`,
            m.`state`,
            m.`status`,
            'VFIO_MDEV_VIRTUAL',
            m.`chooser`,
            m.`vendor`,
            p.`vendorId`, p.`deviceId`, p.`subvendorId`, p.`subdeviceId`,
            m.`mdevDeviceAddress`,
            m.`createDate`, m.`lastOpDate`
        FROM `MdevDeviceVO` m
        LEFT JOIN `PciDeviceVO` p ON m.`parentUuid` = p.`uuid`
        WHERE NOT EXISTS (
            SELECT 1 FROM `PciDeviceVO` pci WHERE pci.`uuid` = m.`uuid`
        );

        -- Step 1.5: Drop FK constraints on old MdevDeviceVO before RENAME
        -- MySQL InnoDB FK constraint names are DB-global unique; RENAME TABLE does NOT
        -- rename them, so the new table's CREATE would fail with duplicate constraint name.
        ALTER TABLE `MdevDeviceVO` DROP FOREIGN KEY `fkMdevDeviceVOHostEO`;
        ALTER TABLE `MdevDeviceVO` DROP FOREIGN KEY `fkMdevDeviceVOPciDeviceVO`;
        ALTER TABLE `MdevDeviceVO` DROP FOREIGN KEY `fkMdevDeviceVOVmInstanceEO`;
        ALTER TABLE `MdevDeviceVO` DROP FOREIGN KEY `fkMdevDeviceVOMdevSpecVO`;

        -- fkMdevDeviceVOMttyDeviceVO added in V4.5.11; may not exist in older deployments
        IF EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
                   WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'MdevDeviceVO'
                   AND CONSTRAINT_NAME = 'fkMdevDeviceVOMttyDeviceVO') THEN
            ALTER TABLE `MdevDeviceVO` DROP FOREIGN KEY `fkMdevDeviceVOMttyDeviceVO`;
        END IF;

        -- Drop child FK from VmInstanceMdevSpecDeviceRefVO that references MdevDeviceVO;
        -- after RENAME it would follow the old table, breaking referential integrity.
        IF EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
                   WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'VmInstanceMdevSpecDeviceRefVO'
                   AND CONSTRAINT_NAME = 'fkVmMdevDeviceRefMdevDeviceUuid') THEN
            ALTER TABLE `VmInstanceMdevSpecDeviceRefVO` DROP FOREIGN KEY `fkVmMdevDeviceRefMdevDeviceUuid`;
        END IF;

        -- Step 2: Rename old MdevDeviceVO table (now safe — no FK name collisions)
        RENAME TABLE `MdevDeviceVO` TO `MdevDeviceVO_old`;

        -- Step 3: Create new MdevDeviceVO as subtable of PciDeviceVO
        CREATE TABLE `MdevDeviceVO` (
            `uuid` VARCHAR(32) NOT NULL UNIQUE,
            `mdevSpecUuid` VARCHAR(32) DEFAULT NULL,
            `mttyUuid` VARCHAR(32) DEFAULT NULL,
            `mdevDeviceAddress` VARCHAR(128) DEFAULT NULL,
            PRIMARY KEY (`uuid`),
            CONSTRAINT `fkMdevDeviceVOPciDeviceVO`
                FOREIGN KEY (`uuid`) REFERENCES `PciDeviceVO`(`uuid`) ON DELETE CASCADE,
            CONSTRAINT `fkMdevDeviceVOMdevSpecVO`
                FOREIGN KEY (`mdevSpecUuid`) REFERENCES `MdevDeviceSpecVO`(`uuid`) ON DELETE SET NULL,
            CONSTRAINT `fkMdevDeviceVOMttyDeviceVO`
                FOREIGN KEY (`mttyUuid`) REFERENCES `MttyDeviceVO`(`uuid`) ON DELETE CASCADE
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

        -- Step 4: Migrate mdev-specific data to new subtable
        INSERT IGNORE INTO `MdevDeviceVO` (`uuid`, `mdevSpecUuid`, `mttyUuid`, `mdevDeviceAddress`)
        SELECT `uuid`, `mdevSpecUuid`, `mttyUuid`, `mdevDeviceAddress`
        FROM `MdevDeviceVO_old`;

        -- Step 4.5: Re-create child FK pointing to the NEW MdevDeviceVO
        IF EXISTS (SELECT 1 FROM information_schema.TABLES
                   WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'VmInstanceMdevSpecDeviceRefVO') THEN
            ALTER TABLE `VmInstanceMdevSpecDeviceRefVO`
                ADD CONSTRAINT `fkVmMdevDeviceRefMdevDeviceUuid`
                FOREIGN KEY (`mdevDeviceUuid`) REFERENCES `MdevDeviceVO` (`uuid`) ON DELETE CASCADE;
        END IF;

    END IF;

    -- Only run AccountResourceRefVO migration if MdevDeviceVO table exists
    IF EXISTS (SELECT 1 FROM information_schema.TABLES
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'MdevDeviceVO') THEN

        -- Step 5: Migrate AccountResourceRefVO references (safe to re-run)
        -- AccountResourceRefVO PK is `id` (auto-increment), not `uuid`
        INSERT IGNORE INTO `AccountResourceRefVO` (
            `accountUuid`, `ownerAccountUuid`, `resourceUuid`,
            `resourceType`, `concreteResourceType`, `createDate`, `lastOpDate`
        )
        SELECT
            ar.`accountUuid`, ar.`ownerAccountUuid`,
            ar.`resourceUuid`,
            'PciDeviceVO',
            'org.zstack.pciDevice.PciDeviceVO',
            NOW(), NOW()
        FROM `AccountResourceRefVO` ar
        WHERE ar.`resourceType` = 'MdevDeviceVO'
        AND ar.`resourceUuid` IN (SELECT `uuid` FROM `MdevDeviceVO`)
        AND NOT EXISTS (
            SELECT 1 FROM `AccountResourceRefVO` a2
            WHERE a2.`resourceUuid` = ar.`resourceUuid` AND a2.`resourceType` = 'PciDeviceVO'
        );

        -- Step 6: Remove old MdevDeviceVO AccountResourceRefVO entries
        DELETE FROM `AccountResourceRefVO`
        WHERE `resourceType` = 'MdevDeviceVO'
        AND `resourceUuid` IN (SELECT `uuid` FROM `MdevDeviceVO`);

    END IF;

    -- MdevDeviceVO_old preserved for rollback verification (drop in next version)
END$$
DELIMITER ;

CALL migrate_mdev_subtable();
DROP PROCEDURE IF EXISTS migrate_mdev_subtable;
