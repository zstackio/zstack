package org.zstack.header.storage.snapshot.group

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "快照组和快照连接关系"

	field {
		name "volumeSnapshotUuid"
		desc "云盘快照UUID"
		type "String"
		since ""
	}
	field {
		name "volumeSnapshotGroupUuid"
		desc "快照组UUID"
		type "String"
		since ""
	}
	field {
		name "deviceId"
		desc "打快照时云盘的加载序号"
		type "int"
		since ""
	}
	field {
		name "snapshotDeleted"
		desc "快照是否已经被删除"
		type "boolean"
		since ""
	}
	field {
		name "volumeUuid"
		desc "云盘UUID"
		type "String"
		since ""
	}
	field {
		name "volumeName"
		desc "云盘的名字"
		type "String"
		since ""
	}
	field {
		name "volumeType"
		desc "云盘的类型"
		type "String"
		since ""
	}
	field {
		name "volumeSnapshotInstallPath"
		desc "快照的安装路径"
		type "String"
		since ""
	}
	field {
		name "volumeSnapshotName"
		desc "快照的名字"
		type "String"
		since ""
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since ""
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since ""
	}
}
