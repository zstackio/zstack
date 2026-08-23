package org.zstack.physicalserver

import org.zstack.header.errorcode.ErrorCode

doc {

	title "刷新物理服务器资源分配的返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "5.5.38"
	}
	ref {
		name "error"
		path "org.zstack.physicalserver.APIRefreshPhysicalServerResourceAssignmentsEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.5.38"
		clz ErrorCode.class
	}
}
