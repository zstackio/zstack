UPDATE `zstack`.`GlobalConfigVO`
SET `value` = '100'
WHERE `category` = 'l2Network' AND `name` = 'realize.batch.count'
  AND `value` REGEXP '^[+-]?0+$';
