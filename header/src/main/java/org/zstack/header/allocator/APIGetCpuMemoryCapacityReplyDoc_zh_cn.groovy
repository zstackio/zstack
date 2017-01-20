package org.zstack.header.allocator

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.ErrorCode

doc {

	title "cpu和内存容量"

	ref {
		name "error"
		path "org.zstack.header.allocator.APIGetCpuMemoryCapacityReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	field {
		name "totalCpu"
		desc "cpu总数"
		type "long"
		since "0.6"
	}
	field {
		name "availableCpu"
		desc "可用cpu数量"
		type "long"
		since "0.6"
	}
	field {
		name "totalMemory"
		desc "内存总量"
		type "long"
		since "0.6"
	}
	field {
		name "availableMemory"
		desc "可用内存"
		type "long"
		since "0.6"
	}
	field {
		name "success"
		desc "成功"
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.allocator.APIGetCpuMemoryCapacityReply.error"
		desc "null"
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
}
