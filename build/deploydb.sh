#!/bin/sh

set -e

user="$1"
password="$2"

base=`dirname $0`

mysql --user=${user} --password=${password} << EOF
DROP DATABASE IF EXISTS zstack;
CREATE DATABASE zstack;
DROP DATABASE IF EXISTS zstack_rest;
CREATE DATABASE zstack_rest;
grant all privileges on zstack.* to root@'%' identified by "${password}";
grant all privileges on zstack_rest.* to root@'%' identified by "${password}";
grant all privileges on zstack.* to root@'127.0.0.1' identified by "${password}";
grant all privileges on zstack_rest.* to root@'127.0.0.1' identified by "${password}";
EOF

flyway="$base/../conf//tools/flyway-3.2.1/flyway"
flyway_sql="$base/../conf/tools/flyway-3.2.1/sql/"

mkdir -p ${flyway_sql}

eval "rm -f ${flyway_sql}/*"
cp ${base}/../conf/db/V0.6__schema.sql ${flyway_sql}
cp ${base}/../conf/db/upgrade/* ${flyway_sql}

url="jdbc:mysql://localhost:3306/zstack"
${flyway} -user=${user} -password=${password} -url=${url} clean
${flyway} -outOfOrder=true -user=${user} -password=${password} -url=${url} migrate

eval "rm -f ${flyway_sql}/*"

cp ${base}/../conf/db/V0.6__schema_buildin_httpserver.sql ${flyway_sql}

url="jdbc:mysql://localhost:3306/zstack_rest"
${flyway} -user=${user} -password=${password} -url=${url} clean
${flyway} -outOfOrder=true -user=${user} -password=${password} -url=${url} migrate

eval "rm -f ${flyway_sql}/*"
