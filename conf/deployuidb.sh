#!/bin/sh
set -e
echo "$0 $*"
user="$1"
password="$2"
host="$3"
port="$4"
zstack_user_password="$5"

base=`dirname $0`
flyway="$base/tools/flyway-3.2.1/flyway"
flyway_sql="$base/tools/flyway-3.2.1/sql/"

mysql --user=$user --password=$password --host=$host --port=$port << EOF
DROP DATABASE IF EXISTS zstack_ui;
CREATE DATABASE zstack_ui;
grant all privileges on zstack.* to root@'%' identified by "$password";
grant all privileges on zstack.* to root@'localhost' identified by "$password";
EOF

rm -rf $flyway_sql
mkdir -p $flyway_sql

ui_schema_path="/usr/local/zstack/zstack-ui/tmp/WEB-INF/classes/db/migration/"
if [ -d $ui_schema_path ]; then
    cp $ui_schema_path/* $flyway_sql
    url="jdbc:mysql://$host:$port/zstack_ui"
    bash $flyway -user=$user -password=$password -url=$url clean
    bash $flyway -user=$user -password=$password -url=$url migrate
    eval "rm -f $flyway_sql/*"
fi

hostname=`hostname`
mysql --user=$user --password=$password --host=$host --port=$port << EOF
grant usage on *.* to 'zstack'@'localhost';
grant usage on *.* to 'zstack'@'%';
grant all privileges on zstack_ui.* to zstack@'localhost' identified by "$zstack_user_password";
grant all privileges on zstack_ui.* to zstack@'%' identified by "$zstack_user_password";
grant all privileges on zstack_ui.* to zstack@"$hostname" identified by "$zstack_user_password";
flush privileges;
EOF
