package org.zstack.header.allocator

import org.zstack.header.errorcode.ErrorCode

doc {

	title "物理机分配策略"

	ref {
		name "error"
		path "org.zstack.header.allocator.APIGetHostAllocatorStrategiesReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	field {
		name "strategies"
		desc "策略"
		type "List"
		since "0.6"
	}
}
