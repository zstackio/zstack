# add unique index cleanup duplicate records in ResourceConfigVO
ALTER IGNORE TABLE ResourceConfigVO ADD UNIQUE INDEX (resourceUuid, category, name);