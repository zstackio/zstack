package org.zstack.header.volume

import java.lang.Long
import java.lang.Long
import java.lang.Integer
import java.sql.Timestamp
import java.sql.Timestamp
import java.lang.Boolean

doc {

	title "在这里输入结构的名称"

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
		name "description"
		desc "资源的详细描述"
		type "String"
		since "0.6"
	}
	field {
		name "primaryStorageUuid"
		desc "主存储UUID"
		type "String"
		since "0.6"
	}
	field {
		name "vmInstanceUuid"
		desc "云主机UUID"
		type "String"
		since "0.6"
	}
	field {
		name "diskOfferingUuid"
		desc "云盘规格UUID"
		type "String"
		since "0.6"
	}
	field {
		name "rootImageUuid"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "installPath"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "type"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "format"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "size"
		desc ""
		type "Long"
		since "0.6"
	}
	field {
		name "actualSize"
		desc ""
		type "Long"
		since "0.6"
	}
	field {
		name "deviceId"
		desc ""
		type "Integer"
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
		name "isShareable"
		desc ""
		type "Boolean"
		since "0.6"
	}
	field {
		name "volumeQos"
		desc ""
		type "String"
		since "0.6"
	}
}
