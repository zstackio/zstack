#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../../../.." && pwd -P)"
migration="$repo_root/conf/db/upgrade/V5.5.48.2__schema.sql"
container="zsdataset-uuid-migration-$$"

# Mainline delivery must leave every earlier schema migration unchanged.
git -C "$repo_root" diff --exit-code HEAD -- 'conf/db/upgrade/V5.5.38*__schema.sql'

cleanup() {
    docker rm -f "$container" >/dev/null 2>&1 || true
}
trap cleanup EXIT

docker run --rm -d --network none --name "$container" \
    -e MYSQL_ROOT_PASSWORD=local-test-password mariadb:10.3 >/dev/null

ready=false
for ((attempt = 0; attempt < 30; attempt++)); do
    if docker exec -e MYSQL_PWD=local-test-password "$container" mysql -uroot -e 'SELECT 1' >/dev/null 2>&1; then
        ready=true
        break
    fi
    sleep 1
done
[[ "$ready" == true ]] || { echo 'MariaDB did not become ready' >&2; exit 1; }

mysql() {
    docker exec -i -e MYSQL_PWD=local-test-password "$container" mysql -uroot "$@"
}

uuid36='92b90ae6-af52-4325-b92d-c03b540ce7d8'
uuid37="${uuid36}x"

for original_width in 32 36; do
    mysql <<SQL
DROP DATABASE IF EXISTS zstack;
CREATE DATABASE zstack;
CREATE TABLE zstack.AsyncRestVO (requestData LONGTEXT CHARACTER SET utf8, result MEDIUMTEXT CHARACTER SET utf8);
CREATE TABLE zstack.AuditsVO (requestDump MEDIUMTEXT CHARACTER SET utf8, responseDump MEDIUMTEXT CHARACTER SET utf8);
CREATE TABLE zstack.ZsDatasetSpaceRefVO (uuid varchar(32) PRIMARY KEY, appInstanceUuid varchar($original_width) NOT NULL);
CREATE TABLE zstack.ZsDatasetServiceKeyVO (uuid varchar(32) PRIMARY KEY, appInstanceUuid varchar($original_width) NOT NULL);
CREATE TABLE zstack.ZsDatasetPublicationVO (uuid varchar(32) PRIMARY KEY, appInstanceUuid varchar($original_width) NOT NULL);
INSERT INTO zstack.ZsDatasetSpaceRefVO VALUES ('existing-space', '0123456789abcdef0123456789abcdef');
INSERT INTO zstack.ZsDatasetServiceKeyVO VALUES ('existing-key', '0123456789abcdef0123456789abcdef');
INSERT INTO zstack.ZsDatasetPublicationVO VALUES ('existing-publication', '0123456789abcdef0123456789abcdef');
SQL

    if [[ -f "$migration" ]]; then
        mysql < "$migration"
    fi

    for table in ZsDatasetSpaceRefVO ZsDatasetServiceKeyVO ZsDatasetPublicationVO; do
        width="$(mysql -Nse "SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = 'zstack' AND TABLE_NAME = '$table' AND COLUMN_NAME = 'appInstanceUuid'")"
        [[ "$width" == 36 ]] || { echo "$table: expected varchar(36), got varchar($width)" >&2; exit 1; }

        case "$table" in
            ZsDatasetSpaceRefVO) existing_id='existing-space' ;;
            ZsDatasetServiceKeyVO) existing_id='existing-key' ;;
            ZsDatasetPublicationVO) existing_id='existing-publication' ;;
        esac
        existing="$(mysql -Nse "SELECT appInstanceUuid FROM zstack.$table WHERE uuid = '$existing_id'")"
        [[ "$existing" == '0123456789abcdef0123456789abcdef' ]] || { echo "$table: existing row changed" >&2; exit 1; }

        mysql -e "INSERT INTO zstack.$table VALUES ('new-$table', '$uuid36')"
        stored="$(mysql -Nse "SELECT appInstanceUuid FROM zstack.$table WHERE uuid = 'new-$table'")"
        [[ "$stored" == "$uuid36" ]] || { echo "$table: 36-character UUID was not preserved" >&2; exit 1; }

        if mysql -e "SET SESSION sql_mode = 'STRICT_ALL_TABLES'; INSERT INTO zstack.$table VALUES ('too-long-$table', '$uuid37')" >/dev/null 2>&1; then
            echo "$table: 37-character value unexpectedly accepted" >&2
            exit 1
        fi
    done
done


# A clean 5.5.48 installation must receive all three tables, not only an ALTER.
mysql <<'SQL'
DROP DATABASE zstack;
CREATE DATABASE zstack;
CREATE TABLE zstack.AsyncRestVO (requestData LONGTEXT CHARACTER SET utf8, result MEDIUMTEXT CHARACTER SET utf8);
CREATE TABLE zstack.AuditsVO (requestDump MEDIUMTEXT CHARACTER SET utf8, responseDump MEDIUMTEXT CHARACTER SET utf8);
SQL
mysql < "$migration"
mysql < "$migration"
mysql <<SQL
INSERT INTO zstack.ZsDatasetSpaceRefVO (uuid,spaceId,appInstanceUuid) VALUES ('fresh-space','space','$uuid36');
INSERT INTO zstack.ZsDatasetServiceKeyVO (uuid,appInstanceUuid,keyId,secret) VALUES ('fresh-key','$uuid36','test-key','test-secret');
INSERT INTO zstack.ZsDatasetPublicationVO (uuid,appInstanceUuid,spaceId,publishRequestId,state) VALUES ('fresh-publication','$uuid36','space','request','COMPLETED');
SET NAMES utf8mb4;
INSERT INTO zstack.AsyncRestVO (requestData,result) VALUES ('中文🚀','结果🚀');
INSERT INTO zstack.AuditsVO (requestDump,responseDump) VALUES ('请求🚀','响应🚀');
SQL
fresh_count="$(mysql -Nse 'SELECT (SELECT COUNT(*) FROM zstack.ZsDatasetSpaceRefVO)+(SELECT COUNT(*) FROM zstack.ZsDatasetServiceKeyVO)+(SELECT COUNT(*) FROM zstack.ZsDatasetPublicationVO)')"
[[ "$fresh_count" == 3 ]] || { echo 'fresh-install records were not retained' >&2; exit 1; }
emoji="$(mysql --default-character-set=utf8mb4 -Nse 'SELECT result FROM zstack.AsyncRestVO')"
[[ "$emoji" == '结果🚀' ]] || { echo 'emoji round trip failed' >&2; exit 1; }

echo 'zsdataset appInstanceUuid migration: 32-to-36 and 36-to-36 passed for all three tables'
