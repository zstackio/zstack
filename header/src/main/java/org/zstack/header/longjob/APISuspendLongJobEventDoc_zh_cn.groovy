package org.zstack.header.longjob

import org.zstack.header.longjob.LongJobInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "在这里输入结构的名称"

	ref {
		name "inventory"
		path "org.zstack.header.longjob.APISuspendLongJobEvent.inventory"
		desc "null"
		type "LongJobInventory"
		since "5.5.0"
		clz LongJobInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "5.5.0"
	}
	ref {
		name "error"
		path "org.zstack.header.longjob.APISuspendLongJobEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.5.0"
		clz ErrorCode.class
	}
}
