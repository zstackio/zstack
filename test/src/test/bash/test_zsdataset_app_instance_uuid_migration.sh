#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../../../.." && pwd -P)"
migration="$repo_root/conf/db/upgrade/V5.5.38.5__schema.sql"
container="zsdataset-uuid-migration-$$"

# This version was already applied on 212; changing its bytes would break Flyway validation.
applied_checksum="$(shasum -a 256 "$repo_root/conf/db/upgrade/V5.5.38__schema.sql" | awk '{print $1}')"
[[ "$applied_checksum" == a62d6f3a54a9b3a8828df5de789bcad0ea8adc7acb1ef71f6f838911cea8d170 ]] || {
    echo 'V5.5.38 differs from the already-applied version on 212' >&2
    exit 1
}

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

echo 'zsdataset appInstanceUuid migration: 32-to-36 and 36-to-36 passed for all three tables'
