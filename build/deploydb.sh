#!/bin/sh

user="$1"
password="$2"

database=`dirname $0`/../conf/db/database.sql
schema=`dirname $0`/../conf/db/schema.sql
#trigger=`dirname $0`/../conf/db/trigger.sql
schema_rest=`dirname $0`/../conf/db/schema-rest.sql
view=`dirname $0`/../conf/db/view.sql
foreign_keys=`dirname $0`/../conf/db/foreignKeys.sql
indexes=`dirname $0`/../conf/db/indexes.sql

mysql --user=$user --password=$password < $database
mysql --user=$user --password=$password < $schema
mysql --user=$user --password=$password < $view
#mysql --user=$user --password=$password < $trigger
mysql --user=$user --password=$password < $schema_rest
mysql --user=$user --password=$password -t zstack < $foreign_keys
mysql --user=$user --password=$password -t zstack < $indexes

