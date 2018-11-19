package org.zstack.header.volume

doc {

	title "云盘清单"

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
		desc "云盘根镜像UUID"
		type "String"
		since "0.6"
	}
	field {
		name "installPath"
		desc "云盘在主存储上的路径"
		type "String"
		since "0.6"
	}
	field {
		name "type"
		desc "云盘类型，数据云盘/根云盘"
		type "String"
		since "0.6"
	}
	field {
		name "format"
		desc "云盘格式"
		type "String"
		since "0.6"
	}
	field {
		name "size"
		desc "云盘大小"
		type "Long"
		since "0.6"
	}
	field {
		name "actualSize"
		desc "云盘真实大小"
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
		desc "云盘是否开启"
		type "String"
		since "0.6"
	}
	field {
		name "status"
		desc "云盘状态"
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
		desc "是否共享云盘"
		type "Boolean"
		since "0.6"
	}
	field {
		name "volumeQos"
		desc "云盘Qos，格式如total=1048576"
		type "String"
		since "3.2.0"
	}
}
