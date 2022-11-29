package org.zstack.core.eventlog

import org.zstack.core.eventlog.EventLogInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询事件日志的结果"

	ref {
		name "inventories"
		path "org.zstack.core.eventlog.APIQueryEventLogReply.inventories"
		desc "null"
		type "List"
		since "4.2.0"
		clz EventLogInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.2.0"
	}
	ref {
		name "error"
		path "org.zstack.core.eventlog.APIQueryEventLogReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.2.0"
		clz ErrorCode.class
	}
}
