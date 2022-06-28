ALTER TABLE SNSSmsPlatformVO ADD COLUMN `node1Ip` varchar(32) NOT NULL;
ALTER TABLE SNSSmsPlatformVO ADD COLUMN `node2Ip` varchar(32) NOT NULL;
ALTER TABLE SNSSmsPlatformVO ADD COLUMN `username2` varchar(32) NOT NULL;
ALTER TABLE SNSSmsPlatformVO ADD COLUMN `password2` varchar(32) NOT NULL;
ALTER TABLE SNSSmsPlatformVO ADD COLUMN `downlinkChannelNumber2` varchar(32) NOT NULL;

ALTER TABLE SNSSmsPlatformVO CHANGE COLUMN `username` `username1` varchar(32) NOT NULL;
ALTER TABLE SNSSmsPlatformVO CHANGE COLUMN `password` `password1` varchar(32) NOT NULL;
ALTER TABLE SNSSmsPlatformVO CHANGE COLUMN `downlinkChannelNumber` `downlinkChannelNumber1` varchar(32) NOT NULL;