package org.zstack.header.vm

import org.zstack.header.core.progress.ChainInfo
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取虚拟机上的任务信息"

	ref {
		name "results"
		path "org.zstack.header.vm.APIGetVmTaskReply.results"
		desc "null"
		type "Map"
		since "4.6.0"
		clz ChainInfo.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.6.0"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APIGetVmTaskReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.6.0"
		clz ErrorCode.class
	}
}
