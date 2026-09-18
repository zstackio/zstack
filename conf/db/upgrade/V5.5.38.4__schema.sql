-- Store 4-byte UTF-8 characters (emoji) in API request and result records.
--
-- These columns hold the serialized API message and event. With the 3-byte utf8 charset an API
-- whose request or result contains an emoji fails to persist its async record: the webhook is
-- never sent and the caller waits until its own timeout, although the API itself succeeded.
-- The JDBC connection already negotiates utf8mb4, so converting the columns is sufficient.
-- Same approach as GuestVmScriptExecutedRecordDetailVO in V5.5.6.

ALTER TABLE `zstack`.`AsyncRestVO`
    MODIFY `requestData` LONGTEXT CHARACTER SET `utf8mb4` COLLATE `utf8mb4_unicode_ci` DEFAULT NULL,
    MODIFY `result` MEDIUMTEXT CHARACTER SET `utf8mb4` COLLATE `utf8mb4_unicode_ci` DEFAULT NULL;

ALTER TABLE `zstack`.`AuditsVO`
    MODIFY `requestDump` MEDIUMTEXT CHARACTER SET `utf8mb4` COLLATE `utf8mb4_unicode_ci`,
    MODIFY `responseDump` MEDIUMTEXT CHARACTER SET `utf8mb4` COLLATE `utf8mb4_unicode_ci`;
