CREATE TABLE IF NOT EXISTS `zstack`.`ExponBlockVolumeVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `exponStatus` varchar(32) NULL,
    PRIMARY KEY (`uuid`),
    CONSTRAINT fkExponBlockVolumeVOBlockVolumeVO FOREIGN KEY (uuid) REFERENCES BlockVolumeVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;