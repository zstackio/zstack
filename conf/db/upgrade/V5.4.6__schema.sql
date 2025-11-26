-- Fix UsedIpVO gateway and netmask from empty strings
DROP PROCEDURE IF EXISTS fixUsedIpGatewayAndNetmask;

DELIMITER $$

CREATE PROCEDURE fixUsedIpGatewayAndNetmask()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_usedIpUuid VARCHAR(32);
    DECLARE v_ipRangeUuid VARCHAR(32);
    DECLARE v_gateway VARCHAR(64);
    DECLARE v_netmask VARCHAR(64);
    
    -- Cursor for UsedIpVO records with empty gateway or netmask
    DECLARE cur CURSOR FOR 
        SELECT uuid, ipRangeUuid 
        FROM UsedIpVO 
        WHERE (gateway = '' OR netmask = '' OR gateway IS NULL OR netmask IS NULL)
          AND ipRangeUuid IS NOT NULL;
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    OPEN cur;
    
    read_loop: LOOP
        FETCH cur INTO v_usedIpUuid, v_ipRangeUuid;
        
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        -- Get gateway and netmask from IpRangeVO
        SELECT gateway, netmask 
        INTO v_gateway, v_netmask
        FROM IpRangeVO 
        WHERE uuid = v_ipRangeUuid;
        
        -- Update UsedIpVO with correct gateway and netmask
        IF v_gateway IS NOT NULL AND v_netmask IS NOT NULL THEN
            UPDATE UsedIpVO 
            SET gateway = v_gateway, netmask = v_netmask
            WHERE uuid = v_usedIpUuid;
        END IF;
        
    END LOOP;
    
    CLOSE cur;
    
END$$

DELIMITER ;

-- Execute the procedure
CALL fixUsedIpGatewayAndNetmask();

-- Drop the procedure after use
DROP PROCEDURE IF EXISTS fixUsedIpGatewayAndNetmask;

ALTER TABLE LicenseAuthorizedCapacityVO ADD COLUMN `resourceInfo` text DEFAULT NULL;
