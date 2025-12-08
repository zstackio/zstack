-- Feature: License support CPU core quota type | ZSV-10588

CALL INSERT_COLUMN('LicenseHistoryVO', 'quotaType', 'varchar(64)', 0, 'None', 'vmNum');

UPDATE LicenseHistoryVO
SET quotaType = CASE
    WHEN cpuNum IS NOT NULL AND cpuNum > 0 THEN 'CPUSocket'
    WHEN hostNum IS NOT NULL AND hostNum > 0 THEN 'Host'
    WHEN vmNum IS NOT NULL AND vmNum > 0 THEN 'VM'
    ELSE quotaType
END;

CALL INSERT_COLUMN('HostCapacityVO', 'cpuCoreNum', 'int unsigned', 0, '0', 'cpuSockets');

-- Others
