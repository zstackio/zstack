package org.zstack.storage.surfs.backup

import org.zstack.storage.surfs.backup.SurfsBackupStorageNodeInventory
import java.lang.Integer
import java.lang.Long
import java.lang.Long
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "在这里输入结构的名称"

	ref {
		name "nodes"
		path "org.zstack.storage.surfs.backup.SurfsBackupStorageInventory.nodes"
		desc "null"
		type "List"
		since "0.6"
		clz SurfsBackupStorageNodeInventory.class
	}
	field {
		name "fsid"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "poolName"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "sshPort"
		desc ""
		type "Integer"
		since "0.6"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "0.6"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "0.6"
	}
	field {
		name "url"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "0.6"
	}
	field {
		name "totalCapacity"
		desc ""
		type "Long"
		since "0.6"
	}
	field {
		name "availableCapacity"
		desc ""
		type "Long"
		since "0.6"
	}
	field {
		name "type"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "state"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "status"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "0.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "0.6"
	}
	field {
		name "attachedZoneUuids"
		desc ""
		type "List"
		since "0.6"
	}
}
