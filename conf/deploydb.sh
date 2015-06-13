#!/bin/sh
set -e
#If some arguments are "", the script will be called failed, since shell can't 
#recognize "", when sending it through arguments. 
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
DROP DATABASE IF EXISTS zstack;
CREATE DATABASE zstack;
DROP DATABASE IF EXISTS zstack_rest;
CREATE DATABASE zstack_rest;
grant all privileges on zstack.* to root@'%' identified by "$password";
grant all privileges on zstack_rest.* to root@'%' identified by "$password";
EOF

schema="$base/db/schema.sql"
schema_rest="$base/db/schema-rest.sql"
view="$base/db/view.sql"
foreign_keys="$base/db/foreignKeys.sql"
indexes="$base/db/indexes.sql"

eval "rm -f $flyway_sql/*"

schema_0_6="$flyway_sql/V0.6__schema.sql"
cat $schema > $schema_0_6
cat $foreignKeys >> $schema_0_6
cat $indexes >> $schema_0_6
cat $view >> $schema_0_6
cp db/upgrade/* $flyway_sql

url="jdbc:mysql://$host:$port/zstack"
$flyway -user=$user -password=$password -url=$url clean
$flyway -user=$user -password=$password -url=$url migrate

eval "rm -f $flyway_sql/*"

schema_rest_0_6="$flyway_sql/V0.6__schema_rest.sql"
cat $schema_rest > $schema_rest_0_6

url="jdbc:mysql://$host:$port/zstack_rest"
$flyway -user=$user -password=$password -url=$url clean
$flyway -user=$user -password=$password -url=$url migrate

eval "rm -f $flyway_sql/*"

hostname=`hostname`

[ -z $zstack_user_password ] && zstack_user_password=''
mysql --user=$user --password=$password --host=$host --port=$port << EOF
grant usage on *.* to 'zstack'@'localhost';
grant usage on *.* to 'zstack'@'%';
drop user zstack;
create user 'zstack' identified by "$zstack_user_password";
grant all privileges on zstack.* to zstack@'localhost' identified by "$zstack_user_password";
grant all privileges on zstack.* to zstack@'%' identified by "$zstack_user_password";
grant all privileges on zstack.* to zstack@"$hostname" identified by "$zstack_user_password";
grant all privileges on zstack_rest.* to zstack_rest@'localhost' identified by "$zstack_user_password";
grant all privileges on zstack_rest.* to zstack_rest@"$hostname" identified by "$zstack_user_password";
grant all privileges on zstack_rest.* to zstack_rest@'%' identified by "$zstack_user_password";
flush privileges;
EOF
