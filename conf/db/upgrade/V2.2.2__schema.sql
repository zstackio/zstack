CREATE TABLE `zstack`.VipPeerL3NetworkRefVO (
  `vipUuid` VARCHAR(32) NOT NULL,
  `l3NetworkUuid` VARCHAR(32) NOT NULL,
  `lastOpDate` TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `createDate` TIMESTAMP,
  CONSTRAINT `fkVipPeerL3NetworkRefVOVipVO` FOREIGN KEY (`vipUuid`) REFERENCES `zstack`.`VipVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVipPeerL3NetworkRefVOL3NetworkEO` FOREIGN KEY (`l3NetworkUuid`) REFERENCES `zstack`.`L3NetworkEO` (`uuid`) ON DELETE CASCADE,
  PRIMARY KEY (`vipUuid`, `l3NetworkUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DELIMITER $$
CREATE PROCEDURE migrateVipPeerL3Network()
  BEGIN
    DECLARE vip varchar(32);
    DECLARE peerL3 varchar(32);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR SELECT uuid, peerL3NetworkUuid FROM zstack.VipVO WHERE peerL3NetworkUuid IS NOT NULL;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
      FETCH cur INTO vip, peerL3;
      IF done THEN
        LEAVE read_loop;
      END IF;

      INSERT INTO VipPeerL3NetworkRefVO (`vipUuid`, `l3NetworkUuid`, `lastOpDate`, `createDate`)
      VALUES (vip, peerL3, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

    END LOOP;
    CLOSE cur;
    # work around a bug of mysql : jira.mariadb.org/browse/MDEV-4602
    SELECT CURTIME();
  END $$
DELIMITER ;

CALL migrateVipPeerL3Network();
DROP PROCEDURE IF EXISTS migrateVipPeerL3Network;

ALTER TABLE zstack.VipVO DROP FOREIGN KEY fkVipVOL3NetworkEO1;
ALTER TABLE zstack.VipVO DROP COLUMN peerL3NetworkUuid;


INSERT IGNORE INTO SystemTagVO (`uuid`, `resourceUuid`, `resourceType`, `inherent`, `type`, `tag`, `createDate`, `lastOpDate`)
SELECT REPLACE(UUID(),'-',''), t.uuid, 'LdapServerVO', 0, 'System', "ldapUseAsLoginName::uid", CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP() FROM LdapServerVO t;

INSERT IGNORE INTO SystemTagVO (`uuid`, `resourceUuid`, `resourceType`, `inherent`, `type`, `tag`, `createDate`, `lastOpDate`)
SELECT REPLACE(UUID(),'-',''), t.uuid, 'LdapServerVO', 0, 'System', "ldapServerType::OpenLdap", CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP() FROM LdapServerVO t;
