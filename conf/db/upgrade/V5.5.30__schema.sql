UPDATE `zstack`.`GlobalConfigVO`
SET `value` = JSON_SET(
        `value`,
        '$.baseUrl',
        REPLACE(JSON_UNQUOTE(JSON_EXTRACT(`value`, '$.baseUrl')), ':8201', ':18201')
    )
WHERE `category` = 'mevoco'
  AND `name` = 'license.client.config'
  AND JSON_VALID(`value`)
  AND JSON_UNQUOTE(JSON_EXTRACT(`value`, '$.baseUrl')) REGEXP ':8201(/|$)';
