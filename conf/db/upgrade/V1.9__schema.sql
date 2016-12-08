ALTER TABLE `zstack`.`PriceVO` modify price DOUBLE(9,5) DEFAULT NULL;

ALTER TABLE `VipVO` DROP FOREIGN KEY `fkVipVOL3NetworkEO1`;
ALTER TABLE VipVO ADD CONSTRAINT fkVipVOL3NetworkEO1 FOREIGN KEY (peerL3NetworkUuid) REFERENCES L3NetworkEO (uuid) ON DELETE SET NULL;

CREATE TABLE `zstack`.`VCenterDatacenterVO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'vcenter data-center uuid',
    `vCenterUuid` varchar(32) NOT NULL COMMENT 'vcenter uuid',
    `name` varchar(255) NOT NULL COMMENT 'data-center name',
    `morval` varchar(64) NOT NULL COMMENT 'MOR value',
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# Foreign keys for table VCenterDatacenterVO
ALTER TABLE VCenterDatacenterVO ADD CONSTRAINT fkVCenterDatacenterVOVCenterVO FOREIGN KEY (vCenterUuid) REFERENCES VCenterVO (uuid) ON DELETE CASCADE;
