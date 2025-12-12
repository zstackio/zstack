CREATE TABLE IF NOT EXISTS `OvnControllerVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `remoteOvn` tinyint(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT IGNORE INTO OvnControllerVO (uuid, remoteOvn)
SELECT uuid, 0 FROM SdnControllerVO where vendorType = 'Ovn';

