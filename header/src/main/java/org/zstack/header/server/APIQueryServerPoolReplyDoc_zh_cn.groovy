package org.zstack.header.server

import org.zstack.header.server.ServerPoolInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "在这里输入结构的名称"

	ref {
		name "inventories"
		path "org.zstack.header.server.APIQueryServerPoolReply.inventories"
		desc "null"
		type "List"
		since "5.5.16"
		clz ServerPoolInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "5.5.16"
	}
	ref {
		name "error"
		path "org.zstack.header.server.APIQueryServerPoolReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.5.16"
		clz ErrorCode.class
	}
}
