DELETE FROM ResourceConfigVO WHERE (resourceUuid,category,name) IN
(SELECT a.resourceUuid,a.category,a.name FROM (SELECT resourceUuid,category,name from ResourceConfigVO GROUP BY resourceUuid,category,name HAVING count(uuid)>1) a)
AND uuid NOT IN (SELECT b.uuid FROM (SELECT uuid uuid FROM ResourceConfigVO GROUP BY resourceUuid,category,name HAVING count(uuid)>1) b);

# add unique index cleanup duplicate records in ResourceConfigVO
ALTER TABLE ResourceConfigVO ADD UNIQUE INDEX (resourceUuid, category, name);
