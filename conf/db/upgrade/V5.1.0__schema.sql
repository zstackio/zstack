DELETE FROM `SystemTagVO` WHERE `resourceUuid` IN (SELECT uuid FROM HostCapacityVO WHERE cpuSockets != 1) AND tag LIKE "cpuProcessorNum::%";
