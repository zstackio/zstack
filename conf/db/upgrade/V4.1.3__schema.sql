DELIMITER $$
CREATE PROCEDURE upgradeSystemEndpointNotifier()
BEGIN
    DECLARE oldAppUuid VARCHAR(32);
    SELECT `uuid` INTO oldAppUuid FROM `SNSHttpEndpointVO` WHERE `url` = 'http://system-endpoint.XXX_XXX.default' LIMIT 1;
    read_loop: LOOP
        IF oldAppUuid IS NULL THEN
            LEAVE read_loop;
        END IF;
        DELETE FROM `SNSSubscriberVO` WHERE `endpointUuid` = oldAppUuid;
        DELETE FROM `SNSApplicationEndpointVO` WHERE `uuid` = oldAppUuid;
        DELETE FROM `ResourceVO` WHERE `uuid` = oldAppUuid;
        DELETE FROM `SNSHttpEndpointVO` WHERE `uuid` = oldAppUuid;
        DELETE FROM `AccountResourceRefVO` WHERE `resourceUuid` = oldAppUuid;
        DELETE FROM `SharedResourceVO` WHERE `resourceUuid` = oldAppUuid;
        LEAVE read_loop;
    END LOOP;
    SELECT CURTIME();
END $$
DELIMITER ;
CALL upgradeSystemEndpointNotifier();