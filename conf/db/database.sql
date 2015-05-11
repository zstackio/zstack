DROP DATABASE IF EXISTS `zstack`;
CREATE DATABASE `zstack`;
DROP DATABASE IF EXISTS `zstack_rest`;
CREATE DATABASE `zstack_rest`;
grant all privileges on zstack.* to root@'%' identified by '';
grant all privileges on zstack_rest.* to root@'%' identified by '';
