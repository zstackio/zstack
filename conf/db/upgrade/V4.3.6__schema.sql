ALTER TABLE `zstack`.`LicenseHistoryVO` ADD COLUMN `hash` char(32) DEFAULT 'unknown';
DROP INDEX idxLicenseHistoryVOUploadDate ON LicenseHistoryVO;
CREATE INDEX idxLicenseHistoryVOHash ON LicenseHistoryVO (hash);

alter table IAM2OrganizationVO add column organizationDetail varchar(255);
ALTER table IAM2OrganizationVO ADD INDEX indexOrganizationDetail(organizationDetail);
alter table IAM2OrganizationVO add column organizationId int(11);
alter table IAM2OrganizationVO ADD KEY organizationId(organizationId);
ALTER TABLE IAM2OrganizationVO MODIFY organizationId BIGINT(11) auto_increment;
alter table IAM2OrganizationVO auto_increment=1000;

update IAM2OrganizationVO set organizationDetail = organizationId where type ='Company';

DROP PROCEDURE IF EXISTS addIAM2OrganizationOrganizationDetail;
DElIMITER $$
CREATE PROCEDURE addIAM2OrganizationOrganizationDetail()
BEGIN
  DECLARE organizationDetail VARCHAR(255);
  DECLARE selfId int(11);
  DECLARE organizationId int(11);
  DECLARE parentUuid VARCHAR(32);
  DECLARE organizationUuid VARCHAR(32);
  DECLARE parentList VARCHAR(1000);      # 返回父节点结果集
  DECLARE tempParent VARCHAR(1000);      # 临时存放父节点
  DECLARE done INT DEFAULT FALSE;
  DECLARE cur CURSOR FOR select vo.parentUuid, vo.uuid, vo.organizationId from IAM2OrganizationVO vo where vo.type !='Company' and vo.uuid != '6e3d19dab98348d8bd67657378843f82';
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
  open cur;
  read_loop: LOOP
      FETCH cur INTO parentUuid,organizationUuid, selfID;
      IF done THEN
            LEAVE read_loop;
      END IF;

      set parentList= '';
      select vo.parentUuid into tempParent from IAM2OrganizationVO vo where vo.uuid = organizationUuid;

      WHILE tempParent is not null and parentList is not null DO
        select vo.parentUuid, vo.organizationId into tempParent, organizationId from IAM2OrganizationVO vo where vo.uuid = tempParent;
        if organizationId is not null THEN
			set parentList = CONCAT(organizationId, '-', parentList);
        end if;
      END WHILE;

      update IAM2OrganizationVO vo set vo.organizationDetail = CONCAT(parentList, selfId) where vo.uuid=organizationUuid;

 END LOOP;
 CLOSE cur;
 SELECT CURTIME();
END $$
DELIMITER ;
CALL addIAM2OrganizationOrganizationDetail();







