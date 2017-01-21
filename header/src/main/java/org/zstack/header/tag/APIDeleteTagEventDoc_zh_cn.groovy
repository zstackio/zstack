package org.zstack.header.tag

import org.zstack.header.errorcode.ErrorCode

doc {

	title "标签清单"

	ref {
		name "error"
		path "org.zstack.header.tag.APIDeleteTagEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
}
