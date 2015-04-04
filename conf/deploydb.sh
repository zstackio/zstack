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

mysql --user=$user --password=$password --host=$host --port=$port << EOF
DROP DATABASE IF EXISTS zstack;
CREATE DATABASE zstack;
DROP DATABASE IF EXISTS zstack_rest;
CREATE DATABASE zstack_rest;
grant all privileges on zstack.* to root@'%' identified by "$password";
grant all privileges on zstack_rest.* to root@'%' identified by "$password";
EOF


schema=`dirname $0`/db/schema.sql
trigger=`dirname $0`/db/trigger.sql
schema_rest=`dirname $0`/db/schema-rest.sql
view=`dirname $0`/db/view.sql
foreign_keys=`dirname $0`/db/foreignKeys.sql
indexes=`dirname $0`/db/indexes.sql

[ -z $password ] && password=''
mysql --user=$user --password=$password --host=$host --port=$port< $schema
mysql --user=$user --password=$password --host=$host --port=$port< $view
mysql --user=$user --password=$password --host=$host --port=$port< $schema_rest
mysql --user=$user --password=$password --host=$host --port=$port -t zstack < $foreign_keys
mysql --user=$user --password=$password --host=$host --port=$port -t zstack < $indexes

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
