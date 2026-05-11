package org.zstack.header.server

import org.zstack.header.server.PhysicalServerInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "在这里输入结构的名称"

	field {
		name "discoveredCount"
		desc ""
		type "int"
		since "5.5.16"
	}
	field {
		name "existingCount"
		desc ""
		type "int"
		since "5.5.16"
	}
	field {
		name "unreachableCount"
		desc ""
		type "int"
		since "5.5.16"
	}
	field {
		name "authFailedCount"
		desc ""
		type "int"
		since "5.5.16"
	}
	ref {
		name "discoveredServers"
		path "org.zstack.header.server.APIScanPhysicalServersEvent.discoveredServers"
		desc "null"
		type "List"
		since "5.5.16"
		clz PhysicalServerInventory.class
	}
	field {
		name "authFailedIps"
		desc ""
		type "List"
		since "5.5.16"
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "5.5.16"
	}
	ref {
		name "error"
		path "org.zstack.header.server.APIScanPhysicalServersEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.5.16"
		clz ErrorCode.class
	}
}
