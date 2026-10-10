#!/bin/bash
set -e
#If some arguments are "", the script will be called failed, since shell can't 
#recognize "", when sending it through arguments. 
user="$1"
password="$2"
host="$3"
port="$4"
zstack_user_password="$5"
bootstrap_mode="$6"
bootstrap_request_uuid="$7"
mysql_host="$host"

case "$mysql_host" in
  \[*\]) mysql_host="${mysql_host#\[}"; mysql_host="${mysql_host%\]}" ;;
esac

jdbc_host="$mysql_host"
case "$jdbc_host" in
  *:*) jdbc_host="[$jdbc_host]" ;;
esac

base=`dirname $0`

# assign flyway version if not defined
: "${flywayver:=3.2.1}"
flyway="$base/tools/flyway-$flywayver/flyway"
flyway_sql="$base/tools/flyway-$flywayver/sql/"

MYSQL='mysql'

if [[ `id -u` -ne 0 ]] && [[ x"$user" = x"root" ]]; then
    MYSQL='sudo mysql'
fi

if command -v greatdb &> /dev/null; then
    MYSQL='greatdb'
    if [[ `id -u` -ne 0 ]] && [[ x"$user" = x"root" ]]; then
        MYSQL='sudo greatdb'
    fi
fi

mysql_run() {
    $MYSQL --user=$user --password=$password --host=$mysql_host --port=$port "$@"
}

if [[ -n "$bootstrap_mode" && "$bootstrap_mode" != "fresh-cloud-bootstrap" ]]; then
  echo "Invalid internal database bootstrap mode" >&2
  exit 2
fi
if [[ "$bootstrap_mode" == "fresh-cloud-bootstrap" ]]; then
  if [[ ! "$bootstrap_request_uuid" =~ ^[0-9a-f]{32}$ ]]; then
    echo "Invalid fresh-cloud bootstrap request identifier" >&2
    exit 2
  fi
  existing_schema=$(mysql_run --batch --skip-column-names -e "SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='zstack'") || {
    echo "Cannot verify target database absence; refusing fresh-cloud bootstrap" >&2
    exit 2
  }
  if [[ -n "$existing_schema" ]]; then
    echo "Target zstack database already exists; refusing fresh-cloud bootstrap" >&2
    exit 2
  fi
fi

if command -v greatdb &> /dev/null; then
  mysql_run << EOF
    set global log_bin_trust_function_creators=1;
    DROP DATABASE IF EXISTS zstack;
    CREATE DATABASE zstack;
    DROP DATABASE IF EXISTS zstack_rest;
    CREATE DATABASE zstack_rest;
    CREATE USER IF NOT EXISTS 'root'@'%' IDENTIFIED BY "${password}";
    CREATE USER IF NOT EXISTS 'root'@'127.0.0.1' IDENTIFIED BY "${password}";
    grant all privileges on zstack.* to root@'%';
    grant all privileges on zstack_rest.* to root@'%';
    grant all privileges on zstack.* to root@'127.0.0.1';
    grant all privileges on zstack_rest.* to root@'127.0.0.1';
EOF
else
  mysql_run << EOF
  set global log_bin_trust_function_creators=1;
  DROP DATABASE IF EXISTS zstack;
  CREATE DATABASE zstack;
  DROP DATABASE IF EXISTS zstack_rest;
  CREATE DATABASE zstack_rest;
  grant all privileges on zstack.* to root@'%' identified by "${password}";
  grant all privileges on zstack_rest.* to root@'%' identified by "${password}";
  grant all privileges on zstack.* to root@'127.0.0.1' identified by "${password}";
  grant all privileges on zstack_rest.* to root@'127.0.0.1' identified by "${password}";
EOF
fi

rm -rf $flyway_sql
mkdir -p $flyway_sql

cp $base/db/V0.6__schema.sql $flyway_sql
cp $base/db/upgrade/* $flyway_sql

url="jdbc:mysql://$jdbc_host:$port/zstack"

bash $flyway -user=$user -password=$password -url=$url clean

# create baseline and clean its contents for 'beforeValidate.sql'
bash $flyway -user=$user -password=$password -url=$url baseline
mysql_run zstack -e "DELETE FROM schema_version"

bash $flyway -user=$user -password=$password -url=$url migrate

eval "rm -f $flyway_sql/*"

cp $base/db/V0.6__schema_buildin_httpserver.sql $flyway_sql

url="jdbc:mysql://$jdbc_host:$port/zstack_rest"
bash $flyway -user=$user -password=$password -url=$url clean
bash $flyway -user=$user -password=$password -url=$url migrate

eval "rm -f $flyway_sql/*"

hostname=`hostname`

[ -z $zstack_user_password ] && zstack_user_password=''

if command -v greatdb &> /dev/null; then
  $MYSQL --user=$user --password=$password --host=$mysql_host --port=$port << EOF
    drop user if exists zstack;
    drop user if exists zstack_rest;
    create user if not exists 'zstack'@'localhost' identified by "$zstack_user_password";
    create user if not exists 'zstack'@'%' identified by "$zstack_user_password";
    create user if not exists 'zstack_rest'@'localhost' identified by "$zstack_user_password";
    create user if not exists 'zstack_rest'@'%' identified by "$zstack_user_password";
    grant all privileges on zstack.* to zstack@'localhost';
    grant all privileges on zstack.* to zstack@'%';
    grant system_user on *.* to zstack@'localhost';
    grant system_user on *.* to zstack@'%';
    grant all privileges on zstack_rest.* to zstack@'localhost';
    grant all privileges on zstack_rest.* to zstack@'%';
    flush privileges;
EOF
else
  db_version=`$MYSQL --version | awk '/Distrib/{print $5}' |awk -F'.' '{print $1}'`
  if [ $db_version -ge 10 ];then
      $MYSQL --user=$user --password=$password --host=$mysql_host --port=$port << EOF
  drop user if exists zstack;
  drop user if exists zstack_rest;
  create user 'zstack' identified by "$zstack_user_password";
  create user 'zstack_rest' identified by "$zstack_user_password";
  grant all privileges on zstack.* to zstack@'localhost' identified by "$zstack_user_password";
  grant all privileges on zstack.* to zstack@'%' identified by "$zstack_user_password";
  grant all privileges on zstack_rest.* to zstack@'localhost' identified by "$zstack_user_password";
  grant all privileges on zstack_rest.* to zstack@'%' identified by "$zstack_user_password";
  flush privileges;
EOF
  else
      $MYSQL --user=$user --password=$password --host=$mysql_host --port=$port << EOF
  grant usage on *.* to 'zstack'@'localhost';
  grant usage on *.* to 'zstack'@'%';
  drop user zstack;
  create user 'zstack' identified by "$zstack_user_password";
  grant all privileges on zstack.* to zstack@'localhost' identified by "$zstack_user_password";
  grant all privileges on zstack.* to zstack@'%' identified by "$zstack_user_password";
  grant all privileges on zstack.* to zstack@"$hostname" identified by "$zstack_user_password";
  grant all privileges on zstack_rest.* to zstack@'localhost' identified by "$zstack_user_password";
  grant all privileges on zstack_rest.* to zstack@"$hostname" identified by "$zstack_user_password";
  grant all privileges on zstack_rest.* to zstack@'%' identified by "$zstack_user_password";
  flush privileges;
EOF
  fi
fi

if [[ "$bootstrap_mode" == "fresh-cloud-bootstrap" ]]; then
  mysql_run zstack -e "INSERT INTO MemoryCloudBootstrapVO (uuid, status, requestUuid, reason) VALUES ('Global:global', 'Pending', '$bootstrap_request_uuid', 'Awaiting KSM and zero-pages capability/license checks')"
fi
