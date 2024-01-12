CREATE TABLE IF NOT EXISTS `zstack`.`OTPDeviceInfoVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `vendor` varchar(32) NOT NULL,
    `serialNumber` varchar(256) NOT NULL,
    `privateData` varchar(256) NOT NULL,
    `keyLength` int(40) NOT NULL,
    `keyValue` varchar(256) NOT NULL,
    `otpLength` int(10) NOT NULL,
    `intervalInSeconds` int(100) NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;