package org.zstack.header.core.progress

import org.zstack.header.errorcode.ErrorCode

doc {

	title "在这里输入结构的名称"

	ref {
		name "error"
		path "org.zstack.header.core.progress.APIGetTaskProgressReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	field {
		name "progress"
		desc "任务的进度(0-100)"
		type "String"
		since "0.6"
	}
	field {
		name "resourceUuid"
		desc "任务资源的Uuid"
		type "String"
		since "0.6"
	}
	field {
		name "processType"
		desc "任务类型"
		type "String"
		since "0.6"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "0.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "0.6"
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.core.progress.APIGetTaskProgressReply.error"
		desc "null"
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
}
