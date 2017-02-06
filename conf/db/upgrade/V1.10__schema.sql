# VmInstanceEO
DROP TRIGGER IF EXISTS trigger_clean_ImageEO_for_VmInstanceEO;
DELIMITER $$
CREATE TRIGGER trigger_clean_ImageEO_for_VmInstanceEO AFTER DELETE ON `VmInstanceEO`
FOR EACH ROW
    BEGIN
        DELETE FROM ImageEO WHERE `deleted` IS NOT NULL AND `uuid` NOT IN (SELECT imageUuid FROM VmInstanceVO WHERE imageUuid IS NOT NULL);
    END $$
DELIMITER ;
