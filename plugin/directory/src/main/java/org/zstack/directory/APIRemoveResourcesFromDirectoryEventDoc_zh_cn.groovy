package org.zstack.directory

import org.zstack.header.errorcode.ErrorCode

doc {

	title "资源从指定目录中移除返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "4.6.0"
	}
	ref {
		name "error"
		path "org.zstack.directory.APIRemoveResourcesFromDirectoryEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.6.0"
		clz ErrorCode.class
	}
}
