UPDATE `zstack`.`GlobalConfigVO` SET value="64", defaultValue="64" WHERE category="volumeSnapshot" AND name="incrementalSnapshot.maxNum" AND value > 120;