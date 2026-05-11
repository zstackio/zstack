package org.zstack.header.server

import java.lang.Integer
import org.zstack.header.server.PhysicalServerRoleInventory
import java.sql.Timestamp

doc {

	title "在这里输入结构的名称"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "5.5.16"
	}
	field {
		name "zoneUuid"
		desc "区域UUID"
		type "String"
		since "5.5.16"
	}
	field {
		name "poolUuid"
		desc ""
		type "String"
		since "5.5.16"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "5.5.16"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "5.5.16"
	}
	field {
		name "managementIp"
		desc ""
		type "String"
		since "5.5.16"
	}
	field {
		name "architecture"
		desc ""
		type "String"
		since "5.5.16"
	}
	field {
		name "serialNumber"
		desc ""
		type "String"
		since "5.5.16"
	}
	field {
		name "manufacturer"
		desc ""
		type "String"
		since "5.5.16"
	}
	field {
		name "model"
		desc ""
		type "String"
		since "5.5.16"
	}
	field {
		name "state"
		desc ""
		type "String"
		since "5.5.16"
	}
	field {
		name "powerStatus"
		desc ""
		type "String"
		since "5.5.16"
	}
	field {
		name "oobManagementType"
		desc ""
		type "String"
		since "5.5.16"
	}
	field {
		name "oobAddress"
		desc ""
		type "String"
		since "5.5.16"
	}
	field {
		name "oobPort"
		desc ""
		type "Integer"
		since "5.5.16"
	}
	field {
		name "oobUsername"
		desc ""
		type "String"
		since "5.5.16"
	}
	ref {
		name "roles"
		path "org.zstack.header.server.PhysicalServerInventory.roles"
		desc "null"
		type "List"
		since "5.5.16"
		clz PhysicalServerRoleInventory.class
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "5.5.16"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "5.5.16"
	}
}
