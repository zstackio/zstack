ALTER TABLE JsonLabelVO MODIFY COLUMN labelValue MEDIUMTEXT;

CREATE INDEX idxTaskProgressVOapiId ON TaskProgressVO(apiId);
