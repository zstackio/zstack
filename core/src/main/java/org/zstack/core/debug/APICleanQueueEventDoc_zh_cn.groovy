package org.zstack.core.debug

import org.zstack.header.errorcode.ErrorCode

doc {

	title "清理管理节点队列的结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "4.1.3"
	}
	ref {
		name "error"
		path "org.zstack.core.debug.APICleanQueueEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.1.3"
		clz ErrorCode.class
	}
}
