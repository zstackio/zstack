package org.zstack.header.host

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.core.progress.ChainInfo
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取物理机上的任务信息"

	ref {
		name "error"
		path "org.zstack.header.host.APIGetHostTaskReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.6.0"
		clz ErrorCode.class
	}
	ref {
		name "results"
		path "org.zstack.header.host.APIGetHostTaskReply.results"
		desc "获取的结果"
		type "Map"
		since "3.6.0"
		clz ChainInfo.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "3.6.0"
	}
}
