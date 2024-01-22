CREATE TABLE `zstack`.`XgsSsoClientVO` (
    `tokenUrl`  varchar(255) default null,
    `authorizationUrl`  varchar(255) default null,
    `verifyAccessTokenUrl`  varchar(255) default null,
    `logoutUrl`  varchar(255) default null,
    `uuid` varchar(32) not null unique,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkXgsSsoClientVOSSOClientVO` FOREIGN KEY (`uuid`) REFERENCES `SSOClientVO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`PoliceVO` (
    `username`  varchar(255) default null,
    `gender`  varchar(255) default null,
    `email`  varchar(255) default null,
    `phone`  varchar(255) default null,
    `address`  varchar(255) default null,
    `police_id`  varchar(255) default null,
    `organization_id`  varchar(255) default null,
    `organization_name`  varchar(255) default null,
    `uuid` varchar(32) not null unique,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkPoliceVOIAM2VirtualIDVO` FOREIGN KEY (`uuid`) REFERENCES `IAM2VirtualIDVO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
