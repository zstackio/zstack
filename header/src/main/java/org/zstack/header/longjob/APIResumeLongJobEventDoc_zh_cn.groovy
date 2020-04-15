package org.zstack.header.longjob

import org.zstack.header.errorcode.ErrorCode

doc {

	title "恢复运行长任务结果"

	ref {
		name "error"
		path "org.zstack.header.longjob.APIResumeLongJobEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.9.0"
		clz ErrorCode.class
	}
}
